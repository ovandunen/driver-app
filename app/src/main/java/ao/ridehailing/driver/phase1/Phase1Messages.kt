package ao.ridehailing.driver.phase1

import ao.ridehailing.driver.core.mqtt.TopicMapper

object Phase1Messages {
    fun account(driverId: String) = "driver/$driverId/account"

    fun cashReceived(rideId: String, driverId: String, passengerId: String, fare: String, messageId: String): String =
        envelope("ride.payment.cash_received", messageId,
            """{"rideId":"$rideId","driverId":"$driverId","passengerId":"$passengerId","fare":"$fare"}""")

    fun noShow(rideId: String, passengerId: String, driverId: String, arrivedAt: String, pickupLat: Double,
        pickupLng: Double, driverLat: Double, driverLng: Double, messageId: String): String =
        envelope("ride.passenger_no_show", messageId,
            """{"rideId":"$rideId","passengerId":"$passengerId","driverId":"$driverId","arrivedAt":"$arrivedAt","pickupLat":$pickupLat,"pickupLng":$pickupLng,"driverLat":$driverLat,"driverLng":$driverLng}""")

    fun headingHome(messageId: String) = envelope("driver.heading_home", messageId, "{}")

    fun homeBase(lat: Double, lng: Double, messageId: String) =
        envelope("driver.home_base_set", messageId, """{"lat":$lat,"lng":$lng}""")

    fun payment(rideId: String) = TopicMapper.ridePayment(rideId)

    fun state(rideId: String) = TopicMapper.rideState(rideId)

    private fun envelope(type: String, messageId: String, payload: String) =
        """{"type":"$type","messageId":"$messageId","payload":$payload}"""
}
