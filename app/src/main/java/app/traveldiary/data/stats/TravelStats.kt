package app.traveldiary.data.stats

import app.traveldiary.data.countries.Continent
import app.traveldiary.data.countries.Country
import app.traveldiary.data.diary.PlaceWithVisits
import app.traveldiary.data.diary.VisitEntity
import java.time.LocalDate

data class ContinentProgress(
    val continent: Continent,
    /** UN member states visited on this continent. */
    val visitedCountries: Int,
    val totalCountries: Int,
    /** True as soon as any place on the continent is in the diary, territories included. */
    val visited: Boolean,
)

data class CountryVisits(val country: Country, val places: Int, val firstVisit: LocalDate?)

data class TravelStats(
    /** Countries and territories with at least one place. */
    val countries: Int,
    val unCountries: Int,
    val unCountriesTotal: Int,
    val places: Int,
    val visits: Int,
    val continents: Int,
    /** Days covered by dated visits, counting overlapping trips once. */
    val travelDays: Int,
    val byContinent: List<ContinentProgress>,
    val byCountry: List<CountryVisits>,
) {
    val worldShare: Float get() = if (unCountriesTotal == 0) 0f else unCountries.toFloat() / unCountriesTotal

    companion object {
        fun compute(places: List<PlaceWithVisits>, allCountries: List<Country>): TravelStats {
            val countryByCode = allCountries.associateBy { it.code }
            val placesByCountry = places.groupBy { it.place.countryCode }
            val visited = placesByCountry.keys.mapNotNull { countryByCode[it] }
            val unMembers = allCountries.filter { it.isUnMember }

            val byContinent = Continent.entries.map { continent ->
                ContinentProgress(
                    continent = continent,
                    visitedCountries = visited.count { it.continent == continent && it.isUnMember },
                    totalCountries = unMembers.count { it.continent == continent },
                    visited = visited.any { it.continent == continent },
                )
            }
            val byCountry = visited.map { country ->
                val countryPlaces = placesByCountry.getValue(country.code)
                CountryVisits(
                    country = country,
                    places = countryPlaces.size,
                    firstVisit = countryPlaces.flatMap { it.visits }.mapNotNull { it.startDate }.minOrNull(),
                )
            }.sortedWith(compareByDescending<CountryVisits> { it.places }.thenBy { it.country.name })

            val allVisits = places.flatMap { it.visits }
            return TravelStats(
                countries = placesByCountry.size,
                unCountries = visited.count { it.isUnMember },
                unCountriesTotal = unMembers.size,
                places = places.size,
                visits = allVisits.size,
                continents = byContinent.count { it.visited },
                travelDays = travelDays(allVisits),
                byContinent = byContinent,
                byCountry = byCountry,
            )
        }

        fun travelDays(visits: List<VisitEntity>): Int {
            val ranges = visits.mapNotNull { visit ->
                val start = visit.startDate ?: return@mapNotNull null
                val end = maxOf(visit.endDate ?: start, start)
                start.toEpochDay()..end.toEpochDay()
            }.sortedBy { it.first }

            var total = 0L
            var current: LongRange? = null
            for (range in ranges) {
                current = when {
                    current == null -> range
                    range.first > current.last + 1 -> {
                        total += current.last - current.first + 1
                        range
                    }
                    else -> current.first..maxOf(current.last, range.last)
                }
            }
            current?.let { total += it.last - it.first + 1 }
            return total.toInt()
        }
    }
}
