package ao.ridehailing.driver.feature.activeride.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ao.ridehailing.driver.R

@Composable
fun NavigationPanel(eta: String, onExternal: () -> Unit) {
    Column {
        Text(stringResource(R.string.nav_eta, eta))
        Button(onClick = onExternal) { Text(stringResource(R.string.nav_external)) }
    }
}
