package ao.ridehailing.driver

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import ao.ridehailing.driver.core.mqtt.MqttService
import ao.ridehailing.driver.core.ui.theme.RideHailingTheme
import ao.ridehailing.driver.navigation.AppRoot
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContextCompat.startForegroundService(this, Intent(this, MqttService::class.java))
        enableEdgeToEdge()
        setContent {
            RideHailingTheme {
                AppRoot()
            }
        }
    }
}
