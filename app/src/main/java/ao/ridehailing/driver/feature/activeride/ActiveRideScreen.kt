package ao.ridehailing.driver.feature.activeride

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ao.ridehailing.driver.R
import ao.ridehailing.driver.feature.activeride.components.NavigationPanel

@Composable
fun ActiveRideScreen(
    rideId: String,
    driverId: String,
    now: String,
    eta: String,
    onExternal: () -> Unit,
    onChat: () -> Unit,
    onCall: () -> Unit,
    viewModel: ActiveRideViewModel = hiltViewModel(),
) {
    val stage by viewModel.stage.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(rideId) { viewModel.restore(rideId, driverId, now) }
    Column(Modifier.padding(24.dp)) {
        Text(stringResource(R.string.nav_active_ride))
        NavigationPanel(eta, onExternal)
        Button(onClick = onCall) { Text(stringResource(R.string.nav_call)) }
        Button(onClick = onChat) { Text(stringResource(R.string.nav_chat)) }
        val label = when (stage) {
            RideStage.EnRouteToPickup -> R.string.ride_arrived_pickup
            RideStage.AtPickup -> R.string.ride_confirm_board
            RideStage.PassengerBoarded, RideStage.EnRouteToDestination -> R.string.ride_confirm_destination
            else -> R.string.ride_done
        }
        Button(onClick = { viewModel.advance(now) }) { Text(stringResource(label)) }
        CancelPanel { reason, text -> viewModel.cancel(reason, text, now) }
    }
}
