package ao.ridehailing.driver.feature.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ao.ridehailing.driver.BuildConfig
import ao.ridehailing.driver.R
import ao.ridehailing.driver.core.money.AoaFormat
import ao.ridehailing.driver.feature.home.components.DemandZone
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Teal = Color(0xFF1B6B7A)
private val Ink = Color(0xFF0F172A)
private val Muted = Color(0xFF94A3B8)
private val Slate = Color(0xFF475569)
private val Line = Color(0xFFE2E8F0)
private val Canvas = Color(0xFFF8FAFB)
private val Card = Color.White
private val Success = Color(0xFF10B981)
private val SuccessInk = Color(0xFF065F46)
private val SuccessBg = Color(0xFFECFDF5)
private val MapBlue = Color(0xFFEFF6FF)
private val Amber = Color(0xFFFFC107)
private val DailyGoalAoa = 80_000L

@Composable
fun HomeScreen(
    driverId: String,
    zones: List<DemandZone> = emptyList(),
    onZone: (String) -> Unit = {},
    onOpen: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val online = state is HomeState.Online || state is HomeState.GoingOnline
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted[Manifest.permission.ACCESS_FINE_LOCATION] == true) viewModel.goOnline(driverId)
    }
    val name = if (BuildConfig.DEV_BYPASS_LOGIN) BuildConfig.DEV_DRIVER_NAME else driverId
    val vehicle = if (BuildConfig.DEV_BYPASS_LOGIN) "Dev Car · LD-0000-XX" else driverId
    DriverScaffold(selected = "home", onOpen = onOpen) {
        if (BuildConfig.DEV_BYPASS_LOGIN) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Amber)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "DEV MODE — LOGIN BYPASSED (driver ${BuildConfig.DEV_DRIVER_ID})",
                    color = Color.Black,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            DevScreenLinks(onOpen)
        }
        HomeHeader(
            name = name,
            vehicle = vehicle,
            online = online,
            busy = state is HomeState.GoingOnline,
            onToggle = { checked ->
                if (!checked) {
                    viewModel.goOffline(driverId)
                } else if (
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    viewModel.goOnline(driverId)
                } else {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                }
            },
        )
        if (state is HomeState.Error) {
            Text(
                (state as HomeState.Error).message,
                color = Color(0xFFDC2626),
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        if (online) {
            OnlineMap(zones, onZone, Modifier.weight(1f))
        } else {
            OfflineEmpty(Modifier.weight(1f))
        }
        EarningsCard()
    }
}

@Composable
fun TripsScreen(onOpen: (String) -> Unit) {
    DriverScaffold(selected = "trips", onOpen = onOpen) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("📋", fontSize = 36.sp)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.trips_empty_title), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.trips_empty_body),
                color = Slate,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun ProfileScreen(
    onOpen: (String) -> Unit,
) {
    val name = if (BuildConfig.DEV_BYPASS_LOGIN) BuildConfig.DEV_DRIVER_NAME else ""
    val vehicle = if (BuildConfig.DEV_BYPASS_LOGIN) "Dev Car · LD-0000-XX" else ""
    DriverScaffold(selected = "profile", onOpen = onOpen) {
        Column(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Card)
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Avatar(name, 72.dp)
                Spacer(Modifier.height(12.dp))
                Text(name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("★ 4.8", color = Muted, fontSize = 12.sp)
            }
            SectionLabel(stringResource(R.string.profile_vehicle))
            ProfileRow("🚗", vehicle, "")
            SectionLabel(stringResource(R.string.wallet_title))
            ProfileRow("💰", stringResource(R.string.wallet_title), stringResource(R.string.payout_weekly), onClick = { onOpen("wallet") })
            SectionLabel(stringResource(R.string.profile_preferences))
            ProfileRow("📄", stringResource(R.string.onboarding_documents), "", onClick = { onOpen("onboarding") })
            ProfileRow("🎁", stringResource(R.string.referral_title), "", onClick = { onOpen("referral") })
        }
    }
}

@Composable
fun DriverScaffold(
    selected: String,
    onOpen: (String) -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Canvas)
            .statusBarsPadding(),
    ) {
        content()
        BottomNav(selected, onOpen)
    }
}

