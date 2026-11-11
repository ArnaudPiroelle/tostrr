package app.tostrr.domain.order

import java.net.URLEncoder
import java.util.Locale

/**
 * The car's parts on parts.tesla.com, in [locale]'s language and country (`fr-FR`); null until the parts catalogue has
 * listed its VIN. The site needs the catalogue's reference along with the VIN.
 */
fun Order.partsCatalogUrl(locale: Locale = Locale.getDefault()): String? {
    val vin = vin ?: return null
    val reference = partsCatalogReference ?: return null
    val language = locale.language.ifEmpty { "en" }
    val country = locale.country.ifEmpty { if (language == "en") "US" else language.uppercase() }
    return "https://parts.tesla.com/$language-$country/catalogs" +
        "?catalogExternalReference=${URLEncoder.encode(reference, "UTF-8")}&vin=${URLEncoder.encode(vin, "UTF-8")}"
}
