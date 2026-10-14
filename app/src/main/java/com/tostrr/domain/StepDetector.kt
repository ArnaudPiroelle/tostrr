package com.tostrr.domain

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlin.math.abs

/**
 * Decides whether one order step is done from Tesla's payloads.
 *
 * Tesla doesn't document how "documents sent" or "vehicle registered" show up, so each step goes through
 * this interface: [PayloadStepDetectors] reads fields from the JSON, and another implementation can replace
 * it without touching [OrderMapper].
 */
fun interface StepDetector {
    /** [order] is the entry of `/api/1/users/orders`, [tasks] is `details.tasks` of the `/tasks` response. */
    fun isDone(order: JsonObject, tasks: JsonObject): Boolean
}

class StepDetectors(
    val carBuilt: StepDetector,
    val documentsSent: StepDetector,
    val vehicleRegistered: StepDetector,
) {
    companion object {
        /** The rules in use (see [PayloadStepDetectors]). */
        val Default = StepDetectors(
            carBuilt = PayloadStepDetectors.CarBuilt,
            documentsSent = PayloadStepDetectors.DocumentsSent,
            vehicleRegistered = PayloadStepDetectors.VehicleRegistered,
        )
    }
}

/**
 * Rules for the registration steps, guessed from the fields Tesla's payloads carry. Not documented by Tesla: the
 * paths and values below are hypotheses, not taken from a payload where they changed.
 */
object PayloadStepDetectors {

    /** Registration paperwork has been sent: an alert reports that the proof of registration is pending. */
    private val ProofOfRegistrationPending = StepDetector { _, tasks ->
        ALERT_PATHS.any { tasks.stringAt(it)?.normalised() == PROOF_OF_REG_PENDING }
    }

    /** A plate exists, or the registration status says the pre-delivery validation is complete. */
    val VehicleRegistered = StepDetector { _, tasks ->
        PLATE_PATHS.any { !tasks.stringAt(it).isNullOrBlank() } ||
            STATUS_PATHS.any { tasks.stringAt(it)?.normalised() == PRE_DELIVERY_VALIDATION_COMPLETE }
    }

    /** A registered car necessarily had its papers sent, even once the "pending" alert has gone. */
    val DocumentsSent = StepDetector { order, tasks ->
        ProofOfRegistrationPending.isDone(order, tasks) || VehicleRegistered.isDone(order, tasks)
    }

    /**
     * The factory gate says whether the car left the factory: not blocking means built. When the payload has
     * no gate yet, the odometer decides: Tesla seeds it with 30 until the car is built.
     */
    val CarBuilt = StepDetector { _, tasks ->
        when (val blocker = factoryGateBlocker(tasks)) {
            null -> odometerIsReal(tasks)
            else -> !blocker
        }
    }

    private fun odometerIsReal(tasks: JsonObject): Boolean {
        val km = tasks.deepFind("vehicleOdometer")?.toDoubleOrNull() ?: return false
        return km > 0 && abs(km - PLACEHOLDER_ODOMETER) > 0.01
    }

    private fun factoryGateBlocker(tasks: JsonObject): Boolean? {
        for (root in READINESS_ROOTS) {
            (tasks.at("$root.formattedGates.factory_gate.isBlocker") as? JsonPrimitive)?.booleanOrNull?.let { return it }
        }
        return GATE_LISTS.firstNotNullOfOrNull { gateBlocker(tasks.at(it)) }
    }

    /** `gates` is either a list of `{gateName, isBlocker}` or an object keyed by gate name. */
    private fun gateBlocker(gates: JsonElement?): Boolean? = when (gates) {
        is JsonArray -> gates.filterIsInstance<JsonObject>()
            .firstOrNull { it.at("gateName")?.let { n -> (n as? JsonPrimitive)?.contentOrNull.equals(FACTORY_GATE, ignoreCase = true) } == true }
            ?.let { (it.at("isBlocker") as? JsonPrimitive)?.booleanOrNull }
        is JsonObject -> ((gates.at(FACTORY_GATE) as? JsonObject)?.at("isBlocker") as? JsonPrimitive)?.booleanOrNull
        else -> null
    }

    private const val FACTORY_GATE = "FACTORY_GATE"
    private const val PLACEHOLDER_ODOMETER = 30.0
    private val READINESS_ROOTS = listOf("finalPayment.data.deliveryReadiness", "finalPayment.data.deliveryReadinessDetail")
    private val GATE_LISTS = READINESS_ROOTS.map { "$it.gates" } + "deliveryAcceptance.gates"

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
}
