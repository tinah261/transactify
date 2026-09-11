package com.tinah.transactify.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tinah.transactify.di.AppContainer
import com.tinah.transactify.domain.model.OperatorType
import com.tinah.transactify.domain.model.TransactionItem
import com.tinah.transactify.domain.usecase.GetTransactionsUseCase
import com.tinah.transactify.domain.usecase.TransactionFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** ViewModel de l'écran Transactions : liste filtrable par opérateur. */
class TransactionViewModel(
    getTransactions: GetTransactionsUseCase,
) : ViewModel() {

    private val filter = MutableStateFlow(TransactionFilter())

    /** Opérateur actuellement sélectionné dans les chips de filtre (`null` = tous). */
    val selectedOperator: StateFlow<OperatorType?> = filter
        .map { it.operator }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), filter.value.operator)

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<TransactionItem>> = filter
        .flatMapLatest { getTransactions(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setOperatorFilter(operator: OperatorType?) {
        filter.update { it.copy(operator = operator) }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
                "ViewModel inconnu : ${modelClass.name}"
            }
            return TransactionViewModel(container.getTransactionsUseCase) as T
        }
    }
}
