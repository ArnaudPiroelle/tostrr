package app.tostrr.domain.order

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertTrue(o.tradeInInfo.isEmpty)
        assertTrue(o.registrationInfo.isEmpty)
        assertNull(o.deliveryMethod)
    }

    @Test fun `real pre-match payload gives the final payment and the pick-up`() {
        val o = map(
            """{"finalPayment":{"status":"MAKE_YOUR_FINAL_PAYMENT","amountDue":5000,"currencyFormat":{"currencyCode":"EUR"}},
               "agreements":{"teslaFinanceDetails":{"partnerName":"COFICA BAIL"}},
               "deliveryAcceptance":{"deliveryType":"PICKUP_SERVICE_CENTER","isDeliveryToAddress":false}}""",
        )
        assertEquals("MAKE_YOUR_FINAL_PAYMENT", o.paymentInfo.status)
        assertEquals("5 000 €", o.paymentInfo.amountDue.spaces())
        assertEquals("COFICA BAIL", o.paymentInfo.financePartner)
        assertEquals(DeliveryMethod.PICKUP, o.deliveryMethod)
    }

    @Test fun `vehicle fields are read from the registration order details`() {
        val o = map(
            """{"registration":{"orderDetails":{"reservationDate":"2026-06-01T10:00:00Z","orderBookedDate":"2026-06-02T10:00:00Z",
               "vehicleOdometer":12.0,"vehicleOdometerType":"KM","vehicleRoutingLocation":"Paris"}}}""",
        )
        with(o.vehicleInfo) {
            assertEquals("01/06/26", reservationDate)
            assertEquals("02/06/26", bookedDate)
            assertEquals("12 km", odometer)
            assertEquals("Paris", routingLocation)
        }
    }

    @Test fun `a company registration, as in a real payload, is in the company's name once the details are sent`() {
        val o = map(
            """{"registration":{"complete":true,"enabled":true,"currentStep":"REG_TYPE","isPendingCoSigner":false,
               "regData":{"registrantType":"COMPANY","owner":{"user":{"firstName":"Jeanne","lastName":"Martin"}},
                 "coOwner":{"user":{"firstName":null,"lastName":null}}}},
               "deliveryDetails":{"companyName":"ACME","verificationStatus":null}}""",
        )
        with(o.registrationInfo) {
            assertEquals(RegistrationStage.DETAILS_SENT, stage)
            assertEquals("ACME", holder)
            assertNull(coHolder)
            assertTrue(todo.isEmpty())
        }
    }

    @Test fun `a personal registration is in the owner's name, and says what is missing`() {
        val o = map(
            """{"registration":{"complete":false,"hasDLDetails":false,"isPendingCoSigner":true,
               "regData":{"registrantType":"INDIVIDUAL","owner":{"user":{"firstName":"Jeanne","lastName":"Martin"}},
                 "coOwner":{"user":{"firstName":"Paul","lastName":"Martin"}}}},
               "deliveryDetails":{"companyName":"ACME","fourthline":{"verificationStatus":"PENDING"}}}""",
        )
        with(o.registrationInfo) {
            assertEquals(RegistrationStage.DETAILS_NEEDED, stage)
            assertEquals("Jeanne Martin", holder)
            assertEquals("Paul Martin", coHolder)
            assertEquals(listOf(RegistrationTodo.DRIVING_LICENCE, RegistrationTodo.CO_OWNER, RegistrationTodo.IDENTITY), todo)
        }
    }

    @Test fun `the registration alert, then the plate, set the stage`() {
        fun stage(alert: String) =
            map("""{"registration":{"complete":true,"regData":{"regAlerts":{"registration":"$alert"}}}}""").registrationInfo.stage
        assertEquals(RegistrationStage.PREPARING_DOCUMENTS, stage("PREPAREDOCS"))
        assertEquals(RegistrationStage.AWAITING_PROOF, stage("AWAITING_CONFIRMATION"))
        assertEquals(RegistrationStage.DOCUMENTS_SENT, stage("APPT_PENDING"))
        assertEquals(RegistrationStage.AWAITING_PLATE, stage("PROOF_OF_REG_PENDING"))
        assertEquals(RegistrationStage.REGISTERED, stage("COMPLETE"))
        assertEquals(RegistrationStage.DETAILS_SENT, stage("SOMETHING_NEW"))

        val plated = map("""{"registration":{"regData":{"reggieLicensePlate":"AB-123-CD","regAlerts":{"registration":"PREPAREDOCS"}}}}""")
        assertEquals(RegistrationStage.REGISTERED, plated.registrationInfo.stage)
    }

    @Test fun `identity checks done or not asked for leave nothing to do`() {
        val o = map("""{"registration":{},"deliveryDetails":{"verificationStatus":"DEFAULT","fourthline":{"validationStatus":"VERIFIED"}}}""")
        assertTrue(o.registrationInfo.todo.isEmpty())
    }

    @Test fun `no registration task, or a disabled one, gives nothing`() {
        assertTrue(map("{}").registrationInfo.isEmpty)
        assertTrue(map("""{"registration":{"enabled":false,"complete":true}}""").registrationInfo.isEmpty)
    }

    @Test fun `the expected registration day is also read under its long name`() {
        assertEquals("01/12/26", map("""{"registration":{"regData":{"regDetails":{"expectedRegistrationDate":"2026-12-01"}}}}""").expectedRegistration)
        // As in a real payload: a label of Tesla's display strings, not a date.
        assertNull(map("""{"registration":{"strings":{"expectedRegistrationDate":"Date d'immatriculation prévue"}}}""").expectedRegistration)
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
            assertEquals("LEASE", financeProduct)
            assertEquals("Tesla Financial", financePartner)
            assertEquals("499 €", monthlyPayment.spaces())
            assertEquals(48, termMonths)
            assertEquals("3,99 %", interestRate)
            assertEquals("15 000", yearlyMileage.spaces())
            assertEquals("40 000 €", approvedAmount.spaces())
        }
    }

    @Test fun `a declined trade-in, as in a real payload, is no trade-in`() {
        val o = map(
            """{"tradeIn":{"enabled":true,"complete":true,"status":"INTENT_NO_COMPLETE","tradeInIntent":"No","nextStep":"",
               "tradeInVehicle":null,"currencyCode":"EUR","canAcceptOffer":false,
               "transfers":{"FSD_TRANSFER":{"enabled":false,"isEligible":false}},
               "card":{"title":"Terminé","subtitle":"Pas de reprise"}}}""",
        )
        with(o.tradeInInfo) {
            assertNull(action)
            assertTrue(transfers.isEmpty())
            assertFalse(hasTradeIn)
        }
    }

    @Test fun `a trade-in reads the car, the offer and the transfers`() {
        val o = map(
            """{"tradeIn":{"enabled":true,"status":"OFFER_READY","tradeInIntent":"Yes","nextStep":"viewoffer","canAcceptOffer":true,
               "currencyCode":"EUR",
               "tradeInVehicle":{"Year":2019,"Make":"Tesla","Model":"Model 3","Mileage":84321.6,"OdometerType":"KILOMETERS",
                 "SelectedValuation":{"Value":21500,"ValuationExpireDate":"2026-12-15T00:00:00Z"}},
               "transfers":{"FSD_TRANSFER":{"enabled":false,"isEligible":true},"EAP_TRANSFER":{"enabled":false,"isEligible":false},
                 "SUPER_CHARGE_TRANSFER":{"enabled":true}}}}""",
        )
        with(o.tradeInInfo) {
            assertTrue(hasTradeIn)
            assertFalse(hasNoOffer)
            assertEquals("2019 Tesla Model 3", vehicle)
            assertEquals("84 322 km", mileage.spaces())
            assertEquals("21 500 €", estimatedValue.spaces())
            assertNull(finalOffer)
            assertEquals("15/12/26", offerExpires)
            // An offer that can be accepted beats the next step Tesla names.
            assertEquals(TradeInAction.ACCEPT_OFFER, action)
            assertEquals(listOf(TradeInTransfer.FSD, TradeInTransfer.FREE_SUPERCHARGING), transfers)
        }
    }

    @Test fun `the next step says what to do, when the app knows it`() {
        assertEquals(TradeInAction.VIEW_OFFER, map("""{"tradeIn":{"tradeInIntent":"Yes","nextStep":"viewoffer"}}""").tradeInInfo.action)
        assertNull(map("""{"tradeIn":{"tradeInIntent":"Yes","nextStep":"uploadphotos"}}""").tradeInInfo.action)
    }

    @Test fun `a final offer replaces the estimate`() {
        val o = map(
            """{"tradeIn":{"tradeInIntent":"Yes","currentVehicle":{"finalOffer":19000,"TradeInCredit":20000,"Mileage":50000,"OdometerType":"MILES"}}}""",
        )
        assertEquals("19 000 €", o.tradeInInfo.finalOffer.spaces())
        assertNull(o.tradeInInfo.estimatedValue)
        assertEquals("50 000 mi", o.tradeInInfo.mileage.spaces())
    }

    @Test fun `the final payment stands in for the trade-in task, unless the task is disabled`() {
        val fallback = map("""{"finalPayment":{"data":{"tradeInIntent":"Yes"}}}""").tradeInInfo
        assertTrue(fallback.hasTradeIn)
        assertTrue(fallback.hasNoOffer)

        val disabled = map("""{"tradeIn":{"enabled":false,"tradeInIntent":"Yes","status":"OFFER_READY"}}""").tradeInInfo
        assertTrue(disabled.isEmpty)
        assertFalse(disabled.hasTradeIn)
    }

    @Test fun `home delivery is recognised`() {
        assertEquals(DeliveryMethod.HOME, map("""{"deliveryAcceptance":{"deliveryType":"DELIVERY_TO_ADDRESS"}}""").deliveryMethod)
        assertEquals(DeliveryMethod.HOME, map("""{"deliveryAcceptance":{"isDeliveryToAddress":true}}""").deliveryMethod)
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
