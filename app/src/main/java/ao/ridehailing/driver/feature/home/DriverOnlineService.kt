package ao.ridehailing.driver.feature.home

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import ao.ridehailing.driver.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

class DriverOnlineService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(21, notification())
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun notification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.online_notification), NotificationManager.IMPORTANCE_LOW),
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(getString(R.string.online_notification))
            .build()
    }

    private companion object {
        const val CHANNEL = "online"
    }
}

@Singleton
class AndroidPresence @Inject constructor(
    @ApplicationContext private val context: Context,
) : PresenceControl {
    override fun start() {
        val allowed = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        if (allowed != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Location permission is required to go online.")
        }
        ContextCompat.startForegroundService(context, Intent(context, DriverOnlineService::class.java))
    }

    override fun stop() {
        context.stopService(Intent(context, DriverOnlineService::class.java))
    }
}
