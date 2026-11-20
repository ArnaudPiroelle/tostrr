package app.tostrr.domain.order

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.io.File
import java.time.Instant

/**
 * One order as a JSON file, for the user to keep or send: what Tesla sent as is, what the app found out on its own
 * ([LocalSignals]) and the change history, oldest first. Nothing about the session is in it.
 */
object OrderExport {
    const val FORMAT = "tostrr-order-export"
    const val VERSION = 1

    fun json(order: Order, history: List<Change>, exportedAtEpochMs: Long): String =
        PRETTY.encodeToString(JsonElement.serializer(), buildJsonObject {
            put("format", FORMAT)
            put("version", VERSION)
            put("exportedAt", iso(exportedAtEpochMs))
            put("reference", order.reference)
            put("vin", order.vin)
            put("fetchedAt", iso(order.fetchedAtEpochMs))
            put("tesla", order.raw)
            put("local", order.local.toJson() ?: JsonNull)
            putJsonArray("history") {
                history.sortedBy { it.timestampEpochMs }.forEach { change ->
                    addJsonObject {
                        put("at", iso(change.timestampEpochMs))
                        put("path", change.key)
                        put("old", change.oldValue.value(change.structured))
                        put("new", change.newValue.value(change.structured))
                    }
                }
            }
        })

    /** A structured side is kept as the JSON it was, rather than as a string holding it. */
    private fun String?.value(structured: Boolean): JsonElement = when {
        this == null -> JsonNull
        structured -> runCatching { Json.parseToJsonElement(this) }.getOrElse { JsonPrimitive(this) }
        else -> JsonPrimitive(this)
    }

    private fun iso(epochMs: Long) = Instant.ofEpochMilli(epochMs).toString()

    private val PRETTY = Json { prettyPrint = true }
}

/** Puts an export where it can be shared from. Implemented in `:data`, which owns the app's files. */
interface OrderExportWriter {
    /** Writes [json] as the export of order [reference], replacing any earlier one, and returns the file. */
    suspend fun write(reference: String, json: String): File

    /** Deletes every export, at logout. */
    fun clear()
}
