package ao.ridehailing.driver.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ao.ridehailing.driver.BuildConfig
import ao.ridehailing.driver.core.storage.TokenStore
import ao.ridehailing.driver.core.ui.DeskTab
import ao.ridehailing.driver.core.ui.findActivity
import ao.ridehailing.driver.feature.auth.AuthScreen
import ao.ridehailing.driver.feature.auth.LoginLook
import ao.ridehailing.driver.feature.auth.OtpLook
import ao.ridehailing.driver.feature.desk.CallPage
import ao.ridehailing.driver.feature.desk.CashPage
import ao.ridehailing.driver.feature.desk.ChatPage
import ao.ridehailing.driver.feature.desk.DeskViewModel
import ao.ridehailing.driver.feature.desk.DonePage
import ao.ridehailing.driver.feature.desk.DriverHome
import ao.ridehailing.driver.feature.desk.EarningsPage
import ao.ridehailing.driver.feature.desk.GoalPage
import ao.ridehailing.driver.feature.desk.NetworkPage
import ao.ridehailing.driver.feature.desk.NfcPage
import ao.ridehailing.driver.feature.desk.OfferPage
import ao.ridehailing.driver.feature.desk.PausePage
import ao.ridehailing.driver.feature.desk.PayPage
import ao.ridehailing.driver.feature.desk.ProfilePage
import ao.ridehailing.driver.feature.desk.ReactivatePage
import ao.ridehailing.driver.feature.desk.ReportPage
import ao.ridehailing.driver.feature.desk.RidePage
import ao.ridehailing.driver.feature.desk.RideStep
import ao.ridehailing.driver.feature.desk.TripsPage
import ao.ridehailing.driver.feature.rideoffer.OfferUi
import ao.ridehailing.driver.feature.rideoffer.RideOfferViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

object Routes {
    const val Auth = "auth"
    const val Home = "home"
    const val Reactivate = "reactivate"
    const val Earnings = "earnings"
    const val Trips = "trips"
    const val Profile = "profile"
    const val Goal = "goal"
    const val Offer = "offer"
    const val Ride = "ride"
    const val Pay = "pay"
    const val Cash = "cash"
    const val Nfc = "nfc"
    const val Done = "done"
    const val Report = "report"
    const val Chat = "chat"
    const val Call = "call"
    const val Pause = "pause"
    const val Network = "network"
    const val Login = "login"
    const val Otp = "otp"
}

@HiltViewModel
class SessionViewModel @Inject constructor(store: TokenStore) : ViewModel() {
    val driverId: String = when {
        BuildConfig.DEV_BYPASS_LOGIN -> BuildConfig.DEV_DRIVER_ID
        else -> store.driverId() ?: "D-1"
    }
    val needsReactivate: Boolean =
        !BuildConfig.LOGIN_UI_ENABLED && !BuildConfig.DEV_BYPASS_LOGIN && store.token().isNullOrBlank()
}

