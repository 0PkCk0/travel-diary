package app.traveldiary.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.traveldiary.R
import app.traveldiary.ui.common.EmptyState
import app.traveldiary.ui.common.Flag
import app.traveldiary.ui.common.formatNumber
import app.traveldiary.ui.common.formatPopulation

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBack: () -> Unit,
    onCitySelected: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SearchScreenContent(
        query = viewModel.query,
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onClearCountry = viewModel::clearCountry,
        onBack = onBack,
        onCitySelected = onCitySelected,
        onOpenSettings = onOpenSettings,
    )
}

@Composable
fun SearchScreenContent(
    query: String,
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onClearCountry: () -> Unit,
    onBack: () -> Unit,
    onCitySelected: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    autoFocus: Boolean = true,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    if (autoFocus) LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(Modifier.fillMaxWidth().statusBarsPadding()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                        TextField(
                            value = query,
                            onValueChange = onQueryChange,
                            placeholder = {
                                Text(
                                    state.country?.let { stringResource(R.string.search_hint_in_country, it.name) }
                                        ?: stringResource(R.string.search_hint),
                                )
                            },
                            singleLine = true,
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { onQueryChange("") }) {
                                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_clear))
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Search,
                            ),
                            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            modifier = Modifier.weight(1f).focusRequester(focusRequester),
                        )
                    }
                    state.country?.let { country ->
                        InputChip(
                            selected = true,
                            onClick = onClearCountry,
                            label = { Text("${country.flag}  ${country.name}") },
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.search_clear_country),
                                    modifier = Modifier.size(InputChipDefaults.IconSize),
                                )
                            },
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                        )
                    }
                }
            }
        },
    ) { padding ->
        val contentModifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()
        when {
            !state.databaseReady -> PreparingDatabase(state.citiesImported, contentModifier)
            state.results.isNotEmpty() -> LazyColumn(contentModifier) {
                items(state.results, key = { it.city.id }) { result ->
                    SearchResultItem(result) {
                        keyboard?.hide()
                        onCitySelected(result.city.id)
                    }
                }
            }
            state.noResults -> Column(contentModifier.verticalScroll(rememberScrollState())) {
                EmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = stringResource(R.string.search_no_results_title),
                    message = stringResource(R.string.search_no_results_message),
                    action = {
                        OutlinedButton(onClick = onOpenSettings) { Text(stringResource(R.string.search_open_city_settings)) }
                    },
                )
            }
            else -> Column(contentModifier.verticalScroll(rememberScrollState())) {
                EmptyState(
                    icon = Icons.Outlined.TravelExplore,
                    title = stringResource(R.string.search_tips_title),
                    message = stringResource(R.string.search_tips_message),
                )
            }
        }
    }
}

@Composable
private fun SearchResultItem(result: SearchResult, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(result.city.name) },
        supportingContent = { Text(listOfNotNull(result.city.region, result.countryName).joinToString(", ")) },
        leadingContent = { Flag(result.city.countryCode, size = 28.sp) },
        trailingContent = {
            if (result.visited) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.search_visited),
                    tint = MaterialTheme.colorScheme.primary,
                )
            } else if (result.city.population > 0) {
                Text(
                    formatPopulation(result.city.population),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun PreparingDatabase(citiesImported: Int?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        CircularProgressIndicator()
        Text(
            stringResource(R.string.search_preparing),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        if (citiesImported != null && citiesImported > 0) {
            Text(
                stringResource(R.string.search_preparing_count, formatNumber(citiesImported)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
