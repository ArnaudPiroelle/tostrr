package app.tostrr.data.order

import app.tostrr.data.api.TeslaApi
import app.tostrr.data.auth.TeslaAuth
import app.tostrr.data.auth.TokenStore
import app.tostrr.data.auth.Tokens
import app.tostrr.data.catalog.OptionCatalog
import app.tostrr.data.catalog.PartsCatalog
import app.tostrr.data.notification.NotificationSettingsRepositoryImpl
import app.tostrr.data.notification.OrderNotifier
import app.tostrr.data.shipping.ShipTracker
import app.tostrr.data.testing.FakeAndroidKeyStore
import app.tostrr.data.testing.FakeHttp
import app.tostrr.domain.error.ApiException
import app.tostrr.domain.error.AuthError
import app.tostrr.domain.error.AuthException
import app.tostrr.domain.order.OrderMapper
import app.tostrr.domain.order.StepKind
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.zip.GZIPInputStream

@RunWith(RobolectricTestRunner::class)
class OrderRepositoryImplTest {
    private val context = RuntimeEnvironment.getApplication()
    private val http = FakeHttp()
    private lateinit var store: TokenStore

    @Before fun setUp() {
        FakeAndroidKeyStore.install()
        store = TokenStore(context)
        store.save(Tokens("A1", "R1", System.currentTimeMillis() / 1000 + 3600))
        orders("""{"referenceNumber":"RN1","modelCode":"my","orderStatus":"BOOKED"}""")
        tasks("""{"scheduling":{"deliveryWindowDisplay":"29 Novembre - 29 Décembre"}}""")
    }

    /** A new repository over the same files, as after a restart of the app. */
    private fun repository(): OrderRepositoryImpl {
        val catalog = OptionCatalog(http.client, context)
        return OrderRepositoryImpl(context, TeslaApi(http.client), TeslaAuth(http.client, store), catalog, PartsCatalog(http.client, context), ShipTracker(http.client, context), OrderNotifier(context, catalog, NotificationSettingsRepositoryImpl(context)))
    }

    private fun orders(vararg orders: String) = http.on("/api/1/users/orders", """{"response":[${orders.joinToString(",")}]}""")
    private fun tasks(tasks: String) = http.on("/tasks", """{"tasks":$tasks}""")

    @Test fun `nothing is known before the first sync`() {
        val repository = repository()
        assertTrue(repository.orders.value.isEmpty())
        assertTrue(repository.history.value.isEmpty())
        assertNull(repository.lastSyncAgeMs())
        assertFalse(repository.syncing.value)
    }

    @Test fun `a sync maps every order with its tasks`() = runTest {
        orders("""{"referenceNumber":"RN1","modelCode":"my"}""", """{"referenceNumber":"RN2","modelCode":"m3"}""")
        val repository = repository()
        repository.refresh()
        assertEquals(listOf("RN1", "RN2"), repository.orders.value.map { it.reference })
        assertEquals(listOf("Model Y", "Model 3"), repository.orders.value.map { it.modelName })
        assertEquals(listOf("RN1", "RN2"), http.callsTo("/tasks").map { it.request.url.queryParameter("referenceNumber") })
        assertTrue(http.callsTo("/tasks").all { it.request.header("Authorization") == "Bearer A1" })
        assertFalse(repository.syncing.value)
    }

    @Test fun `the last sync is read back after a restart`() = runTest {
        repository().refresh()
        val restarted = repository()
        val order = restarted.orders.value.single()
        assertEquals("RN1", order.reference)
        assertEquals("29 Novembre - 29 Décembre", order.deliveryWindow)
        assertTrue(restarted.lastSyncAgeMs()!! in 0..60_000)
    }

    @Test fun `the first sync of an order records no history`() = runTest {
        val repository = repository()
        repository.refresh()
        assertTrue(repository.history.value.isEmpty())
    }

    @Test fun `what changed between two syncs is recorded, newest first, and kept after a restart`() = runTest {
        val repository = repository()
        repository.refresh()
        tasks("""{"scheduling":{"deliveryWindowDisplay":"1 Décembre - 15 Décembre"}}""")
        repository.refresh()
        tasks("""{"scheduling":{"deliveryWindowDisplay":"2 Décembre - 15 Décembre","isMoreThanTwoWeeks":true}}""")
        repository.refresh()

        val changes = repository.history.value.getValue("RN1")
        assertEquals(listOf("details.tasks.scheduling.deliveryWindowDisplay"), changes.map { it.key }.distinct())
        assertEquals(listOf("2 Décembre - 15 Décembre", "1 Décembre - 15 Décembre"), changes.map { it.newValue })
        assertEquals("29 Novembre - 29 Décembre", changes.last().oldValue)
        assertEquals(changes, repository().history.value.getValue("RN1"))
    }

