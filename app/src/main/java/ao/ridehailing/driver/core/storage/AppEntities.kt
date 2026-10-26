package ao.ridehailing.driver.core.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "onboarding")
data class OnboardingEntity(
    @PrimaryKey val driverId: String,
    val step: String,
    val vtc: Boolean,
    val insurance: Boolean,
    val registration: Boolean,
    val identity: Boolean,
    val backgroundStatus: String,
    val videoUploaded: Boolean,
)

@Entity(tableName = "ride_event")
data class RideEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rideId: String,
    val state: String,
    val timestamp: String,
)

@Entity(tableName = "chat_message")
data class ChatMessageEntity(
    @PrimaryKey val messageId: String,
    val rideId: String,
    val senderRole: String,
    val text: String,
    val kind: String,
    val timestamp: String,
)

@Entity(tableName = "trip_earning")
data class TripEarningEntity(
    @PrimaryKey val rideId: String,
    val day: String,
    val fareAoa: Long,
    val commissionAoa: Long,
    val minutes: Int,
)

@Entity(tableName = "payout")
data class PayoutEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val amountAoa: Long,
    val feeAoa: Long,
)

@Entity(tableName = "referral")
data class ReferralEntity(
    @PrimaryKey val code: String,
    val name: String,
    val rides: Int,
)
