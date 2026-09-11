package com.tinah.transactify.domain.model

import com.tinah.transactify.utils.Constants
import org.junit.Assert.assertEquals
import org.junit.Test

class CommissionRatesTest {

    @Test
    fun `DEFAULT expose les six taux configures`() {
        val rates = CommissionRates.DEFAULT
        assertEquals(Constants.DefaultRates.ORANGE_RECU, rates.rateFor(OperatorType.ORANGE_MONEY, TransactionType.RECU), 0.0)
        assertEquals(Constants.DefaultRates.MVOLA_ENVOYE, rates.rateFor(OperatorType.MVOLA, TransactionType.ENVOYE), 0.0)
        assertEquals(6, rates.asList().size)
    }

    @Test
    fun `withRate ne modifie qu'une seule entree`() {
        val updated = CommissionRates.DEFAULT.withRate(OperatorType.AIRTEL_MONEY, TransactionType.RECU, 0.09)
        assertEquals(0.09, updated.rateFor(OperatorType.AIRTEL_MONEY, TransactionType.RECU), 0.0)
        assertEquals(
            Constants.DefaultRates.ORANGE_ENVOYE,
            updated.rateFor(OperatorType.ORANGE_MONEY, TransactionType.ENVOYE),
            0.0,
        )
    }

    @Test
    fun `of complete les valeurs manquantes par DEFAULT`() {
        val partial = CommissionRates.of(
            mapOf(CommissionRates.Key(OperatorType.MVOLA, TransactionType.RECU) to 0.05),
        )
        assertEquals(0.05, partial.rateFor(OperatorType.MVOLA, TransactionType.RECU), 0.0)
        assertEquals(
            Constants.DefaultRates.ORANGE_RECU,
            partial.rateFor(OperatorType.ORANGE_MONEY, TransactionType.RECU),
            0.0,
        )
    }
}
