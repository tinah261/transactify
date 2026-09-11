package com.tinah.transactify.domain.usecase

import com.tinah.transactify.data.repository.ClientRepository
import com.tinah.transactify.domain.model.ClientItem
import com.tinah.transactify.domain.model.toItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Liste des clients, triée par volume décroissant, filtrable par nom/numéro. */
class GetClientsUseCase(
    private val clientRepository: ClientRepository,
) {

    operator fun invoke(query: String = ""): Flow<List<ClientItem>> =
        clientRepository.searchClients(query).map { clients -> clients.map { it.toItem() } }
}
