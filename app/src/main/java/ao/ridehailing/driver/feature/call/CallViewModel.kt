package ao.ridehailing.driver.feature.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ao.ridehailing.driver.core.mqtt.CallTopics
import ao.ridehailing.driver.core.mqtt.DriverTopics
import ao.ridehailing.driver.core.mqtt.MqttQos
import ao.ridehailing.driver.core.mqtt.OnlineSession
import ao.ridehailing.driver.feature.rideoffer.RideCommands
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CallStage { Idle, Initiating, Ringing, Connected, Ended, Missed, Failed }

interface SignallingClient {
    suspend fun open(url: String)
    fun close()
}

interface AudioCall {
    fun mute(on: Boolean)
    fun shareLocation(on: Boolean)
    fun stop()
    fun hear(json: String): Boolean = false
}

@JsonClass(generateAdapter = true)
data class CallResponseEnvelope(val type: String, val payload: CallResponsePayload)

@JsonClass(generateAdapter = true)
data class CallResponsePayload(val callId: String, val signallingUrl: String)

@HiltViewModel
class CallViewModel @Inject constructor(
    private val session: OnlineSession,
    private val signalling: SignallingClient,
    private val audio: AudioCall,
    private val moshi: Moshi,
) : ViewModel() {
    private val stages = MutableStateFlow(CallStage.Idle)
    val stage: StateFlow<CallStage> = stages.asStateFlow()
    var muted: Boolean = false
        private set
    var sharing: Boolean = false
        private set
    private var rideId: String = ""

    fun start(id: String, messageId: String) {
        rideId = id
        stages.value = CallStage.Initiating
        viewModelScope.launch {
            session.publish(DriverTopics.callRequest(id), RideCommands.callRequest(id, messageId), MqttQos.AT_LEAST_ONCE)
        }
    }

    fun onResponse(json: String) {
        if (json.contains("\"sdpType\"")) {
            if (audio.hear(json)) connected()
            return
        }
        val envelope = try {
            moshi.adapter(CallResponseEnvelope::class.java).fromJson(json)
        } catch (_: Exception) {
            null
        } ?: return fail()
        stages.value = CallStage.Ringing
        viewModelScope.launch { signalling.open(envelope.payload.signallingUrl) }
    }

    fun connected() {
        stages.value = CallStage.Connected
    }

    fun end(messageId: String) {
        audio.stop()
        signalling.close()
        stages.value = CallStage.Ended
        viewModelScope.launch {
            session.publish(CallTopics.callEnd(rideId), RideCommands.callEnd(messageId), MqttQos.AT_LEAST_ONCE)
        }
    }

    fun miss() {
        stages.value = CallStage.Missed
        signalling.close()
    }

    fun fail() {
        stages.value = CallStage.Failed
        signalling.close()
    }

    fun mute() {
        muted = !muted
        audio.mute(muted)
    }

    fun shareLocation() {
        sharing = !sharing
        audio.shareLocation(sharing)
    }
}
