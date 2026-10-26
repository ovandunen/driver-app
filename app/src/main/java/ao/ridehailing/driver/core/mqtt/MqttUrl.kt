package ao.ridehailing.driver.core.mqtt

object MqttUrl {
    fun host(url: String): String = body(url).substringBefore(':')

    fun port(url: String): Int = body(url).substringAfter(':').toInt()

    fun tls(url: String): Boolean = url.startsWith("ssl://")

    private fun body(url: String): String = url.substringAfter("://")
}
