package ao.ridehailing.driver.core.mqtt

object TopicMapper {
    fun rideOffer(rideId: String) = "ride/$rideId/offer"
    fun rideAccept(rideId: String) = "ride/$rideId/accept"
    fun rideDecline(rideId: String) = "ride/$rideId/decline"
    fun rideState(rideId: String) = "ride/$rideId/state"
    fun rideLocation(rideId: String) = "ride/$rideId/location"
    fun rideChat(rideId: String) = "ride/$rideId/chat"
    fun ridePayment(rideId: String) = "ride/$rideId/payment"
    fun driverStatus(driverId: String) = "driver/$driverId/status"
}

object DriverTopics {
    fun demandZones(driverId: String) = "driver/$driverId/demand-zones"
    fun rideOffers(driverId: String) = "driver/$driverId/ride-offers"
    fun driverLocation(driverId: String) = "driver/$driverId/location"

    fun cityDrivers() = "city/luanda/drivers"

    fun cityRideRequests() = "city/luanda/ride-requests"
    fun passengerRideStatus(passengerId: String) = "passenger/$passengerId/ride-status"
    fun systemHealth() = "system/health"
    fun rideCancel(rideId: String) = "ride/$rideId/cancel"
    fun callRequest(rideId: String) = "ride/$rideId/call/request"
    fun callResponse(rideId: String) = "ride/$rideId/call/response"
}

object CallTopics {
    fun callEnd(rideId: String) = "ride/$rideId/call/end"
    fun callQuality(rideId: String) = "ride/$rideId/call/quality"
}
