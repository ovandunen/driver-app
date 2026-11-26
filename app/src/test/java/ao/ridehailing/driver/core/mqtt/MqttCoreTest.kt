package ao.ridehailing.driver.core.mqtt

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MqttExtrasTest {
    @Test
    fun acceptsDottedIpv4AndPort() {
        val endpoint = MqttExtras.parse("10.0.2.2", "1883")
        assertEquals("10.0.2.2", endpoint?.host)
        assertEquals(1883, endpoint?.port)
    }

    @Test
    fun rejectsHostnamesAndBadPorts() {
        assertEquals(null, MqttExtras.parse("broker.local", "1883"))
        assertEquals(null, MqttExtras.parse("10.0.2.2", "0"))
        assertEquals(null, MqttExtras.parse("10.0.2", "1883"))
    }
}

class TopicMapperTest {
    @Test
    fun taxonomy() {
        assertEquals("ride/R-1/offer", TopicMapper.rideOffer("R-1"))
        assertEquals("ride/R-1/accept", TopicMapper.rideAccept("R-1"))
        assertEquals("ride/R-1/decline", TopicMapper.rideDecline("R-1"))
        assertEquals("ride/R-1/state", TopicMapper.rideState("R-1"))
        assertEquals("ride/R-1/location", TopicMapper.rideLocation("R-1"))
        assertEquals("ride/R-1/chat", TopicMapper.rideChat("R-1"))
        assertEquals("ride/R-1/payment", TopicMapper.ridePayment("R-1"))
        assertEquals("driver/D-1/status", TopicMapper.driverStatus("D-1"))
        assertEquals("driver/D-1/demand-zones", DriverTopics.demandZones("D-1"))
        assertEquals("driver/D-1/ride-offers", DriverTopics.rideOffers("D-1"))
        assertEquals("driver/D-1/location", DriverTopics.driverLocation("D-1"))
        assertEquals("passenger/P-1/ride-status", DriverTopics.passengerRideStatus("P-1"))
        assertEquals("system/health", DriverTopics.systemHealth())
        assertEquals("ride/R-1/cancel", DriverTopics.rideCancel("R-1"))
        assertEquals("ride/R-1/call/request", DriverTopics.callRequest("R-1"))
        assertEquals("ride/R-1/call/response", DriverTopics.callResponse("R-1"))
        assertEquals("ride/R-1/call/end", CallTopics.callEnd("R-1"))
        assertEquals("ride/R-1/call/quality", CallTopics.callQuality("R-1"))
    }
}

class MqttUrlTest {
    @Test
    fun parsesSslUrl() {
        val url = "ssl://mqtt.example.ao:8883"
        assertEquals("mqtt.example.ao", MqttUrl.host(url))
        assertEquals(8883, MqttUrl.port(url))
        assertTrue(MqttUrl.tls(url))
    }
}

class ReconnectDelayTest {
    @Test
    fun backsOffThenCaps() {
        assertEquals(1_000L, ReconnectDelay.millis(0))
        assertEquals(2_000L, ReconnectDelay.millis(1))
        assertEquals(4_000L, ReconnectDelay.millis(2))
        assertEquals(8_000L, ReconnectDelay.millis(3))
        assertEquals(30_000L, ReconnectDelay.millis(5))
        assertEquals(30_000L, ReconnectDelay.millis(9))
    }
}

class MessageDeduperTest {
    @Test
    fun dropsDuplicates() {
        val deduper = MessageDeduper()
        assertTrue(deduper.firstTime("a"))
        assertFalse(deduper.firstTime("a"))
        assertTrue(deduper.firstTime("b"))
    }
}

class DriverStatusBodyTest {
    @Test
    fun statusBodies() {
        assertTrue(DriverStatusBody.offline().contains("\"status\":\"offline\""))
        assertTrue(DriverStatusBody.online().contains("\"status\":\"online\""))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MqttConnectionManagerTest {
    @Test
    fun connectSetsWillAndRetries() = runTest {
        val gateway = FakeMqttGateway()
        gateway.failTimes = 1
        val manager = MqttConnectionManager(gateway, MqttMessageBus(), this)
        val config = MqttConfig(
            "mqtt.example.ao",
            8883,
            true,
            "",
            "",
            "c1",
            "driver/D-1/status",
            DriverStatusBody.offline(),
        )
        manager.connect(config)
        assertEquals(1, gateway.connects)
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(2, gateway.connects)
        assertEquals("driver/D-1/status", gateway.willTopic)
    }

    @Test
    fun forwardsIncoming() = runTest {
        val gateway = FakeMqttGateway()
        val bus = MqttMessageBus()
        val manager = MqttConnectionManager(gateway, bus, backgroundScope)
        val seen = mutableListOf<String>()
        manager.listen()
        val job = launch { bus.stream.collect { seen.add(it.topic) } }
        runCurrent()
        gateway.emit(MqttMessage("ride/R-1/offer", "{}", 1))
        runCurrent()
        assertEquals(listOf("ride/R-1/offer"), seen)
        job.cancel()
    }
}

private class FakeMqttGateway : MqttGateway {
    var failTimes = 0
    var connects = 0
    var willTopic = ""
    private val incoming = MutableSharedFlow<MqttMessage>(extraBufferCapacity = 8)

    override suspend fun connect(config: MqttConfig) {
        willTopic = config.willTopic
        connects += 1
        if (connects <= failTimes) error("down")
    }

    override suspend fun disconnect() = Unit

    override suspend fun subscribe(topic: String, qos: Int) = Unit

    override suspend fun unsubscribe(topic: String) = Unit

    override suspend fun publish(topic: String, body: String, qos: Int) = Unit

    override fun incoming(): Flow<MqttMessage> = incoming

    override fun setWill(topic: String, body: String) {
        willTopic = topic
    }

    suspend fun emit(message: MqttMessage) {
        incoming.emit(message)
    }
}
