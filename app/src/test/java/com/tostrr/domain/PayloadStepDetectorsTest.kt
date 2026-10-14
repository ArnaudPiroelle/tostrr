package com.tostrr.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PayloadStepDetectorsTest {
    private fun tasks(json: String) = Json.parseToJsonElement(json).jsonObject
    private val noOrder = tasks("{}")

    @Test fun `empty tasks mean nothing is done`() {
        val t = tasks("{}")
        assertFalse(PayloadStepDetectors.DocumentsSent.isDone(noOrder, t))
        assertFalse(PayloadStepDetectors.VehicleRegistered.isDone(noOrder, t))
    }

    @Test fun `current real shape is not done`() {
        val t = tasks("""{"registration":{"regData":{"reggieLicensePlate":"","regDetails":{}},"status":null,"agreementsStatus":null}}""")
        assertFalse(PayloadStepDetectors.DocumentsSent.isDone(noOrder, t))
        assertFalse(PayloadStepDetectors.VehicleRegistered.isDone(noOrder, t))
    }

    @Test fun `pending proof alert means documents sent but not registered`() {
        val t = tasks("""{"registration":{"alertStatuses":{"registration":"proof_of_reg_pending"}}}""")
        assertTrue(PayloadStepDetectors.DocumentsSent.isDone(noOrder, t))
        assertFalse(PayloadStepDetectors.VehicleRegistered.isDone(noOrder, t))
    }

    @Test fun `other alert value is ignored`() {
        val t = tasks("""{"registration":{"regData":{"regAlerts":{"regDelivery":"SOMETHING_ELSE"}}}}""")
        assertFalse(PayloadStepDetectors.DocumentsSent.isDone(noOrder, t))
    }

    @Test fun `plate means registered and implies documents sent`() {
        val t = tasks("""{"deliveryDetails":{"regData":{"reggieLicensePlate":"AB-123-CD"}}}""")
        assertTrue(PayloadStepDetectors.VehicleRegistered.isDone(noOrder, t))
        assertTrue(PayloadStepDetectors.DocumentsSent.isDone(noOrder, t))
    }

    @Test fun `pre-delivery validation complete means registered`() {
        val t = tasks("""{"registration":{"regData":{"reggieRegistrationStatus":" Pre-Delivery Validation Complete "}}}""")
        assertTrue(PayloadStepDetectors.VehicleRegistered.isDone(noOrder, t))
    }

    @Test fun `plate in the final payment data counts`() {
        val t = tasks("""{"finalPayment":{"data":{"vehicleRegistration":{"licenseNumber":"AB-123-CD"}}}}""")
        assertTrue(PayloadStepDetectors.VehicleRegistered.isDone(noOrder, t))
    }
}
