package ao.ridehailing.driver.core.money

import kotlin.math.abs

object AoaFormat {
    fun format(amount: Long): String {
        val sign = if (amount < 0) "-" else ""
        return "${sign}Kz ${grouped(abs(amount))},00"
    }

    /** Whole kwanzas, thousands separated by a dot. No currency prefix and no cents. */
    fun grouped(amount: Long): String {
        val digits = abs(amount).toString()
        val body = digits.reversed().chunked(3).joinToString(".").reversed()
        return if (amount < 0) "-$body" else body
    }

    /** 15% of the passenger gross, half-up to the nearest whole kwanza. */
    fun commissionOf(grossKz: Long): Long {
        val gross = abs(grossKz)
        val commission = (gross * 15 + 50) / 100
        return if (grossKz < 0) -commission else commission
    }

    fun netOf(grossKz: Long): Long = grossKz - commissionOf(grossKz)

    fun split(grossKz: Long): String {
        val commission = commissionOf(grossKz)
        return "${grouped(grossKz)} − ${grouped(commission)} = ${grouped(grossKz - commission)}"
    }
}
