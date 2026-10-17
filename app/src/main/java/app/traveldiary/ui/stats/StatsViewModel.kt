package app.traveldiary.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.traveldiary.AppContainer
import app.traveldiary.data.stats.TravelStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class StatsViewModel(container: AppContainer) : ViewModel() {
    val stats: StateFlow<TravelStats?> = container.diary.places
        .map { places -> TravelStats.compute(places, container.countries.all) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
