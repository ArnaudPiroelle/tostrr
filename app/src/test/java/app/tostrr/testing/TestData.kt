package app.tostrr.testing

import app.tostrr.domain.order.Order
import app.tostrr.domain.order.OrderStep
import kotlinx.serialization.json.JsonObject

/** An order as the mapper would build it, with only what the screens show filled in. */
fun order(
    reference: String = "RN100000001",
    modelName: String = "Model 3",
    deliveryCenter: String? = "Tesla Paris-Ouest",
    countryCode: String? = "FR",
    deliveryWindow: String? = "29 Novembre - 29 Décembre",
    appointment: String? = null,
) = Order(
    reference = reference,
    status = "BOOKED",
    vin = null,
    modelName = modelName,
    optionCodes = emptyList(),
    imageUrl = null,
    steps = listOf(
        OrderStep("Commande passée", "14/07/26", done = true),
        OrderStep("VIN attribué", null, done = false),
        OrderStep("Livraison", null, done = false),
    ),
    deliveryCenter = deliveryCenter,
    deliveryWindow = deliveryWindow,
    deliverySlot = deliveryWindow,
    appointment = appointment,
    etaToDeliveryCenter = null,
    expectedRegistration = null,
    fetchedAtEpochMs = 1_790_000_000_000,
    raw = JsonObject(emptyMap()),
    countryCode = countryCode,
)
