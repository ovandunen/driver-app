package ao.ridehailing.driver.core.network

object ApiErrorParser {
    fun message(body: String): String? {
        val key = "\"message\""
        val start = body.indexOf(key)
        if (start < 0) return null
        val open = body.indexOf('"', start + key.length)
        if (open < 0) return null
        val close = body.indexOf('"', open + 1)
        if (close < 0) return null
        val text = body.substring(open + 1, close)
        return text.ifBlank { null }
    }
}
