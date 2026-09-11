package com.tinah.transactify.domain.model

import com.tinah.transactify.data.db.entity.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionItemTest {

    private fun transaction(
        operator: String = "Orange Money",
        type: String = "REÇU",
    ) = Transaction(
        id = 1,
        operator = operator,
        amount = 10_000.0,
        transactionType = type,
        phoneNumber = "+26132000000",
        reference = "R1",
        timestamp = 1_000L,
        bonusAmount = 500.0,
        bonusLinked = true,
        profitCalculated = 300.0,
    )

    @Test
    fun `convertit une transaction valide en TransactionItem`() {
        val item = transaction().toItemOrNull()

        assertEquals(OperatorType.ORANGE_MONEY, item?.operator)
        assertEquals(TransactionType.RECU, item?.type)
        assertEquals(10_000.0, item?.amount)
        assertEquals(500.0, item?.bonusAmount)
        assertEquals(true, item?.bonusLinked)
        assertEquals(300.0, item?.profit)
    }

    @Test
    fun `renvoie null pour un operateur inconnu`() {
        assertNull(transaction(operator = "Telma Money").toItemOrNull())
    }

    @Test
    fun `renvoie null pour un sens inconnu`() {
        assertNull(transaction(type = "INCONNU").toItemOrNull())
    }
}
