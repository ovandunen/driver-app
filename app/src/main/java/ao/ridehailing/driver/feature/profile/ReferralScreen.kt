package ao.ridehailing.driver.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ao.ridehailing.driver.R
import ao.ridehailing.driver.core.storage.ReferralEntity

@Composable
fun ReferralScreen(code: String, rows: List<ReferralEntity>, onShare: () -> Unit) {
    Column {
        Text(stringResource(R.string.referral_title))
        Text(code)
        Button(onClick = onShare) { Text(stringResource(R.string.referral_share)) }
        rows.forEach { Text(it.name + " " + it.rides) }
    }
}
