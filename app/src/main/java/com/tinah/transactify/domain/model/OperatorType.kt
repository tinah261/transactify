package com.tinah.transactify.domain.model

/**
 * Opérateurs mobile-money supportés.
 *
 * @property storageValue valeur persistée dans la colonne `operator` de la table
 *   `transactions` (conservée telle quelle pour rester compatible avec les
 *   données déjà enregistrées).
 * @property smsKeywords mots-clés recherchés dans le corps du SMS pour
 *   identifier l'opérateur.
 * @property senderIds identifiants d'expéditeur SMS connus de l'opérateur.
 */
enum class OperatorType(
    val storageValue: String,
    val smsKeywords: List<String>,
    val senderIds: List<String>,
) {
    ORANGE_MONEY(
        storageValue = "Orange Money",
        smsKeywords = listOf("Orange Money", "OrangeMoney"),
        senderIds = listOf("OrangeMoney", "Orange Money", "OrangeMg", "Orange"),
    ),
    AIRTEL_MONEY(
        storageValue = "Airtel Money",
        smsKeywords = listOf("Airtel Money", "AirtelMoney"),
        senderIds = listOf("AirtelMoney", "Airtel Money", "Airtel"),
    ),
    MVOLA(
        storageValue = "M-Vola",
        smsKeywords = listOf("M-VOLA", "MVOLA", "M-Vola"),
        senderIds = listOf("MVola", "M-VOLA", "MVOLA", "Mvola"),
    );

    companion object {

        /** Retrouve un opérateur à partir de la valeur persistée en base. */
        fun fromStorage(value: String?): OperatorType? =
            entries.firstOrNull { it.storageValue.equals(value, ignoreCase = true) }

        /**
         * Détecte l'opérateur d'un SMS à partir de son corps, avec l'expéditeur
         * comme indice secondaire.
         */
        fun detect(body: String, sender: String? = null): OperatorType? {
            entries.firstOrNull { operator ->
                operator.smsKeywords.any { body.contains(it, ignoreCase = true) }
            }?.let { return it }

            if (sender.isNullOrBlank()) return null
            return entries.firstOrNull { operator ->
                operator.senderIds.any { it.equals(sender, ignoreCase = true) }
            }
        }
    }
}
