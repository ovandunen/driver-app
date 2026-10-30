package ao.ridehailing.driver.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ao.ridehailing.driver.BuildConfig
import ao.ridehailing.driver.core.storage.TokenStore
import ao.ridehailing.driver.feature.activeride.ActiveRideScreen
import ao.ridehailing.driver.feature.auth.AuthScreen
import ao.ridehailing.driver.feature.call.CallScreen
import ao.ridehailing.driver.feature.call.CallViewModel
import ao.ridehailing.driver.feature.chat.ChatScreen
import ao.ridehailing.driver.feature.chat.ChatViewModel
import ao.ridehailing.driver.feature.earnings.EarningsScreen
import ao.ridehailing.driver.feature.earnings.EarningsViewModel
import ao.ridehailing.driver.feature.home.HomeScreen
import ao.ridehailing.driver.feature.onboarding.OnboardingRoute
import ao.ridehailing.driver.feature.profile.ReferralScreen
import ao.ridehailing.driver.feature.profile.ReferralViewModel
import ao.ridehailing.driver.feature.rideoffer.RideOfferOverlay
import ao.ridehailing.driver.feature.safety.ReportScreen
import ao.ridehailing.driver.feature.safety.ReportViewModel
import ao.ridehailing.driver.feature.wallet.WalletScreen
import ao.ridehailing.driver.feature.wallet.WalletViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject

object Routes {
    const val Auth = "auth"
    const val Home = "home"
    const val RideOffer = "rideOffer"
    const val ActiveRide = "activeRide"
    const val Call = "call"
    const val Chat = "chat"
    const val Earnings = "earnings"
    const val Wallet = "wallet"
    const val Referral = "referral"
    const val Report = "report"
    const val Onboarding = "onboarding"
}

@HiltViewModel
class SessionViewModel @Inject constructor(store: TokenStore) : ViewModel() {
    val driverId: String = if (BuildConfig.DEV_BYPASS_LOGIN) {
        BuildConfig.DEV_DRIVER_ID
    } else {
        store.driverId() ?: "D-1"
    }
}

@Composable
fun AppRoot(session: SessionViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val driverId = session.driverId
    val startDest = if (BuildConfig.DEV_BYPASS_LOGIN) Routes.Home else Routes.Auth
    NavHost(navController = navController, startDestination = startDest) {
        composable(Routes.Auth) {
            if (BuildConfig.DEV_BYPASS_LOGIN) {
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.Home) {
                        popUpTo(Routes.Auth) { inclusive = true }
                    }
                }
            } else {
                AuthScreen(
                    onAuthenticated = {
                        navController.navigate(Routes.Home) {
                            popUpTo(Routes.Auth) { inclusive = true }
                        }
                    },
                )
            }
        }
        composable(Routes.Home) {
            HomeScreen(driverId, onOpen = { route -> navController.navigate(route) })
        }
        composable(Routes.Onboarding) { OnboardingRoute(driverId) }
        composable(Routes.RideOffer) { RideOfferOverlay() }
        composable(Routes.ActiveRide) {
            ActiveRideScreen(rideId = "R-1", driverId = driverId, now = "2026-10-27T10:18:00Z", eta = "12 min", onExternal = {}, onChat = { navController.navigate(Routes.Chat) }, onCall = { navController.navigate(Routes.Call) })
        }
        composable(Routes.Call) { CallRoute("R-1") }
        composable(Routes.Chat) { ChatRoute("R-1") }
        composable(Routes.Earnings) { EarningsRoute() }
        composable(Routes.Wallet) { WalletRoute() }
        composable(Routes.Referral) { ReferralRoute() }
        composable(Routes.Report) { ReportRoute("P-1", "R-1") }
    }
}

@Composable
private fun CallRoute(rideId: String, viewModel: CallViewModel = hiltViewModel()) {
    val stage by viewModel.stage.collectAsStateWithLifecycle()
    CallScreen(stage, viewModel.proxyNumber, viewModel::mute, { viewModel.end(rideId) }, viewModel::shareLocation)
}

@Composable
private fun ChatRoute(rideId: String, viewModel: ChatViewModel = hiltViewModel()) {
    val lines by viewModel.messages.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(rideId) { viewModel.open(rideId) }
    ChatScreen(lines, { viewModel.sendText(it, "local-1", "2026-10-27T10:18:00Z") }, { viewModel.sendPredefined(it, "local-2", "2026-10-27T10:18:00Z") })
}

@Composable
private fun EarningsRoute(viewModel: EarningsViewModel = hiltViewModel()) {
    val stats by viewModel.state.collectAsStateWithLifecycle()
    val today = LocalDate.of(2026, 10, 26)
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.load(today) }
    EarningsScreen(stats, { viewModel.selectPeriod(it, today) }, viewModel::selectRide)
}

@Composable
private fun WalletRoute(viewModel: WalletViewModel = hiltViewModel()) {
    val rows by viewModel.history.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.load() }
    WalletScreen(rows, viewModel.notice, { viewModel.instant(10_000) }, { viewModel.weekly(10_000) })
}

@Composable
private fun ReferralRoute(viewModel: ReferralViewModel = hiltViewModel()) {
    val rows by viewModel.referrals.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.load() }
    ReferralScreen(viewModel.code, rows) { viewModel.generate() }
}

@Composable
private fun ReportRoute(passengerId: String, rideId: String, viewModel: ReportViewModel = hiltViewModel()) {
    val submitted by viewModel.submitted.collectAsStateWithLifecycle()
    ReportScreen(submitted) { viewModel.submit(passengerId, rideId, "safety_concern", "") }
}
