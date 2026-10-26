package ao.ridehailing.driver.core.mqtt

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import ao.ridehailing.driver.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MqttService : Service() {
    @Inject lateinit var manager: MqttConnectionManager

    override fun onCreate() {
        super.onCreate()
        manager.listen()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, notification())
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun notification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL, getString(R.string.mqtt_channel), NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(channel)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(getString(R.string.mqtt_notification))
            .build()
    }

    private companion object {
        const val CHANNEL = "mqtt"
        const val NOTIFICATION_ID = 20
    }
}
