package com.tinah.transactify.domain.usecase

import com.tinah.transactify.domain.model.CommissionRates
import com.tinah.transactify.domain.model.OperatorType
import com.tinah.transactify.domain.model.TransactionType

/**
 * Calcule le bénéfice (commission) d'une transaction : `montant × taux`.
 *
 * Source unique de vérité pour le bénéfice — tout le reste de l'app passe par ici
 * plutôt que de dupliquer les taux.
 *
 * @param ratesProvider fournit les taux courants (branché sur `PreferencesManager`
 *   en production, sur un jeu fixe dans les tests).
 */
class CalculateProfitUseCase(
    private val ratesProvider: suspend () -> CommissionRates,
) {

    /** Bénéfice pour un opérateur / sens connus. */
    suspend operator fun invoke(
        amount: Double,
        operator: OperatorType,
        type: TransactionType,
    ): Double {
        if (amount <= 0.0) return 0.0
        val rate = ratesProvider().rateFor(operator, type)
        return amount * rate
    }

    /**
     * Variante tolérante aux valeurs persistées brutes (chaînes en base). Renvoie
     * `0.0` si l'opérateur ou le sens ne sont pas reconnus.
     */
    suspend fun forStoredValues(
        amount: Double,
        operatorStorageValue: String,
        typeStorageValue: String,
    ): Double {
        val operator = OperatorType.fromStorage(operatorStorageValue) ?: return 0.0
        val type = TransactionType.fromStorage(typeStorageValue) ?: return 0.0
        return invoke(amount, operator, type)
    }
}
