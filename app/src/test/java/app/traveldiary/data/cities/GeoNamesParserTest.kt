package app.traveldiary.data.cities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeoNamesParserTest {

    private val parser = GeoNamesParser(mapOf("FR.11" to "Île-de-France", "DE.16" to "Berlin"))

    private fun line(id: String, name: String, ascii: String, featureCode: String, country: String, admin1: String, population: String) =
        listOf(id, name, ascii, "alt,names", "48.85341", "2.3488", "P", featureCode, country, "", admin1, "", "", "", population, "", "42", "Europe/Paris", "2024-01-01")
            .joinToString("\t")

    @Test
    fun parsesACity() {
        val city = parser.parse(line("2988507", "Paris", "Paris", "PPLC", "FR", "11", "2138551"))!!
        assertEquals(2988507L, city.id)
        assertEquals("Paris", city.name)
        assertEquals("Île-de-France", city.region)
        assertEquals("FR", city.countryCode)
        assertEquals(48.85341, city.latitude, 1e-9)
        assertEquals(2.3488, city.longitude, 1e-9)
        assertEquals(2138551L, city.population)
        assertEquals("paris", city.searchName)
    }

    @Test
    fun keepsBothSpellingsSearchable() {
        val zurich = parser.parse(line("1", "Zürich", "Zuerich", "PPLA", "CH", "ZH", "400000"))!!
        assertEquals("Zürich", zurich.name)
        assertEquals("zurich", zurich.searchName)
        assertEquals("zuerich", zurich.searchAscii)
        assertNull(zurich.region)

        val paris = parser.parse(line("2", "Paris", "Paris", "PPLC", "FR", "11", "1"))!!
        assertNull(paris.searchAscii)
    }

    @Test
    fun dropsARegionNamedLikeTheCity() {
        val city = parser.parse(line("2950159", "Berlin", "Berlin", "PPLC", "DE", "16", "3426354"))!!
        assertNull(city.region)
    }

    @Test
    fun skipsPlacesThatNoLongerExist() {
        assertNull(parser.parse(line("1", "Pompeii", "Pompeii", "PPLH", "IT", "04", "0")))
        assertNull(parser.parse(line("2", "Ghost town", "Ghost town", "PPLQ", "US", "NV", "0")))
    }

    @Test
    fun skipsNumberedCityDistricts() {
        assertNull(parser.parse(line("1", "Paris 15 Vaugirard", "Paris 15 Vaugirard", "PPL", "FR", "11", "235000")))
        assertNull(parser.parse(line("2", "Zürich (Kreis 3)", "Zuerich (Kreis 3)", "PPLX", "CH", "ZH", "50000")))
        assertNull(parser.parse(line("3", "Sector 2", "Sector 2", "PPLX", "RO", "10", "350000")))
        assertNull(parser.parse(line("4", "Marseille 08", "Marseille 08", "PPLA5", "FR", "93", "80000")))
        // Real towns with a number in their name stay.
        assertEquals("6th of October City", parser.parse(line("5", "6th of October City", "6th of October City", "PPL", "EG", "11", "185000"))?.name)
        assertEquals("Mile 91", parser.parse(line("6", "Mile 91", "Mile 91", "PPL", "SL", "02", "20000"))?.name)
        assertEquals("Kowloon", parser.parse(line("7", "Kowloon", "Kowloon", "PPLX", "HK", "00", "2000000"))?.name)
    }

    @Test
    fun skipsBrokenLines() {
        assertNull(parser.parse(""))
        assertNull(parser.parse("123\tParis"))
        assertNull(parser.parse(line("x", "Paris", "Paris", "PPL", "FR", "11", "1")))
    }

    @Test
    fun readsRegionNames() {
        val regions = GeoNamesParser.parseRegionNames(sequenceOf("IT.09\tLiguria", "US.IL\tIllinois\textra", "broken"))
        assertEquals(mapOf("IT.09" to "Liguria", "US.IL" to "Illinois"), regions)
    }
}
