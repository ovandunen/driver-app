package ao.ridehailing.driver.feature.auth

import ao.ridehailing.driver.core.network.ApiService
import ao.ridehailing.driver.core.network.NetworkResult
import ao.ridehailing.driver.core.network.SendOtpRequest
import ao.ridehailing.driver.core.network.VerifyOtpRequest
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: ApiService,
) : AuthRepository {
    override suspend fun sendOtp(phone: String): NetworkResult<Unit> = call {
        val response = api.sendOtp(SendOtpRequest(phone))
        if (!response.otpSent) error("otp not sent")
    }

    override suspend fun verifyOtp(phone: String, code: String): NetworkResult<VerifiedSession> = call {
        val response = api.verifyOtp(VerifyOtpRequest(phone, code))
        VerifiedSession(
            token = response.token,
            driverId = response.driverId,
            expiresAt = response.expiresAt,
        )
    }

    private suspend fun <T> call(block: suspend () -> T): NetworkResult<T> = try {
        NetworkResult.Success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        NetworkResult.Failure()
    }
}
