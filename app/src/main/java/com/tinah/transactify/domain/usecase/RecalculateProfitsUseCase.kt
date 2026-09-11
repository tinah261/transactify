package com.tinah.transactify.domain.usecase

import com.tinah.transactify.data.repository.TransactionRepository
import com.tinah.transactify.domain.model.OperatorType
import com.tinah.transactify.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Recalcule `profitCalculated` de toutes les transactions avec les taux de
 * commission courants — utilisé après une modification des taux (Paramètres)
 * pour que l'historique reflète le nouveau barème plutôt que celui en
 * vigueur au moment de chaque transaction.
 *
 * Le bonus déjà rattaché ([com.tinah.transactify.data.db.entity.Transaction.bonusAmount])
 * n'est pas recalculé : c'est un montant historique fixe versé par
 * l'opérateur, indépendant du taux de commission du cash point.
 */
class RecalculateProfitsUseCase(
    private val transactionRepository: TransactionRepository,
    private val calculateProfit: CalculateProfitUseCase,
) {

    /** @return le nombre de transactions dont le bénéfice a effectivement changé. */
    suspend operator fun invoke(): Int {
        val transactions = transactionRepository.getAllTransactions().first()
        var updatedCount = 0

        transactions.forEach { transaction ->
            val operator = OperatorType.fromStorage(transaction.operator) ?: return@forEach
            val type = TransactionType.fromStorage(transaction.transactionType) ?: return@forEach

            val newProfit = calculateProfit(transaction.amount, operator, type) + transaction.bonusAmount
            if (newProfit != transaction.profitCalculated) {
                transactionRepository.updateTransaction(transaction.copy(profitCalculated = newProfit))
                updatedCount++
            }
        }

        Timber.d("Bénéfices recalculés : %d/%d transaction(s) modifiée(s)", updatedCount, transactions.size)
        return updatedCount
    }
}
