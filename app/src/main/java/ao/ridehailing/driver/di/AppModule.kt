package ao.ridehailing.driver.di

import android.content.Context
import ao.ridehailing.driver.core.storage.EncryptedTokenStore
import ao.ridehailing.driver.core.storage.TokenStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun tokenStore(@ApplicationContext context: Context): TokenStore = EncryptedTokenStore(context)
}
