package ao.ridehailing.driver.core.network

import ao.ridehailing.driver.BuildConfig
import ao.ridehailing.driver.core.storage.TokenStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (BuildConfig.DEV_BYPASS_LOGIN) {
            val bypassed = chain.request().newBuilder()
                .header("Authorization", "Bearer dev-bypass-token")
                .header("X-Dev-Driver-Id", BuildConfig.DEV_DRIVER_ID)
                .build()
            return chain.proceed(bypassed)
        }
        val token = tokenStore.token()
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }
}
