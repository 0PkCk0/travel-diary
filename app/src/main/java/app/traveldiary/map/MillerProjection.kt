package app.traveldiary.map

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.tan

/**
 * Miller cylindrical projection: familiar like Mercator, without its giant poles.
 * Map units run from 0 to [WORLD_WIDTH] west to east and from 0 to [WORLD_HEIGHT] north to south.
 */
object MillerProjection {
    /** The map stops here: north of every island, and south of Antarctica's coast. */
    const val MAX_LATITUDE = 85.0
    const val MIN_LATITUDE = -78.0
    const val WORLD_WIDTH = 1000f

    private val unitsPerRadian = WORLD_WIDTH / (2 * PI)
    private val topY = millerY(MAX_LATITUDE)

    val WORLD_HEIGHT: Float = ((topY - millerY(MIN_LATITUDE)) * unitsPerRadian).toFloat()

    fun x(longitude: Double): Float = ((longitude + 180.0) / 360.0 * WORLD_WIDTH).toFloat()

    fun y(latitude: Double): Float =
        ((topY - millerY(latitude.coerceIn(MIN_LATITUDE, MAX_LATITUDE))) * unitsPerRadian).toFloat()

    fun position(latitude: Double, longitude: Double): Offset = Offset(x(longitude), y(latitude))

    fun longitude(x: Float): Double = wrapX(x) / WORLD_WIDTH * 360.0 - 180.0

    fun latitude(y: Float): Double {
        val millerY = topY - y / unitsPerRadian
        return Math.toDegrees(2.5 * atan(exp(0.8 * millerY)) - 0.625 * PI)
    }

    /** The map repeats east-west: brings any x back into [0, WORLD_WIDTH). */
    fun wrapX(x: Float): Float {
        val wrapped = x % WORLD_WIDTH
        return if (wrapped < 0f) wrapped + WORLD_WIDTH else wrapped
    }

    private fun millerY(latitude: Double): Double = 1.25 * ln(tan(PI / 4 + 0.4 * Math.toRadians(latitude)))
}
