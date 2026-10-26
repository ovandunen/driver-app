package ao.ridehailing.driver.di

import android.content.Context
import androidx.room.Room
import ao.ridehailing.driver.core.storage.AppDatabase
import ao.ridehailing.driver.core.storage.ChatDao
import ao.ridehailing.driver.core.storage.EarningsDao
import ao.ridehailing.driver.core.storage.OnboardingDao
import ao.ridehailing.driver.core.storage.PayoutDao
import ao.ridehailing.driver.core.storage.ReferralDao
import ao.ridehailing.driver.core.storage.RideEventDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "driver.db",
    ).build()

    @Provides fun onboardingDao(db: AppDatabase): OnboardingDao = db.onboardingDao()
    @Provides fun rideEventDao(db: AppDatabase): RideEventDao = db.rideEventDao()
    @Provides fun chatDao(db: AppDatabase): ChatDao = db.chatDao()
    @Provides fun earningsDao(db: AppDatabase): EarningsDao = db.earningsDao()
    @Provides fun payoutDao(db: AppDatabase): PayoutDao = db.payoutDao()
    @Provides fun referralDao(db: AppDatabase): ReferralDao = db.referralDao()
}
