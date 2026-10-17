package app.traveldiary.data.stats

import app.traveldiary.data.countries.Continent
import app.traveldiary.data.countries.Country
import app.traveldiary.data.diary.PlaceEntity
import app.traveldiary.data.diary.PlaceWithVisits
import app.traveldiary.data.diary.VisitEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TravelStatsTest {

    private val countries = listOf(
        Country("FR", "France", Continent.EUROPE, isUnMember = true),
        Country("IT", "Italy", Continent.EUROPE, isUnMember = true),
        Country("JP", "Japan", Continent.ASIA, isUnMember = true),
        Country("HK", "Hong Kong", Continent.ASIA, isUnMember = false),
        Country("BR", "Brazil", Continent.SOUTH_AMERICA, isUnMember = true),
    )

    private var nextId = 1L

    private fun place(country: String, vararg visits: Pair<LocalDate?, LocalDate?>): PlaceWithVisits {
        val id = nextId++
        return PlaceWithVisits(
            PlaceEntity(id, null, "Place $id", null, country, 0.0, 0.0, 0),
            visits.map { (start, end) -> VisitEntity(nextId++, id, start, end, "", 0) },
        )
    }

    private fun day(month: Int, day: Int) = LocalDate.of(2024, month, day)

    @Test
    fun countsCountriesContinentsAndWorldShare() {
        val stats = TravelStats.compute(
            listOf(
                place("FR", day(5, 1) to day(5, 3)),
                place("FR"),
                place("IT", day(5, 3) to day(5, 6)),
                place("HK", day(8, 1) to null),
            ),
            countries,
        )
        assertEquals(3, stats.countries)
        assertEquals(2, stats.unCountries)
        assertEquals(4, stats.unCountriesTotal)
        assertEquals(0.5f, stats.worldShare, 1e-6f)
        assertEquals(4, stats.places)
        assertEquals(3, stats.visits)
        assertEquals(2, stats.continents) // Europe, and Asia thanks to Hong Kong
        assertEquals(7, stats.travelDays) // 1-6 May (overlap counted once) + 1 Aug

        val europe = stats.byContinent.single { it.continent == Continent.EUROPE }
        assertEquals(2, europe.visitedCountries)
        assertEquals(2, europe.totalCountries)
        val asia = stats.byContinent.single { it.continent == Continent.ASIA }
        assertEquals(0, asia.visitedCountries)
        assertEquals(true, asia.visited)

        assertEquals(listOf("FR", "HK", "IT"), stats.byCountry.map { it.country.code })
        assertEquals(day(5, 1), stats.byCountry.first().firstVisit)
    }

    @Test
    fun travelDaysMergesOverlapsAndSkipsUndatedVisits() {
        fun visit(start: LocalDate?, end: LocalDate?) = VisitEntity(0, 0, start, end, "", 0)
        assertEquals(0, TravelStats.travelDays(emptyList()))
        assertEquals(0, TravelStats.travelDays(listOf(visit(null, null))))
        assertEquals(1, TravelStats.travelDays(listOf(visit(day(1, 1), null))))
        assertEquals(10, TravelStats.travelDays(listOf(visit(day(1, 1), day(1, 10)), visit(day(1, 3), day(1, 5)))))
        assertEquals(6, TravelStats.travelDays(listOf(visit(day(1, 4), day(1, 6)), visit(day(1, 1), day(1, 3)))))
        // An end before the start counts as a single day.
        assertEquals(1, TravelStats.travelDays(listOf(visit(day(1, 5), day(1, 1)))))
    }

    @Test
    fun emptyDiary() {
        val stats = TravelStats.compute(emptyList(), countries)
        assertEquals(0, stats.countries)
        assertEquals(0f, stats.worldShare)
        assertEquals(0, stats.continents)
        assertEquals(Continent.entries.size, stats.byContinent.size)
    }
}
