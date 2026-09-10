package com.tinah.transactify.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SMSParserTest {

    @Test
    fun `parses Orange Money received SMS`() {
        val body = "Orange Money: Vous avez reçu 100 000 Ar de +261 32 12 34 56. Ref: ABC123."
        val result = SMSParser.parseSMS(body)

        assertEquals("Orange Money", result?.operator)
        assertEquals(100000.0, result?.amount)
        assertEquals("REÇU", result?.type)
        assertEquals("+26132123456", result?.phoneNumber)
        assertEquals("ABC123", result?.reference)
        assertTrue(result?.isBonus == false)
    }

    @Test
    fun `parses Orange Money sent SMS`() {
        val body = "Orange Money: Vous avez envoyé 50 000 Ar a +261 33 98 76 54. Ref: XYZ999."
        val result = SMSParser.parseSMS(body)

        assertEquals("Orange Money", result?.operator)
        assertEquals(50000.0, result?.amount)
        assertEquals("ENVOYÉ", result?.type)
    }

    @Test
    fun `detects Orange Money bonus SMS`() {
        val body = "Orange Money: BONUS ! Vous avez reçu 2 000 Ar de +261 32 00 00 00. Ref: BON001."
        val result = SMSParser.parseSMS(body)

        assertTrue(result?.isBonus == true)
    }

    @Test
    fun `parses Airtel Money received SMS`() {
        val body = "AIRTEL MONEY: Vous avez reçu 75 000 ar de +261 33 11 22 33. Ref: AIR777."
        val result = SMSParser.parseSMS(body)

        assertEquals("Airtel Money", result?.operator)
        assertEquals(75000.0, result?.amount)
        assertEquals("REÇU", result?.type)
    }

    @Test
    fun `detects Airtel Money rewards as bonus`() {
        val body = "AIRTEL MONEY: REWARDS reçu 1 000 ar de +261 33 44 55 66. Ref: RWD1."
        val result = SMSParser.parseSMS(body)

        assertTrue(result?.isBonus == true)
    }

    @Test
    fun `parses M-Vola sent SMS`() {
        val body = "M-VOLA: Vous avez envoyé 30 000 ar a +261 34 12 34 56. Ref: MVL42."
        val result = SMSParser.parseSMS(body)

        assertEquals("M-Vola", result?.operator)
        assertEquals(30000.0, result?.amount)
        assertEquals("ENVOYÉ", result?.type)
    }

    @Test
    fun `returns null for unrelated SMS`() {
        val body = "Votre code de vérification est 123456."
        assertNull(SMSParser.parseSMS(body))
    }

    @Test
    fun `returns null when amount is missing`() {
        val body = "Orange Money: Vous avez reçu de l'argent de +261 32 12 34 56. Ref: NOAMT."
        assertNull(SMSParser.parseSMS(body))
    }

    @Test
    fun `returns null when phone number is missing`() {
        val body = "Orange Money: Vous avez reçu 10 000 Ar. Ref: NOPHONE."
        assertNull(SMSParser.parseSMS(body))
    }
}