    @Test fun `a new object in the payload is recorded as structured`() = runTest {
        val repository = repository()
        repository.refresh()
        tasks("""{"scheduling":{"deliveryWindowDisplay":"29 Novembre - 29 Décembre"},"registration":{"regData":{"reggieLicensePlate":"AB-123-CD"}}}""")
        repository.refresh()
        val change = repository.history.value.getValue("RN1").single()
        assertEquals("details.tasks.registration", change.key)
        assertNull(change.oldValue)
        assertTrue(change.structured)
        assertTrue(repository.orders.value.single().steps.first { it.kind == StepKind.REGISTERED }.done)
    }

    @Test fun `the vin step is dated by the sync that first saw it, also after a restart`() = runTest {
        val repository = repository()
        repository.refresh()
        assertNull(repository.orders.value.single().steps.first { it.kind == StepKind.VIN_ASSIGNED }.date)

        orders("""{"referenceNumber":"RN1","modelCode":"my","orderStatus":"BOOKED","vin":"LRWYGCEK1TC000001"}""")
        repository.refresh()

        val today = OrderMapper.formatDate(LocalDate.now().toString())
        assertEquals(today, repository.orders.value.single().steps.first { it.kind == StepKind.VIN_ASSIGNED }.date)
        assertEquals(today, repository().orders.value.single().steps.first { it.kind == StepKind.VIN_ASSIGNED }.date)
    }

    @Test fun `a vin already there on the first sync is dated by that sync, once`() = runTest {
        orders("""{"referenceNumber":"RN1","modelCode":"my","vin":"LRWYGCEK1TC000001"}""")
        val repository = repository()
        repository.refresh()
        val step = repository.orders.value.single().steps.first { it.kind == StepKind.VIN_ASSIGNED }
        assertTrue(step.done)
        assertEquals(OrderMapper.formatDate(LocalDate.now().toString()), step.date)

        val recorded = repository.history.value.getValue("RN1").single()
        assertEquals("order.vin", recorded.key)
        repository.refresh()
        assertEquals(listOf(recorded), repository.history.value.getValue("RN1"))
    }

    @Test fun `a vin cached without a date is dated by the sync of that cache`() = runTest {
        val fetchedAt = LocalDate.of(2026, 11, 2).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        File(context.filesDir, "orders.json").writeText(
            """[{"fetchedAt":$fetchedAt,"raw":{"order":{"referenceNumber":"RN1","modelCode":"my","vin":"LRWYGCEK1TC000001"},"details":{"tasks":{}}}}]""",
        )
        orders("""{"referenceNumber":"RN1","modelCode":"my","vin":"LRWYGCEK1TC000001"}""")
        tasks("{}")
        val repository = repository()
        assertNull(repository.orders.value.single().steps.first { it.kind == StepKind.VIN_ASSIGNED }.date)
        repository.refresh()
        assertEquals(OrderMapper.formatDate("2026-11-02"), repository.orders.value.single().steps.first { it.kind == StepKind.VIN_ASSIGNED }.date)
    }

    @Test fun `entries of paths ignored since are dropped from the saved history`() {
        File(context.filesDir, "history.json").writeText(
            """{"RN1":[{"t":1,"k":"details.tasks.scheduling.isMoreThanTwoWeeks","o":"false","n":"true"},
                       {"t":2,"k":"details.tasks.scheduling.deliveryWindowDisplay","o":"a","n":"b"}],
                "RN2":[{"t":3,"k":"details.tasks.finalPayment.complete","o":"false","n":"true"}]}""",
        )
        val history = repository().history.value
        assertEquals(listOf("RN1"), history.keys.toList())
        assertEquals(listOf("details.tasks.scheduling.deliveryWindowDisplay"), history.getValue("RN1").map { it.key })
        assertFalse("isMoreThanTwoWeeks" in File(context.filesDir, "history.json").readText())
    }

