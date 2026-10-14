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
        /** The simple field-based detectors. */
        val Json = StepDetectors(
            documentsSent = JsonStepDetectors.DocumentsSent,
            vehicleRegistered = JsonStepDetectors.VehicleRegistered,
        )

        /** The rules in use (see [PayloadStepDetectors]). */
        val Payload = StepDetectors(
            documentsSent = PayloadStepDetectors.DocumentsSent,
            vehicleRegistered = PayloadStepDetectors.VehicleRegistered,
        )

        val Default = Payload
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

/**
 * Rules for the registration steps, guessed from the fields Tesla's payloads carry. Not documented by Tesla: the
 * paths and values below are hypotheses, not taken from a payload where they changed.
 */
object PayloadStepDetectors {

    /** Registration paperwork has been sent: an alert reports that the proof of registration is pending. */
    private val ProofOfRegistrationPending = StepDetector { _, tasks ->
        ALERT_PATHS.any { tasks.string(it)?.normalised() == PROOF_OF_REG_PENDING }
    }

    /** A plate exists, or the registration status says the pre-delivery validation is complete. */
    val VehicleRegistered = StepDetector { _, tasks ->
        PLATE_PATHS.any { !tasks.string(it).isNullOrBlank() } ||
            STATUS_PATHS.any { tasks.string(it)?.normalised() == PRE_DELIVERY_VALIDATION_COMPLETE }
    }

    /** A registered car necessarily had its papers sent, even once the "pending" alert has gone. */
    val DocumentsSent = StepDetector { order, tasks ->
        ProofOfRegistrationPending.isDone(order, tasks) || VehicleRegistered.isDone(order, tasks)
    }

    private const val PROOF_OF_REG_PENDING = "PROOF_OF_REG_PENDING"
    private const val PRE_DELIVERY_VALIDATION_COMPLETE = "PRE-DELIVERY VALIDATION COMPLETE"

    private fun String.normalised() = trim().uppercase()

    /** Paths below `details.tasks`. */
    private val PLATE_PATHS = listOf(
        "registration.regData.reggieLicensePlate",
        "deliveryDetails.regData.reggieLicensePlate",
        "registration.orderDetails.licenseNumber",
        "finalPayment.data.vehicleRegistration.licenseNumber",
    )
    private val ALERT_PATHS = listOf(
        "registration.alertStatuses.registration",
        "registration.alertStatuses.regDelivery",
        "registration.regData.regAlerts.registration",
        "registration.regData.regAlerts.regDelivery",
        "deliveryDetails.regData.regAlerts.registration",
        "deliveryDetails.regData.regAlerts.regDelivery",
    )
    private val STATUS_PATHS = listOf(
        "registration.regData.reggieRegistrationStatus",
        "deliveryDetails.regData.reggieRegistrationStatus",
    )

    /** String at a dotted [path], or null if any step is missing or the value isn't a string. */
    private fun JsonObject.string(path: String): String? {
        val parts = path.split('.')
        var node: JsonObject = this
        for (part in parts.dropLast(1)) node = node.obj(part)
        return node.str(parts.last())
    }
}
