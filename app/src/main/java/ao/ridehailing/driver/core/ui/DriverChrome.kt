package ao.ridehailing.driver.core.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import ao.ridehailing.driver.core.money.AoaFormat

val CanvasBg = Color(0xFFF5F8F9)
val Ink = Color(0xFF172033)
val Muted = Color(0xFF82909E)
val Teal = Color(0xFF1B6B7A)
val Amber = Color(0xFFD99019)
val Green = Color(0xFF13966B)
val GreenInk = Color(0xFF0C6548)
val Red = Color(0xFFD84A4A)
val Blue = Color(0xFF3A72D8)
val CardWhite = Color(0xFFFFFFFF)
val Hairline = Color(0xFFE5EAED)
val TabIdle = Color(0xFF9AA6AF)

enum class DeskTab { Home, Earnings, Trips, Profile }

fun kz(amount: Long): String = "Kz ${AoaFormat.grouped(amount)}"

@Composable
fun DeskFrame(
    title: String,
    subtitle: String,
    action: String,
    onAction: () -> Unit,
    tab: DeskTab,
    onTab: (DeskTab) -> Unit,
    modifier: Modifier = Modifier,
    initials: String = "JD",
    accent: Color = Teal,
    actionColor: Color = accent,
    showTabs: Boolean = true,
    showAction: Boolean = true,
    lightBars: Boolean = true,
    onAvailability: (() -> Unit)? = null,
    available: Boolean = false,
    body: @Composable ColumnScope.() -> Unit,
) {
    LightBars(lightBars)
    Column(
        modifier
            .fillMaxSize()
            .background(CanvasBg)
            .statusBarsPadding(),
    ) {
        Header(title, subtitle, initials, accent, onAvailability, available)
        Column(Modifier.weight(1f).fillMaxWidth(), content = body)
        if (showAction) {
            BigButton(action, onAction, actionColor, Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
        }
        if (showTabs) TabBar(tab, accent, onTab)
    }
}

@Composable
fun LightBars(light: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val activity = view.context.findActivity() ?: return@SideEffect
        val window = activity.window
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = light
            isAppearanceLightNavigationBars = light
        }
    }
}

@Composable
private fun Header(
    title: String,
    subtitle: String,
    initials: String,
    accent: Color,
    onAvailability: (() -> Unit)?,
    available: Boolean,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(CardWhite)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(initials, accent, 44.dp)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                title,
                color = Ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 22.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                color = Muted,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                maxLines = 2,
            )
        }
        if (onAvailability != null) {
            Text(
                if (available) "Online" else "Offline",
                color = if (available) GreenInk else Muted,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onAvailability)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        } else {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("•••", color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun Avatar(initials: String, color: Color, size: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, color = Color.White, fontSize = if (size < 56.dp) 16.sp else 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
fun TabBar(selected: DeskTab, accent: Color, onTab: (DeskTab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(CardWhite)
            .navigationBarsPadding()
            .heightIn(min = 64.dp)
            .padding(top = 6.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TabItem("Início", DeskTab.Home, selected, accent, onTab) { HomeGlyph(it) }
        TabItem("Kz Ganhos", DeskTab.Earnings, selected, accent, onTab) { EarningsGlyph(it) }
        TabItem("Viagens", DeskTab.Trips, selected, accent, onTab) { TripsGlyph(it) }
        TabItem("Perfil", DeskTab.Profile, selected, accent, onTab) { ProfileGlyph(it) }
    }
}

@Composable
private fun RowScope.TabItem(
    label: String,
    tab: DeskTab,
    selected: DeskTab,
    accent: Color,
    onTab: (DeskTab) -> Unit,
    icon: @Composable (Color) -> Unit,
) {
    val on = tab == selected
    val color = if (on) accent else TabIdle
    Column(
        Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .clickable { onTab(tab) }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        icon(color)
        Text(
            label,
            color = color,
            fontSize = 16.sp,
            fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
            textAlign = TextAlign.Center,
        )
    }
}

private const val STREET_STYLE =
    "https://api.maptiler.com/maps/streets-v2-dark/style.json?key=${ao.ridehailing.driver.BuildConfig.MAPTILER_API_KEY}"

@Composable
fun CityMap(modifier: Modifier = Modifier, lat: Double = -8.8390, lng: Double = 13.2894) {
    val context = LocalContext.current
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    val targetNow = androidx.compose.runtime.rememberUpdatedState(org.maplibre.android.geometry.LatLng(lat, lng))
    val mapView = androidx.compose.runtime.remember {
        org.maplibre.android.MapLibre.getInstance(
            context,
            ao.ridehailing.driver.BuildConfig.MAPTILER_API_KEY,
            org.maplibre.android.WellKnownTileServer.MapTiler,
        )
        org.maplibre.android.maps.MapView(context)
    }
    androidx.compose.runtime.DisposableEffect(lifecycle, mapView) {
        val observer = object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onStart(owner: androidx.lifecycle.LifecycleOwner) = mapView.onStart()
            override fun onResume(owner: androidx.lifecycle.LifecycleOwner) = mapView.onResume()
            override fun onPause(owner: androidx.lifecycle.LifecycleOwner) = mapView.onPause()
            override fun onStop(owner: androidx.lifecycle.LifecycleOwner) = mapView.onStop()
        }
        mapView.onCreate(android.os.Bundle())
        mapView.onStart()
        mapView.onResume()
        lifecycle.addObserver(observer)
        mapView.getMapAsync { map ->
            map.setStyle(org.maplibre.android.maps.Style.Builder().fromUri(STREET_STYLE)) { style ->
                style.addSource(org.maplibre.android.style.sources.GeoJsonSource("me"))
                style.addLayer(
                    org.maplibre.android.style.layers.CircleLayer("me-layer", "me").withProperties(
                        org.maplibre.android.style.layers.PropertyFactory.circleRadius(8f),
                        org.maplibre.android.style.layers.PropertyFactory.circleColor("#0E7C66"),
                        org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth(2f),
                        org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor("#FFFFFF"),
                    ),
                )
                val point = org.maplibre.geojson.Point.fromLngLat(targetNow.value.longitude, targetNow.value.latitude)
                style.getSourceAs<org.maplibre.android.style.sources.GeoJsonSource>("me")?.setGeoJson(point)
                map.cameraPosition = org.maplibre.android.camera.CameraPosition.Builder().target(targetNow.value).zoom(14.0).build()
            }
        }
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }
    Box(modifier.fillMaxSize()) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.getMapAsync { map ->
                    val style = map.style ?: return@getMapAsync
                    val point = org.maplibre.geojson.Point.fromLngLat(targetNow.value.longitude, targetNow.value.latitude)
                    style.getSourceAs<org.maplibre.android.style.sources.GeoJsonSource>("me")?.setGeoJson(point)
                    map.cameraPosition = org.maplibre.android.camera.CameraPosition.Builder()
                        .target(targetNow.value)
                        .zoom(map.cameraPosition.zoom.coerceAtLeast(14.0))
                        .build()
                }
            },
        )
        Text(
            "© OpenStreetMap © CARTO",
            color = Muted,
            fontSize = 10.sp,
            modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
        )
    }
}

