package ao.ridehailing.driver.feature.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import ao.ridehailing.driver.core.storage.ChatMessageEntity

@Composable
fun ChatScreen(
    lines: List<ChatMessageEntity>,
    onSend: (String) -> Unit,
    onPredefined: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    Column {
        val atPickup = stringResource(R.string.chat_at_pickup)
        val late = stringResource(R.string.chat_late)
        lines.forEach { Text(it.text) }
        Row {
            Button(onClick = { onPredefined(atPickup) }) { Text(atPickup) }
            Button(onClick = { onPredefined(late) }) { Text(late) }
        }
        OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text(stringResource(R.string.nav_chat)) })
        Button(onClick = { onSend(text) }) { Text(stringResource(R.string.chat_send)) }
    }
}
