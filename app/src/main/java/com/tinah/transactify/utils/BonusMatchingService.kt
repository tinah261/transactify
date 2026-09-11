package com.tinah.transactify.utils

import com.tinah.transactify.data.db.entity.Transaction
import com.tinah.transactify.data.repository.TransactionRepository
import com.tinah.transactify.domain.model.TransactionType
import com.tinah.transactify.domain.usecase.CalculateProfitUseCase
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Rapproche un SMS de bonus / récompense de sa transaction mère (l'envoi qui l'a
 * déclenché) : le montant du bonus est absorbé dans la transaction mère et le
 * SMS orphelin est supprimé.
 */
class BonusMatchingService(
    private val transactionRepository: TransactionRepository,
    private val calculateProfit: CalculateProfitUseCase,
) {

    suspend fun matchBonusToTransaction(bonusTransaction: Transaction) {
        if (bonusTransaction.bonusLinked) return

        val candidates = transactionRepository
            .getTransactionsByDateRange(
                bonusTransaction.timestamp - Constants.BONUS_MATCH_WINDOW_MS,
                bonusTransaction.timestamp,
            )
            .first()

        val parent = candidates.firstOrNull { candidate ->
            candidate.id != bonusTransaction.id &&
                candidate.reference == bonusTransaction.reference &&
                candidate.operator == bonusTransaction.operator &&
                candidate.transactionType == TransactionType.ENVOYE.storageValue
        }

        if (parent == null) {
            Timber.d("Bonus #%d sans transaction mère (ref %s)", bonusTransaction.id, bonusTransaction.reference)
            return
        }

        val baseProfit = calculateProfit.forStoredValues(
            amount = parent.amount,
            operatorStorageValue = parent.operator,
            typeStorageValue = parent.transactionType,
        )

        transactionRepository.updateTransaction(
            parent.copy(
                bonusAmount = bonusTransaction.amount,
                bonusLinked = true,
                profitCalculated = baseProfit + bonusTransaction.amount,
            ),
        )
        transactionRepository.deleteTransaction(bonusTransaction)

        // La fusion + suppression ci-dessus laisse les stats du client périmées
        // (elles avaient été calculées à l'insertion du bonus, avant fusion) :
        // on les recalcule pour le(s) numéro(s) concerné(s).
        transactionRepository.refreshClientStats(parent.phoneNumber)
        if (bonusTransaction.phoneNumber != parent.phoneNumber) {
            transactionRepository.refreshClientStats(bonusTransaction.phoneNumber)
        }

        Timber.d("Bonus #%d rattaché à la transaction #%d", bonusTransaction.id, parent.id)
    }
}
