package com.tinah.transactify.domain.usecase

import com.tinah.transactify.data.db.entity.Transaction
import com.tinah.transactify.data.repository.TransactionRepository
import com.tinah.transactify.domain.model.OperatorType
import com.tinah.transactify.domain.model.ReportPeriod
import com.tinah.transactify.domain.model.TransactionType
import com.tinah.transactify.utils.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GetReportDataUseCaseTest {

    private val repository = mock<TransactionRepository>()
    private val useCase = GetReportDataUseCase(repository)

    private fun transaction(
        operator: String,
        type: String,
        amount: Double,
        profit: Double,
        timestamp: Long,
    ) = Transaction(
        operator = operator,
        amount = amount,
        transactionType = type,
        phoneNumber = "+26132000000",
        reference = "R1",
        timestamp = timestamp,
        profitCalculated = profit,
    )

    @Test
    fun `agrege les totaux et la ventilation par operateur`() = runTest {
        val day = DateUtils.startOfDay(System.currentTimeMillis()) + 1_000L
        whenever(repository.getFilteredTransactions(anyOrNull(), any(), any())).thenReturn(
            flowOf(
                listOf(
                    transaction("Orange Money", TransactionType.RECU.storageValue, 100_000.0, 2_000.0, day),
                    transaction("Orange Money", TransactionType.ENVOYE.storageValue, 50_000.0, 1_500.0, day),
                    transaction("M-Vola", TransactionType.RECU.storageValue, 20_000.0, 400.0, day),
                ),
            ),
        )

        val data = useCase(ReportPeriod.TODAY).first()

        assertEquals(120_000.0, data.totalReceived, 0.0)
        assertEquals(50_000.0, data.totalSent, 0.0)
        assertEquals(3_900.0, data.totalProfit, 0.0)
        assertEquals(2, data.byOperator.size)

        val orange = data.byOperator.first { it.operator == OperatorType.ORANGE_MONEY }
        assertEquals(2, orange.transactionCount)
        assertEquals(150_000.0, orange.totalVolume, 0.0)
        assertEquals(3_500.0, orange.totalProfit, 0.0)
    }

    @Test
    fun `groupe le benefice quotidien par jour local`() = runTest {
        val today = DateUtils.startOfDay(System.currentTimeMillis()) + 1_000L
        val yesterday = today - 26 * 60 * 60 * 1000L // au moins 24h avant, jour civil différent
        whenever(repository.getFilteredTransactions(anyOrNull(), any(), any())).thenReturn(
            flowOf(
                listOf(
                    transaction("Orange Money", TransactionType.RECU.storageValue, 10_000.0, 300.0, today),
                    transaction("Orange Money", TransactionType.RECU.storageValue, 10_000.0, 300.0, yesterday),
                ),
            ),
        )

        val data = useCase(ReportPeriod.ALL_TIME).first()

        assertEquals(2, data.dailyProfit.size)
        assertEquals(300.0, data.dailyProfit.sumOf { it.profit } / 2, 0.0)
    }

    @Test
    fun `aucune transaction donne des listes vides et des totaux a zero`() = runTest {
        whenever(repository.getFilteredTransactions(anyOrNull(), any(), any())).thenReturn(flowOf(emptyList()))

        val data = useCase(ReportPeriod.ALL_TIME).first()

        assertEquals(0.0, data.totalProfit, 0.0)
        assertEquals(emptyList<Any>(), data.byOperator)
        assertEquals(emptyList<Any>(), data.dailyProfit)
    }
}
