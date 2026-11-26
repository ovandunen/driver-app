package ao.ridehailing.driver.core.money

/**
 * One Saturday in Luanda, shared by every screen.
 * Ana 7.820 + Pedro 17.850 = 25.670 before the sample trip.
 * The sample gross is 15.050; commission and net come from [AoaFormat].
 */
object PilotLedger {
    const val GROSS = 15_050L
    const val ANA = 7_820L
    const val PEDRO = 17_850L
    const val GOAL = 80_000L
    const val REFERENCE = "005 9024 7731"
    const val DRIVER = "João D."
    const val VEHICLE = "Toyota Corolla · LD-0042-AB"
    const val RATING = "4,8"
    const val PASSENGER = "Maria S."
    const val PASSENGER_RATING = "4,9"
    const val PICKUP = "Rua da Missão"
    const val DEST = "Talatona"
    const val DEST_FULL = "Talatona Shopping"

    val commission: Long = AoaFormat.commissionOf(GROSS)
    val net: Long = AoaFormat.netOf(GROSS)
    val todayBefore: Long = ANA + PEDRO
    val todayAfter: Long = todayBefore + net

    val weekBars: LongArray = longArrayOf(40_000, 36_500, 48_200, 30_100, 28_400, todayBefore, 0)
    val weekBefore: Long = weekBars.sum()
    val weekAfter: Long = weekBefore + net

    fun today(sampleDone: Boolean): Long = if (sampleDone) todayAfter else todayBefore
    fun trips(sampleDone: Boolean): Int = if (sampleDone) 3 else 2
    fun week(sampleDone: Boolean): Long = if (sampleDone) weekAfter else weekBefore
    fun perTrip(sampleDone: Boolean): Long? {
        val trips = trips(sampleDone)
        val today = today(sampleDone)
        return if (trips > 0 && today % trips == 0L) today / trips else null
    }
}
