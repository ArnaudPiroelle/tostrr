package com.tostrr.domain

import kotlinx.serialization.json.JsonObject

/**
 * Decides whether one order step is done from Tesla's payloads.
 *
 * Tesla doesn't document how "documents sent" or "vehicle registered" show up, so each step goes through
 * this interface: [JsonStepDetectors] reads fields from the JSON, and another implementation can replace
 * it without touching [OrderMapper].
 */
fun interface StepDetector {
    /** [order] is the entry of `/api/1/users/orders`, [tasks] is `details.tasks` of the `/tasks` response. */
    fun isDone(order: JsonObject, tasks: JsonObject): Boolean
}

class StepDetectors(
    val documentsSent: StepDetector,
    val vehicleRegistered: StepDetector,
) {
    companion object {
        val Default = StepDetectors(
            documentsSent = JsonStepDetectors.DocumentsSent,
            vehicleRegistered = JsonStepDetectors.VehicleRegistered,
        )
    }
}

/** Simple implementation: looks for a telltale field in the JSON. */
object JsonStepDetectors {

    /** A licence plate only exists once the car is registered. */
    val VehicleRegistered = StepDetector { order, tasks ->
        PLATE_KEYS.any { tasks.deepFind(it) != null || order.deepFind(it) != null }
    }

    /**
     * Guess: `registration.agreementsStatus` is null until the contracts can be reviewed. It has never been
     * seen with a value, so this has to be checked against a real payload.
     */
    val DocumentsSent = StepDetector { _, tasks ->
        !tasks.obj("registration").str("agreementsStatus").isNullOrBlank()
    }

    private val PLATE_KEYS = listOf("licenseNumber", "reggieLicensePlate")
}
