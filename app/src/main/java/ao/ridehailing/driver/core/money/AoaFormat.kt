package ao.ridehailing.driver.core.money

import kotlin.math.abs

object AoaFormat {
    fun format(amount: Long): String {
        val digits = abs(amount).toString()
        val grouped = digits.reversed().chunked(3).joinToString(".").reversed()
        val sign = if (amount < 0) "-" else ""
        return "${sign}Kz $grouped,00"
    }
}
