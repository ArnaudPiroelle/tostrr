package app.tostrr.domain.order

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import java.time.LocalDate

/**
 * What the app finds out about an order beyond Tesla's order payloads. It is kept in [Order.raw] under `local`, so it is
 * saved, diffed into the history and archived like them.
 */
data class LocalSignals(
    /**
     * Day Tesla's public parts catalogue was first found for the VIN. It only lists cars that exist, so the car is
     * built; a VIN merely assigned, or an unknown one, gets "not available".
     */
    val partsCatalogSince: LocalDate? = null,
) {
    /** Null when there is nothing to keep, so an order the app knows nothing more about has no `local` node. */
    fun toJson(): JsonObject? =
        if (this == LocalSignals()) null else buildJsonObject {
            partsCatalogSince?.let { put(PARTS_CATALOG_SINCE, it.toString()) }
        }

    companion object {
        const val KEY = "local"
        private const val PARTS_CATALOG_SINCE = "partsCatalogSince"

        fun fromJson(json: JsonObject?): LocalSignals = LocalSignals(
            partsCatalogSince = (json?.get(PARTS_CATALOG_SINCE) as? JsonPrimitive)?.contentOrNull
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        )
    }
}
