package app.tostrr.domain.history

import app.tostrr.domain.order.Change
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangeCategoryTest {
    private fun category(key: String, new: String? = "x") = Change(1L, key, null, new).category

    @Test fun `vin keys are the vin, an insurance vin is insurance`() {
        assertEquals(ChangeCategory.VIN, category("order.vin"))
        assertEquals(ChangeCategory.VIN, category("details.tasks.registration.orderDetails.vin"))
        assertEquals(ChangeCategory.INSURANCE, category("details.tasks.insurance.isSameVIN"))
    }

    @Test fun `the local signals are the build and the delivery`() {
        assertEquals(ChangeCategory.BUILT, category("local.partsCatalogSince"))
        assertEquals(ChangeCategory.BUILT, category("local.partsCatalogReference"))
        assertEquals(ChangeCategory.BUILT, category("local", """{"partsCatalogSince":"2026-11-11"}"""))
        assertEquals(ChangeCategory.DELIVERY, category("local.inAccountSince"))
        assertEquals(ChangeCategory.DELIVERY, category("local", """{"inAccountSince":"2026-12-20"}"""))
    }

    @Test fun `window and appointment win over the task they sit in`() {
        assertEquals(ChangeCategory.DELIVERY_WINDOW, category("details.tasks.scheduling.deliveryWindowDisplay"))
        assertEquals(ChangeCategory.DELIVERY_WINDOW, category("details.tasks.finalPayment.data.selfSchedulingDetails.Display.startDate"))
        assertEquals(ChangeCategory.APPOINTMENT, category("details.tasks.scheduling.deliveryAppointmentDate"))
        assertEquals(ChangeCategory.APPOINTMENT, category("details.tasks.deliveryAcceptance.appointmentDate"))
    }

    @Test fun `other changes go by their task`() {
        assertEquals(ChangeCategory.DELIVERY, category("details.tasks.deliveryAcceptance.queueNumber"))
        assertEquals(ChangeCategory.REGISTRATION, category("details.tasks.registration.agreementsStatus"))
        assertEquals(ChangeCategory.AGREEMENTS, category("details.tasks.agreements.eSignStatus"))
        assertEquals(ChangeCategory.FINANCING, category("details.tasks.financing.status"))
        assertEquals(ChangeCategory.PAYMENT, category("details.tasks.finalPayment.amountDue"))
        assertEquals(ChangeCategory.TRADE_IN, category("details.tasks.tradeIn.partnerRedirectConfig"))
        assertEquals(ChangeCategory.ORDER, category("order.orderStatus"))
        assertEquals(ChangeCategory.OTHER, category("details.tasks.carbonCredit.status"))
        assertEquals(ChangeCategory.OTHER, category("details.homeScreenData"))
    }

    @Test fun `insurance and unknown changes are the minor ones`() {
        assertFalse(ChangeCategory.INSURANCE.important)
        assertFalse(ChangeCategory.OTHER.important)
        assertTrue(ChangeCategory.entries.filterNot { it == ChangeCategory.INSURANCE || it == ChangeCategory.OTHER }.all { it.important })
    }
}
