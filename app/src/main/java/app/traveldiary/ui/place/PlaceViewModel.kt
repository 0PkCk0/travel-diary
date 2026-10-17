package app.traveldiary.ui.place

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.traveldiary.AppContainer
import app.traveldiary.data.diary.PlaceWithVisits
import app.traveldiary.map.WorldMap
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlaceUiState(
    val loading: Boolean = true,
    /** Null once loaded means the place was deleted. */
    val place: PlaceWithVisits? = null,
    val countryName: String = "",
)

class PlaceViewModel(private val container: AppContainer, private val placeId: Long) : ViewModel() {

    val map: StateFlow<WorldMap?> = container.worldMap.map

    val uiState: StateFlow<PlaceUiState> = container.diary.place(placeId)
        .map { place ->
            PlaceUiState(
                loading = false,
                place = place,
                countryName = place?.let { container.countries.name(it.place.countryCode) }.orEmpty(),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaceUiState())

    fun deletePlace() {
        viewModelScope.launch { container.diary.deletePlace(placeId) }
    }
}
