package com.tinah.transactify.ui.dashboard

import com.tinah.transactify.domain.model.OperatorBreakdown
import com.tinah.transactify.domain.model.OperatorType
import com.tinah.transactify.domain.model.ReportData
import com.tinah.transactify.domain.model.ReportPeriod
import com.tinah.transactify.domain.model.TransactionSummary
import com.tinah.transactify.domain.usecase.GetDashboardSummaryUseCase
import com.tinah.transactify.domain.usecase.GetReportDataUseCase
import com.tinah.transactify.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getDashboardSummary = mock<GetDashboardSummaryUseCase>()
    private val getReportData = mock<GetReportDataUseCase>()

    @Test
    fun `expose le resume du tableau de bord et la repartition par operateur`() = runTest {
        val summary = TransactionSummary(totalReceived = 100_000.0, totalSent = 50_000.0, totalProfit = 3_000.0, transactionCountToday = 4)
        val reportData = ReportData(
            period = ReportPeriod.ALL_TIME,
            transactions = emptyList(),
            totalReceived = 100_000.0,
            totalSent = 50_000.0,
            totalProfit = 3_000.0,
            byOperator = listOf(OperatorBreakdown(OperatorType.ORANGE_MONEY, 4, 100_000.0, 3_000.0)),
            dailyProfit = emptyList(),
        )
        whenever(getDashboardSummary()).thenReturn(flowOf(summary))
        whenever(getReportData(ReportPeriod.ALL_TIME)).thenReturn(flowOf(reportData))

        val viewModel = DashboardViewModel(getDashboardSummary, getReportData)
        val summaryJob = launch { viewModel.summary.collect {} }
        val breakdownJob = launch { viewModel.operatorBreakdown.collect {} }
        advanceUntilIdle()

        assertEquals(3_000.0, viewModel.summary.value.totalProfit, 0.0)
        assertEquals(4, viewModel.summary.value.transactionCountToday)
        assertEquals(1, viewModel.operatorBreakdown.value.size)
        assertEquals(OperatorType.ORANGE_MONEY, viewModel.operatorBreakdown.value.first().operator)

        summaryJob.cancel()
        breakdownJob.cancel()
    }

    @Test
    fun `valeurs initiales avant toute emission`() = runTest {
        whenever(getDashboardSummary()).thenReturn(emptyFlow())
        whenever(getReportData(ReportPeriod.ALL_TIME)).thenReturn(emptyFlow())

        val viewModel = DashboardViewModel(getDashboardSummary, getReportData)

        assertEquals(TransactionSummary(), viewModel.summary.value)
        assertEquals(emptyList<OperatorBreakdown>(), viewModel.operatorBreakdown.value)
    }
}
