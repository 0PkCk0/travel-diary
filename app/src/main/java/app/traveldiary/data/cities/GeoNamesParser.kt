package app.traveldiary.data.cities

/**
 * Reads the tab-separated GeoNames "cities" dumps described in
 * https://download.geonames.org/export/dump/readme.txt
 */
class GeoNamesParser(private val regionNames: Map<String, String>) {

    fun parse(line: String): CityEntity? {
        val columns = line.split('\t')
        if (columns.size < 15 || columns[7] in EXCLUDED_FEATURE_CODES) return null
        val id = columns[0].toLongOrNull() ?: return null
        val name = columns[1].trim()
        val latitude = columns[4].toDoubleOrNull() ?: return null
        val longitude = columns[5].toDoubleOrNull() ?: return null
        val countryCode = columns[8]
        if (name.isEmpty() || countryCode.length != 2 || isNumberedDistrict(name, columns[7])) return null
        // GeoNames transliterates "Zürich" as "Zuerich": keep both spellings searchable.
        val asciiName = SearchNormalizer.normalize(columns[2])
        val searchName = SearchNormalizer.normalize(name).ifEmpty { asciiName }
        return CityEntity(
            id = id,
            name = name,
            // "Berlin, Berlin, Germany" reads badly: drop a region named like the city.
            region = regionNames["$countryCode.${columns[10]}"]?.takeIf { it != name },
            countryCode = countryCode,
            latitude = latitude,
            longitude = longitude,
            population = columns[14].toLongOrNull() ?: 0,
            searchName = searchName,
            searchAscii = asciiName.takeIf { it.isNotEmpty() && it != searchName },
        )
    }

    companion object {
        /** Places that no longer exist: historical, abandoned or destroyed. */
        private val EXCLUDED_FEATURE_CODES = setOf("PPLH", "PPLQ", "PPLW", "PPLCH")

        /** Districts named like "Paris 15 Vaugirard" or "Zürich (Kreis 3)". Keep in sync with tools/build_assets.py. */
        private val NUMBERED_DISTRICT = Regex("""^(Paris|Lyon|Marseille) \d|\(Kreis \d+\)""")

        /**
         * City districts such as "Paris 15 Vaugirard" or "Sector 2" (Bucharest): the city itself is
         * enough for a travel diary. Real towns like "6th of October City" or "Mile 91" are kept.
         */
        internal fun isNumberedDistrict(name: String, featureCode: String): Boolean {
            if (name.none { it.isDigit() } || name.first().isDigit()) return false
            return featureCode == "PPLX" || featureCode == "PPLA5" || NUMBERED_DISTRICT.containsMatchIn(name)
        }

        /** Reads assets/data/admin1.tsv: "IT.09<TAB>Liguria" lines. */
        fun parseRegionNames(lines: Sequence<String>): Map<String, String> =
            lines.mapNotNull { line ->
                val tab = line.indexOf('\t')
                if (tab <= 0) null else line.substring(0, tab) to line.substring(tab + 1).substringBefore('\t')
            }.toMap()
    }
}
