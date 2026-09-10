package com.tinah.transactify.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OperatorTypeTest {

    @Test
    fun `fromStorage reconnait les libelles persistes`() {
        assertEquals(OperatorType.ORANGE_MONEY, OperatorType.fromStorage("Orange Money"))
        assertEquals(OperatorType.AIRTEL_MONEY, OperatorType.fromStorage("airtel money"))
        assertEquals(OperatorType.MVOLA, OperatorType.fromStorage("M-Vola"))
        assertNull(OperatorType.fromStorage("Telma"))
        assertNull(OperatorType.fromStorage(null))
    }

    @Test
    fun `detect utilise le corps du SMS en priorite`() {
        assertEquals(OperatorType.ORANGE_MONEY, OperatorType.detect("Orange Money: ..."))
        assertEquals(OperatorType.AIRTEL_MONEY, OperatorType.detect("AIRTEL MONEY vous informe"))
        assertEquals(OperatorType.MVOLA, OperatorType.detect("Transaction M-VOLA reussie"))
    }

    @Test
    fun `detect se rabat sur l'expediteur`() {
        assertEquals(OperatorType.MVOLA, OperatorType.detect("Vous avez recu 10 000 Ar", sender = "MVola"))
        assertNull(OperatorType.detect("Vous avez recu 10 000 Ar", sender = "0340000000"))
    }
}
