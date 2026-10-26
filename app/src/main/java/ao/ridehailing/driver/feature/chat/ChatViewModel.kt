package ao.ridehailing.driver.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ao.ridehailing.driver.core.mqtt.MessageDeduper
import ao.ridehailing.driver.core.mqtt.MqttQos
import ao.ridehailing.driver.core.mqtt.OnlineSession
import ao.ridehailing.driver.core.mqtt.TopicMapper
import ao.ridehailing.driver.core.network.ChatUploadApi
import ao.ridehailing.driver.core.network.NetworkResult
import ao.ridehailing.driver.core.storage.ChatDao
import ao.ridehailing.driver.core.storage.ChatMessageEntity
import ao.ridehailing.driver.feature.rideoffer.RideCommands
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@JsonClass(generateAdapter = true)
data class ChatEnvelope(val type: String, val messageId: String = "", val payload: ChatPayload)

@JsonClass(generateAdapter = true)
data class ChatPayload(
    val messageId: String,
    val senderRole: String,
    val text: String,
    val kind: String = "text",
    val timestamp: String,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val session: OnlineSession,
    private val dao: ChatDao,
    private val uploads: ChatUploadApi,
    private val moshi: Moshi,
) : ViewModel() {
    private val lines = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val messages: StateFlow<List<ChatMessageEntity>> = lines.asStateFlow()
    private val deduper = MessageDeduper()
    var rideId: String = ""
        private set

    fun open(id: String) {
        rideId = id
        viewModelScope.launch {
            session.subscribe(TopicMapper.rideChat(id), MqttQos.AT_MOST_ONCE)
            lines.value = dao.list(id)
        }
    }

    fun onIncoming(json: String) {
        val envelope = moshi.adapter(ChatEnvelope::class.java).fromJson(json) ?: return
        val payload = envelope.payload
        if (!deduper.firstTime(payload.messageId)) return
        store(payload.messageId, payload.senderRole, payload.text, payload.kind, payload.timestamp)
    }

    fun sendText(text: String, messageId: String, now: String) {
        send(messageId, text, "text", now)
    }

    fun sendPredefined(text: String, messageId: String, now: String) {
        send(messageId, text, "predefined", now)
    }

    fun sendImage(bytes: ByteArray, messageId: String, now: String) {
        viewModelScope.launch {
            when (val uploaded = uploads.upload(bytes)) {
                is NetworkResult.Failure -> Unit
                is NetworkResult.Success -> send(messageId, uploaded.value, "image", now)
            }
        }
    }

    private fun send(messageId: String, text: String, kind: String, now: String) {
        if (!deduper.firstTime(messageId)) return
        store(messageId, "driver", text, kind, now)
        viewModelScope.launch {
            session.publish(TopicMapper.rideChat(rideId), RideCommands.chat(messageId, text, kind, now), MqttQos.AT_MOST_ONCE)
        }
    }

    private fun store(messageId: String, role: String, text: String, kind: String, now: String) {
        val entity = ChatMessageEntity(messageId, rideId, role, text, kind, now)
        lines.value = lines.value + entity
        viewModelScope.launch { dao.insert(entity) }
    }
}
