package app.tostrr.ui.detail

import android.content.ClipboardManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import app.tostrr.domain.deliverycenter.DeliveryCenter
import app.tostrr.domain.order.Change
import app.tostrr.domain.order.LocalSignals
import app.tostrr.domain.order.RegistrationInfo
import app.tostrr.domain.order.RegistrationStage
import app.tostrr.domain.order.RegistrationTodo
import app.tostrr.domain.order.TradeInAction
import app.tostrr.domain.order.TradeInInfo
import app.tostrr.domain.order.TradeInTransfer
import app.tostrr.domain.shipping.GeoPosition
import app.tostrr.domain.shipping.Ship
import app.tostrr.domain.shipping.ShipSnapshot
import app.tostrr.domain.vin.VinCode
import app.tostrr.domain.vin.VinSection
import app.tostrr.testing.TestGraph
import app.tostrr.testing.order
import app.tostrr.ui.theme.TostrrTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DetailScreenTest {
    @get:Rule val compose = createComposeRule()

    private val graph = TestGraph().apply {
        orders.orders.value = listOf(order("RN1", deliveryCenter = "Tesla Paris-Ouest"))
        deliveryCenters += DeliveryCenter("Tesla Paris-Ouest", "FR", "Paris", "1 rue de Paris")
    }

    private var openedShip: String? = null

    private fun show(reference: String = "RN1") = compose.setContent {
        TostrrTheme { DetailScreen(reference, onOpenChecklist = {}, onOpenShip = { openedShip = it }, onBack = {}, viewModel = graph.detailViewModel(reference)) }
    }

    @Test fun `shows the order and its delivery centre`() {
        show()

        // In the header, and in the top bar that fades in on scroll.
        compose.onAllNodesWithText("Commande #RN1").assertCountEquals(2)
        compose.onNodeWithText("Tesla Paris-Ouest").performScrollTo().assertIsDisplayed()
    }

    @Test fun `a collapsed section hides its content and is remembered`() {
        show()

        compose.onNodeWithText("Informations de livraison").performScrollTo().performClick()

        compose.onNodeWithText("Tesla Paris-Ouest").assertDoesNotExist()
        assertEquals(listOf("delivery"), graph.layout.layout.value.collapsed)
    }

    @Test fun `the top bar opens the arrangement sheet`() {
        show()

        compose.onNodeWithContentDescription("Organiser la page").performClick()

        compose.onNodeWithText("Réinitialiser").assertIsDisplayed()
    }

    @Test fun `a ship that may carry the car can be opened`() {
        graph.orders.orders.value = listOf(order("RN1").copy(vin = "LRW3E7EU8VC052692"))
        graph.ships.snapshot.value = ShipSnapshot(
            1L,
            listOf(Ship("GLOVIS LIGHTHOUSE", listOf("NANGANG", "ZEEBRUGGE"), "ZEEBRUGGE", null, GeoPosition(-30.0, 37.0), 240.0, listOf("052"), done = false)),
        )
        show()

        compose.onNodeWithText("Suivre ce bateau").performScrollTo().performClick()

        assertEquals("GLOVIS LIGHTHOUSE", openedShip)
    }

    @Test fun `the VIN can be copied`() {
        graph.orders.orders.value = listOf(order("RN1").copy(vin = "LRW3E7EK0PC000001"))
        show()

        compose.onNodeWithContentDescription("Copier le VIN").performClick()
        compose.waitForIdle()

        val clipboard = RuntimeEnvironment.getApplication().getSystemService(ClipboardManager::class.java)
        assertEquals("LRW3E7EK0PC000001", clipboard.primaryClip?.getItemAt(0)?.text?.toString())
    }

    @Test fun `a car the parts catalogue lists links to its parts`() {
        graph.orders.orders.value = listOf(order("RN1").copy(vin = "LRW3E7EK0PC000001", local = LocalSignals(partsCatalogReference = "ref-1")))
        show()

        compose.onNodeWithText("Voir les pièces de la voiture").performScrollTo().assertIsDisplayed()
    }

    @Test fun `no parts link before the catalogue lists the car`() {
        graph.orders.orders.value = listOf(order("RN1").copy(vin = "LRW3E7EK0PC000001"))
        show()

        compose.onNodeWithText("Voir les pièces de la voiture").assertDoesNotExist()
    }

    @Test fun `the history hides minor changes until asked, and filters by category`() {
        graph.orders.history.value = mapOf(
            "RN1" to listOf(
                Change(2L, "details.tasks.agreements.eSignStatus", "COMPLETE", "VOIDED"),
                Change(1L, "details.tasks.insurance.hasVin", "true", "false"),
            ),
        )
        show()

        // The chips sit in a row that scrolls sideways, below the fold of the test window: clicked through their action.
        compose.onNodeWithText("Contrats · 1").assertExists()
        compose.onNodeWithText("Assurance · 1").assertDoesNotExist()
        compose.onNodeWithText("VOIDED", substring = true).assertExists()
        compose.onNodeWithText("details.tasks.insurance.hasVin", substring = true).assertDoesNotExist()

        compose.onNodeWithText("Importants seulement").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithText("Assurance · 1").performSemanticsAction(SemanticsActions.OnClick)

        compose.onNodeWithText("VOIDED", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Assurance", useUnmergedTree = true).assertExists()
    }

    @Test fun `the VIN decoder explains each part of the VIN`() {
        graph.orders.orders.value = listOf(order("RN1").copy(vin = "LRW3E7EU8VC052692"))
        graph.vinCodes.know(
            VinCode(VinSection.MANUFACTURER, "LRW", mapOf("en" to "Tesla, China", "fr" to "Tesla, Chine")),
            VinCode(VinSection.FACTORY, "C", mapOf("en" to "Shanghai (China)", "fr" to "Shanghai (Chine)")),
        )
        show()

        compose.onNodeWithText("Décoder le VIN").performScrollTo().performClick()

        compose.onNodeWithText("Décodeur VIN").assertIsDisplayed()
        compose.onNodeWithText("Tesla, Chine").assertIsDisplayed()
        compose.onNodeWithText("Shanghai (Chine)").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("Pas encore connu").assertCountEquals(6)
        compose.onNodeWithText("Valide").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("052692").assertCountEquals(2)
    }

    @Test fun `an unknown VIN has nothing to copy, nor to decode`() {
        show()

        compose.onNodeWithText("Décoder le VIN").assertDoesNotExist()

        compose.onNodeWithText("VIN: inconnu").assertIsDisplayed()
        compose.onNodeWithContentDescription("Copier le VIN").assertDoesNotExist()
    }

    @Test fun `a trade-in gets its section, worded`() {
        graph.orders.orders.value = listOf(
            order("RN1").copy(
                tradeInInfo = TradeInInfo(
                    intent = "Yes",
                    vehicle = "2019 Tesla Model 3",
                    mileage = "84 322 km",
                    estimatedValue = "21 500 €",
                    action = TradeInAction.ACCEPT_OFFER,
                    transfers = listOf(TradeInTransfer.FSD, TradeInTransfer.FREE_SUPERCHARGING),
                ),
            ),
        )
        show()

        compose.onNodeWithText("Reprise").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("2019 Tesla Model 3").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("84 322 km").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("21 500 €").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Accepter l'offre dans l'app Tesla").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Conduite entièrement autonome, Supercharge gratuite").performScrollTo().assertIsDisplayed()
    }

    @Test fun `a trade-in Tesla hasn't valued yet says so`() {
        graph.orders.orders.value = listOf(order("RN1").copy(tradeInInfo = TradeInInfo(intent = "Yes")))
        show()

        compose.onNodeWithText("Tesla n'a pas encore estimé votre véhicule.").performScrollTo().assertIsDisplayed()
    }

    @Test fun `no trade-in section when the buyer has none`() {
        graph.orders.orders.value = listOf(order("RN1").copy(tradeInInfo = TradeInInfo(intent = "No")))
        show()

        compose.onNodeWithText("Reprise").assertDoesNotExist()
    }

    @Test fun `the registration gets its section, worded`() {
        graph.orders.orders.value = listOf(
            order("RN1").copy(
                licensePlate = "AB-123-CD",
                expectedRegistration = "01/12/26",
                registrationInfo = RegistrationInfo(
                    stage = RegistrationStage.AWAITING_PLATE,
                    holder = "Jeanne Martin",
                    todo = listOf(RegistrationTodo.DRIVING_LICENCE, RegistrationTodo.IDENTITY),
                ),
            ),
        )
        show()

        compose.onNodeWithText("Immatriculation").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("En attente de la plaque").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Jeanne Martin").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("01/12/26").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("AB-123-CD").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Ajouter votre permis de conduire dans l'app Tesla").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Vérifier votre identité dans l'app Tesla").performScrollTo().assertIsDisplayed()
    }

    @Test fun `no registration section before Tesla has anything about it`() {
        show()

        compose.onNodeWithText("Immatriculation").assertDoesNotExist()
    }

    @Test fun `an unknown order shows nothing rather than crashing`() {
        show("RN404")

        compose.onAllNodesWithText("Commande #RN404").assertCountEquals(0)
    }
}
