package app.traveldiary.data.cities

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchNormalizerTest {

    @Test
    fun removesAccentsAndCase() {
        assertEquals("zurich", SearchNormalizer.normalize("Zürich"))
        assertEquals("sao paulo", SearchNormalizer.normalize("  São   Paulo "))
        assertEquals("reykjavik", SearchNormalizer.normalize("REYKJAVÍK"))
    }

    @Test
    fun turnsPunctuationIntoSpaces() {
        assertEquals("aix en provence", SearchNormalizer.normalize("Aix-en-Provence"))
        assertEquals("coeur d alene", SearchNormalizer.normalize("Coeur d'Alene"))
        assertEquals("st john s", SearchNormalizer.normalize("St. John's"))
    }

    @Test
    fun replacesLettersWithoutDecomposition() {
        assertEquals("lodz", SearchNormalizer.normalize("Łódź"))
        assertEquals("strasse", SearchNormalizer.normalize("Straße"))
        assertEquals("aeroskobing", SearchNormalizer.normalize("Ærøskøbing"))
        assertEquals("san pawl il bahar", SearchNormalizer.normalize("San Pawl il-Baħar"))
    }

    @Test
    fun emptyWhenNothingSearchable() {
        assertEquals("", SearchNormalizer.normalize("  -- '' "))
    }
}
