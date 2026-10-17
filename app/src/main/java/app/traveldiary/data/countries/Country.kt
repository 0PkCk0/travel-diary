package app.traveldiary.data.countries

import androidx.annotation.StringRes
import app.traveldiary.R

enum class Continent(val code: String, @StringRes val label: Int) {
    AFRICA("AF", R.string.continent_africa),
    ANTARCTICA("AN", R.string.continent_antarctica),
    ASIA("AS", R.string.continent_asia),
    EUROPE("EU", R.string.continent_europe),
    NORTH_AMERICA("NA", R.string.continent_north_america),
    OCEANIA("OC", R.string.continent_oceania),
    SOUTH_AMERICA("SA", R.string.continent_south_america);

    companion object {
        fun fromCode(code: String): Continent? = entries.firstOrNull { it.code == code }
    }
}

data class Country(
    /** ISO 3166-1 alpha-2 code, as used by GeoNames (plus XK for Kosovo). */
    val code: String,
    val name: String,
    val continent: Continent,
    /** UN member or observer state; these 195 make up "the world" in the statistics. */
    val isUnMember: Boolean,
) {
    val flag: String get() = flagEmoji(code)

    companion object {
        /** Parses a line of assets/data/countries.tsv: code, name, continent code, UN member (1/0). */
        fun fromTsv(line: String): Country? {
            val columns = line.split('\t')
            if (columns.size < 4) return null
            val continent = Continent.fromCode(columns[2]) ?: return null
            return Country(columns[0], columns[1], continent, columns[3] == "1")
        }
    }
}

/** Turns a two-letter country code such as "IT" into its flag emoji. */
fun flagEmoji(code: String): String {
    if (code.length != 2 || code.any { it !in 'A'..'Z' }) return "🏳️"
    return buildString {
        code.forEach { appendCodePoint(REGIONAL_INDICATOR_A + (it - 'A')) }
    }
}

private const val REGIONAL_INDICATOR_A = 0x1F1E6
