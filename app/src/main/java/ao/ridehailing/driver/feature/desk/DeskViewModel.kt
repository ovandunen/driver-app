package ao.ridehailing.driver.feature.desk

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ao.ridehailing.driver.BuildConfig
import ao.ridehailing.driver.core.location.LocationPulse
import ao.ridehailing.driver.core.location.LocationSource
import ao.ridehailing.driver.core.location.SOURCE_DEV
import ao.ridehailing.driver.core.location.DriverLocationBody
import ao.ridehailing.driver.core.location.driverLocationBody
import ao.ridehailing.driver.core.ui.DeskTab
import ao.ridehailing.driver.core.mqtt.DriverStatusBody
import ao.ridehailing.driver.core.mqtt.DriverTopics
import ao.ridehailing.driver.core.mqtt.MqttMessageBus
import ao.ridehailing.driver.core.mqtt.MqttQos
import ao.ridehailing.driver.core.mqtt.OnlineSession
import ao.ridehailing.driver.core.mqtt.TopicMapper
import ao.ridehailing.driver.feature.rideoffer.RideCommands
import ao.ridehailing.driver.feature.rideoffer.RideOffer
import ao.ridehailing.driver.phase1.Phase1Messages
import java.time.Instant
import ao.ridehailing.driver.core.network.DriverStatusApi
import ao.ridehailing.driver.core.network.NetworkResult
import ao.ridehailing.driver.core.storage.TokenStore
import ao.ridehailing.driver.feature.home.PresenceControl
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RideStep { Pickup, Waiting, Riding }

enum class PayMethod { Multicaixa, Cash, Nfc }

data class DeskState(
    val online: Boolean = false,
    val busy: Boolean = false,
    val linkDown: Boolean = false,
    val noLocation: Boolean = false,
    val sheet: Boolean = false,
    val networkSheet: Boolean = false,
    val pendingOffer: Boolean = false,
    val step: RideStep = RideStep.Pickup,
    val pay: PayMethod = PayMethod.Multicaixa,
    val sampleDone: Boolean = false,
    val audioOn: Boolean = false,
    val sosSent: Boolean = false,
    val reportReason: String = "Não apareceu",
    val reportSent: Boolean = false,
    val earningsPeriod: String = "day",
    val withdraw: Boolean = false,
    val declines: Int = 0,
    val tab: DeskTab = DeskTab.Home,
    val rideId: String = "",
    val pickupAddress: String = "",
    val destinationAddress: String = "",
    val pickupLat: Double = 0.0,
    val pickupLng: Double = 0.0,
    val destinationLat: Double = 0.0,
    val destinationLng: Double = 0.0,
    val fareAoa: Long = 0,
    val lat: Double = -8.8390,
    val lng: Double = 13.2894,
    val paid: Boolean = false,
    val incomingCall: Boolean = false,
    val passengerId: String = "",
)