    @Test fun `local signals kept inside the raw payloads by 0_4_0 move out of them, and out of the history`() = runTest {
        File(context.filesDir, "orders.json").writeText(
            """[{"fetchedAt":1,"raw":{"order":{"referenceNumber":"RN1","modelCode":"my","orderStatus":"BOOKED","vin":"LRWYGCEK1TC000001"},
                "details":{"tasks":{}},"local":{"inAccountSince":"2026-11-10"}}}]""",
        )
        File(context.filesDir, "history.json").writeText(
            """{"RN1":[{"t":1,"k":"local","o":null,"n":"{\"inAccountSince\":\"2026-11-10\"}","s":true},
                       {"t":2,"k":"local.partsCatalogReference","o":null,"n":"ref-1"},
                       {"t":3,"k":"details.tasks.scheduling.deliveryWindowDisplay","o":"a","n":"b"}]}""",
        )
        orders("""{"referenceNumber":"RN1","modelCode":"my","orderStatus":"BOOKED","vin":"LRWYGCEK1TC000001"}""")
        tasks("{}")
        val repository = repository()
        assertEquals(listOf("details.tasks.scheduling.deliveryWindowDisplay"), repository.history.value.getValue("RN1").map { it.key })
        assertEquals(LocalDate.of(2026, 11, 10), repository.orders.value.single().inAccountSince)
        assertEquals(setOf("order", "details"), repository.orders.value.single().raw.keys)

        repository.refresh()
        val saved = Json.parseToJsonElement(File(context.filesDir, "orders.json").readText()).jsonArray.single().jsonObject
        assertEquals(setOf("order", "details"), saved.getValue("raw").jsonObject.keys)
        assertEquals("2026-11-10", saved.getValue("local").jsonObject.getValue("inAccountSince").jsonPrimitive.content)
        assertEquals(LocalDate.of(2026, 11, 10), repository().orders.value.single().inAccountSince)
    }

    @Test fun `every sync is archived as a gzipped snapshot`() = runTest {
        repository().refresh()
        val folder = File(context.getExternalFilesDir("snapshots"), "RN1")
        val snapshot = folder.listFiles()!!.single()
        assertTrue(snapshot.name.endsWith(".json.gz"))
        val json = Json.parseToJsonElement(GZIPInputStream(snapshot.inputStream()).bufferedReader().readText()).jsonObject
        assertEquals("RN1", json["raw"]!!.jsonObject["order"]!!.jsonObject["referenceNumber"]!!.jsonPrimitive.content)
    }

    @Test fun `without a session the sync fails and keeps the orders`() = runTest {
        val repository = repository()
        repository.refresh()
        store.clear()
        val e = runCatching { repository.refresh() }.exceptionOrNull()
        assertEquals(AuthError.NOT_LOGGED_IN, (e as AuthException).reason)
        assertEquals(1, repository.orders.value.size)
        assertFalse(repository.syncing.value)
    }

    @Test fun `an api failure keeps the orders and the history as they were`() = runTest {
        val repository = repository()
        repository.refresh()
        val before = repository.orders.value
        tasks("""{"scheduling":{"deliveryWindowDisplay":"1 Décembre - 15 Décembre"}}""")
        http.on("/api/1/users/orders", "", code = 500)
        val e = runCatching { repository.refresh() }.exceptionOrNull()
        assertEquals(500, (e as ApiException).code)
        assertEquals(before, repository.orders.value)
        assertTrue(repository.history.value.isEmpty())
        assertFalse(repository.syncing.value)
    }

    @Test fun `a vin the parts catalogue lists makes the car built from that day, also after a restart`() = runTest {
        orders("""{"referenceNumber":"RN1","modelCode":"my","vin":"LRWYGCEK1TC000001"}""")
        http.on("/api/catalogs", """{"success":true,"responseObject":[{"id":2,"externalReference":"ref-1"}]}""")
        val repository = repository()
        repository.refresh()
        assertTrue(http.callsTo("/api/catalogs").isEmpty())
        assertFalse(repository.orders.value.single().steps.first { it.kind == StepKind.BUILT }.done)

        repository.refresh()
        assertEquals(listOf("LRWYGCEK1TC000001"), http.callsTo("/api/catalogs").map { it.request.url.queryParameter("vin") })
        val today = OrderMapper.formatDate(LocalDate.now().toString())
        val built = repository.orders.value.single().steps.first { it.kind == StepKind.BUILT }
        assertTrue(built.done)
        assertEquals(today, built.date)
        assertTrue(repository.history.value.values.flatten().none { it.key.startsWith("local") })
        assertEquals(setOf("order", "details"), repository.orders.value.single().raw.keys)
        assertEquals("ref-1", repository.orders.value.single().partsCatalogReference)
        assertEquals(built, repository().orders.value.single().steps.first { it.kind == StepKind.BUILT })
        assertEquals("ref-1", repository().orders.value.single().partsCatalogReference)
    }

