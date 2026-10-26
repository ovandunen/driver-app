package ao.ridehailing.driver.core.mqtt

object DriverStatusBody {
    fun offline(): String = """{"type":"driver.status","payload":{"status":"offline"}}"""

    fun online(): String = """{"type":"driver.status","payload":{"status":"online"}}"""
}
