package com.tinah.transactify.ui.clients

import com.tinah.transactify.domain.model.ClientClassification
import com.tinah.transactify.domain.model.ClientItem
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClientAdapterDiffTest {

    private fun item(id: Int, transactionCount: Int = 1) = ClientItem(
        id = id,
        phoneNumber = "+26132000000",
        name = "Rakoto",
        totalReceived = 10_000.0,
        totalSent = 0.0,
        transactionCount = transactionCount,
        lastInteraction = 1_000L,
        classification = ClientClassification.REGULAR,
    )

    @Test
    fun `areItemsTheSame compare seulement l'id`() {
        assertTrue(ClientAdapter.DIFF_CALLBACK.areItemsTheSame(item(1, 1), item(1, 99)))
        assertFalse(ClientAdapter.DIFF_CALLBACK.areItemsTheSame(item(1), item(2)))
    }

    @Test
    fun `areContentsTheSame detecte un changement de classification apres nouvelle transaction`() {
        val a = item(1, transactionCount = 1)
        val b = item(1, transactionCount = 1).copy(classification = ClientClassification.VIP)

        assertTrue(ClientAdapter.DIFF_CALLBACK.areContentsTheSame(a, a))
        assertFalse(ClientAdapter.DIFF_CALLBACK.areContentsTheSame(a, b))
    }
}
