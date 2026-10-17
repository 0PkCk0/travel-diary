package app.traveldiary.data.cities

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Imports the real built-in city list and searches it. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CityRepositoryTest {

    private lateinit var database: CityDatabase
    private lateinit var repository: CityRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CityDatabase::class.java).build()
        repository = CityRepository(context, database, CoroutineScope(Dispatchers.IO))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun importsTheBuiltInListAndSearchesIt() = runBlocking {
        repository.importBuiltIn()

        val installed = repository.installed.first()!!
        assertEquals(CityDataset.BUILT_IN, installed.dataset)
        assertTrue("got ${installed.cityCount}", installed.cityCount > 30_000)

        // The most populated match comes first, with its region.
        val paris = repository.search("paris", null).first()
        assertEquals("FR", paris.countryCode)
        assertEquals("Île-de-France", paris.region)

        // Accents and case do not matter, and GeoNames' own transliteration works too.
        assertEquals("Zürich", repository.search("ZURICH", null).first().name)
        assertEquals("Zürich", repository.search("zuerich", null).first().name)
        assertEquals("Düsseldorf", repository.search("dusseldorf", null).first().name)
        assertEquals("Kraków", repository.search("krakow", null).first().name)

        // Exact names first, then names starting with the query, then other words of the name.
        val york = repository.search("york", null)
        assertEquals("York", york.first().name)
        assertTrue(york.any { it.name == "New York City" })
        assertEquals("New York City", repository.search("new york", null).first().name)

        // Among names starting with the query, the most populated come first, whether or not
        // they have an alternative spelling (Sankt Pölten / Sankt Poelten).
        val san = repository.search("san", null)
        val startingWithSan = san.filter { it.searchName != "san" && it.searchName.startsWith("san") }
        assertEquals("Santiago", startingWithSan.first().name)
        assertEquals(startingWithSan.sortedByDescending { it.population }, startingWithSan)

        // Tiny countries are searchable without downloading anything.
        assertTrue(repository.search("vatican", null).any { it.countryCode == "VA" })
        assertTrue(repository.search("san marino", "SM").isNotEmpty())
        assertTrue(repository.search("vaduz", null).any { it.countryCode == "LI" })

        // Country filter, and an empty query listing the country's largest cities.
        assertTrue(repository.search("san", "IT").all { it.countryCode == "IT" })
        assertEquals("Rome", repository.search("", "IT").first().name)
        assertTrue(repository.search("", null).isEmpty())
    }
}
