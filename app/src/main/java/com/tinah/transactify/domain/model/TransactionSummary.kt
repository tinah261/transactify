package com.tinah.transactify.domain.model

/**
 * Agrégats affichés sur le tableau de bord.
 *
 * @property totalReceived somme des montants reçus (Ar).
 * @property totalSent somme des montants envoyés (Ar).
 * @property totalProfit bénéfice cumulé (commissions + bonus) (Ar).
 * @property transactionCountToday nombre de transactions du jour.
 * @property latestTransactions dernières transactions (aperçu, voir écran Transactions pour la liste complète).
 */
data class TransactionSummary(
    val totalReceived: Double = 0.0,
    val totalSent: Double = 0.0,
    val totalProfit: Double = 0.0,
    val transactionCountToday: Int = 0,
    val latestTransactions: List<TransactionItem> = emptyList(),
)
