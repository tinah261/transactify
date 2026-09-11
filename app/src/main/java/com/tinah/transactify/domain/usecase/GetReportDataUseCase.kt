package com.tinah.transactify.domain.usecase

import com.tinah.transactify.data.repository.TransactionRepository
import com.tinah.transactify.domain.model.DailyProfit
import com.tinah.transactify.domain.model.OperatorBreakdown
import com.tinah.transactify.domain.model.OperatorType
import com.tinah.transactify.domain.model.ReportData
import com.tinah.transactify.domain.model.ReportPeriod
import com.tinah.transactify.domain.model.TransactionItem
import com.tinah.transactify.domain.model.TransactionType
import com.tinah.transactify.domain.model.toItemOrNull
import com.tinah.transactify.utils.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Construit le contenu de l'écran Rapports pour une [ReportPeriod] : totaux,
 * ventilation par opérateur, bénéfice quotidien. Le regroupement par jour se
 * fait côté Kotlin (pas en SQL) : il doit respecter le fuseau horaire de
 * l'appareil, ce que `GROUP BY` sur un timestamp UTC ne peut pas faire
 * correctement (même piège que la Phase 2 — voir [DateUtils]). La période est
 * bornée (30 jours max hors "tout"), donc le volume à trier en mémoire reste
 * raisonnable pour un cash point.
 */
class GetReportDataUseCase(
    private val transactionRepository: TransactionRepository,
) {

    operator fun invoke(period: ReportPeriod): Flow<ReportData> {
        val (startInclusive, endExclusive) = DateUtils.boundsOrAllTime(period.toDateRange())

        return transactionRepository
            .getFilteredTransactions(operator = null, startInclusive, endExclusive)
            .map { transactions -> build(period, transactions.mapNotNull { it.toItemOrNull() }) }
    }

    private fun build(period: ReportPeriod, items: List<TransactionItem>): ReportData {
        val totalReceived = items.filter { it.type == TransactionType.RECU }.sumOf { it.amount }
        val totalSent = items.filter { it.type == TransactionType.ENVOYE }.sumOf { it.amount }
        val totalProfit = items.sumOf { it.profit }

        val byOperator = OperatorType.entries.mapNotNull { operator ->
            val operatorItems = items.filter { it.operator == operator }
            if (operatorItems.isEmpty()) return@mapNotNull null
            OperatorBreakdown(
                operator = operator,
                transactionCount = operatorItems.size,
                totalVolume = operatorItems.sumOf { it.amount },
                totalProfit = operatorItems.sumOf { it.profit },
            )
        }

        val dailyProfit = items
            .groupBy { DateUtils.toLocalDate(it.timestamp) }
            .map { (date, dayItems) -> DailyProfit(date, dayItems.sumOf { it.profit }) }
            .sortedBy { it.date }

        return ReportData(
            period = period,
            transactions = items,
            totalReceived = totalReceived,
            totalSent = totalSent,
            totalProfit = totalProfit,
            byOperator = byOperator,
            dailyProfit = dailyProfit,
        )
    }
}
