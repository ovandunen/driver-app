package ao.ridehailing.driver

import android.content.Intent
import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import ao.ridehailing.driver.core.location.DevLocationStore
import ao.ridehailing.driver.core.mqtt.MqttEndpointStore
import ao.ridehailing.driver.core.mqtt.MqttExtras
import ao.ridehailing.driver.core.mqtt.MqttService
import ao.ridehailing.driver.core.ui.theme.RideHailingTheme
import ao.ridehailing.driver.navigation.AppRoot
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var endpoints: MqttEndpointStore
    @Inject lateinit var devLocations: DevLocationStore

    private val launches = MutableStateFlow<Pair<String, Long>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyBroker(intent)
        devLocations.apply(intent)
        publish(intent)
        ContextCompat.startForegroundService(this, Intent(this, MqttService::class.java))
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.WHITE, Color.WHITE),
            navigationBarStyle = SystemBarStyle.light(Color.WHITE, Color.WHITE),
        )
        setContent {
            RideHailingTheme {
                AppRoot(launches)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyBroker(intent)
        devLocations.apply(intent)
        publish(intent)
    }

    private fun applyBroker(intent: Intent?) {
        val endpoint = MqttExtras.from(intent) ?: return
        endpoints.save(endpoint)
    }

    private fun publish(intent: Intent?) {
        val screen = intent?.getStringExtra("screen") ?: return
        launches.value = screen to System.nanoTime()
    }
}
