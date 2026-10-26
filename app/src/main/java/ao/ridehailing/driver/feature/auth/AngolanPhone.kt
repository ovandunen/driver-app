package ao.ridehailing.driver.feature.auth

internal object AngolanPhone {
    private val national = Regex("9[0-9]{8}")

    fun normalize(raw: String): String? {
        val digits = raw.filter(Char::isDigit)
        val nationalNumber = when {
            digits.length == 12 && digits.startsWith("244") -> digits.substring(3)
            digits.length == 9 -> digits
            else -> return null
        }
        if (!national.matches(nationalNumber)) return null
        return "+244$nationalNumber"
    }
}
