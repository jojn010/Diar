package com.example.diar

import org.junit.Assert.*
import org.junit.Test

class PropertyNumberTest {
    @Test fun acceptsEnglishAndArabicNumbers() {
        assertEquals("125.50", PropertyStore.number("125.50")?.toPlainString())
        assertEquals("125.50", PropertyStore.number("١٢٥٫٥٠")?.toPlainString())
        assertEquals("450000", PropertyStore.number("۴۵۰۰۰۰")?.toPlainString())
    }
    @Test fun rejectsAmbiguousOrInvalidValues() {
        listOf("", "0", "-12", "NaN", "Infinity", "1,000", "1e5", "1000000001", "2.123", "١٢٫٣٫٤")
            .forEach { assertNull(it, PropertyStore.number(it)) }
    }
}
