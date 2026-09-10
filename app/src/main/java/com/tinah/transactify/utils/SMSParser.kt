package com.tinah.transactify.utils

object SMSParser {

    data class ParseResult(
        val operator: String,
        val amount: Double,
        val phoneNumber: String,
        val type: String, // REÇU ou ENVOYÉ
        val reference: String?,
        val isBonus: Boolean = false
    )

    private val AMOUNT_REGEX = Regex("""(\d+(?:[\s.]\d{3})*)\s*(?:Ar|ar)""")
    private val PHONE_REGEX = Regex("""\+?261\s?3[234]\s?\d{2}\s?\d{2}\s?\d{2}""")
    private val REF_REGEX = Regex("""Ref\s*:?\s*([A-Z0-9]+)""", RegexOption.IGNORE_CASE)

    private fun extractAmount(body: String): Double? =
        AMOUNT_REGEX.find(body)
            ?.groupValues?.get(1)
            ?.replace(" ", "")
            ?.replace(".", "")
            ?.toDoubleOrNull()

    private fun extractPhone(body: String): String? =
        PHONE_REGEX.find(body)?.value?.replace(" ", "")

    private fun extractReference(body: String): String? =
        REF_REGEX.find(body)?.groupValues?.get(1)

    private fun extractType(body: String): String? = when {
        body.contains("reçu", ignoreCase = true) -> "REÇU"
        body.contains("envoyé", ignoreCase = true) -> "ENVOYÉ"
        else -> null
    }

    fun parseOrangeMoneySMS(body: String): ParseResult? {
        if (!body.contains("Orange Money", ignoreCase = true)) return null

        val type = extractType(body) ?: return null
        val amount = extractAmount(body) ?: return null
        val phoneNumber = extractPhone(body) ?: return null
        val isBonus = body.contains("BONUS", ignoreCase = true)
        val reference = extractReference(body)

        return ParseResult(
            operator = "Orange Money",
            amount = amount,
            phoneNumber = phoneNumber,
            type = type,
            reference = reference,
            isBonus = isBonus
        )
    }

    fun parseAirtelMoneySMS(body: String): ParseResult? {
        if (!body.contains("AIRTEL MONEY", ignoreCase = true)) return null

        val type = extractType(body) ?: return null
        val amount = extractAmount(body) ?: return null
        val phoneNumber = extractPhone(body) ?: return null
        val isBonus = body.contains("REWARDS", ignoreCase = true) ||
            body.contains("BONUS", ignoreCase = true)
        val reference = extractReference(body)

        return ParseResult(
            operator = "Airtel Money",
            amount = amount,
            phoneNumber = phoneNumber,
            type = type,
            reference = reference,
            isBonus = isBonus
        )
    }

    fun parseMVolaSMS(body: String): ParseResult? {
        if (!body.contains("M-VOLA", ignoreCase = true)) return null

        val type = extractType(body) ?: return null
        val amount = extractAmount(body) ?: return null
        val phoneNumber = extractPhone(body) ?: return null
        val isBonus = body.contains("Bonus", ignoreCase = true)
        val reference = extractReference(body)

        return ParseResult(
            operator = "M-Vola",
            amount = amount,
            phoneNumber = phoneNumber,
            type = type,
            reference = reference,
            isBonus = isBonus
        )
    }

    fun parseSMS(body: String): ParseResult? =
        parseOrangeMoneySMS(body)
            ?: parseAirtelMoneySMS(body)
            ?: parseMVolaSMS(body)
}
