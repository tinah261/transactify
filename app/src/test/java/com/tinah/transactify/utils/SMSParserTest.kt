package com.tinah.transactify.utils

import com.tinah.transactify.domain.model.OperatorType
import com.tinah.transactify.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SMSParserTest {

    @Test
    fun `parse Orange Money recu`() {
        val body = "Orange Money: Vous avez recu 100 000 Ar de +261 32 12 34 56. Ref: ABC123."
        val result = SMSParser.parse(body)

        assertEquals(OperatorType.ORANGE_MONEY, result?.operator)
        assertEquals(100_000.0, result?.amount)
        assertEquals(TransactionType.RECU, result?.type)
        assertEquals("+26132123456", result?.phoneNumber)
        assertEquals("ABC123", result?.reference)
        assertFalse(result?.isBonus ?: true)
    }

    @Test
    fun `parse Orange Money envoye`() {
        val body = "Orange Money: Vous avez envoye 50 000 Ar a +261 33 98 76 54. Ref: XYZ999."
        val result = SMSParser.parse(body)

        assertEquals(OperatorType.ORANGE_MONEY, result?.operator)
        assertEquals(50_000.0, result?.amount)
        assertEquals(TransactionType.ENVOYE, result?.type)
    }

    @Test
    fun `detecte un bonus Orange Money`() {
        val body = "Orange Money: BONUS ! Vous avez recu 2 000 Ar de +261 32 00 00 00. Ref: BON001."
        assertTrue(SMSParser.parse(body)?.isBonus == true)
    }

    @Test
    fun `parse Airtel Money recu`() {
        val body = "AIRTEL MONEY: Vous avez recu 75 000 ar de +261 33 11 22 33. Ref: AIR777."
        val result = SMSParser.parse(body)

        assertEquals(OperatorType.AIRTEL_MONEY, result?.operator)
        assertEquals(75_000.0, result?.amount)
        assertEquals(TransactionType.RECU, result?.type)
    }

    @Test
    fun `detecte les rewards Airtel comme bonus`() {
        val body = "AIRTEL MONEY: REWARDS recu 1 000 ar de +261 33 44 55 66. Ref: RWD1."
        assertTrue(SMSParser.parse(body)?.isBonus == true)
    }

    @Test
    fun `parse M-Vola envoye`() {
        val body = "M-VOLA: Vous avez envoye 30 000 ar a +261 34 12 34 56. Ref: MVL42."
        val result = SMSParser.parse(body)

        assertEquals(OperatorType.MVOLA, result?.operator)
        assertEquals(30_000.0, result?.amount)
        assertEquals(TransactionType.ENVOYE, result?.type)
    }

    @Test
    fun `identifie l'operateur via l'expediteur quand le corps ne le nomme pas`() {
        val body = "Vous avez recu 10 000 Ar de +261 34 55 66 77. Ref: SND1."
        val result = SMSParser.parse(body, sender = "MVola")

        assertEquals(OperatorType.MVOLA, result?.operator)
    }

    @Test
    fun `renvoie null pour un SMS sans rapport`() {
        assertNull(SMSParser.parse("Votre code de verification est 123456."))
    }

    @Test
    fun `renvoie null quand le montant est absent`() {
        val body = "Orange Money: Vous avez recu de l'argent de +261 32 12 34 56. Ref: NOAMT."
        assertNull(SMSParser.parse(body))
    }

    @Test
    fun `renvoie null quand le numero est absent`() {
        assertNull(SMSParser.parse("Orange Money: Vous avez recu 10 000 Ar. Ref: NOPHONE."))
    }

    @Test
    fun `renvoie null pour un corps vide`() {
        assertNull(SMSParser.parse(null))
        assertNull(SMSParser.parse("   "))
    }
}
