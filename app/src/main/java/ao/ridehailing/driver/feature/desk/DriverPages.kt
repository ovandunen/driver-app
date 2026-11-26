package ao.ridehailing.driver.feature.desk

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ao.ridehailing.driver.core.money.PilotLedger
import ao.ridehailing.driver.phase1.EarningsReadout
import ao.ridehailing.driver.feature.rideoffer.RideOfferOverlay
import ao.ridehailing.driver.feature.rideoffer.RideOfferViewModel
import ao.ridehailing.driver.core.ui.Amber
import ao.ridehailing.driver.core.ui.Avatar
import ao.ridehailing.driver.core.ui.BigButton
import ao.ridehailing.driver.core.ui.Blue
import ao.ridehailing.driver.core.ui.CanvasBg
import ao.ridehailing.driver.core.ui.CardWhite
import ao.ridehailing.driver.core.ui.CityMap
import ao.ridehailing.driver.core.ui.ClockGlyph
import ao.ridehailing.driver.core.ui.DeskFrame
import ao.ridehailing.driver.core.ui.DeskTab
import ao.ridehailing.driver.core.ui.Green
import ao.ridehailing.driver.core.ui.GreenInk
import ao.ridehailing.driver.core.ui.Ink
import ao.ridehailing.driver.core.ui.LightBars
import ao.ridehailing.driver.core.ui.Muted
import ao.ridehailing.driver.core.ui.PowerGlyph
import ao.ridehailing.driver.core.ui.Red
import ao.ridehailing.driver.core.ui.SoftIcon
import ao.ridehailing.driver.core.ui.Teal
import ao.ridehailing.driver.core.ui.findActivity
import ao.ridehailing.driver.core.ui.kz

private const val DailyGoal = 80_000L
private val GoalPresets = longArrayOf(40_000L, 80_000L, 120_000L)

@Composable
fun DriverHome(
    desk: DeskViewModel,
    offers: RideOfferViewModel,
    onTab: (DeskTab) -> Unit,
) {
    val state by desk.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val askLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted[Manifest.permission.ACCESS_FINE_LOCATION] == true) desk.goOnline() else desk.denyLocation()
    }
    val goOnline = {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!desk.needsGps() || granted) desk.goOnline() else askLocation.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
        )
    }
    Box(Modifier.fillMaxSize()) {
        if (state.online) {
            OnlineHome(
                onToggle = { desk.goOffline() },
                tab = state.tab,
                onTab = onTab,
                lat = state.lat,
                lng = state.lng,
                earned = if (state.paid) state.fareAoa else 0L,
            )
            RideOfferOverlay(viewModel = offers, modifier = Modifier.align(Alignment.BottomCenter))
        } else {
            OfflineHome(onToggle = goOnline, tab = state.tab, onTab = onTab)
        }
        if (state.linkDown) {
            Text(
                "Sem ligação MQTT. Inicie a app com mqtt_host e mqtt_port.",
                color = Red,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp, start = 24.dp, end = 24.dp)
                    .background(Color.White)
                    .padding(12.dp),
            )
        }
        if (state.noLocation) {
            Text(
                "Sem permissão de localização. A sua posição não será enviada.",
                color = Red,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 96.dp, start = 24.dp, end = 24.dp)
                    .background(Color.White)
                    .padding(12.dp),
            )
        }
    }
}

@Composable
private fun OfflineHome(onToggle: () -> Unit, tab: DeskTab, onTab: (DeskTab) -> Unit) {
    DeskFrame(
        title = "Disponibilidade",
        subtitle = "Início · Offline",
        action = "",
        onAction = {},
        tab = tab,
        onTab = onTab,
        showAction = false,
        onAvailability = onToggle,
        available = false,
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F0F2))
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center,
            ) {
                PowerGlyph(Teal, Modifier.size(48.dp))
            }
            Spacer(Modifier.height(28.dp))
            Text("Está Offline", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Ative para receber pedidos", color = Muted, fontSize = 16.sp)
            Spacer(Modifier.height(20.dp))
            Text("Kz 0,00 hoje · 0 corridas · 0 min online", color = Ink, fontSize = 16.sp)
        }
    }
}

