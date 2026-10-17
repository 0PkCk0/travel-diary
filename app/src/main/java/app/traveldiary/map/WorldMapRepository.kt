package app.traveldiary.map

import android.content.Context
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The world map, ready to draw: one [Path] per country in map units. */
class WorldMap(val geometry: WorldGeometry) {
    private val countryPaths: Map<String, Path> = geometry.countries.associate { it.code to it.toPath() }

    /** All countries in one path: filled for the land, stroked for the borders. */
    val land: Path = Path().apply {
        fillType = PathFillType.EvenOdd
        countryPaths.values.forEach { addPath(it) }
    }

    fun path(code: String): Path? = countryPaths[code]

    fun pathOf(codes: Collection<String>): Path = Path().apply {
        fillType = PathFillType.EvenOdd
        codes.forEach { code -> countryPaths[code]?.let { addPath(it) } }
    }

    private fun CountryGeometry.toPath() = Path().apply {
        fillType = PathFillType.EvenOdd
        rings.forEach { ring ->
            moveTo(ring[0], ring[1])
            for (i in 2 until ring.size step 2) lineTo(ring[i], ring[i + 1])
            close()
        }
    }
}

/** Loads the bundled world map once, in the background. */
class WorldMapRepository(private val context: Context, private val scope: CoroutineScope) {
    private val _map = MutableStateFlow<WorldMap?>(null)
    val map: StateFlow<WorldMap?> = _map.asStateFlow()

    fun load() {
        if (_map.value != null) return
        scope.launch(Dispatchers.Default) {
            val bytes = context.assets.open("map/world.bin").use { it.readBytes() }
            _map.value = WorldMap(WorldGeometry.read(bytes))
        }
    }

    /** Country at a position, or null when the map is not loaded yet or the position is at sea. */
    fun countryAt(latitude: Double, longitude: Double): String? =
        _map.value?.geometry?.countryAt(latitude, longitude)
}
