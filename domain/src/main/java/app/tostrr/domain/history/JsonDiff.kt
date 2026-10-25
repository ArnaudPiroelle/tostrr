package app.tostrr.domain.history

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Structural diff between two JSON trees, reported at the highest level where the change is meaningful.
 *
 * - Objects are compared key by key, so a single edited field is reported on its own path.
 * - A key that appears or disappears is reported once, with its whole subtree as the value.
 * - When a container becomes empty (or an empty one gets filled), or when the type changes, the container itself
 *   is reported: `details.tasks` going to `{}` is ONE change, not one per field it used to hold.
 * - Arrays are compared index by index only when their size is unchanged; otherwise the array is reported whole.
 */
object JsonDiff {

    /** [old] is null when the path is new, [new] is null when it vanished. */
    data class Delta(val path: String, val old: JsonElement?, val new: JsonElement?)

    fun diff(old: JsonElement, new: JsonElement): List<Delta> {
        val out = ArrayList<Delta>()
        walk("", old, new, out)
        return out.sortedBy { it.path }
    }

    private fun walk(path: String, old: JsonElement, new: JsonElement, out: MutableList<Delta>) {
        if (old == new) return
        when {
            old is JsonObject && new is JsonObject && old.isNotEmpty() && new.isNotEmpty() -> {
                for (key in old.keys + new.keys) {
                    val child = if (path.isEmpty()) key else "$path.$key"
                    val o = old[key]
                    val n = new[key]
                    when {
                        o == null -> out += Delta(child, null, n)
                        n == null -> out += Delta(child, o, null)
                        else -> walk(child, o, n, out)
                    }
                }
            }
            old is JsonArray && new is JsonArray && old.isNotEmpty() && old.size == new.size -> {
                for (i in old.indices) walk("$path.$i", old[i], new[i], out)
            }
            else -> out += Delta(path, old, new)
        }
    }
}
