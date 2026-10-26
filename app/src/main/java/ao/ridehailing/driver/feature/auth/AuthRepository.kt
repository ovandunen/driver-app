package ao.ridehailing.driver.feature.auth

import ao.ridehailing.driver.core.network.NetworkResult

data class VerifiedSession(
    val token: String,
    val driverId: String,
    val expiresAt: String,
)

interface AuthRepository {
    suspend fun sendOtp(phone: String): NetworkResult<Unit>
    suspend fun verifyOtp(phone: String, code: String): NetworkResult<VerifiedSession>
}
