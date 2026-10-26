package ao.ridehailing.driver.feature.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ao.ridehailing.driver.R

@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state) {
        if (state is AuthState.Authenticated) onAuthenticated()
    }
    val phoneKnown = when (val current = state) {
        is AuthState.OtpSent -> current.phone
        is AuthState.Verifying -> current.phone
        is AuthState.Error -> current.phone
        else -> null
    }
    val showOtp = phoneKnown != null
    if (showOtp) {
        OtpStep(
            phone = phoneKnown.orEmpty(),
            state = state,
            onVerify = viewModel::verifyOtp,
            onChangeNumber = viewModel::editPhone,
        )
    } else {
        PhoneStep(
            state = state,
            onSend = viewModel::sendOtp,
        )
    }
}

@Composable
private fun PhoneStep(
    state: AuthState,
    onSend: (String) -> Unit,
) {
    var phone by rememberSaveable { mutableStateOf("") }
    val busy = state is AuthState.SendingOtp
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text(text = stringResource(R.string.auth_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it.filter { char -> char.isDigit() }.take(9) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.auth_phone_hint)) },
            prefix = { Text(stringResource(R.string.auth_phone_prefix)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            enabled = !busy,
        )
        ErrorText(state)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onSend(phone) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy,
        ) {
            Text(stringResource(R.string.auth_send_otp))
        }
        if (busy) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun OtpStep(
    phone: String,
    state: AuthState,
    onVerify: (String) -> Unit,
    onChangeNumber: () -> Unit,
) {
    var code by rememberSaveable { mutableStateOf("") }
    val busy = state is AuthState.Verifying
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text(text = stringResource(R.string.auth_otp_title), style = MaterialTheme.typography.headlineMedium)
        if (phone.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.auth_otp_sent, phone))
        }
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = code,
            onValueChange = { code = it.filter { char -> char.isDigit() }.take(4) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.auth_otp_hint)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            enabled = !busy,
        )
        ErrorText(state)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onVerify(code) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy,
        ) {
            Text(stringResource(R.string.auth_verify))
        }
        TextButton(onClick = onChangeNumber, enabled = !busy) {
            Text(stringResource(R.string.auth_change_number))
        }
        if (busy) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun ErrorText(state: AuthState) {
    val error = state as? AuthState.Error ?: return
    val message = when (error.reason) {
        AuthState.Error.Reason.InvalidPhone -> stringResource(R.string.auth_error_invalid_phone)
        AuthState.Error.Reason.InvalidCode -> stringResource(R.string.auth_error_invalid_code)
        AuthState.Error.Reason.RequestFailed -> stringResource(R.string.auth_error_request)
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(text = message, color = MaterialTheme.colorScheme.error)
}
