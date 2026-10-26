package ao.ridehailing.driver.feature.wallet

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ao.ridehailing.driver.R
import ao.ridehailing.driver.core.money.AoaFormat
import ao.ridehailing.driver.core.storage.PayoutEntity

@Composable
fun WalletScreen(rows: List<PayoutEntity>, notice: String?, onInstant: () -> Unit, onWeekly: () -> Unit) {
    Column {
        Text(stringResource(R.string.wallet_title))
        Button(onClick = onInstant) { Text(stringResource(R.string.payout_instant)) }
        Button(onClick = onWeekly) { Text(stringResource(R.string.payout_weekly)) }
        notice?.let { Text(it) }
        rows.forEach { Text(it.kind + " " + AoaFormat.format(it.amountAoa)) }
    }
}
