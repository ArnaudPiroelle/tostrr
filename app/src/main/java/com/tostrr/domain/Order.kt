package com.tostrr.domain

import kotlinx.serialization.json.JsonObject

data class OrderStep(
    val label: String,
    val date: String?,
    val done: Boolean,
    /** Notification text shown when this step becomes done. */
    val doneMessage: String = "",
)

data class Order(
    val reference: String,
    val status: String,
    val vin: String?,
    val modelName: String,
    val optionCodes: List<String>,
    val imageUrl: String?,
    val bookedDate: String?,
    val steps: List<OrderStep>,
    val deliveryCenter: String?,
    val deliveryWindow: String?,
    /** Short one-line delivery slot for cards: the appointment if confirmed, else the abbreviated window. */
    val deliverySlot: String?,
    val appointment: String?,
    val etaToDeliveryCenter: String?,
    val expectedRegistration: String?,
    val fetchedAtEpochMs: Long,
    val raw: JsonObject,
) {
    /** Index of the first step not completed yet (the "current" one), or the last step when all are done. */
    val currentStepIndex: Int
        get() = steps.indexOfFirst { !it.done }.let { if (it == -1) steps.lastIndex else it }
}

data class Change(
    val timestampEpochMs: Long,
    val key: String,
    /** Scalars as text; objects/arrays as compact JSON. Null means the path didn't exist on that side. */
    val oldValue: String?,
    val newValue: String?,
    /** True when either side is an object or array, so the details can be inspected as JSON. */
    val structured: Boolean = false,
)
