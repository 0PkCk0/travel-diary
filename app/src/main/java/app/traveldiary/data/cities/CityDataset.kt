package app.traveldiary.data.cities

import androidx.annotation.StringRes
import app.traveldiary.R

/**
 * The city lists the app can use. The built-in one ships in the APK; the others are
 * downloaded from GeoNames (https://download.geonames.org/export/dump/) on request.
 */
enum class CityDataset(
    val id: String,
    @StringRes val title: Int,
    @StringRes val description: Int,
    /** GeoNames file name without extension, null for the built-in list. */
    val geonamesFile: String?,
    val approximateCities: Int,
    val downloadBytes: Long,
) {
    BUILT_IN("builtin-1", R.string.dataset_builtin, R.string.dataset_builtin_description, null, 34_000, 0),
    MEDIUM("cities5000", R.string.dataset_medium, R.string.dataset_medium_description, "cities5000", 70_000, 5_700_000),
    LARGE("cities1000", R.string.dataset_large, R.string.dataset_large_description, "cities1000", 171_000, 11_100_000),
    COMPLETE("cities500", R.string.dataset_complete, R.string.dataset_complete_description, "cities500", 236_000, 13_900_000);

    val url: String? get() = geonamesFile?.let { "https://download.geonames.org/export/dump/$it.zip" }

    companion object {
        fun fromId(id: String): CityDataset? = entries.firstOrNull { it.id == id }
    }
}
