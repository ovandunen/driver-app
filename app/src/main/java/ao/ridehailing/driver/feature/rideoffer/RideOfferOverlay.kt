package ao.ridehailing.driver.feature.rideoffer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ao.ridehailing.driver.R
import ao.ridehailing.driver.core.money.AoaFormat
import kotlinx.coroutines.delay

@Composable
fun RideOfferOverlay(
    viewModel: RideOfferViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showing = state as? OfferUi.Showing
    val paused = state as? OfferUi.Paused
    LaunchedEffect(showing?.offer?.rideId, showing?.seconds, paused?.seconds) {
        if (showing == null && paused == null) return@LaunchedEffect
        delay(1_000)
        viewModel.tick()
    }
    if (showing == null && paused == null) return
    BackHandler { }
    val offer = showing?.offer
    Column(modifier.fillMaxWidth().background(Color.White).padding(24.dp)) {
        Text(stringResource(R.string.ride_offer_title))
        Text(stringResource(R.string.ride_offer_seconds, showing?.seconds ?: (state as OfferUi.Paused).seconds))
        Text(stringResource(R.string.ride_offer_pickup))
        Text(offer?.pickupAddress.orEmpty())
        Text(stringResource(R.string.ride_offer_destination))
        Text(offer?.destinationAddress.orEmpty())
        Text(offer?.distanceKm?.toString().orEmpty())
        Text(offer?.durationMin?.toString().orEmpty())
        Text(stringResource(R.string.ride_offer_payout_label))
        Text(AoaFormat.format(offer?.payoutAoa ?: 0))
        Text(offer?.passengerRating?.toString().orEmpty())
        if (state is OfferUi.Paused) {
            Text(stringResource(R.string.offer_paused))
            Button(onClick = viewModel::resume) { Text(stringResource(R.string.offer_resume)) }
        }
        Row(Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = viewModel::decline, Modifier.weight(1f)) {
                Text(stringResource(R.string.ride_offer_decline))
            }
            Button(onClick = viewModel::accept, Modifier.weight(1f)) {
                Text(stringResource(R.string.ride_offer_accept))
            }
        }
    }
}
