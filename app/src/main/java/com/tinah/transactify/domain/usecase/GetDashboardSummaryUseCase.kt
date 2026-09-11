package com.tinah.transactify.domain.usecase

import com.tinah.transactify.data.repository.TransactionRepository
import com.tinah.transactify.domain.model.TransactionSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Fournit les agrégats du tableau de bord sous forme d'un flux unique. */
class GetDashboardSummaryUseCase(
    private val transactionRepository: TransactionRepository,
) {

    operator fun invoke(): Flow<TransactionSummary> = combine(
        transactionRepository.getTotalReceived(),
        transactionRepository.getTotalSent(),
        transactionRepository.getTotalProfit(),
    ) { received, sent, profit ->
        TransactionSummary(
            totalReceived = received ?: 0.0,
            totalSent = sent ?: 0.0,
            totalProfit = profit ?: 0.0,
        )
    }
}
