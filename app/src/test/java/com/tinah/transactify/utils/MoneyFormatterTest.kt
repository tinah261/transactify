package com.tinah.transactify.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatterTest {

    /**
     * Le séparateur de milliers de `Locale.FRENCH` est une espace insécable
     * (U+00A0 ou U+202F selon la JVM/ICU) — on la normalise en espace simple
     * pour ne pas dépendre de l'environnement d'exécution.
     */
    private fun normalize(text: String) = text.replace(Regex("[\\s\\u00A0\\u202F]+"), " ")

    @Test
    fun `format ajoute le suffixe Ar avec separateur de milliers`() {
        assertEquals("125 000 Ar", normalize(MoneyFormatter.format(125_000.0)))
    }

    @Test
    fun `format arrondit a l'entier, sans decimales`() {
        assertEquals("1 000 Ar", normalize(MoneyFormatter.format(999.6)))
    }

    @Test
    fun `format gere les petits montants et zero`() {
        assertEquals("0 Ar", MoneyFormatter.format(0.0))
        assertEquals("500 Ar", MoneyFormatter.format(500.0))
    }

    @Test
    fun `formatRate convertit une fraction decimale en pourcentage`() {
        assertEquals("3 %", MoneyFormatter.formatRate(0.03))
        assertEquals("2,5 %", MoneyFormatter.formatRate(0.025))
    }
}
