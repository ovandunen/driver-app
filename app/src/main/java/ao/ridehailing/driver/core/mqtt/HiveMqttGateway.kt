package ao.ridehailing.driver.core.mqtt

import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.concurrent.TimeUnit

class HiveMqttGateway : MqttGateway {
    private val incoming = MutableSharedFlow<MqttMessage>(extraBufferCapacity = 64)
    private var client: Mqtt5AsyncClient? = null
    private var willTopic = ""
    private var willBody = ""

    override suspend fun connect(config: MqttConfig) {
        val built = MqttClient.builder()
            .useMqttVersion5()
            .identifier(config.clientId)
            .serverHost(config.host)
            .serverPort(config.port)
        if (config.tls) built.sslWithDefaultConfig()
        val next = built.buildAsync()
        var connect = next.connectWith().cleanStart(true)
        if (config.username.isNotEmpty()) {
            connect = connect.simpleAuth()
                .username(config.username)
                .password(config.password.toByteArray(Charsets.UTF_8))
                .applySimpleAuth()
        }
        val topic = config.willTopic.ifEmpty { willTopic }
        val body = config.willBody.ifEmpty { willBody }
        if (topic.isNotEmpty()) {
            connect = connect.willPublish()
                .topic(topic)
                .payload(body.toByteArray(Charsets.UTF_8))
                .qos(MqttQos.AT_LEAST_ONCE)
                .applyWillPublish()
        }
        connect.send().get(15, TimeUnit.SECONDS)
        client = next
    }

    override suspend fun disconnect() {
        client?.disconnect()?.get(15, TimeUnit.SECONDS)
        client = null
    }

    override suspend fun subscribe(topic: String, qos: Int) {
        client().subscribeWith()
            .topicFilter(topic)
            .qos(qosOf(qos))
            .callback { publish ->
                val text = String(publish.payloadAsBytes, Charsets.UTF_8)
                incoming.tryEmit(MqttMessage(publish.topic.toString(), text, qos))
            }
            .send()
            .get(15, TimeUnit.SECONDS)
    }

    override suspend fun unsubscribe(topic: String) {
        client().unsubscribeWith().topicFilter(topic).send().get(15, TimeUnit.SECONDS)
    }

    override suspend fun publish(topic: String, body: String, qos: Int) {
        client().publishWith()
            .topic(topic)
            .payload(body.toByteArray(Charsets.UTF_8))
            .qos(qosOf(qos))
            .send()
            .get(15, TimeUnit.SECONDS)
    }

    override fun incoming(): Flow<MqttMessage> = incoming

    override fun setWill(topic: String, body: String) {
        willTopic = topic
        willBody = body
    }

    private fun client(): Mqtt5AsyncClient = client ?: error("mqtt not connected")
}

private fun qosOf(qos: Int): MqttQos = if (qos == 0) MqttQos.AT_MOST_ONCE else MqttQos.AT_LEAST_ONCE