@Composable
fun SoftIcon(color: Color, modifier: Modifier = Modifier, glyph: @Composable () -> Unit) {
    Box(
        modifier
            .size(88.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
        content = { glyph() },
    )
}

@Composable
private fun HomeGlyph(color: Color) {
    Canvas(Modifier.size(18.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.5f, size.height * 0.12f)
            lineTo(size.width * 0.12f, size.height * 0.46f)
            lineTo(size.width * 0.28f, size.height * 0.46f)
            lineTo(size.width * 0.28f, size.height * 0.86f)
            lineTo(size.width * 0.72f, size.height * 0.86f)
            lineTo(size.width * 0.72f, size.height * 0.46f)
            lineTo(size.width * 0.88f, size.height * 0.46f)
            close()
        }
        drawPath(path, color)
    }
}

@Composable
private fun EarningsGlyph(color: Color) {
    Canvas(Modifier.size(18.dp)) {
        val bar = size.width * 0.22f
        drawRoundRect(color, topLeft = Offset(0f, size.height * 0.45f), size = androidx.compose.ui.geometry.Size(bar, size.height * 0.55f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f))
        drawRoundRect(color, topLeft = Offset(size.width * 0.39f, size.height * 0.2f), size = androidx.compose.ui.geometry.Size(bar, size.height * 0.8f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f))
        drawRoundRect(color, topLeft = Offset(size.width * 0.78f, size.height * 0.05f), size = androidx.compose.ui.geometry.Size(bar, size.height * 0.95f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f))
    }
}

@Composable
private fun TripsGlyph(color: Color) {
    Canvas(Modifier.size(18.dp)) {
        val cell = size.width * 0.38f
        val gap = size.width * 0.24f
        drawRoundRect(color, size = androidx.compose.ui.geometry.Size(cell, cell), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f))
        drawRoundRect(color, topLeft = Offset(cell + gap, 0f), size = androidx.compose.ui.geometry.Size(cell, cell), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f))
        drawRoundRect(color, topLeft = Offset(0f, cell + gap), size = androidx.compose.ui.geometry.Size(cell, cell), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f))
        drawRoundRect(color, topLeft = Offset(cell + gap, cell + gap), size = androidx.compose.ui.geometry.Size(cell, cell), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f))
    }
}

@Composable
private fun ProfileGlyph(color: Color) {
    Canvas(Modifier.size(18.dp)) {
        drawCircle(color, radius = size.minDimension * 0.22f, center = Offset(size.width / 2f, size.height * 0.32f))
        drawArc(
            color,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(size.width * 0.12f, size.height * 0.48f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.76f, size.height * 0.7f),
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
fun PowerGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(40.dp)) {
        val stroke = 3.5.dp.toPx()
        val radius = size.minDimension * 0.28f
        drawArc(
            color,
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius + 4.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
        drawLine(
            color,
            Offset(center.x, center.y - radius - 2.dp.toPx()),
            Offset(center.x, center.y + 2.dp.toPx()),
            stroke,
            StrokeCap.Round,
        )
    }
}

@Composable
fun ClockGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(36.dp)) {
        val stroke = 3.dp.toPx()
        drawCircle(color, style = Stroke(stroke))
        drawLine(color, center, Offset(center.x, center.y - size.minDimension * 0.28f), stroke, StrokeCap.Round)
        drawLine(color, center, Offset(center.x + size.minDimension * 0.22f, center.y + 2f), stroke, StrokeCap.Round)
    }
}

fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
