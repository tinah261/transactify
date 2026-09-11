package com.tinah.transactify.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tinah.transactify.data.db.entity.Client
import com.tinah.transactify.data.repository.ClientRepository
import com.tinah.transactify.data.repository.TransactionRepository
import com.tinah.transactify.di.AppContainer
import com.tinah.transactify.domain.model.ClientItem
import com.tinah.transactify.domain.model.TransactionItem
import com.tinah.transactify.domain.model.toItem
import com.tinah.transactify.domain.model.toItemOrNull
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel du détail d'un client : stats, historique de transactions, renommage. */
class ClientDetailViewModel(
    phoneNumber: String,
    private val clientRepository: ClientRepository,
    transactionRepository: TransactionRepository,
) : ViewModel() {

    private val rawClient: StateFlow<Client?> =
        clientRepository.observeClientByPhone(phoneNumber)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val client: StateFlow<ClientItem?> = rawClient
        .map { it?.toItem() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val transactions: StateFlow<List<TransactionItem>> = transactionRepository
        .getTransactionsByClient(phoneNumber)
        .map { transactions -> transactions.mapNotNull { it.toItemOrNull() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** `newName` vide = efface le nom (le numéro sert alors d'affichage). */
    fun updateName(newName: String) {
        val current = rawClient.value ?: return
        viewModelScope.launch {
            clientRepository.updateClient(current.copy(name = newName.trim().ifBlank { null }))
        }
    }

    class Factory(
        private val phoneNumber: String,
        private val container: AppContainer,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ClientDetailViewModel::class.java)) {
                "ViewModel inconnu : ${modelClass.name}"
            }
            return ClientDetailViewModel(
                phoneNumber,
                container.clientRepository,
                container.transactionRepository,
            ) as T
        }
    }
}
