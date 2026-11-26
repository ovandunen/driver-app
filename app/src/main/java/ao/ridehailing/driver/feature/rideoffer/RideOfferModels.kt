package ao.ridehailing.driver.feature.rideoffer

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.time.Instant
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class RideOffer(
    val rideId: String,
    val messageId: String,
    val pickupAddress: String,
    val destinationAddress: String,
    val distanceKm: Double,
    val durationMin: Int,
    val payoutAoa: Long,
    val passengerRating: Double,
    val expiresAt: String = "",
    val pickupLat: Double = 0.0,
    val pickupLng: Double = 0.0,
    val destinationLat: Double = 0.0,
    val destinationLng: Double = 0.0,
    val passengerId: String = "",
)

@JsonClass(generateAdapter = true)
data class PlaceJson(val lat: Double, val lng: Double, val address: String)

@JsonClass(generateAdapter = true)
data class RideOfferPayload(
    val rideId: String,
    val pickup: PlaceJson,
    val destination: PlaceJson,
    val distanceKm: Double,
    val durationMin: Int,
    val payoutAoa: Long,
    val passengerRating: Double,
    val expiresAt: String,
)

@JsonClass(generateAdapter = true)
data class RideOfferEnvelope(
    val type: String,
    val messageId: String = "",
    val payload: RideOfferPayload,
)

@JsonClass(generateAdapter = true)
data class RatingEnvelope(
    val type: String,
    val messageId: String = "",
    val payload: RatingPayload,
)

@JsonClass(generateAdapter = true)
data class RatingPayload(
    val ratingImpact: Int = 0,
    val acceptanceToken: String = "",
)

data class RatingEffect(val impact: Int, val token: String)

@JsonClass(generateAdapter = true)
data class RideRequestEnvelope(
    val type: String,
    val messageId: String = "",
    val payload: RideRequestPayload,
)

@JsonClass(generateAdapter = true)
data class RideRequestPayload(
    val rideId: String,
    val pickup: PlaceJson,
    val destination: PlaceJson,
    val fareAoa: Long = 0,
    val passengerId: String = "",
)

class OfferParser @Inject constructor(private val moshi: Moshi) {
    fun parse(json: String): RideOffer? {
        val offer = runCatching { moshi.adapter(RideOfferEnvelope::class.java).fromJson(json) }.getOrNull()
        if (offer != null && offer.type == "ride.offer") {
            val payload = offer.payload
            return RideOffer(
                payload.rideId,
                offer.messageId.ifEmpty { payload.rideId },
                payload.pickup.address,
                payload.destination.address,
                payload.distanceKm,
                payload.durationMin,
                payload.payoutAoa,
                payload.passengerRating,
                payload.expiresAt,
                payload.pickup.lat,
                payload.pickup.lng,
                payload.destination.lat,
                payload.destination.lng,
            )
        }
        val hail = runCatching { moshi.adapter(RideRequestEnvelope::class.java).fromJson(json) }.getOrNull() ?: return null
        if (hail.type != "ride.request") return null
        val payload = hail.payload
        val km = distanceKm(payload.pickup.lat, payload.pickup.lng, payload.destination.lat, payload.destination.lng)
        val minutes = km.div(0.4).toInt().coerceAtLeast(1)
        val payout = if (payload.fareAoa > 0) payload.fareAoa * 85 / 100 else 2_500L
        return RideOffer(
            payload.rideId,
            hail.messageId.ifEmpty { payload.rideId },
            payload.pickup.address,
            payload.destination.address,
            km,
            minutes,
            payout,
            4.8,
            Instant.now().plusSeconds(120).toString(),
            payload.pickup.lat,
            payload.pickup.lng,
            payload.destination.lat,
            payload.destination.lng,
            payload.passengerId,
        )
    }

    fun ratingImpact(json: String): RatingEffect? {
        val envelope = moshi.adapter(RatingEnvelope::class.java).fromJson(json) ?: return null
        return RatingEffect(envelope.payload.ratingImpact, envelope.payload.acceptanceToken)
    }
}

private fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val radius = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
    val km = radius * 2 * atan2(sqrt(a), sqrt(1 - a))
    return (km * 10).toInt() / 10.0
}
