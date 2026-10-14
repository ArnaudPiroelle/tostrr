package com.tostrr.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonStepDetectorsTest {
    private fun obj(json: String) = Json.parseToJsonElement(json).jsonObject
    private val noOrder = obj("{}")

    @Test fun `no plate means not registered`() {
        val tasks = obj("""{"registration":{"orderDetails":null,"regData":{"reggieLicensePlate":""}}}""")
        assertFalse(JsonStepDetectors.VehicleRegistered.isDone(noOrder, tasks))
    }

    @Test fun `plate in regData means registered`() {
        val tasks = obj("""{"registration":{"regData":{"reggieLicensePlate":"AB-123-CD"}}}""")
        assertTrue(JsonStepDetectors.VehicleRegistered.isDone(noOrder, tasks))
    }

    @Test fun `plate in orderDetails means registered`() {
        val tasks = obj("""{"registration":{"orderDetails":{"licenseNumber":"AB-123-CD"}}}""")
        assertTrue(JsonStepDetectors.VehicleRegistered.isDone(noOrder, tasks))
    }

    @Test fun `null agreementsStatus means documents not sent`() {
        val tasks = obj("""{"registration":{"agreementsStatus":null}}""")
        assertFalse(JsonStepDetectors.DocumentsSent.isDone(noOrder, tasks))
    }

    @Test fun `agreementsStatus with a value means documents sent`() {
        val tasks = obj("""{"registration":{"agreementsStatus":"AVAILABLE"}}""")
        assertTrue(JsonStepDetectors.DocumentsSent.isDone(noOrder, tasks))
    }

    @Test fun `mapper uses the injected detectors`() {
        val always = StepDetector { _, _ -> true }
        val order = OrderMapper.map(obj("""{"referenceNumber":"RN1"}"""), obj("{}"), 0L, StepDetectors(always, always))
        assertTrue(order.steps.first { it.label == "Documents envoyés" }.done)
        assertTrue(order.steps.first { it.label == "Véhicule immatriculé" }.done)
    }
}
