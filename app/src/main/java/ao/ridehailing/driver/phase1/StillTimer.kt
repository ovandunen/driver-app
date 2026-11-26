package ao.ridehailing.driver.phase1

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class FixSample(val lat: Double, val lng: Double, val atMillis: Long)

class StillTimer {
    fun stillMillis(samples: List<FixSample>, pickupLat: Double, pickupLng: Double, metres: Int): Long {
        var total = 0L
        var previous: FixSample? = null
        for (sample in samples) {
            val prior = previous
            if (prior != null && near(prior, sample, pickupLat, pickupLng, metres)) {
                total += sample.atMillis - prior.atMillis
            }
            previous = sample
        }
        return total
    }

    fun enabled(stillMillis: Long, minutes: Int): Boolean = stillMillis >= minutes * 60_000L

    private fun near(start: FixSample, end: FixSample, pickupLat: Double, pickupLng: Double, metres: Int): Boolean =
        distanceMetres(start.lat, start.lng, pickupLat, pickupLng) <= metres &&
            distanceMetres(end.lat, end.lng, pickupLat, pickupLng) <= metres

    private fun distanceMetres(lat: Double, lng: Double, otherLat: Double, otherLng: Double): Double {
        val radius = 6_371_000.0
        val dLat = Math.toRadians(otherLat - lat)
        val dLng = Math.toRadians(otherLng - lng)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat)) * cos(Math.toRadians(otherLat)) * sin(dLng / 2) * sin(dLng / 2)
        return radius * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}

class HomeBaseOnce {
    var saved = false
        private set

    fun accept(): Boolean {
        saved = true
        return true
    }
}

enum class HomeSetting { MAP, GPS }

object EarningsReadout {
    fun totalKz(recorded: Long): Long = recorded
}
