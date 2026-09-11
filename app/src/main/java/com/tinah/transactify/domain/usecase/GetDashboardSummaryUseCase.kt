package com.tinah.transactify.domain.usecase

import com.tinah.transactify.data.repository.TransactionRepository
import com.tinah.transactify.domain.model.TransactionSummary
import com.tinah.transactify.utils.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Fournit les agrégats du tableau de bord sous forme d'un flux unique. */
class GetDashboardSummaryUseCase(
    private val transactionRepository: TransactionRepository,
) {

    operator fun invoke(): Flow<TransactionSummary> {
        val now = System.currentTimeMillis()
        val startOfToday = DateUtils.startOfDay(now)
        val endOfToday = DateUtils.endOfDay(now)

        return combine(
            transactionRepository.getTotalReceived(),
            transactionRepository.getTotalSent(),
            transactionRepository.getTotalProfit(),
            transactionRepository.getTransactionCountInRange(startOfToday, endOfToday),
        ) { received, sent, profit, countToday ->
            TransactionSummary(
                totalReceived = received ?: 0.0,
                totalSent = sent ?: 0.0,
                totalProfit = profit ?: 0.0,
                transactionCountToday = countToday,
            )
        }
    }
}
