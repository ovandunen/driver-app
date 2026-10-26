package ao.ridehailing.driver.core.network

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockOnboardingApi @Inject constructor() : OnboardingApi {
    override suspend fun uploadDocument(kind: String, bytes: ByteArray): NetworkResult<Unit> {
        if (bytes.isEmpty()) return NetworkResult.Failure("empty file")
        return NetworkResult.Success(Unit)
    }

    override suspend fun status(): NetworkResult<String> = NetworkResult.Success("approved")

    override suspend fun uploadVideo(bytes: ByteArray): NetworkResult<Unit> {
        if (bytes.size < 8) return NetworkResult.Failure("video too short")
        return NetworkResult.Success(Unit)
    }
}

@Singleton
class MockDriverStatusApi @Inject constructor() : DriverStatusApi {
    override suspend fun setStatus(status: String): NetworkResult<Unit> {
        if (status != "online" && status != "offline") return NetworkResult.Failure("status")
        return NetworkResult.Success(Unit)
    }
}

@Singleton
class MockEarningsApi @Inject constructor() : EarningsApi {
    override suspend fun earnings(period: String): NetworkResult<List<TripEarning>> = NetworkResult.Success(
        listOf(TripEarning("R-1", "2026-10-26", 13_850, 2_000, 34)),
    )
}

@Singleton
class MockPayoutApi @Inject constructor() : PayoutApi {
    override suspend fun instant(amountAoa: Long): NetworkResult<PayoutReceipt> = NetworkResult.Success(
        PayoutReceipt("p-instant", "instant", amountAoa, PayoutFee.instant(amountAoa)),
    )

    override suspend fun weekly(amountAoa: Long): NetworkResult<PayoutReceipt> = NetworkResult.Success(
        PayoutReceipt("p-weekly", "weekly", amountAoa, 0),
    )
}

object PayoutFee {
    fun instant(amountAoa: Long): Long = amountAoa * 15 / 1_000
}

@Singleton
class MockReportApi @Inject constructor() : ReportApi {
    override suspend fun report(
        passengerId: String,
        rideId: String,
        reason: String,
        description: String,
    ): NetworkResult<Unit> {
        if (description.length > 500) return NetworkResult.Failure("description")
        return NetworkResult.Success(Unit)
    }
}

@Singleton
class MockReferralApi @Inject constructor() : ReferralApi {
    override suspend fun create(): NetworkResult<String> = NetworkResult.Success("LUANDA-1")

    override suspend fun list(): NetworkResult<List<ReferralRow>> = NetworkResult.Success(
        listOf(ReferralRow("LUANDA-1", "Ana", 10)),
    )
}

@Singleton
class MockChatUploadApi @Inject constructor() : ChatUploadApi {
    override suspend fun upload(bytes: ByteArray): NetworkResult<String> = NetworkResult.Success("https://cdn.example.ao/chat/1.jpg")
}

@Singleton
class MockRouteClient @Inject constructor() : RouteClient {
    override suspend fun route(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double): NetworkResult<RouteLeg> =
        NetworkResult.Success(RouteLeg(600))
}
