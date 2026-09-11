package com.tinah.transactify.domain.model

import com.tinah.transactify.utils.Constants

/**
 * Taux de commission du cash point, indexés par (opérateur, sens de transaction).
 *
 * Un taux est exprimé en fraction décimale : `0.03` = 3 %.
 */
class CommissionRates private constructor(
    private val values: Map<Key, Double>,
) {

    /** Clé composite (opérateur + sens). */
    data class Key(val operator: OperatorType, val type: TransactionType)

    /** Taux applicable, `0.0` si non configuré. */
    fun rateFor(operator: OperatorType, type: TransactionType): Double =
        values[Key(operator, type)] ?: 0.0

    /** Nouveau jeu de taux avec une seule entrée remplacée. */
    fun withRate(operator: OperatorType, type: TransactionType, rate: Double): CommissionRates =
        CommissionRates(values + (Key(operator, type) to rate))

    override fun equals(other: Any?): Boolean =
        this === other || (other is CommissionRates && other.values == values)

    override fun hashCode(): Int = values.hashCode()

    override fun toString(): String = "CommissionRates(${values.mapKeys { "${it.key.operator}/${it.key.type}" }})"

    /** Toutes les entrées, pour l'affichage dans l'écran Paramètres. */
    fun asList(): List<Triple<OperatorType, TransactionType, Double>> =
        OperatorType.entries.flatMap { operator ->
            TransactionType.entries.map { type ->
                Triple(operator, type, rateFor(operator, type))
            }
        }

    companion object {

        /** Taux par défaut (repris de la configuration historique du cash point). */
        val DEFAULT: CommissionRates = CommissionRates(
            mapOf(
                Key(OperatorType.ORANGE_MONEY, TransactionType.RECU) to Constants.DefaultRates.ORANGE_RECU,
                Key(OperatorType.ORANGE_MONEY, TransactionType.ENVOYE) to Constants.DefaultRates.ORANGE_ENVOYE,
                Key(OperatorType.AIRTEL_MONEY, TransactionType.RECU) to Constants.DefaultRates.AIRTEL_RECU,
                Key(OperatorType.AIRTEL_MONEY, TransactionType.ENVOYE) to Constants.DefaultRates.AIRTEL_ENVOYE,
                Key(OperatorType.MVOLA, TransactionType.RECU) to Constants.DefaultRates.MVOLA_RECU,
                Key(OperatorType.MVOLA, TransactionType.ENVOYE) to Constants.DefaultRates.MVOLA_ENVOYE,
            ),
        )

        /** Construit un jeu de taux en complétant les valeurs manquantes par [DEFAULT]. */
        fun of(values: Map<Key, Double>): CommissionRates =
            CommissionRates(DEFAULT.values + values)
    }
}
