package ao.ridehailing.driver.feature.rideoffer

object RideCommands {
    fun accept(rideId: String, messageId: String) = wrap("ride.accept", messageId, """{"rideId":"$rideId"}""")
    fun decline(rideId: String, messageId: String) = wrap("ride.decline", messageId, """{"rideId":"$rideId"}""")
    fun state(state: String, timestamp: String, driverId: String, messageId: String) =
        wrap("ride.state", messageId, """{"state":"$state","timestamp":"$timestamp","driverId":"$driverId"}""")
    fun cancel(reason: String, text: String, timestamp: String, messageId: String) =
        wrap("ride.cancel", messageId, """{"reasonCode":"$reason","reasonText":"${esc(text)}","timestamp":"$timestamp"}""")
    fun chat(messageId: String, text: String, kind: String, timestamp: String) =
        wrap("chat.message", messageId, """{"messageId":"$messageId","senderRole":"driver","text":"${esc(text)}","kind":"$kind","timestamp":"$timestamp"}""")
    fun callRequest(rideId: String, messageId: String) = wrap("call.request", messageId, """{"rideId":"$rideId"}""")
    fun callEnd(messageId: String) = wrap("call.end", messageId, """{}""")
    fun quality(messageId: String, bitrate: Int) = wrap("call.quality", messageId, """{"bitrate":$bitrate}""")
}

private fun wrap(type: String, messageId: String, payload: String) =
    """{"type":"$type","messageId":"$messageId","payload":$payload}"""

private fun esc(value: String) = value.replace("\\", "\\\\").replace("\"", "\\\"")
