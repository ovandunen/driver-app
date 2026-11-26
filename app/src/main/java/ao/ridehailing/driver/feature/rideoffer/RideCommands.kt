package ao.ridehailing.driver.feature.rideoffer

object RideCommands {
    fun accept(rideId: String, messageId: String) = wrap("ride.accept", messageId, """{"rideId":"$rideId"}""")
    fun accepted(
        rideId: String,
        timestamp: String,
        driverId: String,
        driverName: String,
        make: String,
        plate: String,
        pickupAddress: String,
        destinationAddress: String,
        pickupLat: Double,
        pickupLng: Double,
        destLat: Double,
        destLng: Double,
        driverLat: Double,
        driverLng: Double,
        fareAoa: Long,
        messageId: String,
    ) = wrap(
        "ride.state",
        messageId,
        """{"rideId":"${esc(rideId)}","state":"accepted","timestamp":"${esc(timestamp)}","driverId":"${esc(driverId)}","driverName":"${esc(driverName)}","make":"${esc(make)}","plate":"${esc(plate)}","pickupAddress":"${esc(pickupAddress)}","destinationAddress":"${esc(destinationAddress)}","pickupLat":$pickupLat,"pickupLng":$pickupLng,"destLat":$destLat,"destLng":$destLng,"driverLat":$driverLat,"driverLng":$driverLng,"fareAoa":$fareAoa}""",
    )
    fun decline(rideId: String, messageId: String) = wrap("ride.decline", messageId, """{"rideId":"$rideId"}""")
    fun state(state: String, timestamp: String, driverId: String, messageId: String, rideId: String) =
        wrap("ride.state", messageId, """{"rideId":"$rideId","state":"$state","timestamp":"$timestamp","driverId":"$driverId"}""")
    fun cancel(reason: String, text: String, timestamp: String, messageId: String, driverId: String, state: String) =
        wrap("ride.cancel", messageId, """{"reasonCode":"$reason","reasonText":"${esc(text)}","who":"driver","driverId":"$driverId","state":"$state","timestamp":"$timestamp"}""")
    fun chat(messageId: String, text: String, kind: String, timestamp: String) =
        wrap("chat.message", messageId, """{"messageId":"$messageId","senderRole":"driver","text":"${esc(text)}","kind":"$kind","timestamp":"$timestamp"}""")
    fun callRequest(rideId: String, messageId: String) =
        wrap("call.request", messageId, """{"rideId":"$rideId","who":"driver"}""")
    fun callEnd(messageId: String) = wrap("call.end", messageId, """{}""")
    fun quality(messageId: String, bitrate: Int) = wrap("call.quality", messageId, """{"bitrate":$bitrate}""")
}

private fun wrap(type: String, messageId: String, payload: String) =
    """{"type":"$type","messageId":"$messageId","payload":$payload}"""

private fun esc(value: String) = value.replace("\\", "\\\\").replace("\"", "\\\"")
