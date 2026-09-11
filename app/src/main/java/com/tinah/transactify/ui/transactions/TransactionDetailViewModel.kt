package com.tinah.transactify.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tinah.transactify.data.db.entity.Transaction
import com.tinah.transactify.data.repository.TransactionRepository
import com.tinah.transactify.di.AppContainer
import com.tinah.transactify.domain.model.TransactionItem
import com.tinah.transactify.domain.model.toItemOrNull
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

/** ViewModel du détail d'une transaction (consultation, correction du bénéfice, suppression). */
class TransactionDetailViewModel(
    transactionId: Int,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val rawTransaction: StateFlow<Transaction?> =
        transactionRepository.getTransactionById(transactionId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val transaction: StateFlow<TransactionItem?> = rawTransaction
        .map { it?.toItemOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Supprime la transaction et rafraîchit les stats du client, puis appelle [onDeleted]. */
    fun delete(onDeleted: () -> Unit) {
        val current = rawTransaction.value ?: return
        viewModelScope.launch {
            transactionRepository.deleteTransaction(current)
            transactionRepository.refreshClientStats(current.phoneNumber)
            Timber.d("Transaction #%d supprimée", current.id)
            onDeleted()
        }
    }

    /** Corrige manuellement le bénéfice enregistré (ex. taux renégocié après coup). */
    fun updateProfit(newProfit: Double, onDone: () -> Unit = {}) {
        val current = rawTransaction.value ?: return
        if (newProfit < 0.0) return
        viewModelScope.launch {
            transactionRepository.updateTransaction(current.copy(profitCalculated = newProfit))
            onDone()
        }
    }

    class Factory(
        private val transactionId: Int,
        private val container: AppContainer,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(TransactionDetailViewModel::class.java)) {
                "ViewModel inconnu : ${modelClass.name}"
            }
            return TransactionDetailViewModel(transactionId, container.transactionRepository) as T
        }
    }
}
