package app.tostrr.data.catalog

import android.content.Context
import app.tostrr.domain.vin.VinCode
import app.tostrr.domain.vin.VinCodeRepository
import app.tostrr.domain.vin.VinCodeTable
import app.tostrr.domain.vin.VinSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Serializable
private data class VinCodeFile(val updated: String, val codes: List<VinCodeEntry>)

@Serializable
private data class VinCodeEntry(
    val section: String,
    val code: String,
    val value: Map<String, String>,
)

/**
 * The VIN codes bundled in `assets/vin_codes.json`: the table of tesla-order-status-tracker.de, with French texts
 * added, refreshed with the project skill `refresh-vin-codes` (`.claude/skills/`); the app never downloads it. Read
 * once, on first use; an entry of an unknown section is skipped.
 */
@Single
class VinCodeCatalog(private val context: Context) : VinCodeRepository {
    private var cached: VinCodeTable? = null

    override suspend fun table(): VinCodeTable = cached ?: withContext(Dispatchers.IO) {
        val text = context.assets.open(ASSET).bufferedReader().use { it.readText() }
        val file = json.decodeFromString<VinCodeFile>(text)
        VinCodeTable(
            updated = file.updated,
            codes = file.codes.mapNotNull { entry ->
                VinSection.ofId(entry.section)?.let { VinCode(it, entry.code, entry.value) }
            },
        )
    }.also { cached = it }

    private companion object {
        const val ASSET = "vin_codes.json"

        val json = Json { ignoreUnknownKeys = true }
    }
}
