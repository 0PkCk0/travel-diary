package app.traveldiary.ui.place

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.traveldiary.R
import app.traveldiary.data.diary.PlaceWithVisits
import app.traveldiary.data.diary.VisitEntity
import app.traveldiary.map.MapFocus
import app.traveldiary.map.MapPin
import app.traveldiary.map.MillerProjection
import app.traveldiary.map.WorldMap
import app.traveldiary.map.rememberMapViewportState
import app.traveldiary.ui.common.Flag
import app.traveldiary.ui.common.formatCoordinates
import app.traveldiary.ui.common.formatDateRange
import app.traveldiary.ui.common.stayLength

@Composable
fun PlaceScreen(
    viewModel: PlaceViewModel,
    onBack: () -> Unit,
    onAddVisit: () -> Unit,
    onEditVisit: (visitId: Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val map by viewModel.map.collectAsStateWithLifecycle()

    // Deleted, or opened for a place that no longer exists.
    LaunchedEffect(state) {
        if (!state.loading && state.place == null) onBack()
    }

    val place = state.place ?: return
    PlaceScreenContent(
        place = place,
        countryName = state.countryName,
        map = map,
        onBack = onBack,
        onAddVisit = onAddVisit,
        onEditVisit = onEditVisit,
        onDelete = viewModel::deletePlace,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceScreenContent(
    place: PlaceWithVisits,
    countryName: String,
    map: WorldMap?,
    onBack: () -> Unit,
    onAddVisit: () -> Unit,
    onEditVisit: (visitId: Long) -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val details = place.place

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(details.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.place_delete))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddVisit,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.place_add_visit)) },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "map") {
                val position = MillerProjection.position(details.latitude, details.longitude)
                Card(Modifier.fillMaxWidth().height(200.dp)) {
                    WorldMap(
                        map = map,
                        state = rememberMapViewportState(MapFocus.Point(position, zoom = 10f)),
                        visitedCountries = setOf(details.countryCode),
                        pins = listOf(MapPin(details.id, position)),
                        interactive = false,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            item(key = "header") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Flag(details.countryCode, size = 32.sp)
                    Column {
                        Text(
                            listOfNotNull(details.region, countryName).joinToString(", "),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            formatCoordinates(details.latitude, details.longitude),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item(key = "visits-title") {
                Text(
                    pluralStringResource(R.plurals.place_visits, place.visits.size, place.visits.size),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (place.visits.isEmpty()) {
                item(key = "no-visits") {
                    Text(
                        stringResource(R.string.place_no_visits),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(place.sortedVisits, key = { it.id }) { visit ->
                VisitCard(visit, onClick = { onEditVisit(visit.id) })
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.place_delete_title, details.name)) },
            text = { Text(stringResource(R.string.place_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun VisitCard(visit: VisitEntity, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    formatDateRange(visit.startDate, visit.endDate) ?: stringResource(R.string.visit_no_date),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                stayLength(visit.startDate, visit.endDate)?.let { days ->
                    Text(
                        pluralStringResource(R.plurals.visit_days, days, days),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                visit.notes.ifBlank { stringResource(R.string.visit_no_notes) },
                style = MaterialTheme.typography.bodyMedium,
                color = if (visit.notes.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
