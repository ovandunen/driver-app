package ao.ridehailing.driver.core.network

import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("auth/otp/send")
    suspend fun sendOtp(@Body body: SendOtpRequest): SendOtpResponse

    @POST("auth/otp/verify")
    suspend fun verifyOtp(@Body body: VerifyOtpRequest): VerifyOtpResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): RefreshResponse
}
