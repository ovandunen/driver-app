package ao.ridehailing.driver.core.mqtt

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class MqttConnectionManager(
    private val gateway: MqttGateway,
    private val bus: MqttMessageBus,
    private val scope: CoroutineScope,
) : OnlineSession {
    private var config: MqttConfig? = null
    private var attempt = 0

    val messages: SharedFlow<MqttMessage> = bus.stream

    fun listen() {
        scope.launch {
            gateway.incoming().collect { bus.emit(it) }
        }
    }

    suspend fun connect(next: MqttConfig) {
        config = next
        gateway.setWill(next.willTopic, next.willBody)
        open()
    }

    override suspend fun subscribe(topic: String, qos: Int) {
        gateway.subscribe(topic, qos)
    }

    override suspend fun unsubscribe(topic: String) {
        gateway.unsubscribe(topic)
    }

    override suspend fun publish(topic: String, body: String, qos: Int) {
        gateway.publish(topic, body, qos)
    }

    override suspend fun disconnectSession() {
        gateway.disconnect()
    }

    private suspend fun open() {
        val current = config ?: return
        try {
            gateway.connect(current)
            attempt = 0
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            retry()
        }
    }

    private fun retry() {
        val wait = ReconnectDelay.millis(attempt)
        attempt += 1
        scope.launch {
            delay(wait)
            open()
        }
    }
}
