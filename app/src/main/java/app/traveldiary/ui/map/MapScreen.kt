package app.traveldiary.ui.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.traveldiary.R
import app.traveldiary.data.countries.Country
import app.traveldiary.data.diary.PlaceWithVisits
import app.traveldiary.map.MapFocus
import app.traveldiary.map.MapViewportState
import app.traveldiary.map.MillerProjection
import app.traveldiary.map.WorldMap
import app.traveldiary.map.rememberMapViewportState
import app.traveldiary.ui.common.Flag
import app.traveldiary.ui.common.formatDateRange
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first

@Composable
fun MapScreen(
    viewModel: MapViewModel,
    focusPlaceId: Long?,
    onFocusHandled: () -> Unit,
    onSearch: (countryCode: String?) -> Unit,
    onOpenPlace: (Long) -> Unit,
    onAddCustomPlace: (latitude: Double, longitude: Double) -> Unit,
) {
    val map by viewModel.map.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MapScreenContent(
        map = map,
        state = state,
        countryOf = viewModel::country,
        onSearch = onSearch,
        onOpenPlace = onOpenPlace,
        onAddCustomPlace = onAddCustomPlace,
        focusPlaceId = focusPlaceId,
        onFocusHandled = onFocusHandled,
    )
}

@Composable
fun MapScreenContent(
    map: WorldMap?,
    state: MapUiState,
    countryOf: (String) -> Country?,
    onSearch: (countryCode: String?) -> Unit,
    onOpenPlace: (Long) -> Unit,
    onAddCustomPlace: (latitude: Double, longitude: Double) -> Unit,
    modifier: Modifier = Modifier,
    focusPlaceId: Long? = null,
    onFocusHandled: () -> Unit = {},
    viewport: MapViewportState = rememberMapViewportState(),
    initialSelectedCountry: String? = null,
) {
    val scope = rememberCoroutineScope()
    var selectedCountry by rememberSaveable { mutableStateOf(initialSelectedCountry) }

    // After adding a place, fly to it.
    LaunchedEffect(focusPlaceId, state.places) {
        val place = state.places.firstOrNull { it.place.id == focusPlaceId }?.place ?: return@LaunchedEffect
        snapshotFlow { viewport.isReady }.first { it }
        viewport.animateTo(scope, MapFocus.Point(MillerProjection.position(place.latitude, place.longitude), zoom = 6f))
        onFocusHandled()
    }

    Box(modifier.fillMaxSize()) {
        WorldMap(
            map = map,
            state = viewport,
            visitedCountries = state.visitedCountries,
            pins = state.pins,
            selectedCountry = selectedCountry,
            onCountryClick = { selectedCountry = it },
            onPinClick = onOpenPlace,
            onLongPress = { position ->
                onAddCustomPlace(MillerProjection.latitude(position.y), MillerProjection.longitude(position.x))
            },
            modifier = Modifier.fillMaxSize(),
        )

        SearchPill(
            onClick = { onSearch(null) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SummaryCard(state, Modifier.weight(1f))
            SmallFloatingActionButton(onClick = {
                selectedCountry = null
                viewport.animateTo(scope, MapFocus.World)
            }) {
                Icon(Icons.Filled.Public, contentDescription = stringResource(R.string.map_show_world))
            }
        }

        if (map == null) CircularProgressIndicator(Modifier.align(Alignment.Center))
    }

    selectedCountry?.let { code ->
        CountrySheet(
            code = code,
            country = countryOf(code),
            places = state.places.filter { it.place.countryCode == code },
            onDismiss = { selectedCountry = null },
            onAddCity = {
                selectedCountry = null
                onSearch(code)
            },
            onOpenPlace = { placeId ->
                selectedCountry = null
                onOpenPlace(placeId)
            },
        )
    }
}

@Composable
private fun SearchPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 4.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 20.dp)) {
            Icon(Icons.Filled.Search, contentDescription = null)
            Spacer(Modifier.width(16.dp))
            Text(
                stringResource(R.string.map_search_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SummaryCard(state: MapUiState, modifier: Modifier = Modifier) {
    if (!state.loaded) return
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 4.dp,
    ) {
        if (state.places.isEmpty()) {
            Text(
                stringResource(R.string.map_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
                val countries = state.visitedCountries.size
                SummaryValue(
                    value = countries.toString(),
                    label = pluralStringResource(R.plurals.summary_countries, countries),
                    modifier = Modifier.weight(1f),
                )
                SummaryValue(
                    value = state.places.size.toString(),
                    label = pluralStringResource(R.plurals.summary_places, state.places.size),
                    modifier = Modifier.weight(1f),
                )
                SummaryValue(
                    value = formatShare(state.worldShare),
                    label = stringResource(R.string.summary_world),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SummaryValue(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** 0.004 -> "<1%", 0.123 -> "12%". */
fun formatShare(share: Float): String {
    val percent = share * 100
    return if (percent > 0f && percent < 1f) "<1%" else "${percent.roundToInt()}%"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountrySheet(
    code: String,
    country: Country?,
    places: List<PlaceWithVisits>,
    onDismiss: () -> Unit,
    onAddCity: () -> Unit,
    onOpenPlace: (Long) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        CountrySheetContent(code, country, places, onAddCity, onOpenPlace)
    }
}

@Composable
fun CountrySheetContent(
    code: String,
    country: Country?,
    places: List<PlaceWithVisits>,
    onAddCity: () -> Unit,
    onOpenPlace: (Long) -> Unit,
) {
    val name = country?.name ?: code
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Flag(code, size = 44.sp)
            Column {
                Text(name, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = if (places.isEmpty()) {
                        stringResource(R.string.country_not_visited)
                    } else {
                        pluralStringResource(R.plurals.country_places_visited, places.size, places.size)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (places.isNotEmpty()) {
            Column {
                places.forEach { item ->
                    val visit = item.sortedVisits.firstOrNull()
                    ListItem(
                        headlineContent = { Text(item.place.name) },
                        supportingContent = {
                            val subtitle = formatDateRange(visit?.startDate, visit?.endDate) ?: item.place.region
                            subtitle?.let { Text(it) }
                        },
                        leadingContent = {
                            Icon(Icons.Filled.Place, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onOpenPlace(item.place.id) },
                    )
                }
            }
        }
        Button(onClick = onAddCity, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.country_add_city, name))
        }
    }
}
