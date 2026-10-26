package ao.ridehailing.driver.core.network

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SendOtpRequest(val phone: String)

@JsonClass(generateAdapter = true)
data class SendOtpResponse(val otpSent: Boolean)

@JsonClass(generateAdapter = true)
data class VerifyOtpRequest(val phone: String, val code: String)

@JsonClass(generateAdapter = true)
data class VerifyOtpResponse(
    val token: String,
    val driverId: String,
    val expiresAt: String,
)

@JsonClass(generateAdapter = true)
data class RefreshRequest(val refreshToken: String)

@JsonClass(generateAdapter = true)
data class RefreshResponse(val token: String, val expiresAt: String)
