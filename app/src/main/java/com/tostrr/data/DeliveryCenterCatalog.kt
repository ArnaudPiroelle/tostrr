package com.tostrr.data

import android.content.Context
import com.tostrr.domain.DeliveryCenter
import com.tostrr.domain.DeliveryCenters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class CenterEntry(
    val name: String,
    val country: String,
    val city: String,
    val address: String,
    val lat: Double,
    val lng: Double,
)

/**
 * The delivery centres bundled in `assets/delivery_centers.json`, a snapshot of tesla.com/findus taken on
 * 2026-10-25 (Tesla has no public API for it). Read once, on first use.
 */
class DeliveryCenterCatalog(private val context: Context) {
    private var cached: DeliveryCenters? = null

    suspend fun centers(): DeliveryCenters = cached ?: withContext(Dispatchers.IO) {
        val text = context.assets.open(ASSET).bufferedReader().use { it.readText() }
        val entries = Json.decodeFromString<List<CenterEntry>>(text)
        DeliveryCenters(entries.map { DeliveryCenter(it.name, it.country, it.city, it.address, it.lat, it.lng) })
    }.also { cached = it }

    private companion object {
        const val ASSET = "delivery_centers.json"
    }
}
