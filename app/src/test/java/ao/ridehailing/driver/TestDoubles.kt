package ao.ridehailing.driver

import ao.ridehailing.driver.core.mqtt.OnlineSession
import ao.ridehailing.driver.core.network.ChatUploadApi
import ao.ridehailing.driver.core.network.DriverStatusApi
import ao.ridehailing.driver.core.network.EarningsApi
import ao.ridehailing.driver.core.network.NetworkResult
import ao.ridehailing.driver.core.network.OnboardingApi
import ao.ridehailing.driver.core.network.TripEarning
import ao.ridehailing.driver.core.storage.ChatDao
import ao.ridehailing.driver.core.storage.ChatMessageEntity
import ao.ridehailing.driver.core.storage.EarningsDao
import ao.ridehailing.driver.core.storage.OnboardingDao
import ao.ridehailing.driver.core.storage.OnboardingEntity
import ao.ridehailing.driver.core.storage.PayoutDao
import ao.ridehailing.driver.core.storage.PayoutEntity
import ao.ridehailing.driver.core.storage.ReferralDao
import ao.ridehailing.driver.core.storage.ReferralEntity
import ao.ridehailing.driver.core.storage.RideEventDao
import ao.ridehailing.driver.core.storage.RideEventEntity
import ao.ridehailing.driver.core.storage.TripEarningEntity
import ao.ridehailing.driver.feature.call.AudioCall
import ao.ridehailing.driver.feature.call.SignallingClient
import ao.ridehailing.driver.feature.home.PresenceControl
import ao.ridehailing.driver.feature.onboarding.OnboardingSchedule
import ao.ridehailing.driver.core.location.LocationPulse

class MemorySession : OnlineSession {
    val events = mutableListOf<String>()

    override suspend fun subscribe(topic: String, qos: Int) {
        events += "sub|$topic|$qos"
    }

    override suspend fun unsubscribe(topic: String) {
        events += "unsub|$topic"
    }

    override suspend fun publish(topic: String, body: String, qos: Int) {
        events += "pub|$topic|$qos|$body"
    }

    override suspend fun disconnectSession() {
        events += "disconnect"
    }
}

class MemoryPulse : LocationPulse {
    var running = false
    override fun start(block: suspend () -> Unit) {
        running = true
    }

    override fun stop() {
        running = false
    }
}

class MemoryPresence : PresenceControl {
    var started = false
    override fun start() {
        started = true
    }

    override fun stop() {
        started = false
    }
}

class MemorySchedule : OnboardingSchedule {
    var calls = 0
    override fun enqueue(driverId: String) {
        calls += 1
    }
}

class MemoryOnboardingDao : OnboardingDao {
    var saved: OnboardingEntity? = null
    override suspend fun find(driverId: String) = saved?.takeIf { it.driverId == driverId }
    override suspend fun save(entity: OnboardingEntity) {
        saved = entity
    }
}

class MemoryRideDao : RideEventDao {
    val rows = mutableListOf<RideEventEntity>()
    override suspend fun insert(entity: RideEventEntity) {
        rows += entity
    }

    override suspend fun latest(rideId: String) = rows.lastOrNull { it.rideId == rideId }
}

class MemoryChatDao : ChatDao {
    val rows = mutableListOf<ChatMessageEntity>()
    override suspend fun insert(entity: ChatMessageEntity) {
        rows += entity
    }

    override suspend fun list(rideId: String) = rows.filter { it.rideId == rideId }
}

class MemoryEarningsDao : EarningsDao {
    val rows = mutableListOf<TripEarningEntity>()
    override suspend fun save(entity: TripEarningEntity) {
        rows.removeAll { it.rideId == entity.rideId }
        rows += entity
    }

    override suspend fun all() = rows.toList()
}

class MemoryPayoutDao : PayoutDao {
    val rows = mutableListOf<PayoutEntity>()
    override suspend fun save(entity: PayoutEntity) {
        rows += entity
    }

    override suspend fun all() = rows.toList()
}

class MemoryReferralDao : ReferralDao {
    val rows = mutableListOf<ReferralEntity>()
    override suspend fun save(entity: ReferralEntity) {
        rows += entity
    }

    override suspend fun all() = rows.toList()
}

class MemoryStatusApi : DriverStatusApi {
    var status = ""
    override suspend fun setStatus(status: String): NetworkResult<Unit> {
        this.status = status
        return NetworkResult.Success(Unit)
    }
}

class FailingStatusApi : DriverStatusApi {
    override suspend fun setStatus(status: String) = NetworkResult.Failure("offline")
}

class MemorySignal : SignallingClient {
    var url = ""
    var closed = false
    override suspend fun open(url: String) {
        this.url = url
    }

    override fun close() {
        closed = true
    }
}

class MemoryAudio : AudioCall {
    var muted = false
    var sharing = false
    var stopped = false
    override fun mute(on: Boolean) {
        muted = on
    }

    override fun shareLocation(on: Boolean) {
        sharing = on
    }

    override fun stop() {
        stopped = true
    }
}

class MemoryEarningsApi : EarningsApi {
    override suspend fun earnings(period: String) = NetworkResult.Success(
        listOf(TripEarning("R-1", "2026-10-26", 13_850, 2_000, 34)),
    )
}

class MemoryUpload : ChatUploadApi {
    override suspend fun upload(bytes: ByteArray) = NetworkResult.Success("https://cdn.example.ao/chat/1.jpg")
}

class MemoryOnboardingApi : OnboardingApi {
    override suspend fun uploadDocument(kind: String, bytes: ByteArray): NetworkResult<Unit> {
        if (bytes.isEmpty()) return NetworkResult.Failure("empty file")
        return NetworkResult.Success(Unit)
    }

    override suspend fun status() = NetworkResult.Success("approved")

    override suspend fun uploadVideo(bytes: ByteArray): NetworkResult<Unit> {
        if (bytes.size < 8) return NetworkResult.Failure("video too short")
        return NetworkResult.Success(Unit)
    }
}
