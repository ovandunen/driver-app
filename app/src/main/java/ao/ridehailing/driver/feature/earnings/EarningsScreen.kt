package ao.ridehailing.driver.feature.earnings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ao.ridehailing.driver.R
import ao.ridehailing.driver.core.money.AoaFormat

@Composable
fun EarningsScreen(stats: EarningsStats, onPeriod: (String) -> Unit, onBar: (String) -> Unit) {
    Column {
        Text(stringResource(R.string.earnings_title))
        Button(onClick = { onPeriod("day") }) { Text(stringResource(R.string.earnings_tab_day)) }
        Button(onClick = { onPeriod("week") }) { Text(stringResource(R.string.earnings_tab_week)) }
        Button(onClick = { onPeriod("month") }) { Text(stringResource(R.string.earnings_tab_month)) }
        Text(AoaFormat.format(stats.totalAoa))
        Text(stringResource(R.string.earnings_stat_trips) + " " + stats.trips)
        Text(stringResource(R.string.earnings_stat_online) + " " + stats.minutes)
        Text(AoaFormat.format(stats.perTrip))
        Text(AoaFormat.format(stats.perHour))
        Text(stringResource(R.string.earnings_commission, AoaFormat.format(-stats.commissionAoa)), color = Color(0xFFDC2626))
        Column(Modifier.width(24.dp).height(80.dp).clickable { onBar("R-1") }) {}
    }
}
