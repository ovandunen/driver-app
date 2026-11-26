package ao.ridehailing.driver.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ao.ridehailing.driver.core.location.LocationPulse
import ao.ridehailing.driver.core.location.LocationSource
import ao.ridehailing.driver.core.location.driverLocationBody
import ao.ridehailing.driver.core.mqtt.DriverStatusBody
import ao.ridehailing.driver.core.mqtt.DriverTopics
import ao.ridehailing.driver.core.mqtt.MqttQos
import ao.ridehailing.driver.core.mqtt.OnlineSession
import ao.ridehailing.driver.core.mqtt.TopicMapper
import ao.ridehailing.driver.core.network.DriverStatusApi
import ao.ridehailing.driver.core.network.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HomeState {
    data object Offline : HomeState
    data object GoingOnline : HomeState
    data object Online : HomeState
    data class Error(val message: String) : HomeState
}

interface PresenceControl {
    fun start()
    fun stop()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val api: DriverStatusApi,
    private val session: OnlineSession,
    private val pulse: LocationPulse,
    private val locations: LocationSource,
    private val presence: PresenceControl,
) : ViewModel() {
    private val states = MutableStateFlow<HomeState>(HomeState.Offline)
    val state: StateFlow<HomeState> = states.asStateFlow()

    fun goOnline(driverId: String) {
        if (states.value is HomeState.GoingOnline || states.value is HomeState.Online) return
        states.value = HomeState.GoingOnline
        viewModelScope.launch {
            try {
                when (val result = api.setStatus("online")) {
                    is NetworkResult.Failure -> states.value = HomeState.Error(result.message)
                    is NetworkResult.Success -> enterOnline(driverId)
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                states.value = HomeState.Error(error.message ?: "offline")
            }
        }
    }

    fun goOffline(driverId: String) {
        viewModelScope.launch {
            try {
                session.unsubscribe(DriverTopics.rideOffers(driverId))
                session.unsubscribe(DriverTopics.demandZones(driverId))
                session.publish(TopicMapper.driverStatus(driverId), DriverStatusBody.offline(), MqttQos.AT_LEAST_ONCE)
                pulse.stop()
                locations.stop()
                presence.stop()
                session.disconnectSession()
                api.setStatus("offline")
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
            }
            states.value = HomeState.Offline
        }
    }

    private suspend fun enterOnline(driverId: String) {
        session.connect(driverId)
        presence.start()
        session.subscribe(DriverTopics.rideOffers(driverId), MqttQos.AT_LEAST_ONCE)
        session.subscribe(DriverTopics.demandZones(driverId), MqttQos.AT_MOST_ONCE)
        session.publish(TopicMapper.driverStatus(driverId), DriverStatusBody.online(), MqttQos.AT_LEAST_ONCE)
        locations.start()
        pulse.start {
            val body = driverLocationBody(locations, gpsAllowed = true) ?: return@start
            session.publish(DriverTopics.driverLocation(driverId), body, MqttQos.AT_MOST_ONCE)
        }
        states.value = HomeState.Online
    }
}
