package ao.ridehailing.driver.core.mqtt

data class MqttMessage(
    val topic: String,
    val body: String,
    val qos: Int,
)

data class MqttConfig(
    val host: String,
    val port: Int,
    val tls: Boolean,
    val username: String,
    val password: String,
    val clientId: String,
    val willTopic: String,
    val willBody: String,
)
