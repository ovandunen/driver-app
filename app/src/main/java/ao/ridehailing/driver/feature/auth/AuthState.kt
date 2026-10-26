package ao.ridehailing.driver.feature.auth

import ao.ridehailing.driver.core.architecture.ViewState

sealed interface AuthState : ViewState {
    data object Idle : AuthState
    data object SendingOtp : AuthState
    data class OtpSent(val phone: String) : AuthState
    data class Verifying(val phone: String) : AuthState
    data class Authenticated(val driverId: String) : AuthState
    data class Error(val reason: Reason, val phone: String? = null) : AuthState {
        enum class Reason { InvalidPhone, InvalidCode, RequestFailed }
    }
}
