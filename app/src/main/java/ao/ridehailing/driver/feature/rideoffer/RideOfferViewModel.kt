package ao.ridehailing.driver.feature.rideoffer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ao.ridehailing.driver.core.mqtt.MqttQos
import ao.ridehailing.driver.core.mqtt.OnlineSession
import ao.ridehailing.driver.core.mqtt.TopicMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
) : ViewModel() {
    private val states = MutableStateFlow<OfferUi>(OfferUi.Idle)
    val state: StateFlow<OfferUi> = states.asStateFlow()
    var consecutiveDeclines: Int = 0
        private set
    var ratingScore: Int = 0
        private set
    private val seenRides = HashSet<String>()
    private val seenTokens = HashSet<String>()

    fun onMessage(json: String) {
        val offer = parser.parse(json) ?: return
        if (!seenRides.add(offer.rideId) || states.value is OfferUi.Paused) return
        states.value = OfferUi.Showing(offer, 15)
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
        publish(TopicMapper.rideAccept(offer.rideId), RideCommands.accept(offer.rideId, offer.messageId))
        consecutiveDeclines = 0
        states.value = OfferUi.Accepted
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
        states.value = if (consecutiveDeclines >= 3) OfferUi.Paused(120) else OfferUi.Declined
    }

    private fun publish(topic: String, body: String) {
        viewModelScope.launch { session.publish(topic, body, MqttQos.AT_LEAST_ONCE) }
    }
}
