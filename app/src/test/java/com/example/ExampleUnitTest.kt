package com.example

import com.example.core.i18n.stringsFor
import com.example.core.preferences.AppLanguage
import com.example.features.tools.ToolCalculators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun `calculator evaluates arithmetic expressions and rejects divide by zero`() {
        assertEquals(1100.0, ToolCalculators.evaluateArithmetic("250*4+100").getOrThrow(), 0.001)
        assertEquals(15.0, ToolCalculators.evaluateArithmetic("(10+20)/2").getOrThrow(), 0.001)
        assertTrue(ToolCalculators.evaluateArithmetic("10/0").isFailure)
    }

    @Test
    fun `pakistan marla and kanal land conversions are accurate`() {
        assertEquals(272.25, ToolCalculators.convertUnit(1.0, "Marla (272.25)", "Sq.Ft"), 0.01)
        assertEquals(20.0, ToolCalculators.convertUnit(1.0, "Kanal (20 Marla)", "Marla (272.25)"), 0.01)
        assertEquals(10L, ToolCalculators.daysBetween("2026-01-01", "2026-01-11").getOrThrow())
    }

    @Test
    fun `password generator honors requested length`() {
        val pw = ToolCalculators.generatePassword(length = 16, includeSymbols = true, includeDigits = true)
        assertEquals(16, pw.length)
    }

    @Test
    fun `all three languages provide non-empty localized labels`() {
        AppLanguage.entries.forEach { lang ->
            val s = stringsFor(lang)
            assertEquals("ApnaHub", s.appName)
            assertTrue(s.tagline.isNotBlank())
            assertTrue(s.safetyMarketplaceBanner.isNotBlank())
        }
    }
}
