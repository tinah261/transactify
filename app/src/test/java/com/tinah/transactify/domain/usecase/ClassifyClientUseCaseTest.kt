package com.tinah.transactify.domain.usecase

import com.tinah.transactify.domain.model.ClientClassification
import org.junit.Assert.assertEquals
import org.junit.Test

class ClassifyClientUseCaseTest {

    private val useCase = ClassifyClientUseCase()

    @Test
    fun `un seul echange est un client ponctuel`() {
        assertEquals(ClientClassification.ONE_TIME, useCase(transactionCount = 1, totalVolume = 5_000.0))
        assertEquals(ClientClassification.ONE_TIME, useCase(transactionCount = 0, totalVolume = 0.0))
    }

    @Test
    fun `activite intermediaire est un client regulier`() {
        assertEquals(ClientClassification.REGULAR, useCase(transactionCount = 5, totalVolume = 200_000.0))
    }

    @Test
    fun `beaucoup de transactions donne VIP`() {
        assertEquals(ClientClassification.VIP, useCase(transactionCount = 20, totalVolume = 10_000.0))
    }

    @Test
    fun `gros volume donne VIP meme avec peu de transactions`() {
        assertEquals(ClientClassification.VIP, useCase(transactionCount = 3, totalVolume = 1_000_000.0))
    }
}
