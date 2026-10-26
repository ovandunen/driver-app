package ao.ridehailing.driver.feature.rideoffer

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import javax.inject.Inject

data class RideOffer(
    val rideId: String,
    val messageId: String,
    val pickupAddress: String,
    val destinationAddress: String,
    val distanceKm: Double,
    val durationMin: Int,
    val payoutAoa: Long,
    val passengerRating: Double,
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

class OfferParser @Inject constructor(private val moshi: Moshi) {
    fun parse(json: String): RideOffer? {
        val envelope = moshi.adapter(RideOfferEnvelope::class.java).fromJson(json) ?: return null
        if (envelope.type != "ride.offer") return null
        val payload = envelope.payload
        return RideOffer(
            payload.rideId,
            envelope.messageId.ifEmpty { payload.rideId },
            payload.pickup.address,
            payload.destination.address,
            payload.distanceKm,
            payload.durationMin,
            payload.payoutAoa,
            payload.passengerRating,
        )
    }

    fun ratingImpact(json: String): RatingEffect? {
        val envelope = moshi.adapter(RatingEnvelope::class.java).fromJson(json) ?: return null
        return RatingEffect(envelope.payload.ratingImpact, envelope.payload.acceptanceToken)
    }
}
