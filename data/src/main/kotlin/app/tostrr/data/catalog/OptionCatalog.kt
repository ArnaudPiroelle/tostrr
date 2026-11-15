package app.tostrr.data.catalog

import android.content.Context
import app.tostrr.domain.order.OptionInfo
import app.tostrr.domain.order.OptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Serializable
private data class OptionCodeFile(val updated: String, val codes: List<OptionCodeEntry>)

@Serializable
private data class OptionCodeEntry(val code: String, val label: String, val category: String? = null)

/**
 * Option code -> description (e.g. `W38A` -> `18" Photon Wheels`).
 *
 * Tesla doesn't expose these labels. They come from the community catalogue of tesla-order-status-tracker.de
 * (also used by github.com/chrisi51/tesla-order-status), bundled in `assets/option_codes.json` and refreshed with the
 * project skill `refresh-code-tables` (`.claude/skills/`); the app never downloads it. Read once, when first used.
 */
@Single
class OptionCatalog(context: Context) : OptionRepository {
    private val _codes = MutableStateFlow(load(context))
    override val codes: StateFlow<Map<String, OptionInfo>> = _codes.asStateFlow()

    private fun load(context: Context): Map<String, OptionInfo> = runCatching {
        val text = context.assets.open(ASSET).bufferedReader().use { it.readText() }
        json.decodeFromString<OptionCodeFile>(text).codes.associate { it.code to OptionInfo(it.label, it.category) }
    }.getOrDefault(emptyMap())

    private companion object {
        const val ASSET = "option_codes.json"
        val json = Json { ignoreUnknownKeys = true }
    }
}
