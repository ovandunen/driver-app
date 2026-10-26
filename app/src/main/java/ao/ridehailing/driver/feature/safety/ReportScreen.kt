package ao.ridehailing.driver.feature.safety

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ao.ridehailing.driver.R

@Composable
fun ReportScreen(submitted: Boolean, onSubmit: () -> Unit) {
    Column {
        Text(stringResource(R.string.report_title))
        if (submitted) {
            Text(stringResource(R.string.report_send))
        } else {
            Button(onClick = onSubmit) { Text(stringResource(R.string.report_send)) }
        }
    }
}
