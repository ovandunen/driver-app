package ao.ridehailing.driver.feature.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

data class DemandZone(
    val lat: Double,
    val lng: Double,
    val radiusMeters: Int,
    val intensity: Double,
    val label: String,
)

@JsonClass(generateAdapter = true)
data class DemandZoneJson(
    val lat: Double,
    val lng: Double,
    val radiusMeters: Int,
    val intensity: Double,
    val label: String,
)

@JsonClass(generateAdapter = true)
data class DemandEnvelope(val type: String, val payload: DemandPayload)

@JsonClass(generateAdapter = true)
data class DemandPayload(val zones: List<DemandZoneJson>)

object DemandZones {
    fun parse(moshi: Moshi, json: String): List<DemandZone> {
        val envelope = moshi.adapter(DemandEnvelope::class.java).fromJson(json) ?: return emptyList()
        return envelope.payload.zones.map {
            DemandZone(it.lat, it.lng, it.radiusMeters, it.intensity, it.label)
        }
    }
}

@Composable
fun HeatmapOverlay(zones: List<DemandZone>, onZone: (String) -> Unit) {
    Column {
        zones.forEach { zone ->
            Text(zone.label, modifier = androidx.compose.ui.Modifier.clickable { onZone(zone.label) })
        }
    }
}
