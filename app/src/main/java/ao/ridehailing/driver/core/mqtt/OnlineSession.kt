package ao.ridehailing.driver.core.mqtt

interface OnlineSession {
    suspend fun subscribe(topic: String, qos: Int)
    suspend fun unsubscribe(topic: String)
    suspend fun publish(topic: String, body: String, qos: Int)
    suspend fun disconnectSession()
}
