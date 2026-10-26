package ao.ridehailing.driver.feature

import ao.ridehailing.driver.FailingStatusApi
import ao.ridehailing.driver.MemoryAudio
import ao.ridehailing.driver.MemoryChatDao
import ao.ridehailing.driver.MemoryEarningsApi
import ao.ridehailing.driver.MemoryEarningsDao
import ao.ridehailing.driver.MemoryOnboardingApi
import ao.ridehailing.driver.MemoryOnboardingDao
import ao.ridehailing.driver.MemoryPayoutDao
import ao.ridehailing.driver.MemoryPresence
import ao.ridehailing.driver.MemoryPulse
import ao.ridehailing.driver.MemoryReferralDao
import ao.ridehailing.driver.MemoryRideDao
import ao.ridehailing.driver.MemorySchedule
import ao.ridehailing.driver.MemorySession
import ao.ridehailing.driver.MemorySignal
import ao.ridehailing.driver.MemoryStatusApi
import ao.ridehailing.driver.MemoryUpload
import ao.ridehailing.driver.core.money.AoaFormat
import ao.ridehailing.driver.core.mqtt.CallTopics
import ao.ridehailing.driver.core.mqtt.DriverTopics
import ao.ridehailing.driver.core.mqtt.MqttQos
import ao.ridehailing.driver.core.mqtt.TopicMapper
import ao.ridehailing.driver.core.network.ApiErrorParser
import ao.ridehailing.driver.core.network.MockPayoutApi
import ao.ridehailing.driver.core.network.MockReferralApi
import ao.ridehailing.driver.core.network.MockReportApi
import ao.ridehailing.driver.core.network.PayoutFee
import ao.ridehailing.driver.core.network.TripEarning
import ao.ridehailing.driver.core.webrtc.CallQualityMonitor
import ao.ridehailing.driver.feature.activeride.ActiveRideViewModel
import ao.ridehailing.driver.feature.activeride.RideStage
import ao.ridehailing.driver.feature.call.CallQualityPublisher
import ao.ridehailing.driver.feature.call.CallStage
import ao.ridehailing.driver.feature.call.CallViewModel
import ao.ridehailing.driver.feature.chat.ChatViewModel
import ao.ridehailing.driver.feature.earnings.EarningsMath
import ao.ridehailing.driver.feature.earnings.EarningsViewModel
import ao.ridehailing.driver.feature.home.HomeState
import ao.ridehailing.driver.feature.home.HomeViewModel
import ao.ridehailing.driver.feature.home.components.DemandZones
import ao.ridehailing.driver.feature.onboarding.OnboardingPoll
import ao.ridehailing.driver.feature.onboarding.OnboardingViewModel
import ao.ridehailing.driver.feature.profile.ReferralViewModel
import ao.ridehailing.driver.feature.rideoffer.OfferParser
import ao.ridehailing.driver.feature.rideoffer.OfferUi
import ao.ridehailing.driver.feature.rideoffer.RideOfferViewModel
import ao.ridehailing.driver.feature.safety.ReportViewModel
import ao.ridehailing.driver.feature.wallet.WalletViewModel
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.time.LocalDate

private fun offer(rideId: String) = """
    {"type":"ride.offer","messageId":"m-$rideId","payload":{"rideId":"$rideId","pickup":{"lat":-8.8368,"lng":13.2343,"address":"Rua da Missão"},"destination":{"lat":-8.9175,"lng":13.1839,"address":"Talatona Shopping"},"distanceKm":14.3,"durationMin":34,"payoutAoa":13850,"passengerRating":4.9,"expiresAt":"2026-10-27T10:18:00Z"}}
""".trim()

private fun rating(impact: Int, token: String) =
    """{"type":"ride.state","messageId":"rate","payload":{"ratingImpact":$impact,"acceptanceToken":"$token"}}"""

