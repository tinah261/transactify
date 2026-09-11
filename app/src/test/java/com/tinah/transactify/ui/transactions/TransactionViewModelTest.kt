package com.tinah.transactify.ui.transactions

import com.tinah.transactify.domain.model.OperatorType
import com.tinah.transactify.domain.model.TransactionItem
import com.tinah.transactify.domain.model.TransactionType
import com.tinah.transactify.domain.usecase.GetTransactionsUseCase
import com.tinah.transactify.domain.usecase.TransactionFilter
import com.tinah.transactify.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class TransactionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getTransactions = mock<GetTransactionsUseCase>()

    private fun item(operator: OperatorType) = TransactionItem(
        id = 1,
        operator = operator,
        type = TransactionType.RECU,
        amount = 10_000.0,
        phoneNumber = "+26132000000",
        reference = null,
        timestamp = 0L,
        bonusAmount = 0.0,
        bonusLinked = false,
        profit = 0.0,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `changer le filtre relance la requete avec l'operateur choisi`() = runTest {
        whenever(getTransactions(TransactionFilter(operator = null)))
            .thenReturn(flowOf(listOf(item(OperatorType.ORANGE_MONEY))))
        whenever(getTransactions(TransactionFilter(operator = OperatorType.MVOLA)))
            .thenReturn(flowOf(listOf(item(OperatorType.MVOLA))))

        val viewModel = TransactionViewModel(getTransactions)
        val transactionsJob = launch { viewModel.transactions.collect {} }
        val selectedJob = launch { viewModel.selectedOperator.collect {} }
        advanceUntilIdle()

        assertEquals(OperatorType.ORANGE_MONEY, viewModel.transactions.value.single().operator)
        assertNull(viewModel.selectedOperator.value)

        viewModel.setOperatorFilter(OperatorType.MVOLA)
        advanceUntilIdle()

        assertEquals(OperatorType.MVOLA, viewModel.transactions.value.single().operator)
        assertEquals(OperatorType.MVOLA, viewModel.selectedOperator.value)

        transactionsJob.cancel()
        selectedJob.cancel()
    }
}
