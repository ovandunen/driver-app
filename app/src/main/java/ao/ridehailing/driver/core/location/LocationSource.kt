package ao.ridehailing.driver.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

const val SOURCE_GPS = "gps"
const val SOURCE_DEV = "dev-override"

data class DriverFix(
    val lat: Double,
    val lng: Double,
    val accuracyMeters: Double,
    val timeMillis: Long,
)

interface LocationSource {
    val kind: String
    fun start()
    fun stop()
    suspend fun read(): DriverFix?
}

object DevLocationExtras {
    const val LAT = "dev_lat"
    const val LNG = "dev_lng"

    fun from(intent: Intent?): DriverFix? = parse(intent?.getStringExtra(LAT), intent?.getStringExtra(LNG))

    fun parse(lat: String?, lng: String?): DriverFix? {
        val latitude = lat?.trim()?.toDoubleOrNull() ?: return null
        val longitude = lng?.trim()?.toDoubleOrNull() ?: return null
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
        return DriverFix(latitude, longitude, accuracyMeters = 0.0, timeMillis = 0L)
    }
}

@Singleton
class DevLocationStore @Inject constructor() {
    @Volatile private var point: DriverFix? = null

    fun apply(intent: Intent?) {
        point = DevLocationExtras.from(intent)
    }

    fun current(): DriverFix? = point
}

class DevOverrideSource(private val fix: DriverFix) : LocationSource {
    override val kind = SOURCE_DEV
    override fun start() = Unit
    override fun stop() = Unit
    override suspend fun read(): DriverFix = fix.copy(timeMillis = System.currentTimeMillis())
}

fun activeSource(override: DriverFix?, gps: LocationSource): LocationSource =
    if (override != null) DevOverrideSource(override) else gps

object DriverLocationBody {
    fun of(fix: DriverFix): String {
        val whenMillis = if (fix.timeMillis > 0) fix.timeMillis else System.currentTimeMillis()
        val stamp = Instant.ofEpochMilli(whenMillis).toString()
        return """{"type":"driver.location","payload":{"lat":${fix.lat},"lng":${fix.lng},"accuracy":${fix.accuracyMeters},"timestamp":"$stamp"}}"""
    }

    fun city(driverId: String, fix: DriverFix): String =
        """{"type":"driver.location","payload":{"driverId":"$driverId","lat":${fix.lat},"lng":${fix.lng}}}"""
}

suspend fun driverLocationBody(source: LocationSource, gpsAllowed: Boolean): String? {
    if (source.kind != SOURCE_DEV && !gpsAllowed) return null
    val fix = source.read() ?: return null
    return DriverLocationBody.of(fix)
}

fun logLocationSource(kind: String) {
    try {
        Log.i("driver-location", "source=$kind")
    } catch (_: Throwable) {
    }
}

@Singleton
class FusedLocationSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocationSource {
    override val kind = SOURCE_GPS
    private val latest = AtomicReference<DriverFix?>(null)
    private var callback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    override fun start() {
        if (!granted() || callback != null) return
        try {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4_000L)
                .setMinUpdateIntervalMillis(3_000L)
                .setMaxUpdateDelayMillis(5_000L)
                .build()
            val next = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return
                    latest.set(DriverFix(location.latitude, location.longitude, location.accuracy.toDouble(), location.time))
                }
            }
            callback = next
            client().requestLocationUpdates(request, next, Looper.getMainLooper())
        } catch (_: SecurityException) {
            callback = null
        } catch (_: Exception) {
            callback = null
        }
    }

    override fun stop() {
        val current = callback ?: return
        callback = null
        runCatching { client().removeLocationUpdates(current) }
    }

    override suspend fun read(): DriverFix? {
        if (!granted()) return null
        latest.get()?.let { return it }
        return runCatching { currentFix() }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    private suspend fun currentFix(): DriverFix? = suspendCancellableCoroutine { cont ->
        try {
            val task = client().getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            task.addOnSuccessListener { location ->
                val fix = location?.let { DriverFix(it.latitude, it.longitude, it.accuracy.toDouble(), it.time) }
                if (fix != null) latest.set(fix)
                if (cont.isActive) cont.resume(fix)
            }
            task.addOnFailureListener {
                if (cont.isActive) cont.resume(null)
            }
        } catch (_: SecurityException) {
            if (cont.isActive) cont.resume(null)
        } catch (_: Exception) {
            if (cont.isActive) cont.resume(null)
        }
    }

    private fun granted(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun client() = LocationServices.getFusedLocationProviderClient(context)
}

@Singleton
class ActiveLocationSource @Inject constructor(
    private val overrides: DevLocationStore,
    private val gps: FusedLocationSource,
) : LocationSource {
    private var running = false

    override val kind: String
        get() = if (overrides.current() != null) SOURCE_DEV else gps.kind

    override fun start() {
        if (running) return
        running = true
        logLocationSource(kind)
        if (overrides.current() == null) gps.start()
    }

    override fun stop() {
        running = false
        gps.stop()
    }

    override suspend fun read(): DriverFix? = activeSource(overrides.current(), gps).read()
}
