package com.tinah.transactify.ui.reports

import com.tinah.transactify.domain.model.ReportData
import com.tinah.transactify.domain.model.ReportPeriod
import com.tinah.transactify.domain.usecase.GetReportDataUseCase
import com.tinah.transactify.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ReportViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getReportData = mock<GetReportDataUseCase>()

    private fun data(period: ReportPeriod, profit: Double) = ReportData(
        period = period,
        transactions = emptyList(),
        totalReceived = 0.0,
        totalSent = 0.0,
        totalProfit = profit,
        byOperator = emptyList(),
        dailyProfit = emptyList(),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `charge la periode par defaut puis reagit au changement`() = runTest {
        whenever(getReportData(ReportPeriod.LAST_30_DAYS)).thenReturn(flowOf(data(ReportPeriod.LAST_30_DAYS, 1_000.0)))
        whenever(getReportData(ReportPeriod.TODAY)).thenReturn(flowOf(data(ReportPeriod.TODAY, 50.0)))

        val viewModel = ReportViewModel(getReportData)
        val dataJob = launch { viewModel.reportData.collect {} }
        val periodJob = launch { viewModel.selectedPeriod.collect {} }
        advanceUntilIdle()

        assertEquals(ReportPeriod.LAST_30_DAYS, viewModel.selectedPeriod.value)
        assertEquals(1_000.0, viewModel.reportData.value?.totalProfit)

        viewModel.setPeriod(ReportPeriod.TODAY)
        advanceUntilIdle()

        assertEquals(ReportPeriod.TODAY, viewModel.selectedPeriod.value)
        assertEquals(50.0, viewModel.reportData.value?.totalProfit)

        dataJob.cancel()
        periodJob.cancel()
    }
}
