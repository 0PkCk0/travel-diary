package app.traveldiary.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.traveldiary.R
import app.traveldiary.data.stats.ContinentProgress
import app.traveldiary.data.stats.TravelStats
import app.traveldiary.ui.common.Flag
import app.traveldiary.ui.common.formatNumber
import app.traveldiary.ui.map.formatShare

@Composable
fun StatsScreen(viewModel: StatsViewModel) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    StatsScreenContent(stats)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreenContent(stats: TravelStats?) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.stats_title)) }) }) { padding ->
        if (stats == null) return@Scaffold
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "world") { WorldCard(stats) }
            item(key = "tiles") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        value = formatNumber(stats.places),
                        label = pluralStringResource(R.plurals.summary_places, stats.places),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        value = "${stats.continents}/${stats.byContinent.size}",
                        label = stringResource(R.string.stats_continents),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        value = formatNumber(stats.travelDays),
                        label = pluralStringResource(R.plurals.stats_travel_days, stats.travelDays),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item(key = "continents-title") { SectionTitle(stringResource(R.string.stats_by_continent)) }
            items(stats.byContinent, key = { it.continent.name }) { ContinentRow(it) }
            if (stats.byCountry.isNotEmpty()) {
                item(key = "countries-title") { SectionTitle(stringResource(R.string.stats_by_country)) }
                items(stats.byCountry, key = { it.country.code }) { entry ->
                    ListItem(
                        headlineContent = { Text(entry.country.name) },
                        supportingContent = entry.firstVisit?.let { date ->
                            { Text(stringResource(R.string.stats_first_visit, date.year)) }
                        },
                        leadingContent = { Flag(entry.country.code, size = 28.sp) },
                        trailingContent = {
                            Text(
                                pluralStringResource(R.plurals.places_count, entry.places, entry.places),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun WorldCard(stats: TravelStats) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.stats_you_visited), style = MaterialTheme.typography.bodyLarge)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    stats.countries.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    " " + pluralStringResource(R.plurals.summary_countries, stats.countries),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Text(
                stringResource(
                    R.string.stats_world_share,
                    formatShare(stats.worldShare),
                    stats.unCountries,
                    stats.unCountriesTotal,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            LinearProgressIndicator(
                progress = { stats.worldShare },
                modifier = Modifier.fillMaxWidth().height(8.dp),
            )
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
}

@Composable
private fun ContinentRow(progress: ContinentProgress) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(progress.continent.label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                when {
                    progress.totalCountries > 0 -> "${progress.visitedCountries}/${progress.totalCountries}"
                    progress.visited -> stringResource(R.string.stats_continent_visited)
                    else -> stringResource(R.string.stats_continent_not_visited)
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = {
                when {
                    progress.totalCountries > 0 -> progress.visitedCountries.toFloat() / progress.totalCountries
                    progress.visited -> 1f
                    else -> 0f
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
