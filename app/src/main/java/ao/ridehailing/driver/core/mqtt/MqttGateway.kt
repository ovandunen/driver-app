package ao.ridehailing.driver.core.mqtt

import kotlinx.coroutines.flow.Flow

interface MqttGateway {
    suspend fun connect(config: MqttConfig)
    suspend fun disconnect()
    suspend fun subscribe(topic: String, qos: Int)
    suspend fun unsubscribe(topic: String)
    suspend fun publish(topic: String, body: String, qos: Int)
    fun incoming(): Flow<MqttMessage>
    fun setWill(topic: String, body: String)
}
