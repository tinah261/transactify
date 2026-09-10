package com.tinah.transactify.utils

import com.tinah.transactify.data.db.dao.TransactionDao
import com.tinah.transactify.data.db.entity.Transaction
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any

class BonusMatchingServiceTest {

    @Test
    fun `links bonus to matching parent transaction and removes the orphan`() = runTest {
        val dao = mock(TransactionDao::class.java)
        val service = BonusMatchingService(dao)

        val parent = Transaction(
            id = 1,
            operator = "Orange Money",
            amount = 50_000.0,
            transactionType = "ENVOYÉ",
            phoneNumber = "+26132123456",
            reference = "ABC123",
            timestamp = 1_000_000L
        )
        val bonus = Transaction(
            id = 2,
            operator = "Orange Money",
            amount = 1_500.0,
            transactionType = "REÇU",
            phoneNumber = "+26132123456",
            reference = "ABC123",
            timestamp = 1_050_000L
        )

        `when`(dao.getTransactionsByDateRange(any(), any())).thenReturn(flowOf(listOf(parent)))

        service.matchBonusToTransaction(bonus)

        val captor = org.mockito.kotlin.argumentCaptor<Transaction>()
        verify(dao).updateTransaction(captor.capture())
        assertEquals(1_500.0, captor.firstValue.bonusAmount, 0.0)
        assertTrue(captor.firstValue.bonusLinked)
        assertEquals(50_000.0 * 0.03 + 1_500.0, captor.firstValue.profitCalculated, 0.0)

        verify(dao).deleteTransaction(bonus)
    }

    @Test
    fun `does nothing when no parent transaction matches`() = runTest {
        val dao = mock(TransactionDao::class.java)
        val service = BonusMatchingService(dao)

        val bonus = Transaction(
            id = 2,
            operator = "Orange Money",
            amount = 1_500.0,
            transactionType = "REÇU",
            phoneNumber = "+26132123456",
            reference = "NOMATCH",
            timestamp = 1_050_000L
        )

        `when`(dao.getTransactionsByDateRange(any(), any())).thenReturn(flowOf(emptyList()))

        service.matchBonusToTransaction(bonus)

        verify(dao, org.mockito.Mockito.never()).updateTransaction(any())
        verify(dao, org.mockito.Mockito.never()).deleteTransaction(any())
    }
}
