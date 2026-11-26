package ao.ridehailing.driver.core.location

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LocationSourceTest {
    @Test
    fun overrideUsedWhenExtrasArePresent() = runTest {
        val gps = FakeGps()
        val override = DevLocationExtras.parse("-8.8390", "13.2894")
        val source = activeSource(override, gps)
        assertEquals(SOURCE_DEV, source.kind)
        val fix = source.read()
        assertEquals(-8.8390, fix!!.lat, 0.00001)
        assertEquals(13.2894, fix.lng, 0.00001)
        val body = driverLocationBody(source, gpsAllowed = false)
        assertTrue(body!!.contains("\"type\":\"driver.location\""))
        assertTrue(body.contains("\"lat\":-8.839"))
        assertTrue(body.contains("\"lng\":13.2894"))
        assertTrue(body.contains("\"accuracy\":"))
        assertTrue(body.contains("\"timestamp\":"))
        assertEquals(0, gps.reads)
    }

    @Test
    fun realSourceUsedWhenExtrasAreAbsent() = runTest {
        val gps = FakeGps(DriverFix(1.0, 2.0, 4.5, 1_700_000_000_000L))
        val source = activeSource(DevLocationExtras.parse(null, null), gps)
        assertSame(gps, source)
        assertEquals(SOURCE_GPS, source.kind)
        val body = driverLocationBody(source, gpsAllowed = true)
        assertTrue(body!!.startsWith("{\"type\":\"driver.location\",\"payload\":{\"lat\":1.0,\"lng\":2.0,"))
        assertTrue(body.contains("\"accuracy\":4.5"))
        assertTrue(body.contains("\"timestamp\":\"2023-11-14T22:13:20Z\""))
        assertEquals(1, gps.reads)
    }

    @Test
    fun noPublishWithoutPermission() = runTest {
        val gps = FakeGps(DriverFix(-8.84, 13.23, 8.0, 1_700_000_000_000L))
        assertNull(driverLocationBody(gps, gpsAllowed = false))
        assertEquals(0, gps.reads)
    }
}

private class FakeGps(private val fix: DriverFix? = null) : LocationSource {
    var reads = 0
    override val kind = SOURCE_GPS
    override fun start() = Unit
    override fun stop() = Unit
    override suspend fun read(): DriverFix? {
        reads += 1
        return fix
    }
}
