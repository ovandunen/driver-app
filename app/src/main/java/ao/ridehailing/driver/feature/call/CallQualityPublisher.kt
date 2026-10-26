package ao.ridehailing.driver.feature.call

import ao.ridehailing.driver.core.mqtt.CallTopics
import ao.ridehailing.driver.core.mqtt.MqttQos
import ao.ridehailing.driver.core.mqtt.OnlineSession
import ao.ridehailing.driver.core.webrtc.CallQualityMonitor
import ao.ridehailing.driver.feature.rideoffer.RideCommands
import javax.inject.Inject

class CallQualityPublisher @Inject constructor(
    private val session: OnlineSession,
) {
    suspend fun publish(rideId: String, packetLoss: Double, rttMs: Int, messageId: String) {
        val bitrate = CallQualityMonitor().bitrate(packetLoss, rttMs)
        session.publish(CallTopics.callQuality(rideId), RideCommands.quality(messageId, bitrate), MqttQos.AT_MOST_ONCE)
    }
}
