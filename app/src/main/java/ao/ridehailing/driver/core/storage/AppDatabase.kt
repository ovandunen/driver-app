package ao.ridehailing.driver.core.storage

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        OnboardingEntity::class,
        RideEventEntity::class,
        ChatMessageEntity::class,
        TripEarningEntity::class,
        PayoutEntity::class,
        ReferralEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun onboardingDao(): OnboardingDao
    abstract fun rideEventDao(): RideEventDao
    abstract fun chatDao(): ChatDao
    abstract fun earningsDao(): EarningsDao
    abstract fun payoutDao(): PayoutDao
    abstract fun referralDao(): ReferralDao
}
