package ao.ridehailing.driver.phase1

import org.json.JSONObject
import java.math.BigDecimal

data class AccountView(
    val debt: String,
    val currency: String,
    val ratio: String,
    val choice: String,
    val choiceRequired: Boolean,
    val blocked: Boolean,
    val blockedUntil: String,
    val debtCeiling: String,
    val pickupWaitMinutes: Int,
    val noShowMetres: Int,
)

object AccountJson {
    fun read(body: String): AccountView {
        val json = JSONObject(body)
        return AccountView(
            debt = json.optString("debt"),
            currency = json.optString("currency"),
            ratio = json.optString("ratio"),
            choice = json.optString("choice"),
            choiceRequired = json.optBoolean("choiceRequired"),
            blocked = json.optBoolean("blocked"),
            blockedUntil = json.optString("blockedUntil"),
            debtCeiling = json.optString("debtCeiling"),
            pickupWaitMinutes = json.optInt("pickupWaitMinutes"),
            noShowMetres = json.optInt("noShowMetres"),
        )
    }
}

object DebtGate {
    fun onlineBlock(account: AccountView): String? {
        if (account.blocked && BigDecimal(account.ratio).compareTo(BigDecimal.ONE) >= 0) return "DEBT_LIMIT"
        if (account.blocked) return "BLOCKED"
        if (account.choiceRequired) return "CHOICE_REQUIRED"
        return null
    }

    fun choiceBody(choice: String) = """{"choice":"$choice"}"""

    fun paymentBody(debt: String) = """{"amount":"$debt"}"""
}

object RefusalText {
    fun online(reason: String): String = when (reason) {
        "ACTIVE_RIDE" -> "Termine a corrida antes de ficar offline."
        "DEBT_LIMIT" -> "A dívida atingiu o limite. Não pode ficar online."
        "BLOCKED" -> "A conta está bloqueada."
        "CHOICE_REQUIRED" -> "Registe como vai pagar a dívida antes de ficar online."
        else -> reason
    }

    fun noShow(reason: String): String? = if (reason == "TOO_FAR") {
        "Está demasiado longe do local de embarque. O não-comparecimento não foi registado."
    } else {
        null
    }
}
