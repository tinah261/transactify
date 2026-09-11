package com.tinah.transactify.utils

import com.tinah.transactify.data.db.dao.TransactionDao
import com.tinah.transactify.data.db.entity.Transaction
import com.tinah.transactify.domain.model.CommissionRates
import com.tinah.transactify.domain.model.TransactionType
import com.tinah.transactify.domain.usecase.CalculateProfitUseCase
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor

class BonusMatchingServiceTest {

    private val calculateProfit = CalculateProfitUseCase { CommissionRates.DEFAULT }

    private fun envoye(id: Int, ref: String, ts: Long) = Transaction(
        id = id,
        operator = "Orange Money",
        amount = 50_000.0,
        transactionType = TransactionType.ENVOYE.storageValue,
        phoneNumber = "+26132123456",
        reference = ref,
        timestamp = ts,
    )

    private fun bonus(id: Int, ref: String, ts: Long) = Transaction(
        id = id,
        operator = "Orange Money",
        amount = 1_500.0,
        transactionType = TransactionType.RECU.storageValue,
        phoneNumber = "+26132123456",
        reference = ref,
        timestamp = ts,
    )

    @Test
    fun `rattache le bonus a la transaction mere et supprime l'orphelin`() = runTest {
        val dao = mock(TransactionDao::class.java)
        val service = BonusMatchingService(dao, calculateProfit)
        val parent = envoye(id = 1, ref = "ABC123", ts = 1_000_000L)

        `when`(dao.getTransactionsByDateRange(any(), any())).thenReturn(flowOf(listOf(parent)))

        service.matchBonusToTransaction(bonus(id = 2, ref = "ABC123", ts = 1_050_000L))

        val captor = argumentCaptor<Transaction>()
        verify(dao).updateTransaction(captor.capture())
        assertEquals(1_500.0, captor.firstValue.bonusAmount, 0.0)
        assertTrue(captor.firstValue.bonusLinked)
        // 50 000 * 0.03 (Orange envoye) + 1 500 de bonus
        assertEquals(1_500.0 + 1_500.0, captor.firstValue.profitCalculated, 0.0)
        verify(dao).deleteTransaction(any())
    }

    @Test
    fun `ne fait rien sans transaction mere correspondante`() = runTest {
        val dao = mock(TransactionDao::class.java)
        val service = BonusMatchingService(dao, calculateProfit)

        `when`(dao.getTransactionsByDateRange(any(), any())).thenReturn(flowOf(emptyList()))

        service.matchBonusToTransaction(bonus(id = 2, ref = "NOMATCH", ts = 1_050_000L))

        verify(dao, never()).updateTransaction(any())
        verify(dao, never()).deleteTransaction(any())
    }

    @Test
    fun `ignore un bonus deja rattache`() = runTest {
        val dao = mock(TransactionDao::class.java)
        val service = BonusMatchingService(dao, calculateProfit)

        service.matchBonusToTransaction(
            bonus(id = 2, ref = "ABC123", ts = 1_050_000L).copy(bonusLinked = true),
        )

        verify(dao, never()).getTransactionsByDateRange(any(), any())
    }
}
