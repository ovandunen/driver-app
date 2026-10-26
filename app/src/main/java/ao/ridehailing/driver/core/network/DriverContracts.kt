package ao.ridehailing.driver.core.network

interface OnboardingApi {
    suspend fun uploadDocument(kind: String, bytes: ByteArray): NetworkResult<Unit>
    suspend fun status(): NetworkResult<String>
    suspend fun uploadVideo(bytes: ByteArray): NetworkResult<Unit>
}

interface DriverStatusApi {
    suspend fun setStatus(status: String): NetworkResult<Unit>
}

data class TripEarning(
    val rideId: String,
    val day: String,
    val fareAoa: Long,
    val commissionAoa: Long,
    val minutes: Int,
)

interface EarningsApi {
    suspend fun earnings(period: String): NetworkResult<List<TripEarning>>
}

data class PayoutReceipt(val id: String, val kind: String, val amountAoa: Long, val feeAoa: Long)

interface PayoutApi {
    suspend fun instant(amountAoa: Long): NetworkResult<PayoutReceipt>
    suspend fun weekly(amountAoa: Long): NetworkResult<PayoutReceipt>
}

interface ReportApi {
    suspend fun report(passengerId: String, rideId: String, reason: String, description: String): NetworkResult<Unit>
}

data class ReferralRow(val code: String, val name: String, val rides: Int)

interface ReferralApi {
    suspend fun create(): NetworkResult<String>
    suspend fun list(): NetworkResult<List<ReferralRow>>
}

interface ChatUploadApi {
    suspend fun upload(bytes: ByteArray): NetworkResult<String>
}

data class RouteLeg(val durationSec: Int)

interface RouteClient {
    suspend fun route(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double): NetworkResult<RouteLeg>
}
