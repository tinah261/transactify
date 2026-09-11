package com.tinah.transactify.utils

import com.tinah.transactify.domain.model.OperatorType
import com.tinah.transactify.domain.model.TransactionType

/**
 * Analyse les SMS des operateurs mobile-money (Orange Money, Airtel Money,
 * M-Vola) pour en extraire une transaction structuree.
 */
object SMSParser {

    /**
     * Resultat d'analyse d'un SMS.
     *
     * @property operator operateur emetteur.
     * @property amount montant en ariary.
     * @property phoneNumber numero de la contrepartie (format `+261XXXXXXXXX`).
     * @property type sens de la transaction.
     * @property reference reference de la transaction si presente.
     * @property isBonus vrai si le SMS decrit un bonus / une recompense.
     */
    data class ParseResult(
        val operator: OperatorType,
        val amount: Double,
        val phoneNumber: String,
        val type: TransactionType,
        val reference: String?,
        val isBonus: Boolean = false,
    )

    /** Espaces utilisables comme separateur de milliers : espace, NBSP, NNBSP. */
    private const val SPACES = " \\u00A0\\u202F"

    private val AMOUNT_REGEX = Regex("(\\d+(?:[$SPACES.]\\d{3})*)\\s*(?:Ar|ar|MGA)")
    private val PHONE_REGEX = Regex("\\+?261[$SPACES]?3[234](?:[$SPACES]?\\d{2}){3}")
    private val REF_REGEX = Regex("Ref\\s*:?\\s*([A-Z0-9]+)", RegexOption.IGNORE_CASE)
    private val AMOUNT_SEPARATORS = Regex("[$SPACES.]")
    private val BONUS_KEYWORDS = listOf("BONUS", "REWARDS", "RECOMPENSE", "RÉCOMPENSE")

    /**
     * Analyse un SMS. [sender] (identifiant d'expediteur) sert d'indice
     * secondaire pour reconnaitre l'operateur.
     *
     * @return la transaction extraite, ou `null` si le SMS n'est pas une
     *   notification de transaction exploitable.
     */
    fun parse(body: String?, sender: String? = null): ParseResult? {
        if (body.isNullOrBlank()) return null

        val operator = OperatorType.detect(body, sender) ?: return null
        val type = TransactionType.detect(body) ?: return null
        val amount = extractAmount(body) ?: return null
        val phoneNumber = extractPhone(body) ?: return null

        return ParseResult(
            operator = operator,
            amount = amount,
            phoneNumber = phoneNumber,
            type = type,
            reference = extractReference(body),
            isBonus = BONUS_KEYWORDS.any { body.contains(it, ignoreCase = true) },
        )
    }

    private fun extractAmount(body: String): Double? =
        AMOUNT_REGEX.find(body)
            ?.groupValues?.get(1)
            ?.replace(AMOUNT_SEPARATORS, "")
            ?.toDoubleOrNull()

    private fun extractPhone(body: String): String? =
        PHONE_REGEX.find(body)?.value
            ?.replace(AMOUNT_SEPARATORS, "")
            ?.let { raw -> if (raw.startsWith("+")) raw else "+$raw" }

    private fun extractReference(body: String): String? =
        REF_REGEX.find(body)?.groupValues?.get(1)
}