@Composable
private fun DevScreenLinks(onOpen: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .background(Card)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            "Login" to "auth",
            "Offer" to "rideOffer",
            "Ride" to "activeRide",
            "Call" to "call",
            "Chat" to "chat",
            "Report" to "report",
        ).forEach { (label, route) ->
            Text(
                label,
                color = Teal,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MapBlue)
                    .clickable { onOpen(route) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun HomeHeader(
    name: String,
    vehicle: String,
    online: Boolean,
    busy: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Card)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(name, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            if (online) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SuccessBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Success))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.home_online_status), color = SuccessInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(stringResource(R.string.home_online_sub), color = Muted, fontSize = 12.sp)
            } else {
                Text(name, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("★ 4.8", color = Color(0xFFF59E0B), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(vehicle, color = Muted, fontSize = 12.sp)
            }
        }
        OnlineToggle(checked = online, enabled = !busy, onToggle = onToggle)
    }
}

@Composable
private fun Avatar(name: String, size: androidx.compose.ui.unit.Dp) {
    val initials = name.split(" ")
        .mapNotNull { part -> part.firstOrNull()?.uppercaseChar() }
        .take(2)
        .joinToString("")
        .ifBlank { "D" }
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Teal),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = if (size > 48.dp) 24.sp else 14.sp)
    }
}

@Composable
private fun OnlineToggle(checked: Boolean, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Box(
        Modifier
            .width(52.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (checked) Success else Muted)
            .clickable(enabled = enabled) { onToggle(!checked) },
    ) {
        Box(
            Modifier
                .padding(3.dp)
                .size(24.dp)
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Composable
private fun PowerMark() {
    Canvas(Modifier.size(48.dp)) {
        val stroke = 3.dp.toPx()
        drawArc(
            color = Muted,
            startAngle = 40f,
            sweepAngle = 280f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawLine(
            color = Muted,
            start = Offset(size.width / 2f, size.height * 0.12f),
            end = Offset(size.width / 2f, size.height * 0.48f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun OfflineEmpty(modifier: Modifier) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PowerMark()
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.home_offline_title), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.home_offline_hint),
            color = Slate,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )
    }
}

@Composable
private fun OnlineMap(zones: List<DemandZone>, onZone: (String) -> Unit, modifier: Modifier) {
    Box(modifier.fillMaxWidth().background(MapBlue)) {
        Text(
            stringResource(R.string.home_map_label),
            modifier = Modifier.align(Alignment.Center),
            color = Muted,
            fontSize = 12.sp,
        )
        Column(Modifier.align(Alignment.TopStart).padding(16.dp)) {
            zones.forEach { zone ->
                Text(
                    zone.label,
                    color = Color(0xFF991B1B),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                        .clickable { onZone(zone.label) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun EarningsCard() {
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))
    Column(
        Modifier
            .fillMaxWidth()
            .background(Card, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                stringResource(R.string.home_today_label),
                color = Muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
            )
            Text(date, color = Muted, fontSize = 11.sp)
        }
        Text(AoaFormat.format(0), color = Ink, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Text(stringResource(R.string.home_stat_line, 0, "0h"), color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.home_goal_label, AoaFormat.format(DailyGoalAoa)), color = Slate, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Line),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = Muted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp),
    )
}

@Composable
private fun ProfileRow(icon: String, title: String, subtitle: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Card)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, fontSize = 18.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle.isNotEmpty()) Text(subtitle, color = Muted, fontSize = 12.sp)
        }
        if (onClick != null) Text("›", color = Muted, fontSize = 18.sp)
    }
}

@Composable
private fun BottomNav(selected: String, onOpen: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Card)
            .navigationBarsPadding()
            .height(64.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavItem("🏠", stringResource(R.string.nav_home), selected == "home") { onOpen("home") }
        NavItem("💰", stringResource(R.string.earnings_title), selected == "earnings") { onOpen("earnings") }
        NavItem("📋", stringResource(R.string.nav_trips), selected == "trips") { onOpen("trips") }
        NavItem("👤", stringResource(R.string.nav_profile), selected == "profile") { onOpen("profile") }
    }
}

@Composable
private fun NavItem(icon: String, label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(icon, fontSize = 16.sp)
        Text(
            label,
            color = if (active) Teal else Muted,
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
