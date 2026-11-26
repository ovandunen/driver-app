package ao.ridehailing.driver.phase1

import ao.ridehailing.driver.core.webrtc.VoicePath
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VoiceConnectionTest {
    @Test
    fun anOfferAndAnswerOpenTheVoicePathAndAPhoneIsRefused() {
        val voice = VoicePath()
        val offer = voice.localOffer("R-1", "driver", "v=0")
        assertTrue(offer.contains("\"sdpType\":\"offer\""))
        assertFalse(offer.contains("phone"))
        assertFalse(voice.live())
        assertFalse(voice.remote("""{"sdpType":"answer","phone":"+244923000111","sdp":"v=0"}"""))
        assertFalse(voice.live())
        assertTrue(voice.remote("""{"sdpType":"answer","sdp":"v=0"}"""))
        assertTrue(voice.live())
        voice.stop()
        assertFalse(voice.live())
    }
}
