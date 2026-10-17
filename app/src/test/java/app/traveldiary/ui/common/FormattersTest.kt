package app.traveldiary.ui.common

import app.traveldiary.ui.map.formatShare
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormattersTest {

    private fun date(year: Int, month: Int, day: Int) = LocalDate.of(year, month, day)

    @Test
    fun dateRanges() {
        assertNull(formatDateRange(null, null))
        assertEquals("12 May 2024", formatDateRange(date(2024, 5, 12), null))
        assertEquals("12 May 2024", formatDateRange(date(2024, 5, 12), date(2024, 5, 12)))
        assertEquals("12 – 18 May 2024", formatDateRange(date(2024, 5, 12), date(2024, 5, 18)))
        assertEquals("28 Apr – 3 May 2024", formatDateRange(date(2024, 4, 28), date(2024, 5, 3)))
        assertEquals("28 Dec 2023 – 3 Jan 2024", formatDateRange(date(2023, 12, 28), date(2024, 1, 3)))
    }

    @Test
    fun stayLengths() {
        assertNull(stayLength(null, null))
        assertEquals(1, stayLength(date(2024, 5, 12), null))
        assertEquals(7, stayLength(date(2024, 5, 12), date(2024, 5, 18)))
    }

    @Test
    fun populations() {
        assertEquals("800", formatPopulation(800))
        assertEquals("1.3K", formatPopulation(1_250))
        assertEquals("2K", formatPopulation(2_000))
        assertEquals("48K", formatPopulation(48_500))
        assertEquals("2.1M", formatPopulation(2_148_000))
        assertEquals("3M", formatPopulation(3_000_000))
    }

    @Test
    fun worldShare() {
        assertEquals("0%", formatShare(0f))
        assertEquals("<1%", formatShare(1f / 195))
        assertEquals("12%", formatShare(0.123f))
        assertEquals("100%", formatShare(1f))
    }

    @Test
    fun datePickerTimestampsAreUtcMidnight() {
        val date = date(2024, 2, 29)
        assertEquals(date, pickerMillisToDate(date.toPickerMillis()))
        assertEquals(1_709_164_800_000L, date.toPickerMillis())
    }

    @Test
    fun coordinates() {
        assertEquals("48.857° N, 2.352° E", formatCoordinates(48.8566, 2.3522))
        assertEquals("33.869° S, 151.209° E", formatCoordinates(-33.8688, 151.2093))
        assertEquals("40.713° N, 74.006° W", formatCoordinates(40.7128, -74.0060))
    }
}
