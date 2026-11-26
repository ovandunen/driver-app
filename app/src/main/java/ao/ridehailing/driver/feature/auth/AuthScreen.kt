package ao.ridehailing.driver.feature.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ao.ridehailing.driver.R
import ao.ridehailing.driver.core.ui.CardWhite
import ao.ridehailing.driver.core.ui.DeskFrame
import ao.ridehailing.driver.core.ui.DeskTab
import ao.ridehailing.driver.core.ui.Ink
import ao.ridehailing.driver.core.ui.Muted
import ao.ridehailing.driver.core.ui.Red
import ao.ridehailing.driver.core.ui.Teal

@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    onTab: (DeskTab) -> Unit,
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
    val error = (state as? AuthState.Error)?.let { authError(it.reason) }
    if (phoneKnown != null) {
        OtpLook(
            phoneLabel = prettyPhone(phoneKnown),
            onSubmit = viewModel::verifyOtp,
            onTab = onTab,
            error = error,
        )
    } else {
        LoginLook(
            onSubmit = viewModel::sendOtp,
            onTab = onTab,
            busy = state is AuthState.SendingOtp,
            error = error,
        )
    }
}

@Composable
fun LoginLook(
    onSubmit: (String) -> Unit,
    onTab: (DeskTab) -> Unit,
    busy: Boolean = false,
    error: String? = null,
) {
    var digits by rememberSaveable { mutableStateOf("912345678") }
    DeskFrame(
        title = "Autenticação",
        subtitle = "Entrar",
        initials = "R",
        action = "OTP",
        onAction = { if (!busy) onSubmit(digits) },
        tab = DeskTab.Home,
        onTab = onTab,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardWhite)
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Teal),
                    contentAlignment = Alignment.Center,
                ) {
                    ArrowMark()
                }
                Spacer(Modifier.height(16.dp))
                Text("Ride Luanda", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text("App do Motorista", color = Muted, fontSize = 16.sp)
                Spacer(Modifier.height(22.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AngolaFlag()
                    Text("  +244", color = Ink, fontSize = 16.sp)
                    Text("   |   ", color = Muted, fontSize = 16.sp)
                    BasicTextField(
                        value = groupPhone(digits),
                        onValueChange = { digits = it.filter(Char::isDigit).take(9) },
                        textStyle = TextStyle(color = Ink, fontSize = 16.sp),
                        cursorBrush = SolidColor(Teal),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(error, color = Red, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun OtpLook(
    onSubmit: (String) -> Unit,
    onTab: (DeskTab) -> Unit,
    phoneLabel: String = "+244 912 345 678",
    initialCode: String = "",
    error: String? = null,
) {
    var code by rememberSaveable { mutableStateOf(initialCode.filter(Char::isDigit).take(6)) }
    DeskFrame(
        title = "Autenticação",
        subtitle = "Verificar código",
        action = "6 dígitos",
        onAction = { onSubmit(code) },
        tab = DeskTab.Home,
        onTab = onTab,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Text("Verificar código", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Enviámos um código para", color = Color(0xFF738292), fontSize = 16.sp)
            Text(phoneLabel, color = Color(0xFF738292), fontSize = 16.sp)
            Spacer(Modifier.height(20.dp))
            Box(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(6) { index ->
                        val char = code.getOrNull(index)?.toString() ?: "–"
                        val filled = index < code.length
                        Box(
                            Modifier
                                .weight(1f)
                                .heightIn(min = 64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (filled) Color(0xFFE7F4F5) else CardWhite)
                                .border(
                                    1.5.dp,
                                    if (filled) Teal else Color(0xFFDCE4E8),
                                    RoundedCornerShape(12.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(char, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                BasicTextField(
                    value = code,
                    onValueChange = { code = it.filter(Char::isDigit).take(6) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    textStyle = TextStyle(color = Color.Transparent, fontSize = 16.sp),
                    cursorBrush = SolidColor(Color.Transparent),
                    modifier = Modifier.matchParentSize(),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Reenviar código em 0:47",
                color = Color(0xFF8996A3),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error, color = Red, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun authError(reason: AuthState.Error.Reason): String = when (reason) {
    AuthState.Error.Reason.InvalidPhone -> stringResource(R.string.auth_error_invalid_phone)
    AuthState.Error.Reason.InvalidCode -> stringResource(R.string.auth_error_invalid_code)
    AuthState.Error.Reason.RequestFailed -> stringResource(R.string.auth_error_request)
}

private fun groupPhone(digits: String): String = digits.chunked(3).joinToString(" ")

private fun prettyPhone(phone: String): String {
    val national = phone.filter(Char::isDigit).takeLast(9)
    return "+244 ${groupPhone(national)}"
}

@Composable
private fun AngolaFlag() {
    Canvas(Modifier.width(22.dp).height(16.dp)) {
        drawRect(Color(0xFFCC0000), size = androidx.compose.ui.geometry.Size(size.width, size.height / 2f))
        drawRect(
            Color(0xFF111111),
            topLeft = Offset(0f, size.height / 2f),
            size = androidx.compose.ui.geometry.Size(size.width, size.height / 2f),
        )
        drawCircle(Color(0xFFFFCC00), radius = size.minDimension * 0.22f, center = center)
    }
}

@Composable
private fun ArrowMark() {
    Canvas(Modifier.size(32.dp)) {
        val stroke = 3.dp.toPx()
        drawLine(Color.White, Offset(size.width * 0.2f, size.height * 0.72f), Offset(size.width * 0.72f, size.height * 0.22f), stroke)
        drawLine(Color.White, Offset(size.width * 0.42f, size.height * 0.22f), Offset(size.width * 0.72f, size.height * 0.22f), stroke)
        drawLine(Color.White, Offset(size.width * 0.72f, size.height * 0.22f), Offset(size.width * 0.72f, size.height * 0.52f), stroke)
    }
}
