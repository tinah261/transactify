package com.tinah.transactify.domain.model

import com.tinah.transactify.data.db.entity.Client
import org.junit.Assert.assertEquals
import org.junit.Test

class ClientItemTest {

    private fun client(name: String? = null, classification: String = "VIP") = Client(
        id = 1,
        phoneNumber = "+26132000000",
        name = name,
        totalReceived = 100_000.0,
        totalSent = 50_000.0,
        transactionCount = 5,
        lastInteraction = 1_000L,
        classification = classification,
    )

    @Test
    fun `convertit un client en ClientItem type`() {
        val item = client(classification = "REGULAR").toItem()

        assertEquals(ClientClassification.REGULAR, item.classification)
        assertEquals(150_000.0, item.totalVolume, 0.0)
    }

    @Test
    fun `classification inconnue retombe sur REGULAR`() {
        assertEquals(ClientClassification.REGULAR, client(classification = "???").toItem().classification)
    }

    @Test
    fun `displayName utilise le nom si renseigne, sinon le numero`() {
        assertEquals("Rakoto", client(name = "Rakoto").toItem().displayName)
        assertEquals("+26132000000", client(name = null).toItem().displayName)
        assertEquals("+26132000000", client(name = "   ").toItem().displayName)
    }
}
