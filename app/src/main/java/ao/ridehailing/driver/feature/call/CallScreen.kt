package ao.ridehailing.driver.feature.call

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ao.ridehailing.driver.R

@Composable
fun CallScreen(
    stage: CallStage,
    proxyNumber: String,
    onMute: () -> Unit,
    onEnd: () -> Unit,
    onShare: () -> Unit,
) {
    Column(Modifier.padding(24.dp)) {
        Text(stringResource(R.string.call_encrypted_label))
        Text(stringResource(R.string.call_status_connected))
        Text(stage.name)
        Text(proxyNumber)
        Button(onClick = onMute) { Text(stringResource(R.string.call_action_mute)) }
        Button(onClick = onEnd) { Text(stringResource(R.string.call_action_end)) }
        Button(onClick = onShare) { Text(stringResource(R.string.call_action_location)) }
        Text(stringResource(R.string.call_privacy_note))
    }
}
