package ao.ridehailing.driver.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    Column(Modifier.fillMaxSize().padding(24.dp)) {
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
