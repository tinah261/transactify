package com.tinah.transactify.domain.model

import java.time.LocalDate

/** Agrégats d'un opérateur sur la période du rapport. */
data class OperatorBreakdown(
    val operator: OperatorType,
    val transactionCount: Int,
    val totalVolume: Double,
    val totalProfit: Double,
)

/** Bénéfice cumulé d'une journée (fuseau de l'appareil), pour le graphique en barres. */
data class DailyProfit(
    val date: LocalDate,
    val profit: Double,
)

/** Contenu complet de l'écran/export Rapports pour une [période][period] donnée. */
data class ReportData(
    val period: ReportPeriod,
    val transactions: List<TransactionItem>,
    val totalReceived: Double,
    val totalSent: Double,
    val totalProfit: Double,
    val byOperator: List<OperatorBreakdown>,
    val dailyProfit: List<DailyProfit>,
)
