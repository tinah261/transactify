package com.tinah.transactify.ui.clients

import com.tinah.transactify.data.db.entity.Client
import com.tinah.transactify.data.db.entity.Transaction
import com.tinah.transactify.data.repository.ClientRepository
import com.tinah.transactify.data.repository.TransactionRepository
import com.tinah.transactify.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ClientDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clientRepository = mock<ClientRepository>()
    private val transactionRepository = mock<TransactionRepository>()

    private val phone = "+26132000000"
    private val client = Client(id = 1, phoneNumber = phone, name = "Rakoto")

    private fun viewModel(): ClientDetailViewModel {
        whenever(clientRepository.observeClientByPhone(phone)).thenReturn(flowOf(client))
        whenever(transactionRepository.getTransactionsByClient(phone)).thenReturn(flowOf(emptyList()))
        return ClientDetailViewModel(phone, clientRepository, transactionRepository)
    }

    @Test
    fun `expose le client et son historique de transactions`() = runTest {
        val transaction = Transaction(
            operator = "Orange Money",
            amount = 10_000.0,
            transactionType = "REÇU",
            phoneNumber = phone,
            reference = "R1",
            timestamp = 1_000L,
        )
        whenever(clientRepository.observeClientByPhone(phone)).thenReturn(flowOf(client))
        whenever(transactionRepository.getTransactionsByClient(phone)).thenReturn(flowOf(listOf(transaction)))

        val viewModel = ClientDetailViewModel(phone, clientRepository, transactionRepository)
        val clientJob = launch { viewModel.client.collect {} }
        val txJob = launch { viewModel.transactions.collect {} }
        advanceUntilIdle()

        assertEquals("Rakoto", viewModel.client.value?.name)
        assertEquals(1, viewModel.transactions.value.size)

        clientJob.cancel()
        txJob.cancel()
    }

    @Test
    fun `updateName vide efface le nom (le numero sert d'affichage)`() = runTest {
        val viewModel = viewModel()
        val job = launch { viewModel.client.collect {} }
        advanceUntilIdle()

        viewModel.updateName("   ")
        advanceUntilIdle()

        val captor = argumentCaptor<Client>()
        verify(clientRepository).updateClient(captor.capture())
        assertNull(captor.firstValue.name)

        job.cancel()
    }

    @Test
    fun `updateName retire les espaces superflus`() = runTest {
        val viewModel = viewModel()
        val job = launch { viewModel.client.collect {} }
        advanceUntilIdle()

        viewModel.updateName("  Rabe  ")
        advanceUntilIdle()

        val captor = argumentCaptor<Client>()
        verify(clientRepository).updateClient(captor.capture())
        assertEquals("Rabe", captor.firstValue.name)

        job.cancel()
    }

    @Test
    fun `updateName ne fait rien tant que le client n'a pas ete charge`() = runTest {
        whenever(clientRepository.observeClientByPhone(phone)).thenReturn(flowOf(client))
        whenever(transactionRepository.getTransactionsByClient(phone)).thenReturn(flowOf(emptyList()))
        val viewModel = ClientDetailViewModel(phone, clientRepository, transactionRepository)
        // Pas de collecte : rawClient.value est encore `null`.

        viewModel.updateName("Rabe")
        advanceUntilIdle()

        verify(clientRepository, never()).updateClient(any())
    }
}
