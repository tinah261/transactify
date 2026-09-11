package com.tinah.transactify.ui.settings

import com.tinah.transactify.data.datastore.PreferencesManager
import com.tinah.transactify.domain.model.CommissionRates
import com.tinah.transactify.domain.model.OperatorType
import com.tinah.transactify.domain.model.TransactionType
import com.tinah.transactify.domain.usecase.RecalculateProfitsUseCase
import com.tinah.transactify.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferencesManager = mock<PreferencesManager>()
    private val recalculateProfits = mock<RecalculateProfitsUseCase>()

    @Test
    fun `expose les taux et l'etat du service depuis PreferencesManager`() = runTest {
        whenever(preferencesManager.commissionRates).thenReturn(flowOf(CommissionRates.DEFAULT))
        whenever(preferencesManager.foregroundServiceEnabled).thenReturn(flowOf(false))

        val viewModel = SettingsViewModel(preferencesManager, recalculateProfits)
        val ratesJob = launch { viewModel.rates.collect {} }
        val serviceJob = launch { viewModel.foregroundServiceEnabled.collect {} }
        advanceUntilIdle()

        assertEquals(
            CommissionRates.DEFAULT.rateFor(OperatorType.ORANGE_MONEY, TransactionType.RECU),
            viewModel.rates.value.rateFor(OperatorType.ORANGE_MONEY, TransactionType.RECU),
            0.0,
        )
        assertFalse(viewModel.foregroundServiceEnabled.value)

        ratesJob.cancel()
        serviceJob.cancel()
    }

    @Test
    fun `setRate ignore les valeurs hors bornes`() = runTest {
        val viewModel = SettingsViewModel(preferencesManager, recalculateProfits)

        viewModel.setRate(OperatorType.ORANGE_MONEY, TransactionType.RECU, 1.5)
        advanceUntilIdle()

        verify(preferencesManager, never()).setRate(any(), any(), any())
    }

    @Test
    fun `setRate valide delegue a PreferencesManager`() = runTest {
        val viewModel = SettingsViewModel(preferencesManager, recalculateProfits)

        viewModel.setRate(OperatorType.MVOLA, TransactionType.ENVOYE, 0.05)
        advanceUntilIdle()

        verify(preferencesManager).setRate(OperatorType.MVOLA, TransactionType.ENVOYE, 0.05)
    }

    @Test
    fun `recalculateAllProfits transmet le nombre de transactions mises a jour`() = runTest {
        whenever(recalculateProfits.invoke()).thenReturn(7)
        val viewModel = SettingsViewModel(preferencesManager, recalculateProfits)

        var result: Int? = null
        viewModel.recalculateAllProfits { result = it }
        advanceUntilIdle()

        assertEquals(7, result)
    }
}
