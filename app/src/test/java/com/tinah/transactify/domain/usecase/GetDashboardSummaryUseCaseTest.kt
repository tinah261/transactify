package com.tinah.transactify.domain.usecase

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

    @Test
    fun `combine les totaux et le compte du jour, en tolerant les nulls`() = runTest {
        val repository = mock<TransactionRepository>()
        whenever(repository.getTotalReceived()).thenReturn(flowOf(100_000.0))
        whenever(repository.getTotalSent()).thenReturn(flowOf(null))
        whenever(repository.getTotalProfit()).thenReturn(flowOf(3_000.0))
        whenever(repository.getTransactionCountInRange(any(), any())).thenReturn(flowOf(4))

        val summary = GetDashboardSummaryUseCase(repository)().first()

        assertEquals(100_000.0, summary.totalReceived, 0.0)
        assertEquals(0.0, summary.totalSent, 0.0)
        assertEquals(3_000.0, summary.totalProfit, 0.0)
        assertEquals(4, summary.transactionCountToday)
    }
}
