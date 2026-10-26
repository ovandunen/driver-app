package ao.ridehailing.driver.feature.auth

import androidx.lifecycle.viewModelScope
import ao.ridehailing.driver.core.architecture.BaseViewModel
import ao.ridehailing.driver.core.network.NetworkResult
import ao.ridehailing.driver.core.storage.TokenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val tokenStore: TokenStore,
) : BaseViewModel<AuthState>(AuthState.Idle) {

    fun sendOtp(rawPhone: String) {
        if (currentState is AuthState.SendingOtp || currentState is AuthState.Verifying) return
        val phone = AngolanPhone.normalize(rawPhone)
        if (phone == null) {
            setState(AuthState.Error(AuthState.Error.Reason.InvalidPhone))
            return
        }
        setState(AuthState.SendingOtp)
        viewModelScope.launch {
            when (repository.sendOtp(phone)) {
                is NetworkResult.Success -> setState(AuthState.OtpSent(phone))
                is NetworkResult.Failure -> setState(
                    AuthState.Error(AuthState.Error.Reason.RequestFailed),
                )
            }
        }
    }

    fun verifyOtp(rawCode: String) {
        val phone = phoneAwaitingCode() ?: return
        if (currentState is AuthState.Verifying) return
        if (rawCode.length != 4 || rawCode.any { !it.isDigit() }) {
            setState(AuthState.Error(AuthState.Error.Reason.InvalidCode, phone))
            return
        }
        setState(AuthState.Verifying(phone))
        viewModelScope.launch {
            when (val result = repository.verifyOtp(phone, rawCode)) {
                is NetworkResult.Success -> {
                    val session = result.value
                    tokenStore.save(session.token, session.driverId, session.expiresAt)
                    setState(AuthState.Authenticated(session.driverId))
                }
                is NetworkResult.Failure -> setState(
                    AuthState.Error(AuthState.Error.Reason.RequestFailed, phone),
                )
            }
        }
    }

    fun editPhone() {
        if (currentState is AuthState.SendingOtp || currentState is AuthState.Verifying) return
        setState(AuthState.Idle)
    }

    private fun phoneAwaitingCode(): String? = when (val current = currentState) {
        is AuthState.OtpSent -> current.phone
        is AuthState.Error -> current.phone
        else -> null
    }
}
