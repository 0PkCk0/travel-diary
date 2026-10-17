package app.traveldiary.map

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import app.traveldiary.map.MillerProjection.WORLD_HEIGHT
import app.traveldiary.map.MillerProjection.WORLD_WIDTH
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed interface MapFocus {
    data object World : MapFocus

    data class Area(val bounds: Rect) : MapFocus

    /** [zoom] is relative to the whole-world view. */
    data class Point(val position: Offset, val zoom: Float) : MapFocus
}

/**
 * Where the map is looking: [scale] is screen pixels per map unit and [offset] is where the map's
 * top-left corner is on screen. The map repeats east-west, so [offset] x stays in (-world width, 0].
 */
@Stable
class MapViewportState(private val initialFocus: MapFocus = MapFocus.World) {
    var scale by mutableFloatStateOf(0f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set

    private var size = IntSize.Zero
    private var animation: Job? = null

    val isReady: Boolean get() = scale > 0f

    /** The whole world fits the width of the view. */
    internal val minScale: Float get() = size.width / WORLD_WIDTH
    private val maxScale: Float get() = minScale * MAX_ZOOM

    fun onSizeChanged(newSize: IntSize) {
        if (newSize.width == 0 || newSize.height == 0 || newSize == size) return
        val previousCenter = if (isReady) center() else null
        size = newSize
        if (previousCenter == null) {
            val (targetScale, targetCenter) = target(initialFocus)
            moveTo(targetScale, targetCenter)
        } else {
            moveTo(scale, previousCenter)
        }
    }

    /** Map position at the centre of the view. */
    fun center(): Offset = Offset((size.width / 2f - offset.x) / scale, (size.height / 2f - offset.y) / scale)

    /** Map position under a point of the view. */
    fun toMap(screen: Offset): Offset =
        Offset(MillerProjection.wrapX((screen.x - offset.x) / scale), (screen.y - offset.y) / scale)

    /** Pinch and drag. */
    fun onGesture(centroid: Offset, pan: Offset, zoom: Float) {
        if (!isReady) return
        stopAnimation()
        val newScale = (scale * zoom).coerceIn(minScale, maxScale)
        val newOffset = centroid + (offset - centroid) * (newScale / scale) + pan
        scale = newScale
        offset = clampOffset(newScale, newOffset)
    }

    fun animateTo(scope: CoroutineScope, focus: MapFocus) {
        if (!isReady) return
        val (targetScale, targetCenter) = target(focus)
        animate(scope, targetScale, targetCenter)
    }

    /** Double tap: zoom in, keeping the tapped point under the finger. */
    fun zoomIn(scope: CoroutineScope, around: Offset) {
        if (!isReady) return
        val targetScale = (scale * DOUBLE_TAP_ZOOM).coerceAtMost(maxScale)
        val tapped = (around - offset) / scale
        val viewCenter = Offset(size.width / 2f, size.height / 2f)
        animate(scope, targetScale, tapped + (viewCenter - around) / targetScale)
    }

    fun stopAnimation() {
        animation?.cancel()
        animation = null
    }

    /** The pin closest to a point of the view, within [radius] pixels. */
    fun pinAt(screen: Offset, pins: List<MapPin>, radius: Float): MapPin? {
        if (!isReady) return null
        val worldWidthPx = WORLD_WIDTH * scale
        return pins
            .map { pin ->
                var dx = (offset.x + pin.position.x * scale - screen.x) % worldWidthPx
                if (dx > worldWidthPx / 2) dx -= worldWidthPx
                if (dx < -worldWidthPx / 2) dx += worldWidthPx
                val dy = offset.y + pin.position.y * scale - screen.y
                pin to (dx * dx + dy * dy)
            }
            .filter { (_, distance2) -> distance2 <= radius * radius }
            .minByOrNull { (_, distance2) -> distance2 }
            ?.first
    }

    private fun animate(scope: CoroutineScope, targetScale: Float, targetCenter: Offset) {
        stopAnimation()
        val startScale = scale
        val startCenter = center()
        val endScale = targetScale.coerceIn(minScale, maxScale)
        // Go the short way around the world.
        var targetX = targetCenter.x
        while (targetX - startCenter.x > WORLD_WIDTH / 2) targetX -= WORLD_WIDTH
        while (startCenter.x - targetX > WORLD_WIDTH / 2) targetX += WORLD_WIDTH
        animation = scope.launch {
            animate(0f, 1f, animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)) { t, _ ->
                moveTo(
                    newScale = startScale * (endScale / startScale).pow(t),
                    center = Offset(lerp(startCenter.x, targetX, t), lerp(startCenter.y, targetCenter.y, t)),
                )
            }
        }
    }

    private fun target(focus: MapFocus): Pair<Float, Offset> = when (focus) {
        MapFocus.World -> minScale to Offset(WORLD_WIDTH / 2, MillerProjection.y(WORLD_CENTER_LATITUDE))
        is MapFocus.Area -> {
            val bounds = focus.bounds
            val fit = min(size.width / (bounds.width * 1.4f), size.height / (bounds.height * 1.8f))
            fit.coerceIn(minScale, minScale * MAX_AREA_ZOOM) to bounds.center
        }
        is MapFocus.Point -> minScale * focus.zoom to focus.position
    }

    private fun moveTo(newScale: Float, center: Offset) {
        val clamped = newScale.coerceIn(minScale, maxScale)
        scale = clamped
        offset = clampOffset(clamped, Offset(size.width / 2f - center.x * clamped, size.height / 2f - center.y * clamped))
    }

    private fun clampOffset(scale: Float, raw: Offset): Offset {
        val worldWidthPx = WORLD_WIDTH * scale
        val worldHeightPx = WORLD_HEIGHT * scale
        val y = if (worldHeightPx <= size.height) {
            (size.height - worldHeightPx) / 2f
        } else {
            raw.y.coerceIn(size.height - worldHeightPx, 0f)
        }
        var x = raw.x % worldWidthPx
        if (x > 0f) x -= worldWidthPx
        if (abs(x + worldWidthPx) < 0.01f) x = 0f
        return Offset(x, y)
    }

    private fun lerp(start: Float, stop: Float, fraction: Float) = start + (stop - start) * fraction

    companion object {
        private const val MAX_ZOOM = 150f
        private const val MAX_AREA_ZOOM = 40f
        private const val DOUBLE_TAP_ZOOM = 2.5f
        private const val WORLD_CENTER_LATITUDE = 20.0

        /** Keeps the view across rotation and when coming back to a tab. */
        val Saver = listSaver<MapViewportState, Float>(
            save = { state ->
                if (state.isReady) {
                    val center = state.center()
                    listOf(center.x, center.y, state.scale / state.minScale)
                } else {
                    emptyList()
                }
            },
            restore = { saved ->
                if (saved.size == 3) MapViewportState(MapFocus.Point(Offset(saved[0], saved[1]), saved[2])) else MapViewportState()
            },
        )
    }
}

@Composable
fun rememberMapViewportState(initialFocus: MapFocus = MapFocus.World): MapViewportState =
    rememberSaveable(saver = MapViewportState.Saver) { MapViewportState(initialFocus) }
