package ao.ridehailing.driver.di

import ao.ridehailing.driver.core.location.ActiveLocationSource
import ao.ridehailing.driver.core.location.IntervalPulse
import ao.ridehailing.driver.core.location.LocationPulse
import ao.ridehailing.driver.core.location.LocationSource
import ao.ridehailing.driver.core.mqtt.MqttConnectionManager
import ao.ridehailing.driver.core.mqtt.OnlineSession
import ao.ridehailing.driver.core.network.ChatUploadApi
import ao.ridehailing.driver.core.network.DriverStatusApi
import ao.ridehailing.driver.core.network.EarningsApi
import ao.ridehailing.driver.core.network.MockChatUploadApi
import ao.ridehailing.driver.core.network.MockDriverStatusApi
import ao.ridehailing.driver.core.network.MockEarningsApi
import ao.ridehailing.driver.core.network.MockOnboardingApi
import ao.ridehailing.driver.core.network.MockPayoutApi
import ao.ridehailing.driver.core.network.MockReferralApi
import ao.ridehailing.driver.core.network.MockReportApi
import ao.ridehailing.driver.core.network.MockRouteClient
import ao.ridehailing.driver.core.network.OnboardingApi
import ao.ridehailing.driver.core.network.PayoutApi
import ao.ridehailing.driver.core.network.ReferralApi
import ao.ridehailing.driver.core.network.ReportApi
import ao.ridehailing.driver.core.network.RouteClient
import ao.ridehailing.driver.core.webrtc.WebRtcCall
import ao.ridehailing.driver.core.webrtc.WebSocketSignalling
import ao.ridehailing.driver.feature.call.AudioCall
import ao.ridehailing.driver.feature.call.SignallingClient
import ao.ridehailing.driver.feature.home.AndroidPresence
import ao.ridehailing.driver.feature.home.PresenceControl
import ao.ridehailing.driver.feature.onboarding.OnboardingSchedule
import ao.ridehailing.driver.feature.onboarding.WorkOnboardingSchedule
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StatusBindModule {
    @Binds @Singleton abstract fun onboarding(impl: MockOnboardingApi): OnboardingApi
    @Binds @Singleton abstract fun status(impl: MockDriverStatusApi): DriverStatusApi
    @Binds @Singleton abstract fun earnings(impl: MockEarningsApi): EarningsApi
    @Binds @Singleton abstract fun payout(impl: MockPayoutApi): PayoutApi
    @Binds @Singleton abstract fun report(impl: MockReportApi): ReportApi
    @Binds @Singleton abstract fun referral(impl: MockReferralApi): ReferralApi
    @Binds @Singleton abstract fun chatUpload(impl: MockChatUploadApi): ChatUploadApi
    @Binds @Singleton abstract fun route(impl: MockRouteClient): RouteClient
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RuntimeBindModule {
    @Binds @Singleton abstract fun pulse(impl: IntervalPulse): LocationPulse
    @Binds @Singleton abstract fun locations(impl: ActiveLocationSource): LocationSource
    @Binds @Singleton abstract fun presence(impl: AndroidPresence): PresenceControl
    @Binds @Singleton abstract fun schedule(impl: WorkOnboardingSchedule): OnboardingSchedule
    @Binds @Singleton abstract fun signalling(impl: WebSocketSignalling): SignallingClient
    @Binds @Singleton abstract fun audio(impl: WebRtcCall): AudioCall
}

@Module
@InstallIn(SingletonComponent::class)
object SessionModule {
    @Provides
    @Singleton
    fun session(manager: MqttConnectionManager): OnlineSession = manager
}
