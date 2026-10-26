package ao.ridehailing.driver.feature.earnings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ao.ridehailing.driver.core.network.EarningsApi
import ao.ridehailing.driver.core.network.NetworkResult
import ao.ridehailing.driver.core.network.TripEarning
import ao.ridehailing.driver.core.storage.EarningsDao
import ao.ridehailing.driver.core.storage.TripEarningEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class EarningsStats(
    val trips: Int,
    val minutes: Int,
    val totalAoa: Long,
    val commissionAoa: Long,
    val perTrip: Long,
    val perHour: Long,
)

object EarningsMath {
    fun stats(trips: List<TripEarning>, period: String, today: LocalDate): EarningsStats {
        val selected = trips.filter { trip ->
            val day = LocalDate.parse(trip.day)
            when (period) {
                "week" -> !day.isBefore(today.minusDays(6))
                "month" -> !day.isBefore(today.minusDays(30))
                else -> day == today
            }
        }
        val total = selected.sumOf { it.fareAoa }
        val minutes = selected.sumOf { it.minutes }
        val count = selected.size
        val perTrip = if (count == 0) 0 else total / count
        val perHour = if (minutes == 0) 0 else total * 60 / minutes
        return EarningsStats(count, minutes, total, selected.sumOf { it.commissionAoa }, perTrip, perHour)
    }

    fun rate(cancelled: Int, offered: Int): Int = if (offered == 0) 0 else cancelled * 100 / offered
}

@HiltViewModel
class EarningsViewModel @Inject constructor(
    private val api: EarningsApi,
    private val dao: EarningsDao,
) : ViewModel() {
    private val stats = MutableStateFlow(EarningsStats(0, 0, 0, 0, 0, 0))
    val state: StateFlow<EarningsStats> = stats.asStateFlow()
    var period: String = "day"
        private set
    var selectedRide: String? = null
        private set
    var cancellationRate: Int = 0
        private set

    fun load(today: LocalDate) {
        viewModelScope.launch {
            when (val result = api.earnings(period)) {
                is NetworkResult.Success -> {
                    result.value.forEach { trip ->
                        dao.save(TripEarningEntity(trip.rideId, trip.day, trip.fareAoa, trip.commissionAoa, trip.minutes))
                    }
                }
                is NetworkResult.Failure -> Unit
            }
            val stored = dao.all().map { TripEarning(it.rideId, it.day, it.fareAoa, it.commissionAoa, it.minutes) }
            stats.value = EarningsMath.stats(stored, period, today)
        }
    }

    fun selectPeriod(next: String, today: LocalDate) {
        period = next
        load(today)
    }

    fun selectRide(rideId: String) {
        selectedRide = rideId
    }

    fun recordCancellations(cancelled: Int, offered: Int) {
        cancellationRate = EarningsMath.rate(cancelled, offered)
    }
}
