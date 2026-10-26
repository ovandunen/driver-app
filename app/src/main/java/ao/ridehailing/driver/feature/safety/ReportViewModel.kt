package ao.ridehailing.driver.feature.safety

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ao.ridehailing.driver.core.network.NetworkResult
import ao.ridehailing.driver.core.network.ReportApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val api: ReportApi,
) : ViewModel() {
    private val done = MutableStateFlow(false)
    val submitted: StateFlow<Boolean> = done.asStateFlow()
    var message: String? = null
        private set
    private val blocked = HashSet<String>()

    fun submit(passengerId: String, rideId: String, reason: String, description: String) {
        if (reason !in REASONS) {
            message = "reason"
            return
        }
        if (description.length > 500) {
            message = "length"
            return
        }
        viewModelScope.launch {
            when (val result = api.report(passengerId, rideId, reason, description)) {
                is NetworkResult.Failure -> message = result.message
                is NetworkResult.Success -> {
                    blocked.add(passengerId)
                    message = null
                    done.value = true
                }
            }
        }
    }

    fun isBlocked(passengerId: String): Boolean = passengerId in blocked

    private companion object {
        val REASONS = setOf("abusive_behavior", "no_show", "fraud", "safety_concern", "other")
    }
}
