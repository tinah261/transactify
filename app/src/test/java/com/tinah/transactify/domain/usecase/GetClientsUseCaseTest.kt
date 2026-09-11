package com.tinah.transactify.domain.usecase

import com.tinah.transactify.data.db.entity.Client
import com.tinah.transactify.data.repository.ClientRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class GetClientsUseCaseTest {

    private val repository = mock<ClientRepository>()
    private val useCase = GetClientsUseCase(repository)

    @Test
    fun `transmet la requete de recherche telle quelle au repository`() = runTest {
        whenever(repository.searchClients(eq("rakoto"))).thenReturn(flowOf(emptyList()))

        useCase("rakoto").first()

        verify(repository).searchClients(eq("rakoto"))
    }

    @Test
    fun `convertit chaque client en ClientItem`() = runTest {
        val client = Client(phoneNumber = "+26132000000", classification = "VIP")
        whenever(repository.searchClients(eq(""))).thenReturn(flowOf(listOf(client)))

        val result = useCase().first()

        assertEquals(1, result.size)
        assertEquals("+26132000000", result.first().phoneNumber)
    }
}
