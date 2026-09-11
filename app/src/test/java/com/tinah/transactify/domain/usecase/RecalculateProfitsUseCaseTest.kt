package com.tinah.transactify.domain.usecase

import com.tinah.transactify.data.db.entity.Transaction
import com.tinah.transactify.data.repository.TransactionRepository
import com.tinah.transactify.domain.model.CommissionRates
import com.tinah.transactify.domain.model.OperatorType
import com.tinah.transactify.domain.model.TransactionType
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class RecalculateProfitsUseCaseTest {

    private val repository = mock<TransactionRepository>()

    private fun transaction(
        id: Int,
        operator: String = "Orange Money",
        type: String = TransactionType.ENVOYE.storageValue,
        amount: Double = 100_000.0,
        bonusAmount: Double = 0.0,
        profit: Double,
    ) = Transaction(
        id = id,
        operator = operator,
        amount = amount,
        transactionType = type,
        phoneNumber = "+26132000000",
        reference = "R$id",
        timestamp = 1_000L,
        bonusAmount = bonusAmount,
        profitCalculated = profit,
    )

    @Test
    fun `met a jour uniquement les transactions dont le benefice change`() = runTest {
        // taux courant Orange envoye = 3% (CommissionRates.DEFAULT)
        val useCase = RecalculateProfitsUseCase(repository, CalculateProfitUseCase { CommissionRates.DEFAULT })
        val alreadyCorrect = transaction(id = 1, profit = 3_000.0) // 100 000 * 0.03
        val stale = transaction(id = 2, profit = 1_000.0) // taux a changé depuis
        whenever(repository.getAllTransactions()).thenReturn(flowOf(listOf(alreadyCorrect, stale)))

        val updatedCount = useCase()

        assertEquals(1, updatedCount)
        val captor = argumentCaptor<Transaction>()
        verify(repository).updateTransaction(captor.capture())
        assertEquals(2, captor.firstValue.id)
        assertEquals(3_000.0, captor.firstValue.profitCalculated, 0.0)
    }

    @Test
    fun `conserve le bonus deja rattache, ne le recalcule pas`() = runTest {
        val useCase = RecalculateProfitsUseCase(repository, CalculateProfitUseCase { CommissionRates.DEFAULT })
        // 100 000 * 0.03 (base) + 1 500 (bonus historique) = 4 500, mais profit stocké = 999 (périmé)
        val withBonus = transaction(id = 1, bonusAmount = 1_500.0, profit = 999.0)
        whenever(repository.getAllTransactions()).thenReturn(flowOf(listOf(withBonus)))

        useCase()

        val captor = argumentCaptor<Transaction>()
        verify(repository).updateTransaction(captor.capture())
        assertEquals(4_500.0, captor.firstValue.profitCalculated, 0.0)
        assertEquals(1_500.0, captor.firstValue.bonusAmount, 0.0)
    }

    @Test
    fun `ignore une transaction dont l'operateur ou le sens est corrompu`() = runTest {
        val useCase = RecalculateProfitsUseCase(repository, CalculateProfitUseCase { CommissionRates.DEFAULT })
        val corrupted = transaction(id = 1, operator = "Telma", profit = 0.0)
        whenever(repository.getAllTransactions()).thenReturn(flowOf(listOf(corrupted)))

        val updatedCount = useCase()

        assertEquals(0, updatedCount)
        verify(repository, never()).updateTransaction(any())
    }

    @Test
    fun `renvoie zero sans transactions`() = runTest {
        val useCase = RecalculateProfitsUseCase(repository, CalculateProfitUseCase { CommissionRates.DEFAULT })
        whenever(repository.getAllTransactions()).thenReturn(flowOf(emptyList()))

        assertEquals(0, useCase())
    }
}
