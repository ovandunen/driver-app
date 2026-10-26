package ao.ridehailing.driver.core.mqtt

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MqttMessageBus {
    private val messages = MutableSharedFlow<MqttMessage>(extraBufferCapacity = 64)
    val stream: SharedFlow<MqttMessage> = messages.asSharedFlow()

    suspend fun emit(message: MqttMessage) {
        messages.emit(message)
    }
}
