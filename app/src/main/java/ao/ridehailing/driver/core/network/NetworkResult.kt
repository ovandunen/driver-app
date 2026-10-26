package ao.ridehailing.driver.core.network

sealed interface NetworkResult<out T> {
    data class Success<T>(val value: T) : NetworkResult<T>
    data class Failure(val message: String = "") : NetworkResult<Nothing>
}