@OptIn(ExperimentalCoroutinesApi::class)
class RideOfferViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val session = MemorySession()
    private lateinit var viewModel: RideOfferViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = RideOfferViewModel(session, OfferParser(Moshi.Builder().build()))
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun countdownStartsAtFifteenAndTimesOutWithoutRatingImpact() = runTest(dispatcher) {
        viewModel.onMessage(offer("R-1"))
        val showing = viewModel.state.value as OfferUi.Showing
        assertEquals(15, showing.seconds)
        assertEquals("Rua da Missão", showing.offer.pickupAddress)
        assertEquals(13_850, showing.offer.payoutAoa)
        repeat(14) { viewModel.tick() }
        assertEquals(1, (viewModel.state.value as OfferUi.Showing).seconds)
        viewModel.tick()
        advanceUntilIdle()
        assertEquals(OfferUi.Declined, viewModel.state.value)
        assertEquals(0, viewModel.ratingScore)
        assertTrue(session.events.any { it.startsWith("pub|${TopicMapper.rideDecline("R-1")}|${MqttQos.AT_LEAST_ONCE}") })
    }

    @Test
    fun duplicateOfferIsIgnored() {
        viewModel.onMessage(offer("R-1"))
        viewModel.onMessage(offer("R-1"))
        assertEquals(15, (viewModel.state.value as OfferUi.Showing).seconds)
    }

    @Test
    fun acceptPublishesOnceAndClearsDeclines() = runTest(dispatcher) {
        viewModel.onMessage(offer("R-1"))
        viewModel.decline()
        viewModel.onMessage(offer("R-2"))
        viewModel.accept()
        viewModel.accept()
        advanceUntilIdle()
        assertEquals(OfferUi.Accepted, viewModel.state.value)
        assertEquals(0, viewModel.consecutiveDeclines)
        assertEquals(1, session.events.count { it.contains("/accept|") })
    }

    @Test
    fun declineNeverRaisesRatingAndDuplicateTokenIsIgnored() {
        viewModel.onMessage(offer("R-1"))
        viewModel.decline()
        viewModel.onRating(rating(0, "tok-1"))
        viewModel.onRating(rating(5, "tok-1"))
        assertEquals(0, viewModel.ratingScore)
        assertEquals(1, viewModel.consecutiveDeclines)
    }

    @Test
    fun threeDeclinesPauseForTwoMinutesThenReset() {
        viewModel.onMessage(offer("R-1"))
        viewModel.decline()
        viewModel.onMessage(offer("R-2"))
        viewModel.decline()
        viewModel.onMessage(offer("R-3"))
        viewModel.decline()
        assertEquals(OfferUi.Paused(120), viewModel.state.value)
        viewModel.onMessage(offer("R-4"))
        assertTrue(viewModel.state.value is OfferUi.Paused)
        repeat(119) { viewModel.tick() }
        assertEquals(OfferUi.Paused(1), viewModel.state.value)
        viewModel.tick()
        assertEquals(OfferUi.Idle, viewModel.state.value)
        assertEquals(0, viewModel.consecutiveDeclines)
    }

    @Test
    fun resumeAllowsOffersAgain() {
        repeat(3) {
            viewModel.onMessage(offer("R-$it"))
            viewModel.decline()
        }
        viewModel.resume()
        viewModel.onMessage(offer("R-9"))
        assertTrue(viewModel.state.value is OfferUi.Showing)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val session = MemorySession()
    private val pulse = MemoryPulse()
    private val presence = MemoryPresence()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onlineSubscribesAndOfflineDisconnects() = runTest(dispatcher) {
        val viewModel = HomeViewModel(MemoryStatusApi(), session, pulse, presence)
        viewModel.goOnline("D-1")
        advanceUntilIdle()
        assertEquals(HomeState.Online, viewModel.state.value)
        assertTrue(presence.started)
        assertTrue(pulse.running)
        assertTrue(session.events.contains("sub|${DriverTopics.rideOffers("D-1")}|${MqttQos.AT_LEAST_ONCE}"))
        assertTrue(session.events.contains("sub|${DriverTopics.demandZones("D-1")}|${MqttQos.AT_MOST_ONCE}"))
        viewModel.goOffline("D-1")
        advanceUntilIdle()
        assertEquals(HomeState.Offline, viewModel.state.value)
        assertFalse(pulse.running)
        assertTrue(session.events.contains("unsub|${DriverTopics.demandZones("D-1")}"))
        assertTrue(session.events.contains("disconnect"))
    }

    @Test
    fun statusFailureStaysInError() = runTest(dispatcher) {
        val viewModel = HomeViewModel(FailingStatusApi(), session, pulse, presence)
        viewModel.goOnline("D-1")
        advanceUntilIdle()
        assertEquals(HomeState.Error("offline"), viewModel.state.value)
        assertTrue(session.events.isEmpty())
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val dao = MemoryOnboardingDao()
    private val schedule = MemorySchedule()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun documentsThenBackgroundThenVideo() = runTest(dispatcher) {
        val viewModel = OnboardingViewModel(MemoryOnboardingApi(), dao, schedule)
        viewModel.resume("D-1")
        advanceUntilIdle()
        listOf("vtc", "insurance", "registration", "id").forEach { viewModel.upload(it, byteArrayOf(1)) }
        advanceUntilIdle()
        assertEquals("background", viewModel.state.value.step)
        viewModel.poll()
        advanceUntilIdle()
        assertEquals("video", viewModel.state.value.step)
        assertEquals(1, schedule.calls)
        assertEquals(OnboardingPoll.SECONDS, 5L)
        viewModel.submitVideo(ByteArray(16))
        advanceUntilIdle()
        assertEquals("complete", viewModel.state.value.step)
        assertEquals("complete", dao.saved?.step)
    }

    @Test
    fun emptyDocumentAndShortVideoFail() = runTest(dispatcher) {
        val viewModel = OnboardingViewModel(MemoryOnboardingApi(), dao, schedule)
        viewModel.upload("vtc", byteArrayOf())
        advanceUntilIdle()
        assertEquals("empty file", viewModel.state.value.message)
        viewModel.submitVideo(byteArrayOf(1))
        advanceUntilIdle()
        assertEquals("video too short", viewModel.state.value.message)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveRideViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val session = MemorySession()
    private val dao = MemoryRideDao()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun advancesThroughPaymentAndRestores() = runTest(dispatcher) {
        val viewModel = ActiveRideViewModel(session, dao)
        viewModel.restore("R-1", "D-1", "2026-10-27T10:18:00Z")
        advanceUntilIdle()
        repeat(5) { viewModel.advance("2026-10-27T10:18:0${it}Z") }
        advanceUntilIdle()
        assertEquals(RideStage.PaymentPending, viewModel.stage.value)
        assertEquals("at_pickup", dao.rows.first().state)
        assertTrue(session.events.any { it.contains("\"state\":\"payment_pending\"") })
        val restored = ActiveRideViewModel(session, dao)
        restored.restore("R-1", "D-1", "2026-10-27T10:19:00Z")
        advanceUntilIdle()
        assertEquals(RideStage.PaymentPending, restored.stage.value)
    }

    @Test
    fun cancelRequiresReasonAndPublishes() = runTest(dispatcher) {
        val viewModel = ActiveRideViewModel(session, dao)
        viewModel.restore("R-1", "D-1", "2026-10-27T10:18:00Z")
        viewModel.cancel("", "", "2026-10-27T10:18:00Z")
        assertEquals("reason", viewModel.message)
        viewModel.cancel("other", "", "2026-10-27T10:18:00Z")
        assertEquals("text", viewModel.message)
        viewModel.cancel("other", "x".repeat(201), "2026-10-27T10:18:00Z")
        assertEquals("length", viewModel.message)
        viewModel.cancel("passenger_no_show", "", "2026-10-27T10:18:00Z")
        advanceUntilIdle()
        assertTrue(session.events.any { it.startsWith("pub|${DriverTopics.rideCancel("R-1")}|") })
        assertTrue(session.events.any { it.contains(TopicMapper.rideState("R-1")) && it.contains("passenger_no_show") })
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatAndCallTest {
    private val dispatcher = StandardTestDispatcher()
    private val session = MemorySession()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun chatDedupesAndUploadsImage() = runTest(dispatcher) {
        val viewModel = ChatViewModel(session, MemoryChatDao(), MemoryUpload(), Moshi.Builder().build())
        viewModel.open("R-1")
        advanceUntilIdle()
        viewModel.sendText("Olá", "m-1", "2026-10-27T10:18:00Z")
        viewModel.sendText("Olá", "m-1", "2026-10-27T10:18:00Z")
        viewModel.sendImage(byteArrayOf(1), "m-2", "2026-10-27T10:18:00Z")
        advanceUntilIdle()
        assertEquals(2, viewModel.messages.value.size)
        assertEquals("image", viewModel.messages.value.last().kind)
        assertTrue(session.events.any { it.contains(TopicMapper.rideChat("R-1")) })
        val incoming = """{"type":"chat.message","messageId":"m-3","payload":{"messageId":"m-3","senderRole":"passenger","text":"ok","timestamp":"2026-10-27T10:19:00Z"}}"""
        viewModel.onIncoming(incoming)
        viewModel.onIncoming(incoming)
        assertEquals(3, viewModel.messages.value.size)
    }

    @Test
    fun callOpensSocketAndQualityDropsBitrate() = runTest(dispatcher) {
        val signal = MemorySignal()
        val audio = MemoryAudio()
        val viewModel = CallViewModel(session, signal, audio, Moshi.Builder().build())
        viewModel.start("R-1", "c-1")
        advanceUntilIdle()
        assertEquals(CallStage.Initiating, viewModel.stage.value)
        viewModel.onResponse(
            """{"type":"call.response","payload":{"callId":"c-1","signallingUrl":"wss://api.example.ao/v1/calls/c-1/signal","proxyNumber":"+244900000"}}""",
        )
        advanceUntilIdle()
        assertEquals(CallStage.Ringing, viewModel.stage.value)
        assertEquals("wss://api.example.ao/v1/calls/c-1/signal", signal.url)
        assertEquals("+244900000", viewModel.proxyNumber)
        viewModel.connected()
        viewModel.mute()
        viewModel.shareLocation()
        assertTrue(audio.muted)
        assertTrue(audio.sharing)
        viewModel.end("c-end")
        advanceUntilIdle()
        assertEquals(CallStage.Ended, viewModel.stage.value)
        assertTrue(audio.stopped)
        assertTrue(signal.closed)
        assertTrue(session.events.any { it.contains(CallTopics.callEnd("R-1")) })
        val publisher = CallQualityPublisher(session)
        publisher.publish("R-1", 0.1, 500, "q-1")
        publisher.publish("R-1", 0.0, 20, "q-2")
        assertTrue(session.events.any { it.contains("\"bitrate\":10000") })
        assertTrue(session.events.any { it.contains("\"bitrate\":32000") })
        assertEquals(10_000, CallQualityMonitor().bitrate(0.2, 10))
        assertEquals(32_000, CallQualityMonitor().bitrate(0.0, 10))
    }

    @Test
    fun missedAndFailedCloseSignalling() = runTest(dispatcher) {
        val signal = MemorySignal()
        val viewModel = CallViewModel(session, signal, MemoryAudio(), Moshi.Builder().build())
        viewModel.miss()
        assertEquals(CallStage.Missed, viewModel.stage.value)
        viewModel.fail()
        assertEquals(CallStage.Failed, viewModel.stage.value)
        assertTrue(signal.closed)
        viewModel.onResponse("{")
        assertEquals(CallStage.Failed, viewModel.stage.value)
    }

    @Test
    fun callSourcesDoNotUseMediaRecorder() {
        val roots = listOf(
            File("src/main/java/ao/ridehailing/driver/feature/call"),
            File("src/main/java/ao/ridehailing/driver/core/webrtc"),
        )
        val text = roots.flatMap { it.walk().filter { file -> file.extension == "kt" }.toList() }
            .joinToString("\n") { it.readText() }
        assertFalse(text.contains("MediaRecorder"))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class EarningsWalletSafetyTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun earningsPeriodsAndCancellationRate() = runTest(dispatcher) {
        val today = LocalDate.of(2026, 10, 26)
        val trips = listOf(
            TripEarning("R-1", "2026-10-26", 13_850, 2_000, 34),
            TripEarning("R-2", "2026-10-20", 1_000, 100, 10),
        )
        assertEquals(1, EarningsMath.stats(trips, "day", today).trips)
        assertEquals(13_850, EarningsMath.stats(trips, "day", today).totalAoa)
        assertEquals(2_000, EarningsMath.stats(trips, "day", today).commissionAoa)
        assertEquals(2, EarningsMath.stats(trips, "week", today).trips)
        assertEquals(2, EarningsMath.stats(trips, "month", today).trips)
        assertEquals(25, EarningsMath.rate(1, 4))
        val viewModel = EarningsViewModel(MemoryEarningsApi(), MemoryEarningsDao())
        viewModel.load(today)
        advanceUntilIdle()
        assertEquals(1, viewModel.state.value.trips)
        viewModel.selectPeriod("week", today)
        advanceUntilIdle()
        assertEquals("week", viewModel.period)
        viewModel.selectRide("R-1")
        assertEquals("R-1", viewModel.selectedRide)
        viewModel.recordCancellations(1, 4)
        assertEquals(25, viewModel.cancellationRate)
        assertEquals("Kz 13.850,00", AoaFormat.format(13_850))
        assertEquals("-Kz 2.000,00", AoaFormat.format(-2_000))
    }

    @Test
    fun payoutFeeAndHistory() = runTest(dispatcher) {
        assertEquals(150, PayoutFee.instant(10_000))
        val viewModel = WalletViewModel(MockPayoutApi(), MemoryPayoutDao())
        viewModel.instant(10_000)
        advanceUntilIdle()
        assertEquals("instant", viewModel.notice)
        assertEquals(150, viewModel.history.value.single().feeAoa)
        viewModel.weekly(10_000)
        advanceUntilIdle()
        assertEquals(0, viewModel.history.value.last().feeAoa)
    }

    @Test
    fun reportBlocksPassengerAndReferralPaysAfterTenRides() = runTest(dispatcher) {
        val report = ReportViewModel(MockReportApi())
        report.submit("P-1", "R-1", "nope", "")
        assertEquals("reason", report.message)
        report.submit("P-1", "R-1", "safety_concern", "x".repeat(501))
        assertEquals("length", report.message)
        report.submit("P-1", "R-1", "safety_concern", "rua")
        advanceUntilIdle()
        assertTrue(report.submitted.value)
        assertTrue(report.isBlocked("P-1"))
        val referral = ReferralViewModel(MockReferralApi(), MemoryReferralDao())
        referral.generate()
        referral.load()
        advanceUntilIdle()
        assertEquals("LUANDA-1", referral.code)
        assertEquals(10, referral.referrals.value.single().rides)
        assertTrue(referral.bonusDue(10))
        assertFalse(referral.bonusDue(9))
    }

    @Test
    fun parsesDemandZonesAndApiErrors() {
        val zones = DemandZones.parse(
            Moshi.Builder().build(),
            """{"type":"demand-zones","payload":{"zones":[{"lat":-8.8368,"lng":13.2343,"radiusMeters":500,"intensity":0.8,"label":"Talatona — 3x rides"}]}}""",
        )
        assertEquals("Talatona — 3x rides", zones.single().label)
        assertEquals(500, zones.single().radiusMeters)
        val body = """{"error":{"code":"RIDE_NOT_FOUND","message":"Corrida não encontrada","details":{}}}"""
        assertEquals("Corrida não encontrada", ApiErrorParser.message(body))
        assertEquals(null, ApiErrorParser.message("{}"))
    }
}
