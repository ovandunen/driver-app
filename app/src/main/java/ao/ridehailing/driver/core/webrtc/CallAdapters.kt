package ao.ridehailing.driver.core.webrtc

import ao.ridehailing.driver.core.mqtt.MqttQos
import ao.ridehailing.driver.core.mqtt.OnlineSession
import ao.ridehailing.driver.feature.call.AudioCall
import ao.ridehailing.driver.feature.call.SignallingClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebRtcCall @Inject constructor(
    private val voice: VoicePath,
    private val socket: VoiceSocket,
) : AudioCall {
    override fun mute(on: Boolean) = Unit
    override fun shareLocation(on: Boolean) = Unit

    override fun stop() {
        voice.stop()
        socket.stop()
    }

    override fun hear(json: String): Boolean {
        val accepted = voice.remote(json)
        val type = quoted(json, "sdpType")
        val sdp = quoted(json, "sdp")
        if (sdp.isNotBlank()) socket.accept(sdp, type)
        return accepted && voice.live()
    }

    private fun quoted(json: String, key: String): String {
        val token = "\"$key\":\""
        val start = json.indexOf(token)
        if (start < 0) return ""
        val from = start + token.length
        val end = json.indexOf('"', from)
        return if (end < 0) "" else json.substring(from, end)
    }
}

@Singleton
class WebSocketSignalling @Inject constructor(
    private val session: OnlineSession,
    private val voice: VoicePath,
    private val socket: VoiceSocket,
) : SignallingClient {
    var url: String = ""
        private set

    override suspend fun open(url: String) {
        this.url = url
        if (!url.startsWith("ride/")) return
        val callId = url.removePrefix("ride/").substringBefore("/")
        session.publish(url, voice.localOffer(callId, "driver", socket.offer(callId)), MqttQos.AT_LEAST_ONCE)
    }

    override fun close() {
        url = ""
        voice.stop()
        socket.stop()
    }
}

class CallQualityMonitor {
    fun bitrate(packetLoss: Double, rttMs: Int): Int = if (packetLoss >= 0.05 || rttMs >= 400) 10_000 else 32_000
}
