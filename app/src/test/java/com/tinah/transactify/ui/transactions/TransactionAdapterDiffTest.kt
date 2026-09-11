package com.tinah.transactify.ui.transactions

import com.tinah.transactify.domain.model.OperatorType
import com.tinah.transactify.domain.model.TransactionItem
import com.tinah.transactify.domain.model.TransactionType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionAdapterDiffTest {

    private fun item(id: Int, amount: Double = 10_000.0) = TransactionItem(
        id = id,
        operator = OperatorType.ORANGE_MONEY,
        type = TransactionType.RECU,
        amount = amount,
        phoneNumber = "+26132000000",
        reference = "R1",
        timestamp = 1_000L,
        bonusAmount = 0.0,
        bonusLinked = false,
        profit = 0.0,
    )

    @Test
    fun `areItemsTheSame compare seulement l'id`() {
        assertTrue(TransactionAdapter.DIFF_CALLBACK.areItemsTheSame(item(1, 100.0), item(1, 999.0)))
        assertFalse(TransactionAdapter.DIFF_CALLBACK.areItemsTheSame(item(1), item(2)))
    }

    @Test
    fun `areContentsTheSame detecte un changement de montant (ex apres correction du benefice)`() {
        val a = item(1, 100.0)
        val b = item(1, 100.0).copy(profit = 5_000.0)

        assertTrue(TransactionAdapter.DIFF_CALLBACK.areContentsTheSame(a, a))
        assertFalse(TransactionAdapter.DIFF_CALLBACK.areContentsTheSame(a, b))
    }
}
