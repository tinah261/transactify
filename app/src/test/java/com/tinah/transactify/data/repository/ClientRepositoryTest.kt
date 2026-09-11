package com.tinah.transactify.data.repository

import com.tinah.transactify.data.db.dao.ClientDao
import com.tinah.transactify.data.db.entity.Client
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ClientRepositoryTest {

    private val clientDao = mock<ClientDao>()
    private val repository = ClientRepository(clientDao)

    private val client = Client(phoneNumber = "+26132000000")

    @Test
    fun `getAllClients delegue au DAO`() = runTest {
        whenever(clientDao.getAllClients()).thenReturn(flowOf(listOf(client)))
        assertEquals(listOf(client), repository.getAllClients().first())
    }

    @Test
    fun `getClientsByClassification delegue au DAO`() = runTest {
        whenever(clientDao.getClientsByClassification("VIP")).thenReturn(flowOf(listOf(client)))
        assertEquals(listOf(client), repository.getClientsByClassification("VIP").first())
    }

    @Test
    fun `getTopClients transmet la limite`() = runTest {
        whenever(clientDao.getTopClients(3)).thenReturn(flowOf(listOf(client)))
        repository.getTopClients(3).first()
        verify(clientDao).getTopClients(3)
    }

    @Test
    fun `getClientCount delegue au DAO`() = runTest {
        whenever(clientDao.getClientCount()).thenReturn(flowOf(7))
        assertEquals(7, repository.getClientCount().first())
    }

    @Test
    fun `getClientByPhone et observeClientByPhone delegue au DAO`() = runTest {
        whenever(clientDao.getClientByPhone("+26132000000")).thenReturn(client)
        whenever(clientDao.observeClientByPhone("+26132000000")).thenReturn(flowOf(client))

        assertEquals(client, repository.getClientByPhone("+26132000000"))
        assertEquals(client, repository.observeClientByPhone("+26132000000").first())
    }

    @Test
    fun `searchClients retire les espaces superflus avant de transmettre la requete`() = runTest {
        whenever(clientDao.searchClients("rakoto")).thenReturn(flowOf(listOf(client)))

        repository.searchClients("  rakoto  ").first()

        verify(clientDao).searchClients(eq("rakoto"))
    }

    @Test
    fun `updateClient delegue au DAO`() = runTest {
        repository.updateClient(client)
        verify(clientDao).updateClient(client)
    }
}
