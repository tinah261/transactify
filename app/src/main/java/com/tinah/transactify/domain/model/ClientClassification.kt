package com.tinah.transactify.domain.model

/**
 * Segmentation d'un client selon son activité.
 *
 * La valeur persistée dans la colonne `classification` correspond au [name] de
 * la constante (`VIP`, `REGULAR`, `ONE_TIME`).
 */
enum class ClientClassification {
    VIP,
    REGULAR,
    ONE_TIME;

    companion object {

        /** Retrouve une classification à partir de la valeur persistée, [REGULAR] par défaut. */
        fun fromStorage(value: String?): ClientClassification =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: REGULAR
    }
}
