package ao.ridehailing.driver.feature.rideoffer

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ao.ridehailing.driver.core.mqtt.DriverTopics
import ao.ridehailing.driver.core.mqtt.MqttMessageBus
import ao.ridehailing.driver.core.mqtt.MqttQos
import ao.ridehailing.driver.core.mqtt.OnlineSession
import ao.ridehailing.driver.core.mqtt.TopicMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

sealed interface OfferUi {
    data object Idle : OfferUi
    data class Showing(val offer: RideOffer, val seconds: Int) : OfferUi
    data class Paused(val seconds: Int) : OfferUi
    data object Accepted : OfferUi
    data object Declined : OfferUi
}

@HiltViewModel
class RideOfferViewModel @Inject constructor(
    private val session: OnlineSession,
    private val parser: OfferParser,
    private val bus: MqttMessageBus,
) : ViewModel() {
    private val states = MutableStateFlow<OfferUi>(OfferUi.Idle)
    val state: StateFlow<OfferUi> = states.asStateFlow()
    var consecutiveDeclines: Int = 0
        private set
    var ratingScore: Int = 0
        private set
    var lastOffer: RideOffer? = null
        private set
    private val seenRides = HashSet<String>()
    private val seenTokens = HashSet<String>()
    private var watching = false
    private var online: () -> Boolean = { true }
    private var nowMillis: () -> Long = { System.currentTimeMillis() }

    fun useClock(now: () -> Long) {
        nowMillis = now
    }

    fun watch(isOnline: () -> Boolean) {
        online = isOnline
        if (watching) return
        watching = true
        viewModelScope.launch {
            bus.stream.collect { message ->
                if (!message.topic.endsWith("/ride-offers") && !message.topic.contains("ride-request")) return@collect
                onMessage(message.body, message.topic)
            }
        }
    }

    fun showPreview() {
        if (states.value !is OfferUi.Idle) return
        states.value = OfferUi.Showing(
            RideOffer(
                rideId = "R-1",
                messageId = "preview",
                pickupAddress = "Maianga",
                destinationAddress = "Talatona",
                distanceKm = 8.2,
                durationMin = 18,
                payoutAoa = 2_500,
                passengerRating = 4.8,
            ),
            15,
        )
    }

    fun onMessage(json: String, topic: String = "") {
        val rideId = rideIdOf(json)
        if (!online()) {
            logLine(topic, rideId, "dropped", "offline")
            return
        }
        val offer = runCatching { parser.parse(json) }.getOrNull()
        if (offer == null) {
            val kind = typeOf(json)
            val reason = if (kind == "ride.offer" || kind == "ride.request") "parse" else "type"
            logLine(topic, rideId, "dropped", reason)
            return
        }
        val seconds = remainingSeconds(offer.expiresAt)
        if (seconds == null) {
            logLine(topic, offer.rideId, "dropped", "expires")
            return
        }
        if (seconds <= 0) {
            logLine(topic, offer.rideId, "dropped", "expired")
            return
        }
        val fresh = seenRides.add(offer.rideId)
        val paused = states.value is OfferUi.Paused
        if (!fresh || paused) {
            logLine(topic, offer.rideId, "dropped", if (!fresh) "duplicate" else "paused")
            return
        }
        states.value = OfferUi.Showing(offer, seconds)
        logLine(topic, offer.rideId, "accepted", "")
    }

    fun tick() {
        when (val current = states.value) {
            is OfferUi.Showing -> if (current.seconds <= 1) decline(current.offer) else {
                states.value = current.copy(seconds = current.seconds - 1)
            }
            is OfferUi.Paused -> if (current.seconds <= 1) {
                consecutiveDeclines = 0
                states.value = OfferUi.Idle
            } else {
                states.value = current.copy(seconds = current.seconds - 1)
            }
            else -> Unit
        }
    }

    fun accept() {
        val offer = (states.value as? OfferUi.Showing)?.offer ?: return
        lastOffer = offer
        consecutiveDeclines = 0
        states.value = OfferUi.Accepted
        viewModelScope.launch {
            try {
                session.subscribe(TopicMapper.rideState(offer.rideId), MqttQos.AT_LEAST_ONCE)
                session.subscribe(TopicMapper.ridePayment(offer.rideId), MqttQos.AT_LEAST_ONCE)
                session.subscribe(DriverTopics.callResponse(offer.rideId), MqttQos.AT_LEAST_ONCE)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
            }
            publish(TopicMapper.rideAccept(offer.rideId), RideCommands.accept(offer.rideId, offer.messageId))
        }
    }

    fun decline() {
        val offer = (states.value as? OfferUi.Showing)?.offer ?: return
        decline(offer)
    }

    fun resume() {
        if (states.value is OfferUi.Paused) states.value = OfferUi.Idle
    }

    fun onRating(json: String) {
        val effect = parser.ratingImpact(json) ?: return
        val token = effect.token.ifEmpty { json }
        if (!seenTokens.add(token)) return
        ratingScore += effect.impact
    }

    private fun decline(offer: RideOffer) {
        publish(TopicMapper.rideDecline(offer.rideId), RideCommands.decline(offer.rideId, offer.messageId))
        consecutiveDeclines += 1
        states.value = OfferUi.Declined
    }

    private fun publish(topic: String, body: String) {
        viewModelScope.launch {
            try {
                session.publish(topic, body, MqttQos.AT_LEAST_ONCE)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
            }
        }
    }

    private fun remainingSeconds(expiresAt: String): Int? {
        val deadline = runCatching { Instant.parse(expiresAt).toEpochMilli() }.getOrNull() ?: return null
        val left = deadline - nowMillis()
        if (left <= 0) return 0
        return ((left + 999) / 1000).toInt()
    }

    private fun logLine(topic: String, rideId: String, result: String, reason: String) {
        val suffix = if (reason.isEmpty()) "" else " reason=$reason"
        val line = "topic=${topic.ifBlank { "-" }} rideId=$rideId $result$suffix"
        try {
            Log.i("driver-mqtt", line)
        } catch (_: Throwable) {
        }
    }
}

private fun typeOf(json: String) = Regex(""""type"\s*:\s*"([^"]*)"""").find(json)?.groupValues?.get(1)

private fun rideIdOf(json: String) = Regex(""""rideId"\s*:\s*"([^"]*)"""").find(json)?.groupValues?.get(1) ?: "-"
