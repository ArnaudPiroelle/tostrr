package com.tostrr.data.catalog

import android.content.Context
import com.tostrr.domain.deliverycenter.DeliveryCenter
import com.tostrr.domain.deliverycenter.DeliveryCenterRepository
import com.tostrr.domain.deliverycenter.DeliveryCenters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Serializable
private data class CenterEntry(
    val name: String,
    val country: String,
    val city: String,
    val address: String,
)

/**
 * The delivery centres bundled in `assets/delivery_centers.json`, a snapshot of tesla.com/findus taken on
 * 2026-10-25 (Tesla has no public API for it). Read once, on first use.
 */
@Single
class DeliveryCenterCatalog(private val context: Context) : DeliveryCenterRepository {
    private var cached: DeliveryCenters? = null

    override suspend fun centers(): DeliveryCenters = cached ?: withContext(Dispatchers.IO) {
        val text = context.assets.open(ASSET).bufferedReader().use { it.readText() }
        val entries = json.decodeFromString<List<CenterEntry>>(text)
        DeliveryCenters(entries.map { DeliveryCenter(it.name, it.country, it.city, it.address) })
    }.also { cached = it }

    private companion object {
        const val ASSET = "delivery_centers.json"

        /** The asset also carries each centre's coordinates, which nothing uses. */
        val json = Json { ignoreUnknownKeys = true }
    }
}
