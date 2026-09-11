package com.tinah.transactify.ui.clients

import com.tinah.transactify.domain.model.ClientClassification
import com.tinah.transactify.domain.model.ClientItem
import com.tinah.transactify.domain.usecase.GetClientsUseCase
import com.tinah.transactify.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ClientViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getClients = mock<GetClientsUseCase>()

    private fun item(phone: String) = ClientItem(
        id = 1,
        phoneNumber = phone,
        name = null,
        totalReceived = 0.0,
        totalSent = 0.0,
        transactionCount = 1,
        lastInteraction = 0L,
        classification = ClientClassification.REGULAR,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `changer la requete relance la recherche`() = runTest {
        whenever(getClients("")).thenReturn(flowOf(listOf(item("+26132000001"))))
        whenever(getClients("rakoto")).thenReturn(flowOf(listOf(item("+26132000002"))))

        val viewModel = ClientViewModel(getClients)
        val job = launch { viewModel.clients.collect {} }
        advanceUntilIdle()

        assertEquals("+26132000001", viewModel.clients.value.single().phoneNumber)

        viewModel.setQuery("rakoto")
        advanceUntilIdle()

        assertEquals("+26132000002", viewModel.clients.value.single().phoneNumber)

        job.cancel()
    }
}
