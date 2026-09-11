package com.tinah.transactify.utils

import com.tinah.transactify.data.db.entity.Transaction
import com.tinah.transactify.data.repository.TransactionRepository
import com.tinah.transactify.domain.model.CommissionRates
import com.tinah.transactify.domain.model.TransactionType
import com.tinah.transactify.domain.usecase.CalculateProfitUseCase
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class BonusMatchingServiceTest {

    private val calculateProfit = CalculateProfitUseCase { CommissionRates.DEFAULT }
    private val phone = "+26132123456"

    private fun envoye(id: Int, ref: String, ts: Long, phoneNumber: String = phone) = Transaction(
        id = id,
        operator = "Orange Money",
        amount = 50_000.0,
        transactionType = TransactionType.ENVOYE.storageValue,
        phoneNumber = phoneNumber,
        reference = ref,
        timestamp = ts,
    )

    private fun bonus(id: Int, ref: String, ts: Long, phoneNumber: String = phone) = Transaction(
        id = id,
        operator = "Orange Money",
        amount = 1_500.0,
        transactionType = TransactionType.RECU.storageValue,
        phoneNumber = phoneNumber,
        reference = ref,
        timestamp = ts,
    )

    @Test
    fun `rattache le bonus a la transaction mere, supprime l'orphelin et rafraichit les stats client`() = runTest {
        val repository = mock<TransactionRepository>()
        val service = BonusMatchingService(repository, calculateProfit)
        val parent = envoye(id = 1, ref = "ABC123", ts = 1_000_000L)

        whenever(repository.getTransactionsByDateRange(any(), any())).thenReturn(flowOf(listOf(parent)))

        service.matchBonusToTransaction(bonus(id = 2, ref = "ABC123", ts = 1_050_000L))

        val captor = argumentCaptor<Transaction>()
        verify(repository).updateTransaction(captor.capture())
        assertEquals(1_500.0, captor.firstValue.bonusAmount, 0.0)
        assertTrue(captor.firstValue.bonusLinked)
        // 50 000 * 0.03 (Orange envoye) + 1 500 de bonus
        assertEquals(1_500.0 + 1_500.0, captor.firstValue.profitCalculated, 0.0)
        verify(repository).deleteTransaction(any())
        // Les stats du client doivent être recalculées après la fusion, sinon
        // elles restent périmées (comptaient encore le bonus comme un REÇU séparé).
        verify(repository).refreshClientStats(phone)
    }

    @Test
    fun `rafraichit les deux numeros si bonus et parent different (cas limite)`() = runTest {
        val repository = mock<TransactionRepository>()
        val service = BonusMatchingService(repository, calculateProfit)
        val parent = envoye(id = 1, ref = "ABC123", ts = 1_000_000L, phoneNumber = "+26132000001")

        whenever(repository.getTransactionsByDateRange(any(), any())).thenReturn(flowOf(listOf(parent)))

        service.matchBonusToTransaction(bonus(id = 2, ref = "ABC123", ts = 1_050_000L, phoneNumber = "+26132000002"))

        verify(repository).refreshClientStats("+26132000001")
        verify(repository).refreshClientStats("+26132000002")
    }

    @Test
    fun `ne fait rien sans transaction mere correspondante`() = runTest {
        val repository = mock<TransactionRepository>()
        val service = BonusMatchingService(repository, calculateProfit)

        whenever(repository.getTransactionsByDateRange(any(), any())).thenReturn(flowOf(emptyList()))

        service.matchBonusToTransaction(bonus(id = 2, ref = "NOMATCH", ts = 1_050_000L))

        verify(repository, never()).updateTransaction(any())
        verify(repository, never()).deleteTransaction(any())
        verify(repository, never()).refreshClientStats(any())
    }

    @Test
    fun `ignore un bonus deja rattache`() = runTest {
        val repository = mock<TransactionRepository>()
        val service = BonusMatchingService(repository, calculateProfit)

        service.matchBonusToTransaction(
            bonus(id = 2, ref = "ABC123", ts = 1_050_000L).copy(bonusLinked = true),
        )

        verify(repository, never()).getTransactionsByDateRange(any(), any())
    }
}
