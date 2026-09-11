package com.tinah.transactify.domain.usecase

import com.tinah.transactify.data.repository.TransactionRepository
import com.tinah.transactify.domain.model.TransactionSummary
import com.tinah.transactify.domain.model.toItemOrNull
import com.tinah.transactify.utils.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

/** Fournit les agrégats du tableau de bord sous forme d'un flux unique. */
class GetDashboardSummaryUseCase(
    private val transactionRepository: TransactionRepository,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<TransactionSummary> =
        combine(
            transactionRepository.getTotalReceived(),
            transactionRepository.getTotalSent(),
            transactionRepository.getTotalProfit(),
            todayBoundaryTicker(),
        ) { received, sent, profit, _ ->
            Triple(received ?: 0.0, sent ?: 0.0, profit ?: 0.0)
        }.flatMapLatest { (received, sent, profit) ->
            val now = System.currentTimeMillis()
            combine(
                transactionRepository.getTransactionCountInRange(DateUtils.startOfDay(now), DateUtils.endOfDay(now)),
                transactionRepository.getLatestTransactions(LATEST_TRANSACTIONS_LIMIT),
            ) { countToday, latest ->
                TransactionSummary(received, sent, profit, countToday, latest.mapNotNull { it.toItemOrNull() })
            }
        }

    /**
     * Émet immédiatement puis à chaque passage de minuit, pour que la fenêtre
     * « aujourd'hui » de [transactionCountToday] se recale sans avoir besoin de
     * recréer le ViewModel — sinon les bornes de la journée, calculées une seule
     * fois à la création du flux, restent figées sur l'ancien jour pour une
     * session qui reste ouverte après minuit (pertinent ici : le point de
     * caisse tourne en continu, service de premier plan 24/7).
     */
    private fun todayBoundaryTicker(): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            val now = System.currentTimeMillis()
            delay((DateUtils.endOfDay(now) - now).coerceAtLeast(1_000L))
        }
    }

    private companion object {
        const val LATEST_TRANSACTIONS_LIMIT = 5
    }
}
