package ao.ridehailing.driver.core.mqtt

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class MqttEndpoint(val host: String, val port: Int)

object MqttExtras {
    const val HOST = "mqtt_host"
    const val PORT = "mqtt_port"

    fun from(intent: Intent?): MqttEndpoint? = parse(intent?.getStringExtra(HOST), intent?.getStringExtra(PORT))

    fun parse(host: String?, port: String?): MqttEndpoint? {
        val address = host?.trim().orEmpty()
        val number = port?.trim()?.toIntOrNull() ?: return null
        if (!ipv4(address) || number !in 1..65535) return null
        return MqttEndpoint(address, number)
    }

    private fun ipv4(value: String): Boolean {
        val parts = value.split('.')
        if (parts.size != 4) return false
        return parts.all { part ->
            part.isNotEmpty() && part.all(Char::isDigit) && part.toInt() in 0..255
        }
    }
}

@Singleton
class MqttEndpointStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun save(endpoint: MqttEndpoint) {
        prefs.edit().putString(MqttExtras.HOST, endpoint.host).putInt(MqttExtras.PORT, endpoint.port).apply()
    }

    fun current(): MqttEndpoint? {
        val host = prefs.getString(MqttExtras.HOST, "").orEmpty()
        val port = prefs.getInt(MqttExtras.PORT, 0)
        return MqttExtras.parse(host, port.toString())
    }

    private companion object {
        const val FILE = "mqtt_endpoint"
    }
}
