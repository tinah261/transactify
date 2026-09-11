package com.tinah.transactify.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tinah.transactify.di.AppContainer
import com.tinah.transactify.domain.model.ClientItem
import com.tinah.transactify.domain.usecase.GetClientsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/** ViewModel de l'écran Clients : liste triée par volume, filtrable par recherche. */
class ClientViewModel(
    getClients: GetClientsUseCase,
) : ViewModel() {

    private val query = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val clients: StateFlow<List<ClientItem>> = query
        .flatMapLatest { getClients(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(text: String) {
        query.value = text
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ClientViewModel::class.java)) {
                "ViewModel inconnu : ${modelClass.name}"
            }
            return ClientViewModel(container.getClientsUseCase) as T
        }
    }
}
