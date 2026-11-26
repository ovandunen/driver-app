package ao.ridehailing.driver.core.webrtc

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoicePath @Inject constructor() {
    private var offer = false
    private var answer = false

    fun localOffer(callId: String, from: String, sdp: String): String {
        offer = true
        val description = sdp.ifBlank { "v=0" }
        return """{"type":"call.signal","messageId":"$callId","payload":{"callId":"$callId","from":"$from","sdpType":"offer","sdp":"$description"}}"""
    }

    fun remote(json: String): Boolean {
        val lower = json.lowercase()
        if (lower.contains("phone") || lower.contains("proxynumber") || lower.contains("+244")) return false
        if (json.contains("\"answer\"")) answer = true
        if (json.contains("\"offer\"")) offer = true
        return true
    }

    fun live(): Boolean = offer && answer

    fun stop() {
        offer = false
        answer = false
    }
}
