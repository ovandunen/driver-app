package ao.ridehailing.driver.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ao.ridehailing.driver.BuildConfig
import ao.ridehailing.driver.R
import ao.ridehailing.driver.feature.home.components.HeatmapOverlay

@Composable
fun HomeScreen(
    driverId: String,
    zones: List<ao.ridehailing.driver.feature.home.components.DemandZone> = emptyList(),
    onZone: (String) -> Unit = {},
    onOpen: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val online = state is HomeState.Online || state is HomeState.GoingOnline
    Column(Modifier.fillMaxSize()) {
        if (BuildConfig.DEV_BYPASS_LOGIN) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFC107))
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
            Text(
                "${BuildConfig.DEV_DRIVER_NAME} · 4.8 · LD-0000-XX · Dev Car",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }
        Column(Modifier.padding(24.dp)) {
            Text(if (online) stringResource(R.string.home_online_status) else stringResource(R.string.home_offline_title))
            Switch(
                checked = online,
                onCheckedChange = { checked ->
                    if (checked) viewModel.goOnline(driverId) else viewModel.goOffline(driverId)
                },
            )
            if (state is HomeState.Error) Text((state as HomeState.Error).message)
            if (online) HeatmapOverlay(zones, onZone)
            Button(onClick = { onOpen("onboarding") }) { Text(stringResource(R.string.onboarding_documents)) }
            Button(onClick = { onOpen("earnings") }) { Text(stringResource(R.string.earnings_title)) }
            Button(onClick = { onOpen("wallet") }) { Text(stringResource(R.string.wallet_title)) }
            Button(onClick = { onOpen("referral") }) { Text(stringResource(R.string.referral_title)) }
        }
    }
}
