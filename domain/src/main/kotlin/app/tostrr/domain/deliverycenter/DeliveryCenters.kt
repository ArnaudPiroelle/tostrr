package app.tostrr.domain.deliverycenter

import java.net.URLEncoder
import java.text.Normalizer

/** A Tesla delivery centre, as listed on tesla.com/findus. */
data class DeliveryCenter(
    val name: String,
    /** ISO 3166-1 alpha-2 code, e.g. `FR`. */
    val country: String,
    val city: String,
    val address: String,
)

/**
 * Finds the centre of an order. Tesla only gives its name (`deliveryAddressTitle`), which is the centre's
 * display name on tesla.com/findus, so the match is on that name, the order's country breaking ties.
 */
class DeliveryCenters(centers: List<DeliveryCenter>) {
    private val byName: Map<String, List<DeliveryCenter>> = centers.groupBy { normalize(it.name) }

    fun find(name: String?, country: String?): DeliveryCenter? {
        val candidates = name?.let { byName[normalize(it)] } ?: return null
        return candidates.firstOrNull { it.country.equals(country, ignoreCase = true) } ?: candidates.singleOrNull()
    }

    companion object {
        /** Case, accents and spacing vary between the order payload and the findus listing. */
        internal fun normalize(text: String): String =
            Normalizer.normalize(text, Normalizer.Form.NFD)
                .replace(Regex("\\p{Mn}+"), "")
                .lowercase()
                .replace(Regex("\\s+"), " ")
                .trim()
    }
}

/**
 * Google Maps link for a delivery centre: searching its name and address lands on the place itself, with its
 * opening hours and reviews, rather than on a bare pin. Without a known centre, the name alone is searched.
 */
fun googleMapsUrl(name: String, center: DeliveryCenter?): String {
    val query = if (center == null) name else "${center.name}, ${center.address}"
    return "https://www.google.com/maps/search/?api=1&query=" + URLEncoder.encode(query, "UTF-8")
}
