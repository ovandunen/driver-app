package ao.ridehailing.driver.core.mqtt

object ReconnectDelay {
    fun millis(attempt: Int): Long {
        val shift = attempt.coerceIn(0, 5)
        return (1_000L shl shift).coerceAtMost(30_000L)
    }
}
