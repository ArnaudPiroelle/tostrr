package app.tostrr.domain.order

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderInfoTest {
    private fun map(tasks: String) =
        OrderMapper.map(Json.parseToJsonElement("""{"referenceNumber":"RN1"}""").jsonObject, Json.parseToJsonElement("""{"tasks":$tasks}""").jsonObject, 0L)

    /** Strips the non-breaking spaces French number formatting uses, so expectations stay readable. */
    private fun String?.spaces() = this?.replace(' ', ' ')?.replace(' ', ' ')

    @Test fun `nothing sent means empty sections`() {
        val o = map("{}")
        assertTrue(o.vehicleInfo.isEmpty)
        assertTrue(o.paymentInfo.isEmpty)
        assertNull(o.deliveryMethod)
    }

    @Test fun `real pre-match payload gives the final payment and the pick-up`() {
        val o = map(
            """{"finalPayment":{"status":"MAKE_YOUR_FINAL_PAYMENT","amountDue":5000,"currencyFormat":{"currencyCode":"EUR"}},
               "agreements":{"teslaFinanceDetails":{"partnerName":"COFICA BAIL"}},
               "deliveryAcceptance":{"deliveryType":"PICKUP_SERVICE_CENTER","isDeliveryToAddress":false}}""",
        )
        assertEquals("Paiement final à effectuer", o.paymentInfo.status)
        assertEquals("5 000 €", o.paymentInfo.amountDue.spaces())
        assertEquals("COFICA BAIL", o.paymentInfo.financePartner)
        assertEquals("Retrait au centre de livraison", o.deliveryMethod)
    }

    @Test fun `vehicle fields are read from the registration order details`() {
        val o = map(
            """{"registration":{"orderDetails":{"reservationDate":"2026-06-01T10:00:00Z","orderBookedDate":"2026-06-02T10:00:00Z",
               "vehicleOdometer":12.0,"vehicleOdometerType":"KM","vehicleRoutingLocation":"Paris","registrationStatus":"IN_PROGRESS"},
               "regData":{"reggieLicensePlate":"AB-123-CD"}}}""",
        )
        with(o.vehicleInfo) {
            assertEquals("01/06/26", reservationDate)
            assertEquals("02/06/26", bookedDate)
            assertEquals("AB-123-CD", licensePlate)
            assertEquals("12 km", odometer)
            assertEquals("Paris", routingLocation)
            assertEquals("In progress", registrationStatus)
        }
    }

    @Test fun `financing details and payments are formatted`() {
        val o = map(
            """{"finalPayment":{"data":{"amountDue":"12 345,00 €","amountDueFinancier":40000.5,
               "paymentDetails":[{"amountPaid":250,"paymentType":"CARD"},{"amountPaid":1750,"paymentType":"WIRE"}],
               "financingDetails":{"financialProductType":"LEASE","teslaFinanceDetails":{"financePartnerName":"Tesla Financial",
               "monthlyPayment":499,"termsInMonths":48,"interestRate":3.99,"mileage":15000,"approvedLoanAmount":40000}}}}}""",
        )
        with(o.paymentInfo) {
            assertEquals("12 345,00 €", amountDue)
            assertEquals("40 000,50 €", financedAmount.spaces())
            assertEquals("2 000 €", amountPaid.spaces())
            assertEquals("Card, Wire", paymentMethod)
            assertEquals("Location (LOA/LLD)", financeProduct)
            assertEquals("Tesla Financial", financePartner)
            assertEquals("499 €", monthlyPayment.spaces())
            assertEquals("48 mois", term)
            assertEquals("3,99 %", interestRate)
            assertEquals("15 000 km/an", yearlyMileage.spaces())
            assertEquals("40 000 €", approvedAmount.spaces())
        }
    }

    @Test fun `home delivery is recognised`() {
        assertEquals("Livraison à domicile", map("""{"deliveryAcceptance":{"deliveryType":"DELIVERY_TO_ADDRESS"}}""").deliveryMethod)
        assertEquals("Livraison à domicile", map("""{"deliveryAcceptance":{"isDeliveryToAddress":true}}""").deliveryMethod)
    }

    @Test fun `delivery window falls back to the self-scheduling dates`() {
        val o = map("""{"finalPayment":{"data":{"selfSchedulingDetails":{"deliveryWindow":{"startDate":"2026-11-29","endDate":"2026-12-29"}}}}}""")
        assertEquals("29/11/26 – 29/12/26", o.deliveryWindow)
    }

    @Test fun `scheduling eta wins over the final payment one`() {
        val o = map("""{"finalPayment":{"data":{"etaToDeliveryCenter":"2026-11-02"}},"scheduling":{"etaToDeliveryCenter":"2026-11-01"}}""")
        assertEquals("01/11/26", o.etaToDeliveryCenter)
    }
}
