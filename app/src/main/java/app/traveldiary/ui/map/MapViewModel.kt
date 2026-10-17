package app.traveldiary.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.traveldiary.AppContainer
import app.traveldiary.data.countries.Country
import app.traveldiary.data.diary.PlaceWithVisits
import app.traveldiary.map.MapPin
import app.traveldiary.map.MillerProjection
import app.traveldiary.map.WorldMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MapUiState(
    val loaded: Boolean = false,
    val places: List<PlaceWithVisits> = emptyList(),
    val pins: List<MapPin> = emptyList(),
    val visitedCountries: Set<String> = emptySet(),
    /** Share of the 195 countries of the world visited, 0 to 1. */
    val worldShare: Float = 0f,
)

class MapViewModel(private val container: AppContainer) : ViewModel() {

    val map: StateFlow<WorldMap?> = container.worldMap.map

    val uiState: StateFlow<MapUiState> = container.diary.places
        .map { places -> mapUiState(places, container.countries.all) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapUiState())

    fun country(code: String): Country? = container.countries[code]
}

fun mapUiState(places: List<PlaceWithVisits>, countries: List<Country>): MapUiState {
    val visited = places.map { it.place.countryCode }.toSet()
    val unMembers = countries.filter { it.isUnMember }
    return MapUiState(
        loaded = true,
        places = places,
        pins = places.map { MapPin(it.place.id, MillerProjection.position(it.place.latitude, it.place.longitude)) },
        visitedCountries = visited,
        worldShare = if (unMembers.isEmpty()) 0f else unMembers.count { it.code in visited }.toFloat() / unMembers.size,
    )
}
