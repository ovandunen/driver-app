package ao.ridehailing.driver.core.webrtc

import ao.ridehailing.driver.feature.call.AudioCall
import ao.ridehailing.driver.feature.call.SignallingClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebRtcCall @Inject constructor() : AudioCall {
    override fun mute(on: Boolean) = Unit
    override fun shareLocation(on: Boolean) = Unit
    override fun stop() = Unit
}

@Singleton
class WebSocketSignalling @Inject constructor() : SignallingClient {
    var url: String = ""
        private set

    override suspend fun open(url: String) {
        this.url = url
    }

    override fun close() {
        url = ""
    }
}

class CallQualityMonitor {
    fun bitrate(packetLoss: Double, rttMs: Int): Int = if (packetLoss >= 0.05 || rttMs >= 400) 10_000 else 32_000
}