@Composable
fun AppRoot(
    launches: StateFlow<Pair<String, Long>?>,
    session: SessionViewModel = hiltViewModel(),
    desk: DeskViewModel = hiltViewModel(),
    offers: RideOfferViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val request by launches.collectAsStateWithLifecycle()
    val offerUi by offers.state.collectAsStateWithLifecycle()
    val deskState by desk.state.collectAsStateWithLifecycle()
    var openedIncoming by remember { mutableStateOf(false) }
    LaunchedEffect(deskState.incomingCall) {
        if (deskState.incomingCall) {
            openedIncoming = true
            desk.dismissCall()
            navController.navigate(Routes.Call) { launchSingleTop = true }
        }
    }
    LaunchedEffect(offers) {
        offers.watch { desk.state.value.online }
    }
    LaunchedEffect(offerUi) {
        when (offerUi) {
            is OfferUi.Showing -> desk.noteOffer()
            is OfferUi.Accepted -> {
                offers.lastOffer?.let { offer ->
                    desk.openTrip(offer)
                    navController.navigate(Routes.Ride) { launchSingleTop = true }
                }
                desk.consumeOffer()
            }
            is OfferUi.Declined -> desk.noteDecline()
            else -> Unit
        }
    }
    val start = when {
        BuildConfig.LOGIN_UI_ENABLED -> Routes.Auth
        session.needsReactivate -> Routes.Reactivate
        else -> Routes.Home
    }
    val onTab: (DeskTab) -> Unit = { tab -> openTab(navController, desk, tab) }
    LaunchedEffect(request) {
        val screen = request?.first ?: return@LaunchedEffect
        openScreen(navController, desk, screen)
    }
    NavHost(navController = navController, startDestination = start) {
        composable(Routes.Auth) {
            if (start == Routes.Auth) StayInApp()
            AuthScreen(
                onAuthenticated = {
                    navController.navigate(Routes.Home) {
                        popUpTo(Routes.Auth) { inclusive = true }
                    }
                },
                onTab = onTab,
            )
        }
        composable(Routes.Login) {
            LoginLook(onSubmit = { _ -> navController.navigate(Routes.Otp) }, onTab = onTab)
        }
        composable(Routes.Otp) {
            OtpLook(initialCode = "4821", onSubmit = { _ -> }, onTab = onTab)
        }
        composable(Routes.Reactivate) {
            ReactivatePage(
                onReactivate = {
                    desk.reactivate()
                    navController.navigate(Routes.Home) {
                        popUpTo(Routes.Reactivate) { inclusive = true }
                    }
                },
                onBack = if (start == Routes.Reactivate) null else ({ navController.popBackStack() }),
            )
        }
        composable(Routes.Home) {
            if (start == Routes.Home) StayInApp()
            DriverHome(desk = desk, offers = offers, onTab = onTab)
        }
        composable(Routes.Earnings) { EarningsPage(onTab) }
        composable(Routes.Trips) { TripsPage(onTab) }
        composable(Routes.Profile) {
            ProfilePage(
                onGoal = { navController.navigate(Routes.Goal) },
                onTab = onTab,
                onHomeMap = { navController.navigate(Routes.Home) },
                onHomeGps = { desk.saveHomeFromGps() },
            )
        }
        composable(Routes.Goal) {
            GoalPage(onSave = { navController.popBackStack() }, onTab = onTab)
        }
        composable(Routes.Offer) {
            OfferPage(
                onAccept = {
                    desk.beginRide()
                    navController.navigate(Routes.Ride) {
                        popUpTo(Routes.Offer) { inclusive = true }
                    }
                },
                onDecline = {
                    desk.noteDecline()
                    if (!navController.popBackStack()) {
                        navController.navigate(Routes.Home) { launchSingleTop = true }
                    }
                },
                onTab = onTab,
            )
        }
        composable(Routes.Ride) {
            RidePage(
                desk = desk,
                onPay = {
                    desk.arriveDestination()
                    navController.navigate(Routes.Pay)
                },
                onHome = { navController.popBackStack(Routes.Home, inclusive = false) },
                onTab = onTab,
            )
        }
        composable(Routes.Pay) {
            val fare by desk.state.collectAsStateWithLifecycle()
            PayPage(
                amountAoa = fare.fareAoa,
                onCollect = {
                    desk.settle("REFERENCE")
                    navController.navigate(Routes.Done) { popUpTo(Routes.Ride) { inclusive = true } }
                },
                onCash = { navController.navigate(Routes.Cash) },
                onNfc = { navController.navigate(Routes.Nfc) },
                onBack = { navController.popBackStack() },
                onTab = onTab,
            )
        }
        composable(Routes.Cash) {
            val fare by desk.state.collectAsStateWithLifecycle()
            CashPage(
                amountAoa = fare.fareAoa,
                onReceived = {
                    desk.settle("CASH")
                    navController.navigate(Routes.Done) { popUpTo(Routes.Ride) { inclusive = true } }
                },
                onMulticaixa = { navController.popBackStack() },
                onNfc = { navController.navigate(Routes.Nfc) },
                onBack = { navController.popBackStack() },
                onTab = onTab,
            )
        }
        composable(Routes.Nfc) {
            val fare by desk.state.collectAsStateWithLifecycle()
            NfcPage(
                amountAoa = fare.fareAoa,
                onCharge = {
                    desk.settle("VISA")
                    navController.navigate(Routes.Done) { popUpTo(Routes.Ride) { inclusive = true } }
                },
                onMulticaixa = {
                    navController.navigate(Routes.Pay) {
                        popUpTo(Routes.Nfc) { inclusive = true }
                    }
                },
                onCash = { navController.navigate(Routes.Cash) },
                onBack = { navController.popBackStack() },
                onTab = onTab,
            )
        }
        composable(Routes.Done) {
            DonePage(
                onConclude = {
                    desk.conclude()
                    navController.popBackStack(Routes.Home, inclusive = false)
                },
                onTab = onTab,
            )
        }
        composable(Routes.Report) {
            ReportPage(onSend = { navController.popBackStack() }, onTab = onTab)
        }
        composable(Routes.Chat) {
            ChatPage(onClose = { navController.popBackStack() }, onTab = onTab)
        }
        composable(Routes.Call) {
            CallPage(
                rideId = deskState.rideId,
                incoming = openedIncoming,
                onClose = {
                    openedIncoming = false
                    navController.popBackStack()
                },
            )
        }
        composable(Routes.Pause) {
            PausePage(onClose = { navController.popBackStack() }, onTab = onTab)
        }
        composable(Routes.Network) {
            NetworkPage(
                onRetry = {
                    desk.retryLink()
                    navController.popBackStack(Routes.Home, inclusive = false)
                },
                onBack = { navController.popBackStack() },
                onTab = onTab,
            )
        }
    }
}

