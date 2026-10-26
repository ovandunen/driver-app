package ao.ridehailing.driver.core.storage

interface TokenStore {
    fun save(token: String, driverId: String, expiresAt: String)
    fun token(): String?
    fun driverId(): String?
    fun clear()
}
