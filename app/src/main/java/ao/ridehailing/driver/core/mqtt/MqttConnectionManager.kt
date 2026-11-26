package ao.ridehailing.driver.core.mqtt

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class MqttConnectionManager(
    private val gateway: MqttGateway,
    private val bus: MqttMessageBus,
    private val scope: CoroutineScope,
    private val brokerHost: () -> String = { "" },
    private val brokerPort: () -> Int = { 0 },
    private val username: String = "",
    private val password: String = "",
) : OnlineSession {
    private var config: MqttConfig? = null
    private var connectedDriver: String? = null
    private var attempt = 0

    val messages: SharedFlow<MqttMessage> = bus.stream

    fun listen() {
        scope.launch {
            gateway.incoming().collect { bus.emit(it) }
        }
    }

    suspend fun connect(next: MqttConfig) {
        config = next
        gateway.setWill(next.willTopic, next.willBody)
        open()
    }

    override suspend fun connect(driverId: String) {
        val next = brokerConfig(driverId)
        if (next.host.isBlank() || next.port !in 1..65535) throw IllegalStateException("mqtt")
        val sameBroker = config?.host == next.host && config?.port == next.port
        if (connectedDriver == driverId && sameBroker) return
        if (connectedDriver != null) gateway.disconnect()
        config = next
        gateway.setWill(next.willTopic, next.willBody)
        gateway.connect(next)
        connectedDriver = driverId
        attempt = 0
    }

    override suspend fun subscribe(topic: String, qos: Int) {
        gateway.subscribe(topic, qos)
    }

    override suspend fun unsubscribe(topic: String) {
        gateway.unsubscribe(topic)
    }

    override suspend fun publish(topic: String, body: String, qos: Int) {
        gateway.publish(topic, body, qos)
    }

    override suspend fun disconnectSession() {
        gateway.disconnect()
        connectedDriver = null
    }

    private fun brokerConfig(driverId: String): MqttConfig = MqttConfig(
        host = brokerHost(),
        port = brokerPort(),
        tls = false,
        username = username,
        password = password,
        clientId = "driver-$driverId",
        willTopic = TopicMapper.driverStatus(driverId),
        willBody = DriverStatusBody.offline(),
    )

    private suspend fun open() {
        val current = config ?: return
        try {
            gateway.connect(current)
            attempt = 0
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            retry()
        }
    }

    private fun retry() {
        val wait = ReconnectDelay.millis(attempt)
        attempt += 1
        scope.launch {
            delay(wait)
            open()
        }
    }
}
