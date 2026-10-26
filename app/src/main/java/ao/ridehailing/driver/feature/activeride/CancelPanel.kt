package ao.ridehailing.driver.feature.activeride

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import ao.ridehailing.driver.R

@Composable
fun CancelPanel(onCancel: (String, String) -> Unit) {
    var reason by rememberSaveable { mutableStateOf("") }
    var text by rememberSaveable { mutableStateOf("") }
    Column {
        Text(stringResource(R.string.cancel_title))
        Button(onClick = { reason = "passenger_no_show" }) { Text(stringResource(R.string.cancel_no_show)) }
        Button(onClick = { reason = "wrong_pickup" }) { Text(stringResource(R.string.cancel_wrong_pickup)) }
        Button(onClick = { reason = "vehicle_issue" }) { Text(stringResource(R.string.cancel_vehicle)) }
        Button(onClick = { reason = "passenger_changed_mind" }) { Text(stringResource(R.string.cancel_changed_mind)) }
        Button(onClick = { reason = "other" }) { Text(stringResource(R.string.cancel_other)) }
        OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text(stringResource(R.string.cancel_other)) })
        Button(onClick = { onCancel(reason, text) }) { Text(stringResource(R.string.cancel_title)) }
    }
}
