package com.tinah.transactify.domain.usecase

import com.tinah.transactify.data.db.entity.Transaction
import com.tinah.transactify.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GetDashboardSummaryUseCaseTest {

    private val repository = mock<TransactionRepository>()

    private fun stubDefaults(latest: List<Transaction> = emptyList()) {
        whenever(repository.getTotalReceived()).thenReturn(flowOf(100_000.0))
        whenever(repository.getTotalSent()).thenReturn(flowOf(null))
        whenever(repository.getTotalProfit()).thenReturn(flowOf(3_000.0))
        whenever(repository.getTransactionCountInRange(any(), any())).thenReturn(flowOf(4))
        whenever(repository.getLatestTransactions(any())).thenReturn(flowOf(latest))
    }

    @Test
    fun `combine les totaux et le compte du jour, en tolerant les nulls`() = runTest {
        stubDefaults()

        val summary = GetDashboardSummaryUseCase(repository)().first()

        assertEquals(100_000.0, summary.totalReceived, 0.0)
        assertEquals(0.0, summary.totalSent, 0.0)
        assertEquals(3_000.0, summary.totalProfit, 0.0)
        assertEquals(4, summary.transactionCountToday)
    }

    @Test
    fun `convertit les dernieres transactions en TransactionItem, en ignorant les corrompues`() = runTest {
        val valid = Transaction(
            operator = "Orange Money",
            amount = 10_000.0,
            transactionType = "REÇU",
            phoneNumber = "+26132000000",
            reference = "R1",
            timestamp = 1_000L,
        )
        val corrupted = valid.copy(id = 2, operator = "Telma")
        stubDefaults(latest = listOf(valid, corrupted))

        val summary = GetDashboardSummaryUseCase(repository)().first()

        assertEquals(1, summary.latestTransactions.size)
    }
}
