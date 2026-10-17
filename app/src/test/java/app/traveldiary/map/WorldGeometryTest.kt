package app.traveldiary.map

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Runs against the real map shipped in the app. */
class WorldGeometryTest {

    private val world = WorldGeometry.read(File("src/main/assets/map/world.bin").readBytes())

    @Test
    fun loadsEveryCountry() {
        assertTrue(world.countries.size > 240)
        assertTrue(world.countries.all { it.rings.isNotEmpty() && it.rings.all { ring -> ring.size >= 6 } })
    }

    @Test
    fun findsCountriesAtCoordinates() {
        assertEquals("FR", world.countryAt(48.86, 2.35)) // Paris
        assertEquals("IT", world.countryAt(41.90, 12.50)) // Rome
        assertEquals("BR", world.countryAt(-15.79, -47.88)) // Brasília
        assertEquals("AU", world.countryAt(-23.70, 133.88)) // Alice Springs
        assertEquals("US", world.countryAt(39.74, -104.99)) // Denver
        assertEquals("JP", world.countryAt(35.68, 139.69)) // Tokyo
    }

    @Test
    fun findsEnclavesInsideOtherCountries() {
        assertEquals("LS", world.countryAt(-29.6, 28.2)) // Lesotho, inside South Africa
        assertEquals("SM", world.countryAt(43.94, 12.45)) // San Marino, inside Italy
        assertEquals("ZA", world.countryAt(-26.2, 28.04)) // Johannesburg
    }

    @Test
    fun overseasRegionsAreTheirOwnCountry() {
        assertEquals("GF", world.countryAt(4.0, -53.0)) // French Guiana
        assertEquals("SJ", world.countryAt(78.0, 16.0)) // Svalbard
        assertEquals("FR", world.countryAt(45.0, 2.0)) // Mainland France
    }

    @Test
    fun bordersMatchTheCityDatabase() {
        // Natural Earth draws de facto borders; GeoNames uses the recognised ones.
        assertEquals("EH", world.countryAt(27.15, -13.2)) // Laayoune, Western Sahara
        assertEquals("EH", world.countryAt(24.0, -14.0))
        assertEquals("MA", world.countryAt(31.6, -8.0)) // Marrakesh
        assertEquals("UA", world.countryAt(44.95, 34.10)) // Simferopol, Crimea
        assertEquals("RU", world.countryAt(45.04, 38.98)) // Krasnodar
    }

    @Test
    fun handlesTheAntimeridian() {
        assertEquals("RU", world.countryAt(66.0, -172.0)) // Chukotka, east of 180°
        assertEquals("RU", world.countryAt(64.0, 175.0)) // Chukotka, west of 180°
    }

    @Test
    fun seaIsNoCountry() {
        assertNull(world.countryAt(0.0, -30.0)) // Atlantic
        assertNull(world.countryAt(-40.0, 80.0)) // Indian Ocean
    }
}
