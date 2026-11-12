package app.tostrr.data.shipping

import app.tostrr.domain.shipping.GeoPosition
import app.tostrr.domain.shipping.Ship
import app.tostrr.domain.shipping.ShipSnapshot
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Reads the ship tracking site's JSON, defensively: a ship without a name is dropped, any other missing field is null. */
internal object ShipSnapshotParser {
    /** The id of the newest snapshot in `snapshots/index.json`, by its `snapshotAt`. */
    fun latestId(index: JsonObject): String? =
        (index["snapshots"] as? JsonArray).orEmpty().filterIsInstance<JsonObject>()
            .maxByOrNull { it.string("snapshotAt").orEmpty() }
            ?.string("id")

    fun snapshot(root: JsonObject): ShipSnapshot? {
        val takenAt = root.string("snapshotAt")?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() } ?: return null
        val ships = (root["ships"] as? JsonArray).orEmpty().filterIsInstance<JsonObject>().mapNotNull(::ship)
        return ShipSnapshot(takenAt, ships)
    }

    private fun ship(o: JsonObject): Ship? {
        val name = o.string("name")?.takeIf { it.isNotBlank() } ?: return null
        val lat = o.double("lat")
        val lon = o.double("lon")
        return Ship(
            name = name,
            route = (o["route"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.contentOrNull },
            nextPort = o.string("nextPort"),
            // "2026-11-28", sometimes with a time after it.
            eta = o.string("eta")?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            position = if (lat != null && lon != null) GeoPosition.of(lat, lon) else null,
            headingDeg = o.double("heading"),
            vinBlocks = (o["vins"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.contentOrNull },
            done = (o["done"] as? JsonPrimitive)?.booleanOrNull == true,
            operator = o.string("line")?.takeIf { it.isNotBlank() },
            // "park" in port, "sail" at sea.
            atBerth = o.string("mode") == "park",
            progress = o.double("prog")?.coerceIn(0.0, 1.0),
            // "2026-11-12 16:00" when the site knows the hour.
            etaTime = o.string("eta")?.drop(10)?.trim()?.takeIf { it.isNotEmpty() }?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
            draughtMeters = o.double("draught"),
        )
    }

    private fun JsonObject.string(key: String) = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
    private fun JsonObject.double(key: String) = (this[key] as? JsonPrimitive)?.doubleOrNull
}
