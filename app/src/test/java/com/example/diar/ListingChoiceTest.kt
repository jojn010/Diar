package com.example.diar

import org.junit.Assert.*
import org.junit.Test

class ListingChoiceTest {
    @Test fun priceIsAlwaysAnIntegerAndSupportsArabicDigits() {
        assertEquals(250000L, PropertyStore.integer("250000"))
        assertEquals(1500L, PropertyStore.integer("١٥٠٠"))
        assertEquals(1000000000L, PropertyStore.integer("1000000000"))
        listOf("1.5", "١٫٥", "0", "-1", "NaN", "1000000001", "1e6", "1,500", "").forEach {
            assertNull(it, PropertyStore.integer(it))
        }
    }
    @Test fun mapCoordinatesAreFiniteAndWithinTheWorld() {
        assertTrue(MapPicker.validCoordinates(31.77, 35.21))
        assertTrue(MapPicker.validCoordinates(-90.0, -180.0))
        assertFalse(MapPicker.validCoordinates(91.0, 35.0))
        assertFalse(MapPicker.validCoordinates(31.0, 181.0))
        assertFalse(MapPicker.validCoordinates(Double.NaN, 0.0))
        assertFalse(MapPicker.validCoordinates(0.0, Double.POSITIVE_INFINITY))
        assertEquals(-31.5, MapPicker.coordinate("−٣١٫٥")!!, 0.000001)
        assertNull(MapPicker.coordinate("Infinity"))
    }
}
