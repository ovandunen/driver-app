package ao.ridehailing.driver.feature.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ao.ridehailing.driver.core.network.NetworkResult
import ao.ridehailing.driver.core.network.PayoutApi
import ao.ridehailing.driver.core.storage.PayoutDao
import ao.ridehailing.driver.core.storage.PayoutEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val api: PayoutApi,
    private val dao: PayoutDao,
) : ViewModel() {
    private val rows = MutableStateFlow<List<PayoutEntity>>(emptyList())
    val history: StateFlow<List<PayoutEntity>> = rows.asStateFlow()
    var notice: String? = null
        private set

    fun load() {
        viewModelScope.launch { rows.value = dao.all() }
    }

    fun instant(amountAoa: Long) {
        payout { api.instant(amountAoa) }
    }

    fun weekly(amountAoa: Long) {
        payout { api.weekly(amountAoa) }
    }

    private fun payout(call: suspend () -> NetworkResult<ao.ridehailing.driver.core.network.PayoutReceipt>) {
        viewModelScope.launch {
            when (val result = call()) {
                is NetworkResult.Failure -> notice = result.message
                is NetworkResult.Success -> {
                    val receipt = result.value
                    dao.save(PayoutEntity(receipt.id, receipt.kind, receipt.amountAoa, receipt.feeAoa))
                    notice = receipt.kind
                    rows.value = dao.all()
                }
            }
        }
    }
}
