package app.traveldiary.ui.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.traveldiary.AppContainer
import app.traveldiary.data.cities.CityEntity
import app.traveldiary.data.cities.CityImportState
import app.traveldiary.data.countries.Country
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

data class SearchResult(val city: CityEntity, val countryName: String, val visited: Boolean)

data class SearchUiState(
    /** When set, only cities of this country are searched. */
    val country: Country? = null,
    val results: List<SearchResult> = emptyList(),
    /** False until the built-in city list has been imported on first launch. */
    val databaseReady: Boolean = true,
    val citiesImported: Int? = null,
    val noResults: Boolean = false,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(private val container: AppContainer, initialCountry: String?) : ViewModel() {

    var query by mutableStateOf("")
        private set

    private val queryFlow = MutableStateFlow("")
    private val countryFilter = MutableStateFlow(initialCountry)

    private data class Request(val query: String, val countryCode: String?, val ready: Boolean)

    private val matches = combine(
        queryFlow.debounce { if (it.isEmpty()) 0L else 150L },
        countryFilter,
        container.cities.installed.map { it != null }.distinctUntilChanged(),
    ) { query, countryCode, ready -> Request(query, countryCode, ready) }
        .mapLatest { request ->
            request to if (request.ready) container.cities.search(request.query, request.countryCode) else emptyList()
        }

    val uiState: StateFlow<SearchUiState> = combine(
        matches,
        container.diary.visitedCityIds,
        container.cities.importState,
    ) { (request, cities), visitedCityIds, importState ->
        SearchUiState(
            country = request.countryCode?.let { container.countries[it] },
            results = cities.map { SearchResult(it, container.countries.name(it.countryCode), it.id in visitedCityIds) },
            databaseReady = request.ready,
            citiesImported = (importState as? CityImportState.Importing)?.citiesRead,
            noResults = request.ready && cities.isEmpty() && request.query.isNotBlank(),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SearchUiState(country = initialCountry?.let { container.countries[it] }),
    )

    fun onQueryChange(value: String) {
        query = value
        queryFlow.value = value
    }

    fun clearCountry() {
        countryFilter.value = null
    }
}
