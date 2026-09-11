package com.tinah.transactify.ui.transactions

import com.tinah.transactify.data.db.entity.Transaction
import com.tinah.transactify.data.repository.TransactionRepository
import com.tinah.transactify.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mock<TransactionRepository>()

    private val transaction = Transaction(
        id = 1,
        operator = "Orange Money",
        amount = 100_000.0,
        transactionType = "ENVOYÉ",
        phoneNumber = "+26132000000",
        reference = "R1",
        timestamp = 1_000L,
        profitCalculated = 3_000.0,
    )

    @Test
    fun `expose la transaction convertie en TransactionItem`() = runTest {
        whenever(repository.getTransactionById(1)).thenReturn(flowOf(transaction))
        val viewModel = TransactionDetailViewModel(1, repository)
        val job = launch { viewModel.transaction.collect {} }
        advanceUntilIdle()

        assertEquals(100_000.0, viewModel.transaction.value?.amount)

        job.cancel()
    }

    @Test
    fun `delete supprime la transaction, rafraichit les stats du client et notifie`() = runTest {
        whenever(repository.getTransactionById(1)).thenReturn(flowOf(transaction))
        val viewModel = TransactionDetailViewModel(1, repository)
        val job = launch { viewModel.transaction.collect {} }
        advanceUntilIdle()

        var deleted = false
        viewModel.delete { deleted = true }
        advanceUntilIdle()

        verify(repository).deleteTransaction(transaction)
        verify(repository).refreshClientStats("+26132000000")
        assertTrue(deleted)

        job.cancel()
    }

    @Test
    fun `delete ne fait rien tant que la transaction n'a pas ete chargee`() = runTest {
        whenever(repository.getTransactionById(1)).thenReturn(flowOf(transaction))
        val viewModel = TransactionDetailViewModel(1, repository)
        // Pas de collecte de `transaction` ici : rawTransaction.value est encore `null`.

        var deleted = false
        viewModel.delete { deleted = true }
        advanceUntilIdle()

        verify(repository, never()).deleteTransaction(any())
        assertFalse(deleted)
    }

    @Test
    fun `updateProfit rejette un montant negatif sans appeler le repository`() = runTest {
        whenever(repository.getTransactionById(1)).thenReturn(flowOf(transaction))
        val viewModel = TransactionDetailViewModel(1, repository)
        val job = launch { viewModel.transaction.collect {} }
        advanceUntilIdle()

        viewModel.updateProfit(-100.0)
        advanceUntilIdle()

        verify(repository, never()).updateTransaction(any())

        job.cancel()
    }

    @Test
    fun `updateProfit valide met a jour uniquement le benefice`() = runTest {
        whenever(repository.getTransactionById(1)).thenReturn(flowOf(transaction))
        val viewModel = TransactionDetailViewModel(1, repository)
        val job = launch { viewModel.transaction.collect {} }
        advanceUntilIdle()

        viewModel.updateProfit(5_000.0)
        advanceUntilIdle()

        val captor = argumentCaptor<Transaction>()
        verify(repository).updateTransaction(captor.capture())
        assertEquals(5_000.0, captor.firstValue.profitCalculated, 0.0)
        assertEquals(transaction.amount, captor.firstValue.amount, 0.0) // le reste est inchangé

        job.cancel()
    }
}
