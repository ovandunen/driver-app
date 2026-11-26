package ao.ridehailing.driver.phase1

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class Phase1UseCaseTest {
    private val account = """
        {"debt":"13500.00","currency":"AOA","ratio":"0.9000","choice":"","choiceRequired":true,"blocked":false,"blockedUntil":"","debtCeiling":"15000.00","pickupWaitMinutes":5,"noShowMetres":100}
    """.trim()

    @Test
    fun theAccountShowsDebtAgainstTheCeilingFromTheServer() {
        val view = AccountJson.read(account)
        assertEquals("13500.00", view.debt)
        assertEquals("15000.00", view.debtCeiling)
        assertEquals(5, view.pickupWaitMinutes)
        assertEquals(100, view.noShowMetres)
        assertEquals("CHOICE_REQUIRED", DebtGate.onlineBlock(view))
    }

    @Test
    fun aRejectedOnlineRequestShowsTheReasonAndDoesNotRetry() {
        assertEquals("A conta está bloqueada.", RefusalText.online("BLOCKED"))
        assertEquals("A dívida atingiu o limite. Não pode ficar online.", RefusalText.online("DEBT_LIMIT"))
        assertNull(RefusalText.noShow("WAIT"))
        assertTrue(RefusalText.noShow("TOO_FAR")!!.contains("demasiado longe"))
    }

    @Test
    fun cashReceivedPublishesOnlyTheRideFare() {
        val body = Phase1Messages.cashReceived("R1", "D1", "P1", "13850.00", "m1")
        assertTrue(body.contains("\"type\":\"ride.payment.cash_received\""))
        assertTrue(body.contains("\"fare\":\"13850.00\""))
        assertEquals("ride/R1/payment", Phase1Messages.payment("R1"))
        assertEquals("""{"choice":"PAY_ONLINE"}""", DebtGate.choiceBody("PAY_ONLINE"))
        assertEquals("""{"amount":"13500.00"}""", DebtGate.paymentBody("13500.00"))
    }

    @Test
    fun theNoShowButtonEnablesOnlyAfterStandingStillInsideTheServerDistance() {
        val timer = StillTimer()
        val samples = listOf(
            FixSample(0.0, 0.0, 0),
            FixSample(0.0, 0.0, 4 * 60_000L),
            FixSample(0.0, 0.002, 10 * 60_000L),
        )
        val still = timer.stillMillis(samples, 0.0, 0.0, 100)
        assertEquals(4 * 60_000L, still)
        assertFalse(timer.enabled(still, 5))
        assertTrue(timer.enabled(5 * 60_000L, 5))
    }

    @Test
    fun headingHomeAndTheHomeBaseArePublishedOnce() {
        val home = HomeBaseOnce()
        assertTrue(home.accept())
        assertTrue(home.accept())
        assertEquals(0L, EarningsReadout.totalKz(0L))
        assertEquals(HomeSetting.MAP, HomeSetting.valueOf("MAP"))
        assertEquals(HomeSetting.GPS, HomeSetting.valueOf("GPS"))
        val heading = Phase1Messages.headingHome("m6")
        val base = Phase1Messages.homeBase(-8.84, 13.23, "m7")
        assertTrue(heading.contains("\"payload\":{}"))
        assertTrue(base.contains("\"lat\":-8.84"))
        assertEquals("driver/D1/account", Phase1Messages.account("D1"))
    }
}
