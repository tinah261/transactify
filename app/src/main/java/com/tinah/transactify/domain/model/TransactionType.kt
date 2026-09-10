package com.tinah.transactify.domain.model

/**
 * Sens d'une transaction du point de vue du cash point.
 *
 * @property storageValue valeur persistée dans la colonne `transaction_type`
 *   (accentuée, conservée pour compatibilité avec les données existantes et les
 *   requêtes SQL `WHERE transaction_type = 'REÇU'`).
 * @property displayName libellé affiché dans l'UI.
 */
enum class TransactionType(
    val storageValue: String,
    val displayName: String,
) {
    RECU("REÇU", "Reçu"),
    ENVOYE("ENVOYÉ", "Envoyé");

    companion object {

        /** Retrouve un type à partir de la valeur persistée en base. */
        fun fromStorage(value: String?): TransactionType? =
            entries.firstOrNull { it.storageValue.equals(value, ignoreCase = true) }

        /** Détecte le sens d'une transaction à partir du corps du SMS. */
        fun detect(body: String): TransactionType? = when {
            RECU_KEYWORDS.any { body.contains(it, ignoreCase = true) } -> RECU
            ENVOYE_KEYWORDS.any { body.contains(it, ignoreCase = true) } -> ENVOYE
            else -> null
        }

        private val RECU_KEYWORDS = listOf("reçu", "recu")
        private val ENVOYE_KEYWORDS = listOf("envoyé", "envoye")
    }
}
