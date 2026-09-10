package com.tinah.transactify.utils

import com.tinah.transactify.data.db.dao.TransactionDao
import com.tinah.transactify.data.db.entity.Transaction
import kotlinx.coroutines.flow.first

class BonusMatchingService(
    private val transactionDao: TransactionDao
) {

    suspend fun matchBonusToTransaction(bonusTransaction: Transaction) {
        if (bonusTransaction.bonusLinked) return

        val parentTransactions = transactionDao
            .getTransactionsByDateRange(
                bonusTransaction.timestamp - 120_000, // -2 minutes
                bonusTransaction.timestamp
            )
            .first()

        val parent = parentTransactions.find { candidate ->
            candidate.reference == bonusTransaction.reference &&
                candidate.operator == bonusTransaction.operator &&
                candidate.transactionType == "ENVOYÉ"
        } ?: return

        val updatedParent = parent.copy(
            bonusAmount = bonusTransaction.amount,
            bonusLinked = true,
            profitCalculated = calculateProfit(parent.amount, parent.operator, parent.transactionType) +
                bonusTransaction.amount
        )
        transactionDao.updateTransaction(updatedParent)

        // La transaction bonus est absorbée dans la transaction mère : on supprime l'orpheline.
        transactionDao.deleteTransaction(bonusTransaction)
    }

    private fun calculateProfit(amount: Double, operator: String, type: String): Double {
        val rate = when {
            operator == "Orange Money" && type == "REÇU" -> 0.02
            operator == "Orange Money" && type == "ENVOYÉ" -> 0.03
            operator == "Airtel Money" && type == "REÇU" -> 0.025
            operator == "Airtel Money" && type == "ENVOYÉ" -> 0.035
            operator == "M-Vola" && type == "REÇU" -> 0.022
            operator == "M-Vola" && type == "ENVOYÉ" -> 0.032
            else -> 0.0
        }
        return amount * rate
    }
}
