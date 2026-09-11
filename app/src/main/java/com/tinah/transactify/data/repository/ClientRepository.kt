package com.tinah.transactify.data.repository

import com.tinah.transactify.data.db.dao.ClientDao
import com.tinah.transactify.data.db.entity.Client
import kotlinx.coroutines.flow.Flow

class ClientRepository(
    private val clientDao: ClientDao
) {

    fun getAllClients(): Flow<List<Client>> =
        clientDao.getAllClients()

    fun getClientsByClassification(classification: String): Flow<List<Client>> =
        clientDao.getClientsByClassification(classification)

    fun getTopClients(limit: Int = 10): Flow<List<Client>> =
        clientDao.getTopClients(limit)

    fun getClientCount(): Flow<Int> =
        clientDao.getClientCount()

    suspend fun getClientByPhone(phoneNumber: String): Client? =
        clientDao.getClientByPhone(phoneNumber)

    fun observeClientByPhone(phoneNumber: String): Flow<Client?> =
        clientDao.observeClientByPhone(phoneNumber)

    /** [query] vide = tous les clients, triés par volume décroissant. */
    fun searchClients(query: String): Flow<List<Client>> =
        clientDao.searchClients(query.trim())

    suspend fun updateClient(client: Client) =
        clientDao.updateClient(client)
}
