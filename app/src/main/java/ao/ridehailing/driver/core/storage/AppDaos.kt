package ao.ridehailing.driver.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface OnboardingDao {
    @Query("SELECT * FROM onboarding WHERE driverId = :driverId LIMIT 1")
    suspend fun find(driverId: String): OnboardingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: OnboardingEntity)
}

@Dao
interface RideEventDao {
    @Insert
    suspend fun insert(entity: RideEventEntity)

    @Query("SELECT * FROM ride_event WHERE rideId = :rideId ORDER BY id DESC LIMIT 1")
    suspend fun latest(rideId: String): RideEventEntity?
}

@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: ChatMessageEntity)

    @Query("SELECT * FROM chat_message WHERE rideId = :rideId ORDER BY timestamp")
    suspend fun list(rideId: String): List<ChatMessageEntity>
}

@Dao
interface EarningsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: TripEarningEntity)

    @Query("SELECT * FROM trip_earning")
    suspend fun all(): List<TripEarningEntity>
}

@Dao
interface PayoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: PayoutEntity)

    @Query("SELECT * FROM payout")
    suspend fun all(): List<PayoutEntity>
}

@Dao
interface ReferralDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: ReferralEntity)

    @Query("SELECT * FROM referral")
    suspend fun all(): List<ReferralEntity>
}
