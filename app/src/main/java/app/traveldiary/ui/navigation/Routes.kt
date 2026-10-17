package app.traveldiary.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object MapRoute

@Serializable
data object PlacesRoute

@Serializable
data object StatsRoute

@Serializable
data object SettingsRoute

/** City search, optionally limited to one country. */
@Serializable
data class SearchRoute(val countryCode: String? = null)

@Serializable
data class PlaceRoute(val placeId: Long)

/**
 * Add or edit a visit. Exactly one way in:
 * - [visitId]: edit that visit,
 * - [placeId]: new visit to a place already in the diary,
 * - [cityId]: new visit to a city picked in the search,
 * - [latitude] and [longitude]: new place dropped on the map.
 */
@Serializable
data class VisitEditorRoute(
    val visitId: Long? = null,
    val placeId: Long? = null,
    val cityId: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)
