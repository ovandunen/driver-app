package ao.ridehailing.driver

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RideHailingApp : Application() {
    override fun onCreate() {
        super.onCreate()
        registerFcmToken()
    }

    private fun registerFcmToken() {
        if (FirebaseApp.getApps(this).isEmpty()) return
        FirebaseMessaging.getInstance().token
    }
}
