package ao.ridehailing.driver.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ao.ridehailing.driver.core.network.NetworkResult
import ao.ridehailing.driver.core.network.ReferralApi
import ao.ridehailing.driver.core.storage.ReferralDao
import ao.ridehailing.driver.core.storage.ReferralEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReferralViewModel @Inject constructor(
    private val api: ReferralApi,
    private val dao: ReferralDao,
) : ViewModel() {
    private val rows = MutableStateFlow<List<ReferralEntity>>(emptyList())
    val referrals: StateFlow<List<ReferralEntity>> = rows.asStateFlow()
    var code: String = ""
        private set

    fun load() {
        viewModelScope.launch {
            when (val result = api.list()) {
                is NetworkResult.Failure -> Unit
                is NetworkResult.Success -> {
                    result.value.forEach { dao.save(ReferralEntity(it.code, it.name, it.rides)) }
                    rows.value = dao.all()
                }
            }
        }
    }

    fun generate() {
        viewModelScope.launch {
            when (val result = api.create()) {
                is NetworkResult.Failure -> Unit
                is NetworkResult.Success -> code = result.value
            }
        }
    }

    fun bonusDue(rides: Int): Boolean = rides >= 10
}
