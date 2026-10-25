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

    @Test fun `mapper uses the injected detectors`() {
        val always = StepDetector { _, _ -> true }
        val order = OrderMapper.map(tasks("""{"referenceNumber":"RN1"}"""), tasks("{}"), 0L, StepDetectors(always, always, always))
        assertTrue(order.steps.first { it.label == "Documents envoyés" }.done)
        assertTrue(order.steps.first { it.label == "Véhicule immatriculé" }.done)
    }

    @Test fun `no gate and placeholder odometer means not built`() {
        assertFalse(PayloadStepDetectors.CarBuilt.isDone(noOrder, tasks("""{"registration":{"orderDetails":{"vehicleOdometer":30}}}""")))
        assertFalse(PayloadStepDetectors.CarBuilt.isDone(noOrder, tasks("{}")))
    }

    @Test fun `no gate and a real odometer means built`() {
        assertTrue(PayloadStepDetectors.CarBuilt.isDone(noOrder, tasks("""{"registration":{"orderDetails":{"vehicleOdometer":"12.4"}}}""")))
    }

    @Test fun `factory gate not blocking means built even without odometer`() {
        val t = tasks("""{"finalPayment":{"data":{"deliveryReadiness":{"formattedGates":{"FACTORY_GATE":{"isBlocker":false}}}}}}""")
        assertTrue(PayloadStepDetectors.CarBuilt.isDone(noOrder, t))
    }

    @Test fun `factory gate blocking beats the odometer`() {
        val t = tasks("""{"deliveryAcceptance":{"gates":[{"gateName":"FACTORY_GATE","isBlocker":true}]},"registration":{"orderDetails":{"vehicleOdometer":50}}}""")
        assertFalse(PayloadStepDetectors.CarBuilt.isDone(noOrder, t))
    }

    @Test fun `gates keyed by name are read`() {
        val t = tasks("""{"finalPayment":{"data":{"deliveryReadinessDetail":{"gates":{"factory_gate":{"isBlocker":false}}}}}}""")
        assertTrue(PayloadStepDetectors.CarBuilt.isDone(noOrder, t))
    }
}