    @Test fun `a car in the account delivers its order, and stays delivered`() = runTest {
        orders("""{"referenceNumber":"RN1","modelCode":"my","orderStatus":"BOOKED","vin":"LRWYGCEK1TC000001"}""")
        val repository = repository()
        repository.refresh()
        // Without a VIN known before this sync, the account isn't asked.
        assertTrue(http.callsTo("/api/1/products").isEmpty())
        http.on("/api/1/products", """{"response":[{"id":1,"vin":"LRWYGCEK1TC000001"}]}""")
        repository.refresh()

        val order = repository.orders.value.single()
        assertTrue(order.delivered)
        assertEquals(LocalDate.now(), order.inAccountSince)
        assertTrue(repository.history.value.values.flatten().none { it.key.startsWith("local") })

        // Delivered: the account is no longer asked, and the day stays, also after a restart.
        http.on("/api/1/products", "", code = 500)
        repository.refresh()
        assertEquals(1, http.callsTo("/api/1/products").size)
        assertEquals(LocalDate.now(), repository().orders.value.single().inAccountSince)
    }

    @Test fun `the account failing doesn't fail the sync`() = runTest {
        orders("""{"referenceNumber":"RN1","modelCode":"my","vin":"LRWYGCEK1TC000001"}""")
        val repository = repository()
        repository.refresh()
        http.on("/api/1/products", "", code = 500)
        repository.refresh()
        assertEquals(1, http.callsTo("/api/1/products").size)
        assertFalse(repository.orders.value.single().delivered)
    }

    @Test fun `an order whose tasks fail keeps its last details while the others sync`() = runTest {
        orders("""{"referenceNumber":"RN1","modelCode":"my"}""", """{"referenceNumber":"RN2","modelCode":"m3"}""")
        val repository = repository()
        repository.refresh()
        val stale = repository.orders.value.first { it.reference == "RN2" }

        orders("""{"referenceNumber":"RN1","modelCode":"my"}""", """{"referenceNumber":"RN2","modelCode":"m3","orderStatus":"DELIVERED"}""")
        tasks("""{"scheduling":{"deliveryWindowDisplay":"1 Décembre - 15 Décembre"}}""")
        http.failOn("/tasks", "referenceNumber", "RN2")
        repository.refresh()

        val (rn1, rn2) = repository.orders.value
        assertEquals("1 Décembre - 15 Décembre", rn1.deliveryWindow)
        assertEquals("29 Novembre - 29 Décembre", rn2.deliveryWindow)
        assertEquals("DELIVERED", rn2.status)
        assertEquals(stale.fetchedAtEpochMs, rn2.fetchedAtEpochMs)
        assertEquals(listOf("order.orderStatus"), repository.history.value.getValue("RN2").map { it.key })
    }

    @Test fun `an order whose tasks fail on its first sync fails the sync`() = runTest {
        orders("""{"referenceNumber":"RN1","modelCode":"my"}""", """{"referenceNumber":"RN2","modelCode":"m3"}""")
        http.failOn("/tasks", "referenceNumber", "RN2")
        val repository = repository()
        val e = runCatching { repository.refresh() }.exceptionOrNull()
        assertEquals(500, (e as ApiException).code)
        assertTrue(repository.orders.value.isEmpty())
    }

    @Test fun `tasks failing for every order fail the sync`() = runTest {
        val repository = repository()
        repository.refresh()
        val before = repository.orders.value
        http.on("/tasks", "", code = 503)
        val e = runCatching { repository.refresh() }.exceptionOrNull()
        assertEquals(503, (e as ApiException).code)
        assertEquals(before, repository.orders.value)
    }

    @Test fun `the option labels are fetched after a sync, and their failure doesn't fail it`() = runTest {
        val repository = repository()
        repository.refresh()
        assertEquals(1, http.callsTo("/get/option_codes.php").size)
        assertEquals(1, repository.orders.value.size)
    }

    @Test fun `clearing forgets the orders and their history, on disk too`() = runTest {
        val repository = repository()
        repository.refresh()
        tasks("""{"scheduling":{"deliveryWindowDisplay":"1 Décembre - 15 Décembre"}}""")
        repository.refresh()
        repository.clear()
        assertTrue(repository.orders.value.isEmpty())
        assertTrue(repository.history.value.isEmpty())
        val restarted = repository()
        assertTrue(restarted.orders.value.isEmpty())
        assertTrue(restarted.history.value.isEmpty())
    }
}
