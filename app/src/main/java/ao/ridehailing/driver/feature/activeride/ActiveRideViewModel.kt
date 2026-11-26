package ao.ridehailing.driver.feature.activeride

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ao.ridehailing.driver.core.mqtt.DriverTopics
import ao.ridehailing.driver.core.mqtt.MqttQos
import ao.ridehailing.driver.core.mqtt.OnlineSession
import ao.ridehailing.driver.core.mqtt.TopicMapper
import ao.ridehailing.driver.core.storage.RideEventDao
import ao.ridehailing.driver.core.storage.RideEventEntity
import ao.ridehailing.driver.feature.rideoffer.RideCommands
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RideStage {
    EnRouteToPickup,
    AtPickup,
    PassengerBoarded,
    EnRouteToDestination,
    AtDestination,
    PaymentPending,
}

@HiltViewModel
class ActiveRideViewModel @Inject constructor(
    private val session: OnlineSession,
    private val events: RideEventDao,
) : ViewModel() {
    private val stages = MutableStateFlow(RideStage.EnRouteToPickup)
    val stage: StateFlow<RideStage> = stages.asStateFlow()
    var rideId: String = ""
        private set
    var driverId: String = ""
        private set
    var message: String? = null
        private set

    fun restore(nextRideId: String, nextDriverId: String, now: String) {
        rideId = nextRideId
        driverId = nextDriverId
        viewModelScope.launch {
            val saved = events.latest(nextRideId)
            if (saved != null) stages.value = stageOf(saved.state)
            session.subscribe(TopicMapper.rideState(nextRideId), MqttQos.AT_LEAST_ONCE)
        }
    }

    fun advance(now: String) {
        val next = when (stages.value) {
            RideStage.EnRouteToPickup -> RideStage.AtPickup
            RideStage.AtPickup -> RideStage.PassengerBoarded
            RideStage.PassengerBoarded -> RideStage.EnRouteToDestination
            RideStage.EnRouteToDestination -> RideStage.AtDestination
            RideStage.AtDestination -> RideStage.PaymentPending
            RideStage.PaymentPending -> return
        }
        publish(next, now)
    }

    fun cancel(reason: String, text: String, now: String) {
        if (reason.isBlank()) {
            message = "reason"
            return
        }
        if (reason == "other" && text.isBlank()) {
            message = "text"
            return
        }
        if (text.length > 200) {
            message = "length"
            return
        }
        viewModelScope.launch {
            val wire = if (stages.value == RideStage.EnRouteToPickup) "en_route_to_pickup" else "at_pickup"
            val body = RideCommands.cancel(reason, text, now, "$rideId-$now", driverId, wire)
            session.publish(DriverTopics.rideCancel(rideId), body, MqttQos.AT_LEAST_ONCE)
            session.publish(TopicMapper.rideState(rideId), body, MqttQos.AT_LEAST_ONCE)
            message = null
        }
    }

    private fun publish(next: RideStage, now: String) {
        stages.value = next
        viewModelScope.launch {
            val wire = when (next) {
                RideStage.EnRouteToPickup -> "en_route_to_pickup"
                RideStage.AtPickup -> "at_pickup"
                RideStage.PassengerBoarded -> "passenger_boarded"
                RideStage.EnRouteToDestination -> "en_route_to_destination"
                RideStage.AtDestination -> "at_destination"
                RideStage.PaymentPending -> "payment_pending"
            }
            events.insert(RideEventEntity(rideId = rideId, state = wire, timestamp = now))
            session.publish(
                TopicMapper.rideState(rideId),
                RideCommands.state(wire, now, driverId, "$rideId-$wire", rideId),
                MqttQos.AT_LEAST_ONCE,
            )
        }
    }

    private fun stageOf(saved: String) = when (saved) {
        "at_pickup" -> RideStage.AtPickup
        "passenger_boarded" -> RideStage.PassengerBoarded
        "en_route_to_destination" -> RideStage.EnRouteToDestination
        "at_destination" -> RideStage.AtDestination
        "payment_pending" -> RideStage.PaymentPending
        else -> RideStage.EnRouteToPickup
    }
}
