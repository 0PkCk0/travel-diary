package app.traveldiary.map

import app.traveldiary.map.MillerProjection.WORLD_HEIGHT
import app.traveldiary.map.MillerProjection.WORLD_WIDTH
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MillerProjectionTest {

    @Test
    fun mapsTheWorldToTheMapRectangle() {
        assertEquals(0f, MillerProjection.x(-180.0), 1e-3f)
        assertEquals(WORLD_WIDTH / 2, MillerProjection.x(0.0), 1e-3f)
        assertEquals(WORLD_WIDTH, MillerProjection.x(180.0), 1e-3f)
        assertEquals(0f, MillerProjection.y(85.0), 1e-3f)
        assertEquals(WORLD_HEIGHT, MillerProjection.y(-78.0), 1e-3f)
        assertTrue(MillerProjection.y(0.0) in WORLD_HEIGHT * 0.5f..WORLD_HEIGHT * 0.6f)
        assertTrue(WORLD_HEIGHT in 590f..620f)
    }

    @Test
    fun clampsThePoles() {
        assertEquals(MillerProjection.y(-78.0), MillerProjection.y(-90.0), 1e-3f)
        assertEquals(MillerProjection.y(85.0), MillerProjection.y(90.0), 1e-3f)
    }

    @Test
    fun inverseGivesBackCoordinates() {
        for ((latitude, longitude) in listOf(48.85 to 2.35, -33.87 to 151.21, 64.13 to -21.9, -54.8 to -68.3, 0.0 to 0.0)) {
            assertEquals(latitude, MillerProjection.latitude(MillerProjection.y(latitude)), 1e-3)
            assertEquals(longitude, MillerProjection.longitude(MillerProjection.x(longitude)), 1e-3)
        }
    }

    @Test
    fun wrapsAroundTheWorld() {
        assertEquals(10f, MillerProjection.wrapX(1010f), 1e-3f)
        assertEquals(990f, MillerProjection.wrapX(-10f), 1e-3f)
        assertEquals(-170.0, MillerProjection.longitude(MillerProjection.x(190.0)), 1e-3)
    }
}