@Composable
private fun OnlineHome(
    onToggle: () -> Unit,
    tab: DeskTab,
    onTab: (DeskTab) -> Unit,
    lat: Double,
    lng: Double,
    earned: Long,
) {
    DeskFrame(
        title = "Online",
        subtitle = "À procura de corridas…",
        action = "",
        onAction = {},
        tab = tab,
        onTab = onTab,
        showAction = false,
        onAvailability = onToggle,
        available = true,
    ) {
        Box(Modifier.fillMaxSize()) {
            CityMap(Modifier.fillMaxSize(), lat, lng)
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardWhite)
                    .padding(16.dp),
            ) {
                Text("${kz(earned)} hoje · 0 corridas", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Meta diária ${kz(DailyGoal)}", color = Muted, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                val fraction = (earned.toFloat() / DailyGoal.toFloat()).coerceIn(0f, 1f)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE8F0F2)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(fraction)
                            .height(8.dp)
                            .background(Teal),
                    )
                }
            }
        }
    }
}

@Composable
fun OfferPage(onAccept: () -> Unit, onDecline: () -> Unit, onTab: (DeskTab) -> Unit) {
    BackHandler(onBack = onDecline)
    val payout = kz(PilotLedger.net)
    DeskFrame(
        title = "Novo pedido · 15 s",
        subtitle = "Oferta de corrida",
        action = payout,
        onAction = onAccept,
        tab = DeskTab.Home,
        onTab = onTab,
        accent = Amber,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Nova corrida", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(Color(0xFFFFF1EC))
                        drawCircle(color = Color(0xFFE16B58), style = Stroke(3.dp.toPx()))
                    }
                    Text("9s", color = Color(0xFFD84A4A), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Spacer(Modifier.height(18.dp))
            RouteStop(green = true, label = "EMBARQUE", title = PilotLedger.PICKUP, detail = "2,1 km · 6 min", drawLine = true)
            RouteStop(green = false, label = "DESTINO", title = PilotLedger.DEST_FULL, detail = null, drawLine = false)
            Spacer(Modifier.height(16.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFEAF8F2))
                    .border(1.dp, Color(0xFFB9EAD5), RoundedCornerShape(16.dp))
                    .padding(vertical = 16.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("O SEU PAGAMENTO FIXO", color = Color(0xFF16825C), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(payout, color = GreenInk, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text(
                "Recusar não afeta a sua avaliação",
                color = Color(0xFF72818E),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(onClick = onDecline)
                    .padding(vertical = 14.dp),
            )
        }
    }
}

@Composable
private fun RouteStop(green: Boolean, label: String, title: String, detail: String?, drawLine: Boolean) {
    Row {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(16.dp)
                    .border(3.dp, if (green) Color(0xFF19A66F) else Red, CircleShape),
            )
            if (drawLine) {
                Box(Modifier.width(2.dp).height(if (detail == null) 28.dp else 46.dp).background(Color(0xFFCFD9DE)))
            }
        }
        Column(Modifier.padding(start = 12.dp, bottom = 8.dp)) {
            Text(label, color = Color(0xFF8996A3), fontSize = 16.sp)
            Text(title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (detail != null) Text(detail, color = Color(0xFF71808E), fontSize = 16.sp)
        }
    }
}

@Composable
fun RidePage(
    desk: DeskViewModel,
    onPay: () -> Unit,
    onHome: () -> Unit,
    onTab: (DeskTab) -> Unit,
) {
    val state by desk.state.collectAsStateWithLifecycle()
    BackHandler {
        when (state.step) {
            RideStep.Waiting -> desk.step(RideStep.Pickup)
            RideStep.Riding -> desk.step(RideStep.Waiting)
            RideStep.Pickup -> onHome()
        }
    }
    when (state.step) {
        RideStep.Pickup -> MapSheet(
            title = state.pickupAddress.ifBlank { "Embarque" },
            subtitle = "A caminho do passageiro",
            sheetTitle = state.pickupAddress.ifBlank { "Embarque" },
            point = pointLabel(state.pickupLat, state.pickupLng),
            lat = state.pickupLat,
            lng = state.pickupLng,
            action = "Cheguei ao embarque",
            actionColor = Teal,
            onAction = { desk.arrivePickup() },
            onTab = onTab,
        )
        RideStep.Waiting -> WaitingPage(
            title = "À espera do passageiro",
            subtitle = state.pickupAddress.ifBlank { "No embarque" },
            onAdvance = { desk.startWithPassenger() },
            onTab = onTab,
        )
        RideStep.Riding -> MapSheet(
            title = state.destinationAddress.ifBlank { "Destino" },
            subtitle = "A caminho do destino",
            sheetTitle = state.destinationAddress.ifBlank { "Destino" },
            point = pointLabel(state.destinationLat, state.destinationLng),
            lat = state.destinationLat,
            lng = state.destinationLng,
            action = "Cheguei ao destino",
            actionColor = Teal,
            onAction = onPay,
            onTab = onTab,
        )
    }
}

@Composable
private fun pointLabel(lat: Double, lng: Double): String =
    if (lat == 0.0 && lng == 0.0) "" else String.format(java.util.Locale.US, "%.5f, %.5f", lat, lng)

@Composable
private fun MapSheet(
    title: String,
    subtitle: String,
    sheetTitle: String,
    point: String = "",
    lat: Double = -8.8390,
    lng: Double = 13.2894,
    action: String,
    actionColor: Color,
    onAction: () -> Unit,
    onTab: (DeskTab) -> Unit,
) {
    DeskFrame(
        title = title,
        subtitle = subtitle,
        action = action,
        onAction = onAction,
        tab = DeskTab.Home,
        onTab = onTab,
        actionColor = actionColor,
        showAction = false,
    ) {
        Box(Modifier.fillMaxSize()) {
            CityMap(
                Modifier.fillMaxSize(),
                if (lat == 0.0 && lng == 0.0) -8.8390 else lat,
                if (lat == 0.0 && lng == 0.0) 13.2894 else lng,
            )
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(CardWhite)
                    .padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 12.dp),
            ) {
                Text(sheetTitle, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                if (point.isNotBlank()) Text(point, color = Color(0xFF71808E), fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                BigButton(action, onAction, actionColor)
            }
        }
    }
}

@Composable
private fun WaitingPage(title: String, subtitle: String, onAdvance: () -> Unit, onTab: (DeskTab) -> Unit) {
    DeskFrame(
        title = title,
        subtitle = subtitle,
        action = "Iniciar viagem",
        onAction = onAdvance,
        tab = DeskTab.Home,
        onTab = onTab,
        accent = Amber,
    ) {
        TimerBody(
            title = "Passageiro no embarque",
            subtitle = "Inicie a viagem para seguir ao destino",
            time = "4:32",
        )
    }
}

@Composable
fun PausePage(onClose: () -> Unit, onTab: (DeskTab) -> Unit) {
    BackHandler(onBack = onClose)
    DeskFrame(
        title = "Ofertas pausadas",
        subtitle = "Pausa automática",
        action = "1:47",
        onAction = onClose,
        tab = DeskTab.Home,
        onTab = onTab,
        accent = Amber,
    ) {
        TimerBody(
            title = "Pausa Automática",
            subtitle = "A sua avaliação não foi afetada",
            time = "1:47",
        )
    }
}

@Composable
private fun TimerBody(title: String, subtitle: String, time: String) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SoftIcon(Color(0xFFFFF5DF)) { ClockGlyph(Amber) }
        Spacer(Modifier.height(22.dp))
        Text(title, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = Muted, fontSize = 16.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Text(time, color = Amber, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun PayPage(onCollect: () -> Unit, onCash: () -> Unit, onNfc: () -> Unit, onBack: () -> Unit, onTab: (DeskTab) -> Unit, amountAoa: Long = 0) {
    BackHandler(onBack = onBack)
    PayLayout(
        title = "Fim da corrida",
        subtitle = "Cobrar pagamento",
        total = kz(if (amountAoa > 0) amountAoa else PilotLedger.GROSS),
        accent = Green,
        selected = PayMethod.Multicaixa,
        noteTitle = "Referência válida por 30 minutos",
        noteBody = "${PilotLedger.PASSENGER} · 14,3 km",
        noteColor = Color(0xFF916516),
        noteBg = Color(0xFFFFF8E7),
        action = kz(if (amountAoa > 0) amountAoa else PilotLedger.GROSS),
        onAction = onCollect,
        onPick = { method ->
            when (method) {
                PayMethod.Cash -> onCash()
                PayMethod.Nfc -> onNfc()
                PayMethod.Multicaixa -> Unit
            }
        },
        onTab = onTab,
    )
}

@Composable
fun CashPage(onReceived: () -> Unit, onMulticaixa: () -> Unit, onNfc: () -> Unit, onBack: () -> Unit, onTab: (DeskTab) -> Unit, amountAoa: Long = 0) {
    BackHandler(onBack = onBack)
    PayLayout(
        title = "Receção em mãos",
        subtitle = "Pagamento · Dinheiro",
        total = kz(if (amountAoa > 0) amountAoa else PilotLedger.GROSS),
        accent = Green,
        selected = PayMethod.Cash,
        noteTitle = "Comissão deduzida no próximo pagamento",
        noteBody = "Comissão deduzida no próximo pagamento",
        noteColor = Color(0xFF916516),
        noteBg = Color(0xFFFFF8E7),
        action = kz(if (amountAoa > 0) amountAoa else PilotLedger.GROSS),
        onAction = onReceived,
        onPick = { method ->
            when (method) {
                PayMethod.Multicaixa -> onMulticaixa()
                PayMethod.Nfc -> onNfc()
                PayMethod.Cash -> Unit
            }
        },
        onTab = onTab,
    )
}

@Composable
fun NfcPage(onCharge: () -> Unit, onMulticaixa: () -> Unit, onCash: () -> Unit, onBack: () -> Unit, onTab: (DeskTab) -> Unit, amountAoa: Long = 0) {
    BackHandler(onBack = onBack)
    PayLayout(
        title = "Visa",
        subtitle = "Pagamento · Visa",
        total = kz(if (amountAoa > 0) amountAoa else PilotLedger.GROSS),
        accent = Blue,
        selected = PayMethod.Nfc,
        noteTitle = "Cartão Visa do passageiro",
        noteBody = "Confirme a cobrança no cartão",
        noteColor = Color(0xFF315CA8),
        noteBg = Color(0xFFEDF4FF),
        action = "Cobrar Visa",
        onAction = onCharge,
        onPick = { method ->
            when (method) {
                PayMethod.Cash -> onCash()
                PayMethod.Nfc -> Unit
                PayMethod.Multicaixa -> onMulticaixa()
            }
        },
        onTab = onTab,
    )
}

@Composable
private fun PayLayout(
    title: String,
    subtitle: String,
    total: String,
    accent: Color,
    selected: PayMethod,
    noteTitle: String,
    noteBody: String,
    noteColor: Color,
    noteBg: Color,
    action: String,
    onAction: () -> Unit,
    onPick: (PayMethod) -> Unit,
    onTab: (DeskTab) -> Unit,
) {
    DeskFrame(
        title = title,
        subtitle = subtitle,
        action = action,
        onAction = onAction,
        tab = DeskTab.Home,
        onTab = onTab,
        accent = accent,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF172033))
                    .padding(vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("TOTAL A PAGAR", color = Color(0xFFB8C2CC), fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(total, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            }
            PayRow("Multicaixa Express", selected == PayMethod.Multicaixa, accent) { onPick(PayMethod.Multicaixa) }
            PayRow("Dinheiro", selected == PayMethod.Cash, accent) { onPick(PayMethod.Cash) }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(noteBg)
                    .padding(vertical = 14.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(noteTitle, color = noteColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text(noteBody, color = Color(0xFF72818E), fontSize = 16.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun PayRow(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CardWhite)
            .border(1.5.dp, if (selected) accent else Color(0xFFDFE6E9), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) accent else Color(0xFFEEF2F3)),
        )
        Spacer(Modifier.width(14.dp))
        Text(label, color = Color(0xFF263544), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun DonePage(onConclude: () -> Unit, onTab: (DeskTab) -> Unit) {
    BackHandler(onBack = onConclude)
    var stars by remember { mutableIntStateOf(5) }
    var punctual by remember { mutableStateOf(true) }
    var polite by remember { mutableStateOf(false) }
    val net = "${kz(PilotLedger.net)} líquidos"
    DeskFrame(
        title = "Pagamento confirmado",
        subtitle = "Corrida concluída",
        action = net,
        onAction = onConclude,
        tab = DeskTab.Home,
        onTab = onTab,
        accent = Green,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE7F8F0)),
                contentAlignment = Alignment.Center,
            ) {
                Text("✓", color = Green, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Text("Corrida Concluída", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(14.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFEAF8F2))
                    .padding(vertical = 16.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("OS SEUS GANHOS", color = Color(0xFF16825C), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(net, color = GreenInk, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(18.dp))
            Text("Avaliar ${PilotLedger.PASSENGER}", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (index in 1..5) {
                    Text(
                        "★",
                        color = if (index <= stars) Color(0xFFE6A12C) else Color(0xFFD5DDE1),
                        fontSize = 36.sp,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clickable { stars = if (stars == index) index - 1 else index },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Tag("Pontual", punctual) { punctual = !punctual }
                Tag("Educada", polite) { polite = !polite }
            }
        }
    }
}

@Composable
private fun Tag(label: String, on: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (on) Teal else Muted,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (on) Color(0xFFE6F3F5) else CardWhite)
            .border(1.dp, if (on) Teal else Color(0xFFDDE6E8), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
    )
}

@Composable
fun EarningsPage(onTab: (DeskTab) -> Unit) {
    val bars = listOf(0, 0, 0, 0, 0, 0, 0)
    val days = listOf("S", "T", "Q", "Q", "S", "S", "D")
    DeskFrame(
        title = "Esta semana",
        subtitle = "Ganhos",
        action = "0 corridas",
        onAction = {},
        tab = DeskTab.Earnings,
        onTab = onTab,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(kz(EarningsReadout.totalKz(0L)), color = Ink, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(20.dp))
            Row(
                Modifier.fillMaxWidth().height(140.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                bars.forEachIndexed { index, height ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .width(22.dp)
                                .height(height.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (index == 3) Teal else Color(0xFFDCE7E9)),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(days[index], color = Color(0xFF8996A3), fontSize = 16.sp)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Kpi("CORRIDAS", "0", Modifier.weight(1f))
                Kpi("TEMPO ONLINE", "0h", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Kpi(label: String, value: String, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .padding(16.dp),
    ) {
        Text(label, color = Color(0xFF83909C), fontSize = 16.sp)
        Text(value, color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TripsPage(onTab: (DeskTab) -> Unit) {
    DeskFrame(
        title = "Hoje · 30 Out",
        subtitle = "Histórico de viagens",
        action = kz(PilotLedger.net),
        onAction = {},
        tab = DeskTab.Trips,
        onTab = onTab,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TripRow("${PilotLedger.PASSENGER} · ${PilotLedger.DEST_FULL}")
            TripRow("Pedro L. · Aeroporto")
            TripRow("Ana M. · Belas Shopping")
        }
    }
}

@Composable
private fun TripRow(title: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFE8F3F4)))
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, color = Color(0xFF263544), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text("Detalhes e informação", color = Color(0xFF8996A3), fontSize = 16.sp)
        }
    }
}

@Composable
fun ProfilePage(
    onGoal: () -> Unit,
    onTab: (DeskTab) -> Unit,
    onHomeMap: () -> Unit = {},
    onHomeGps: () -> Unit = {},
) {
    DeskFrame(
        title = "Motorista",
        subtitle = "Perfil",
        action = "★ ${PilotLedger.RATING}",
        onAction = {},
        tab = DeskTab.Profile,
        onTab = onTab,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Avatar("JD", Teal, 84.dp)
            Spacer(Modifier.height(12.dp))
            Text(PilotLedger.DRIVER, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(PilotLedger.VEHICLE, color = Muted, fontSize = 16.sp)
            Spacer(Modifier.height(16.dp))
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileRow("Veículo", null)
                ProfileRow("Carteira", null)
                ProfileRow("Meta diária", onGoal)
                ProfileRow("Casa no mapa", onHomeMap)
                ProfileRow("Casa com GPS actual", onHomeGps)
                ProfileRow("Idioma", null)
                ProfileRow("Documentos", null)
            }
        }
    }
}

@Composable
private fun ProfileRow(title: String, onClick: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFE8F3F4)))
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, color = Color(0xFF263544), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text("Detalhes e informação", color = Color(0xFF8996A3), fontSize = 16.sp)
        }
    }
}

@Composable
fun GoalPage(onSave: () -> Unit, onTab: (DeskTab) -> Unit) {
    BackHandler(onBack = onSave)
    var goal by remember { mutableFloatStateOf(80_000f) }
    val shown = goal.toLong()
    DeskFrame(
        title = "Meta selecionada",
        subtitle = "Meta diária",
        action = "Guardar meta",
        onAction = onSave,
        tab = DeskTab.Profile,
        onTab = onTab,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("META SELECIONADA", color = Color(0xFF8794A0), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(kz(shown), color = Teal, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
            Text("≈ 6–8 corridas por dia", color = Color(0xFF7F8D99), fontSize = 16.sp)
            Spacer(Modifier.height(28.dp))
            GoalSlider(goal, 40_000f, 120_000f) { goal = it }
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GoalPresets.forEach { preset ->
                    val on = kotlin.math.abs(shown - preset) < 2_500
                    Box(
                        Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (on) Color(0xFFE6F3F5) else CardWhite)
                            .border(1.5.dp, if (on) Teal else Color(0xFFDDE6E8), RoundedCornerShape(12.dp))
                            .clickable { goal = preset.toFloat() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            kz(preset),
                            color = if (on) Teal else Ink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalSlider(value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .pointerInput(min, max) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    fun at(x: Float) {
                        val fraction = (x / size.width).coerceIn(0f, 1f)
                        onChange(min + fraction * (max - min))
                    }
                    at(down.position.x)
                    drag(down.id) { change ->
                        at(change.position.x)
                        change.consume()
                    }
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Canvas(Modifier.fillMaxWidth().height(48.dp)) {
            val y = size.height / 2f
            val fraction = ((value - min) / (max - min)).coerceIn(0f, 1f)
            val x = fraction * size.width
            drawLine(Color(0xFFD5DDE1), Offset(0f, y), Offset(size.width, y), 6.dp.toPx(), StrokeCap.Round)
            drawLine(Teal, Offset(0f, y), Offset(x, y), 6.dp.toPx(), StrokeCap.Round)
            drawCircle(Color.White, radius = 12.dp.toPx(), center = Offset(x, y))
            drawCircle(Teal, radius = 12.dp.toPx(), center = Offset(x, y), style = Stroke(3.dp.toPx()))
        }
    }
}

@Composable
fun ReportPage(onSend: () -> Unit, onTab: (DeskTab) -> Unit) {
    BackHandler(onBack = onSend)
    val reasons = listOf("Comportamento abusivo", "No-show", "Pagamento", "Segurança")
    var picked by remember { mutableStateOf<String?>(null) }
    DeskFrame(
        title = "${PilotLedger.PASSENGER} · corrida R-2026-001",
        subtitle = "Reportar passageiro",
        action = "Enviar reporte",
        onAction = onSend,
        tab = DeskTab.Home,
        onTab = onTab,
        accent = Red,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            reasons.forEach { reason ->
                val on = picked == reason
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardWhite)
                        .border(if (on) 1.5.dp else 0.dp, if (on) Red else Color.Transparent, RoundedCornerShape(16.dp))
                        .clickable { picked = reason }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFFFF0EF)))
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(reason, color = Color(0xFF263544), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text("Seleccionar motivo", color = Color(0xFF8996A3), fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun NetworkPage(onRetry: () -> Unit, onBack: () -> Unit, onTab: (DeskTab) -> Unit) {
    BackHandler(onBack = onBack)
    DeskFrame(
        title = "A tentar reconectar…",
        subtitle = "Sem ligação",
        action = "Tentar novamente",
        onAction = onRetry,
        tab = DeskTab.Home,
        onTab = onTab,
        accent = Red,
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SoftIcon(Color(0xFFFDEDEC)) {
                Text("⌁", color = Red, fontSize = 32.sp)
            }
            Spacer(Modifier.height(18.dp))
            Text("Sem Ligação", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Corrida e pagamento guardados", color = Muted, fontSize = 16.sp)
            Spacer(Modifier.height(18.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardWhite)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("Corrida ativa guardada", "Pagamento pendente", "Mensagens por enviar").forEach { line ->
                    Text("✓  $line", color = Green, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun ChatPage(onClose: () -> Unit, onTab: (DeskTab) -> Unit) {
    BackHandler(onBack = onClose)
    var draft by remember { mutableStateOf("") }
    var lines by remember {
        mutableStateOf(
            listOf(
                false to "Olá! Estou a caminho, chego em 2 min.",
                true to "Ok, estou à porta do edifício azul.",
                false to "Visto! Obrigada 👍",
            ),
        )
    }
    fun send() {
        val text = draft.trim()
        if (text.isEmpty()) return
        lines = lines + (true to text)
        draft = ""
    }
    DeskFrame(
        title = "Identidades protegidas",
        subtitle = "Chat da corrida",
        action = "Mensagem…",
        onAction = { send() },
        tab = DeskTab.Home,
        onTab = onTab,
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                lines.forEach { (mine, text) ->
                    val shape = RoundedCornerShape(16.dp)
                    Text(
                        text,
                        color = if (mine) Color.White else Color(0xFF354555),
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth(0.86f)
                            .align(if (mine) Alignment.End else Alignment.Start)
                            .clip(shape)
                            .background(if (mine) Teal else CardWhite)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(CardWhite)
                    .border(1.dp, Color(0xFFE0E7EA), RoundedCornerShape(26.dp))
                    .padding(start = 16.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Ink, fontSize = 16.sp),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box {
                            if (draft.isEmpty()) Text("Mensagem…", color = Color(0xFF8B98A4), fontSize = 16.sp)
                            inner()
                        }
                    },
                )
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Teal)
                        .clickable { send() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("➤", color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun CallPage(onClose: () -> Unit, rideId: String = "", incoming: Boolean = false) {
    val calls: ao.ridehailing.driver.feature.call.CallViewModel = hiltViewModel()
    LaunchedEffect(rideId, incoming) {
        if (!incoming && rideId.isNotBlank()) calls.start(rideId, java.util.UUID.randomUUID().toString())
    }
    BackHandler(onBack = onClose)
    var muted by remember { mutableStateOf(false) }
    LightBars(light = false)
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF101827))
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(120.dp))
        Box(
            Modifier
                .size(132.dp)
                .clip(CircleShape)
                .background(Color(0xFF214B58)),
            contentAlignment = Alignment.Center,
        ) {
            Text("MS", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(22.dp))
        Text(PilotLedger.PASSENGER, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("A caminho do embarque", color = Color(0xFFA9B5C5), fontSize = 16.sp)
        Spacer(Modifier.height(16.dp))
        Text("●  02:14", color = Color(0xFF35C891), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(28.dp))
        Text(
            "Chamada encriptada",
            color = Color(0xFFC9D4E2),
            fontSize = 16.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1A2739))
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CallButton(if (muted) Color(0xFF4B5568) else Color(0xFF29374B), 64.dp, { MuteGlyph() }) { muted = !muted }
            CallButton(Red, 76.dp, { EndGlyph() }) { onClose() }
            CallButton(Color(0xFF3478DB), 64.dp, { PinGlyph() }) {}
        }
        Spacer(Modifier.height(22.dp))
        Text("Números reais nunca são partilhados", color = Color(0xFF728097), fontSize = 16.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(36.dp))
    }
}

@Composable
private fun CallButton(
    color: Color,
    size: androidx.compose.ui.unit.Dp,
    glyph: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { glyph() },
    )
}

@Composable
private fun MuteGlyph() {
    Canvas(Modifier.size(26.dp)) {
        val w = 3.dp.toPx()
        drawRoundRect(
            Color.White,
            topLeft = Offset(size.width * 0.32f, size.height * 0.08f),
            size = Size(size.width * 0.36f, size.height * 0.52f),
            cornerRadius = CornerRadius(20f, 20f),
        )
        drawArc(
            Color.White,
            20f,
            140f,
            false,
            topLeft = Offset(size.width * 0.18f, size.height * 0.28f),
            size = Size(size.width * 0.64f, size.height * 0.5f),
            style = Stroke(w, cap = StrokeCap.Round),
        )
        drawLine(Color.White, Offset(center.x, size.height * 0.78f), Offset(center.x, size.height * 0.92f), w, StrokeCap.Round)
    }
}

@Composable
private fun EndGlyph() {
    Canvas(Modifier.size(28.dp)) {
        rotate(135f) {
            drawRoundRect(
                Color.White,
                topLeft = Offset(size.width * 0.18f, size.height * 0.38f),
                size = Size(size.width * 0.64f, size.height * 0.24f),
                cornerRadius = CornerRadius(40f, 40f),
            )
        }
    }
}

@Composable
private fun PinGlyph() {
    Canvas(Modifier.size(26.dp)) {
        drawCircle(Color.White, radius = 4.dp.toPx(), center = Offset(center.x, size.height * 0.38f))
        drawCircle(Color.White, radius = 9.dp.toPx(), center = Offset(center.x, size.height * 0.38f), style = Stroke(2.5.dp.toPx()))
        drawLine(Color.White, Offset(center.x, size.height * 0.62f), Offset(center.x, size.height * 0.9f), 2.5.dp.toPx(), StrokeCap.Round)
    }
}

@Composable
fun ReactivatePage(onReactivate: () -> Unit, onBack: (() -> Unit)?) {
    if (onBack != null) BackHandler(onBack = onBack)
    val context = androidx.compose.ui.platform.LocalContext.current
    if (onBack == null) {
        BackHandler { context.findActivity()?.moveTaskToBack(true) }
    }
    DeskFrame(
        title = "Conta",
        subtitle = "Reativar",
        action = "Reativar conta",
        onAction = onReactivate,
        tab = DeskTab.Home,
        onTab = {},
    ) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("A conta precisa de ser reativada neste telemóvel.", color = Ink, fontSize = 18.sp)
        }
    }
}