@Composable
private fun StayInApp() {
    val activity = LocalContext.current.findActivity()
    BackHandler {
        activity?.moveTaskToBack(true)
    }
}

private fun openTab(nav: NavHostController, desk: DeskViewModel, tab: DeskTab) {
    desk.selectTab(tab)
    nav.popBackStack(Routes.Home, inclusive = false)
    val route = when (tab) {
        DeskTab.Home -> return
        DeskTab.Earnings -> Routes.Earnings
        DeskTab.Trips -> Routes.Trips
        DeskTab.Profile -> Routes.Profile
    }
    nav.navigate(route) { launchSingleTop = true }
}

private fun openScreen(nav: NavHostController, desk: DeskViewModel, screen: String) {
    fun go(route: String) {
        if (nav.currentDestination?.route == route) return
        nav.navigate(route) { launchSingleTop = true }
    }
    when (screen) {
        "home", "offline", "sheet" -> {
            desk.showOffline()
            desk.selectTab(DeskTab.Home)
            nav.popBackStack(Routes.Home, inclusive = false)
        }
        "online" -> {
            desk.showOnline()
            desk.selectTab(DeskTab.Home)
            nav.popBackStack(Routes.Home, inclusive = false)
        }
        "nonetwork" -> {
            desk.selectTab(DeskTab.Home)
            go(Routes.Network)
        }
        "earnings" -> openTab(nav, desk, DeskTab.Earnings)
        "trips" -> openTab(nav, desk, DeskTab.Trips)
        "account", "profile" -> openTab(nav, desk, DeskTab.Profile)
        "goal" -> {
            desk.selectTab(DeskTab.Profile)
            go(Routes.Goal)
        }
        "offer", "details" -> {
            desk.selectTab(DeskTab.Home)
            go(Routes.Offer)
        }
        "pickup" -> {
            desk.step(RideStep.Pickup)
            go(Routes.Ride)
        }
        "waiting" -> {
            desk.step(RideStep.Waiting)
            go(Routes.Ride)
        }
        "riding" -> {
            desk.step(RideStep.Riding)
            go(Routes.Ride)
        }
        "pay" -> go(Routes.Pay)
        "other", "cash" -> go(Routes.Cash)
        "nfc" -> go(Routes.Nfc)
        "done" -> go(Routes.Done)
        "report" -> go(Routes.Report)
        "chat" -> go(Routes.Chat)
        "call" -> go(Routes.Call)
        "pause" -> go(Routes.Pause)
        "login" -> go(Routes.Login)
        "otp" -> go(Routes.Otp)
        "reactivate" -> go(Routes.Reactivate)
    }
}
