package com.tinah.transactify.domain.usecase

import com.tinah.transactify.data.db.entity.Transaction
import com.tinah.transactify.data.repository.TransactionRepository
import com.tinah.transactify.domain.model.OperatorType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

class GetTransactionsUseCaseTest {

    private val repository = mock<TransactionRepository>()
    private val useCase = GetTransactionsUseCase(repository)

    private fun transaction(operator: String, type: String) = Transaction(
        operator = operator,
        amount = 10_000.0,
        transactionType = type,
        phoneNumber = "+26132000000",
        reference = "R1",
        timestamp = 1_000L,
    )

    @Test
    fun `sans filtre, transmet toutes les dates et aucun operateur`() = runTest {
        whenever(repository.getFilteredTransactions(eq(null), eq(0L), eq(Long.MAX_VALUE)))
            .thenReturn(flowOf(listOf(transaction("Orange Money", "REÇU"))))

        val result = useCase(TransactionFilter()).first()

        assertEquals(1, result.size)
        assertEquals(OperatorType.ORANGE_MONEY, result.first().operator)
    }

    @Test
    fun `transmet la valeur de stockage de l'operateur filtre`() = runTest {
        whenever(repository.getFilteredTransactions(eq("M-Vola"), any(), any()))
            .thenReturn(flowOf(emptyList()))

        useCase(TransactionFilter(operator = OperatorType.MVOLA)).first()

        verify(repository).getFilteredTransactions(eq("M-Vola"), any(), any())
    }

    @Test
    fun `convertit une periode de dates en bornes epoch coherentes`() = runTest {
        // filter.operator est null ici -> le matcher doit accepter null (any() ne le fait pas)
        whenever(repository.getFilteredTransactions(anyOrNull(), any(), any())).thenReturn(flowOf(emptyList()))
        val day = LocalDate.of(2026, 9, 10)

        useCase(TransactionFilter(dateRange = day..day)).first()

        val startCaptor = argumentCaptor<Long>()
        val endCaptor = argumentCaptor<Long>()
        verify(repository).getFilteredTransactions(anyOrNull(), startCaptor.capture(), endCaptor.capture())
        // la fenêtre couvre exactement 24h (une seule journée)
        assertEquals(24 * 60 * 60 * 1000L, endCaptor.firstValue - startCaptor.firstValue)
    }

    @Test
    fun `ignore les transactions dont l'operateur ou le sens sont corrompus`() = runTest {
        whenever(repository.getFilteredTransactions(anyOrNull(), any(), any()))
            .thenReturn(flowOf(listOf(transaction("Telma", "REÇU"), transaction("Orange Money", "REÇU"))))

        val result = useCase(TransactionFilter()).first()

        assertEquals(1, result.size)
    }
}
