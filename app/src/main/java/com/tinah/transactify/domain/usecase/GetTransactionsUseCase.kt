package com.tinah.transactify.domain.usecase

import com.tinah.transactify.data.repository.TransactionRepository
import com.tinah.transactify.domain.model.OperatorType
import com.tinah.transactify.domain.model.TransactionItem
import com.tinah.transactify.domain.model.toItemOrNull
import com.tinah.transactify.utils.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Filtre appliqué à la liste des transactions.
 *
 * @property operator `null` = tous les opérateurs.
 * @property dateRange `null` = toutes les dates.
 */
data class TransactionFilter(
    val operator: OperatorType? = null,
    val dateRange: ClosedRange<LocalDate>? = null,
)

/** Liste des transactions filtrée, triée du plus récent au plus ancien. */
class GetTransactionsUseCase(
    private val transactionRepository: TransactionRepository,
) {

    operator fun invoke(filter: TransactionFilter = TransactionFilter()): Flow<List<TransactionItem>> {
        val (startInclusive, endExclusive) = DateUtils.boundsOrAllTime(filter.dateRange)

        return transactionRepository
            .getFilteredTransactions(filter.operator?.storageValue, startInclusive, endExclusive)
            .map { transactions -> transactions.mapNotNull { it.toItemOrNull() } }
    }
}