@HiltViewModel
class DeskViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: DriverStatusApi,
    private val session: OnlineSession,
    private val pulse: LocationPulse,
    private val locations: LocationSource,
    private val presence: PresenceControl,
    private val tokens: TokenStore,
    private val bus: MqttMessageBus,
) : ViewModel() {
    private val quotedFares = mutableMapOf<String, Long>()

    init {
        viewModelScope.launch {
            bus.stream.collect { message ->
                val quoted = quotedFare(message.body)
                if (quoted != null) {
                    quotedFares[quoted.first] = quoted.second
                    if (quoted.first == states.value.rideId) {
                        states.update { it.copy(fareAoa = quoted.second) }
                    }
                }
                val ride = states.value.rideId
                if (ride.isBlank() || !message.topic.contains(ride)) return@collect
                if (message.body.contains("\"status\":\"CONFIRMED\"") || message.body.contains("\"state\":\"completed\"")) {
                    states.update { it.copy(paid = true) }
                }
                if (message.body.contains("\"type\":\"call.incoming\"") && message.body.contains("\"from\":\"passenger\"")
                    && !message.body.contains("phone") && !message.body.contains("proxyNumber") && !message.body.contains("+244")
                ) {
                    states.update { it.copy(incomingCall = true) }
                }
            }
        }
    }
    private val states = MutableStateFlow(DeskState())
    val state: StateFlow<DeskState> = states.asStateFlow()
    private val driverId: String = tokens.driverId() ?: BuildConfig.DEV_DRIVER_ID

    fun goOnline() {
        val current = states.value
        if (current.online || current.busy) return
        states.update { it.copy(online = true, busy = true, linkDown = false, pendingOffer = false, networkSheet = false) }
        viewModelScope.launch {
            val linked = tryConnect()
            states.update { it.copy(busy = false, linkDown = !linked, noLocation = missingGps()) }
        }
    }

    fun goOffline() {
        states.update { it.copy(online = false, busy = false, linkDown = false, pendingOffer = false, networkSheet = false) }
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
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
            }
        }
    }

    fun showOnline() {
        states.update { it.copy(online = true, sheet = false, networkSheet = false) }
    }

    fun showOffline() {
        states.update { it.copy(online = false, sheet = false, networkSheet = false, linkDown = false) }
    }

    fun openSheet() = states.update { it.copy(sheet = true, networkSheet = false) }
    fun closeSheet() = states.update { it.copy(sheet = false) }
    fun showNetwork() = states.update { it.copy(networkSheet = true, online = true, linkDown = true, sheet = false) }
    fun dismissNetwork() = states.update { it.copy(networkSheet = false) }

    fun retryLink() {
        states.update { it.copy(networkSheet = false, busy = true, online = true) }
        viewModelScope.launch {
            val linked = tryConnect()
            states.update { it.copy(busy = false, linkDown = !linked, noLocation = missingGps()) }
        }
    }

    fun selectTab(tab: DeskTab) = states.update { it.copy(tab = tab) }

    fun needsGps() = locations.kind != SOURCE_DEV

    fun denyLocation() = states.update { it.copy(noLocation = true) }

    fun noteOffer() = states.update { it.copy(pendingOffer = true) }
    fun consumeOffer() = states.update { it.copy(pendingOffer = false) }
    fun dismissCall() = states.update { it.copy(incomingCall = false) }

    fun saveHome(lat: Double, lng: Double) {
        viewModelScope.launch {
            try {
                session.publish(
                    TopicMapper.driverStatus(driverId),
                    Phase1Messages.homeBase(lat, lng, "$driverId-home"),
                    MqttQos.AT_LEAST_ONCE,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
            }
        }
    }

    fun saveHomeFromGps() {
        viewModelScope.launch {
            val fix = locations.read() ?: return@launch
            saveHome(fix.lat, fix.lng)
        }
    }
    fun noteDecline() = states.update { it.copy(declines = it.declines + 1, pendingOffer = false) }

    fun beginRide() = states.update { it.copy(step = RideStep.Pickup, pay = PayMethod.Multicaixa, sosSent = false) }

    fun openTrip(offer: RideOffer) {
        states.update {
            it.copy(
                step = RideStep.Pickup,
                rideId = offer.rideId,
                pickupAddress = offer.pickupAddress,
                destinationAddress = offer.destinationAddress,
                pickupLat = offer.pickupLat,
                pickupLng = offer.pickupLng,
                destinationLat = offer.destinationLat,
                destinationLng = offer.destinationLng,
                fareAoa = quotedFares[offer.rideId] ?: offer.payoutAoa,
                passengerId = offer.passengerId,
                paid = false,
                sosSent = false,
            )
        }
        val here = states.value
        val name = if (BuildConfig.DEV_BYPASS_LOGIN) BuildConfig.DEV_DRIVER_NAME else driverId
        val make = if (BuildConfig.DEV_BYPASS_LOGIN) BuildConfig.DEV_VEHICLE_MAKE else ""
        val plate = if (BuildConfig.DEV_BYPASS_LOGIN) BuildConfig.DEV_DRIVER_PLATE else ""
        val accepted = RideCommands.accepted(
            offer.rideId,
            Instant.now().toString(),
            driverId,
            name,
            make,
            plate,
            offer.pickupAddress,
            offer.destinationAddress,
            offer.pickupLat,
            offer.pickupLng,
            offer.destinationLat,
            offer.destinationLng,
            here.lat,
            here.lng,
            here.fareAoa,
            "${offer.rideId}-accepted",
        )
        viewModelScope.launch {
            try {
                session.publish(TopicMapper.rideState(offer.rideId), accepted, MqttQos.AT_LEAST_ONCE)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
            }
            try {
                session.subscribe(TopicMapper.rideState(offer.rideId), MqttQos.AT_LEAST_ONCE)
                session.subscribe(TopicMapper.ridePayment(offer.rideId), MqttQos.AT_LEAST_ONCE)
                session.subscribe(DriverTopics.callResponse(offer.rideId), MqttQos.AT_LEAST_ONCE)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
            }
        }
    }

    fun arrivePickup() {
        states.update { it.copy(step = RideStep.Waiting) }
        publishState("at_pickup")
    }

    fun startWithPassenger() {
        states.update { it.copy(step = RideStep.Riding) }
        publishState("passenger_boarded", "en_route_to_destination")
    }

    fun arriveDestination() {
        publishState("at_destination", "payment_pending")
    }

    fun settle(method: String) {
        val current = states.value
        if (current.rideId.isBlank()) return
        viewModelScope.launch {
            try {
                val fare = java.math.BigDecimal.valueOf(current.fareAoa).setScale(2).toPlainString()
                val body = if (method == "CASH") {
                    Phase1Messages.cashReceived(current.rideId, driverId, current.passengerId, fare, "${current.rideId}-cash")
                } else {
                    """{"type":"ride.payment","messageId":"${current.rideId}-$method","payload":{"rideId":"${current.rideId}","method":"$method","amountAoa":${current.fareAoa}}}"""
                }
                session.publish(TopicMapper.ridePayment(current.rideId), body, MqttQos.AT_LEAST_ONCE)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
            }
        }
    }
    private fun quotedFare(body: String): Pair<String, Long>? {
        val rideId = jsonValue(body, "rideId") ?: return null
        val fare = jsonValue(body, "fareAoa")?.toLongOrNull() ?: return null
        if (rideId.isBlank() || fare <= 0) return null
        return rideId to fare
    }

    private fun jsonValue(json: String, name: String): String? {
        val key = "\"$name\""
        val start = json.indexOf(key)
        if (start < 0) return null
        var index = json.indexOf(':', start + key.length)
        if (index < 0) return null
        index += 1
        while (index < json.length && json[index].isWhitespace()) index += 1
        if (index >= json.length) return null
        if (json[index] == '"') {
            val end = json.indexOf('"', index + 1)
            if (end < 0) return null
            return json.substring(index + 1, end)
        }
        val end = json.indexOfAny(charArrayOf(',', '}'), index)
        if (end < 0) return null
        return json.substring(index, end).trim()
    }

    fun step(next: RideStep) = states.update { it.copy(step = next) }
    fun choosePay(method: PayMethod) = states.update { it.copy(pay = method) }
    fun toggleAudio() = states.update { it.copy(audioOn = !it.audioOn) }
    fun markSos() = states.update { it.copy(sosSent = true) }
    fun selectReason(reason: String) = states.update { it.copy(reportReason = reason, reportSent = false) }
    fun sendReport() = states.update { it.copy(reportSent = true) }
    fun selectPeriod(period: String) = states.update { it.copy(earningsPeriod = period) }
    fun toggleWithdraw() = states.update { it.copy(withdraw = !it.withdraw) }

    fun conclude() = states.update { it.copy(sampleDone = true, pendingOffer = false) }

    fun reactivate() {
        tokens.save("device-token", driverId, "2099-01-01T00:00:00Z")
    }

    fun sendPhrase(text: String) {
        viewModelScope.launch {
            try {
                session.publish(
                    TopicMapper.rideChat("R-MARIA"),
                    """{"text":"$text"}""",
                    MqttQos.AT_MOST_ONCE,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun tryConnect(): Boolean {
        return try {
            when (val result = api.setStatus("online")) {
                is NetworkResult.Failure -> false
                is NetworkResult.Success -> {
                    session.connect(driverId)
                    if (locationGranted()) {
                        try {
                            presence.start()
                        } catch (_: Exception) {
                        }
                    }
                    session.subscribe(DriverTopics.rideOffers(driverId), MqttQos.AT_LEAST_ONCE)
                    session.subscribe(DriverTopics.cityRideRequests(), MqttQos.AT_LEAST_ONCE)
                    session.subscribe("passenger/+/ride-request", MqttQos.AT_LEAST_ONCE)
                    session.subscribe(DriverTopics.demandZones(driverId), MqttQos.AT_MOST_ONCE)
                    session.subscribe(Phase1Messages.account(driverId), MqttQos.AT_LEAST_ONCE)
                    session.publish(TopicMapper.driverStatus(driverId), DriverStatusBody.online(), MqttQos.AT_LEAST_ONCE)
                    locations.start()
                    pulse.start {
                        val fix = locations.read()
                        if (fix == null || (locations.kind != SOURCE_DEV && !locationGranted())) return@start
                        session.publish(DriverTopics.driverLocation(driverId), DriverLocationBody.of(fix), MqttQos.AT_MOST_ONCE)
                        session.publish(DriverTopics.cityDrivers(), DriverLocationBody.city(driverId, fix), MqttQos.AT_MOST_ONCE)
                        states.update { it.copy(lat = fix.lat, lng = fix.lng) }
                    }
                    true
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            false
        }
    }

    private fun publishState(vararg wires: String) {
        val current = states.value
        if (current.rideId.isBlank()) return
        viewModelScope.launch {
            for (wire in wires) {
                try {
                    val now = Instant.now().toString()
                    val body = RideCommands.state(wire, now, driverId, "${current.rideId}-$wire", current.rideId)
                    session.publish(TopicMapper.rideState(current.rideId), body, MqttQos.AT_LEAST_ONCE)
                    if (current.passengerId.isNotBlank()) {
                        session.publish(DriverTopics.passengerRideStatus(current.passengerId), body, MqttQos.AT_LEAST_ONCE)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun missingGps(): Boolean = locations.kind != SOURCE_DEV && !locationGranted()

    private fun locationGranted(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
}
