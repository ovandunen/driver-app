package ao.ridehailing.driver.feature.rideoffer

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RideCommandsTest {
    @Test
    fun stateCarriesTheRideId() {
        val body = RideCommands.state("at_pickup", "2026-11-26T00:00:00Z", "D-1", "R-1-at_pickup", "R-1")
        assertTrue(body.contains("\"rideId\":\"R-1\""))
        assertTrue(body.contains("\"state\":\"at_pickup\""))
        assertTrue(body.contains("\"type\":\"ride.state\""))
    }

    @Test
    fun acceptedNamesTheDriver() {
        val body = RideCommands.accepted(
            "R-1",
            "2026-11-26T00:00:00Z",
            "D-1",
            "Dev Driver",
            "Dev Car",
            "LD-0000-XX",
            "Maianga",
            "Talatona",
            -8.839,
            13.2894,
            -8.91,
            13.18,
            -8.84,
            13.29,
            4500,
            "R-1-accepted",
        )
        assertTrue(body.contains("\"type\":\"ride.state\""))
        assertTrue(body.contains("\"state\":\"accepted\""))
        assertTrue(body.contains("\"rideId\":\"R-1\""))
        assertTrue(body.contains("\"driverName\":\"Dev Driver\""))
        assertTrue(body.contains("\"plate\":\"LD-0000-XX\""))
    }
}
