package ao.ridehailing.driver.core.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import ao.ridehailing.driver.BuildConfig

class EncryptedTokenStore(
    context: Context,
) : TokenStore {
    private val preferences: SharedPreferences = EncryptedSharedPreferences.create(
        FILE_NAME,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    init {
        if (BuildConfig.DEV_BYPASS_LOGIN && token().isNullOrBlank()) {
            save("dev-bypass-token", BuildConfig.DEV_DRIVER_ID, "2099-01-01T00:00:00Z")
        }
    }

    override fun save(token: String, driverId: String, expiresAt: String) {
        preferences.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_DRIVER_ID, driverId)
            .putString(KEY_EXPIRES_AT, expiresAt)
            .apply()
    }

    override fun token(): String? = preferences.getString(KEY_TOKEN, null)

    override fun driverId(): String? = preferences.getString(KEY_DRIVER_ID, null)

    override fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val FILE_NAME = "driver_auth"
        const val KEY_TOKEN = "token"
        const val KEY_DRIVER_ID = "driver_id"
        const val KEY_EXPIRES_AT = "expires_at"
    }
}
