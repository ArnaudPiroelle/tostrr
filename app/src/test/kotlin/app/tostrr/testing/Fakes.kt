package app.tostrr.testing

import app.tostrr.domain.appearance.AppearanceRepository
import app.tostrr.domain.appearance.ThemeMode
import app.tostrr.domain.auth.AccountRepository
import app.tostrr.domain.auth.AuthRepository
import app.tostrr.domain.auth.TeslaAccount
import app.tostrr.domain.checklist.Checklist
import app.tostrr.domain.checklist.ChecklistAnswer
import app.tostrr.domain.checklist.ChecklistReportWriter
import app.tostrr.domain.checklist.ChecklistRepository
import app.tostrr.domain.checklist.DeliveryChecklist
import app.tostrr.domain.deliverycenter.DeliveryCenter
import app.tostrr.domain.deliverycenter.DeliveryCenterRepository
import app.tostrr.domain.deliverycenter.DeliveryCenters
import app.tostrr.domain.layout.BlockLayout
import app.tostrr.domain.layout.DetailLayoutRepository
import app.tostrr.domain.notification.NotificationSettings
import app.tostrr.domain.notification.NotificationSettingsRepository
import app.tostrr.domain.order.Change
import app.tostrr.domain.order.OptionInfo
import app.tostrr.domain.order.OptionRepository
import app.tostrr.domain.order.Order
import app.tostrr.domain.order.OrderRepository
import app.tostrr.domain.shipping.ShipSnapshot
import app.tostrr.domain.shipping.ShipTrackingRepository
import app.tostrr.domain.sync.SyncInterval
import app.tostrr.domain.sync.SyncScheduler
import app.tostrr.domain.sync.SyncSettings
import app.tostrr.domain.sync.SyncSettingsRepository
import app.tostrr.domain.update.AppUpdate
import app.tostrr.domain.update.UpdateDownload
import app.tostrr.domain.update.UpdateRepository
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** In-memory stand-ins for the repositories of `:data`; tests set their state and read what was called. */

class FakeAuthRepository(loggedIn: Boolean = true) : AuthRepository {
    override val loggedIn = MutableStateFlow(loggedIn)

    /** Thrown by [completeLogin] instead of logging in. */
    var loginFailure: Exception? = null
    val redirects = mutableListOf<String>()

    override fun startLogin() = LOGIN_URL

    override suspend fun completeLogin(redirect: String) {
        redirects += redirect
        loginFailure?.let { throw it }
        loggedIn.value = true
    }

    override fun logout() {
        loggedIn.value = false
    }

    companion object {
        const val LOGIN_URL = "https://auth.tesla.com/oauth2/v3/authorize?test"
    }
}

class FakeUpdateRepository : UpdateRepository {
    override val update = MutableStateFlow<AppUpdate?>(null)
    override val updateToOffer = MutableStateFlow<AppUpdate?>(null)
    var checks = 0
    val skipped = mutableListOf<String>()

    /** What [download] goes through; fails or stays open as the test needs. */
    var downloading: Flow<UpdateDownload> = flowOf(UpdateDownload.Ready(APK_PATH))

    override suspend fun check() {
        checks++
    }

    override fun skip(version: String) {
        skipped += version
        updateToOffer.value = null
    }

    override fun download(update: AppUpdate) = downloading

    companion object {
        const val APK_PATH = "/updates/tostrr.apk"
    }
}

/** Records each call: the interval when scheduled, null when cancelled. */
class FakeSyncScheduler : SyncScheduler {
    val calls = mutableListOf<SyncInterval?>()

    override fun schedule(interval: SyncInterval) {
        calls += interval
    }

    override fun cancel() {
        calls += null
    }
}

class FakeSyncSettingsRepository(initial: SyncSettings = SyncSettings()) : SyncSettingsRepository {
    override val settings = MutableStateFlow(initial)

    override fun save(settings: SyncSettings) {
        this.settings.value = settings
    }
}

class FakeAppearanceRepository(initial: ThemeMode = ThemeMode.SYSTEM) : AppearanceRepository {
    override val themeMode = MutableStateFlow(initial)

    override fun saveThemeMode(mode: ThemeMode) {
        themeMode.value = mode
    }
}

class FakeNotificationSettingsRepository(initial: NotificationSettings = NotificationSettings()) : NotificationSettingsRepository {
    override val settings = MutableStateFlow(initial)

    override fun save(settings: NotificationSettings) {
        this.settings.value = settings
    }
}

class FakeAccountRepository(initial: TeslaAccount? = null) : AccountRepository {
    override val account = MutableStateFlow(initial)

    /** What [load] brings back; null keeps what is there, like a failed fetch. */
    var fetched: TeslaAccount? = null
    var loads = 0

    override suspend fun load() {
        loads++
        fetched?.let { account.value = it }
    }

    override fun clear() {
        account.value = null
    }
}

class FakeOrderRepository(orders: List<Order> = emptyList()) : OrderRepository {
    override val orders = MutableStateFlow(orders)
    override val history = MutableStateFlow<Map<String, List<Change>>>(emptyMap())
    override val syncing = MutableStateFlow(false)

    /** Run by [refresh]: throw to fail it, or set [orders]. */
    var onRefresh: suspend () -> Unit = {}
    var refreshes = 0

    override fun lastSyncAgeMs(now: Long): Long? = null

    override suspend fun refresh() {
        refreshes++
        syncing.value = true
        try {
            onRefresh()
        } finally {
            syncing.value = false
        }
    }

    override fun clear() {
        orders.value = emptyList()
        history.value = emptyMap()
    }
}

class FakeOptionRepository(codes: Map<String, OptionInfo> = emptyMap()) : OptionRepository {
    override val codes = MutableStateFlow(codes)
}

class FakeShipTrackingRepository(snapshot: ShipSnapshot? = null) : ShipTrackingRepository {
    override val snapshot = MutableStateFlow(snapshot)
}

class FakeDeliveryCenterRepository(private val centers: List<DeliveryCenter> = emptyList()) : DeliveryCenterRepository {
    override suspend fun centers() = DeliveryCenters(centers)
}

class FakeDetailLayoutRepository(initial: BlockLayout = BlockLayout()) : DetailLayoutRepository {
    override val layout = MutableStateFlow(initial)

    override fun save(layout: BlockLayout) {
        this.layout.value = layout
    }
}

/** Follows the contract of the Room implementation closely enough for the screens: one checklist per order. */
class FakeChecklistRepository(private val photoDir: File) : ChecklistRepository {
    val checklists = MutableStateFlow<Map<String, Checklist>>(emptyMap())
    val deletedPhotos = mutableListOf<String>()

    override fun observe(ref: String): Flow<Checklist?> = checklists.map { it[ref] }

    override suspend fun start(ref: String, now: Long) {
        if (ref !in checklists.value) set(ref, Checklist(ref, now, null, 0, emptyMap()))
    }

    override suspend fun moveTo(ref: String, cursor: Int, now: Long) {
        val checklist = checklists.value[ref]?.takeUnless { it.completed } ?: return
        set(
            ref,
            if (cursor > DeliveryChecklist.steps.lastIndex) checklist.copy(completedAtEpochMs = now)
            else checklist.copy(cursor = cursor),
        )
    }

    override suspend fun lock(ref: String, now: Long) {
        checklists.value[ref]?.takeIf { it.completed && !it.locked }?.let { set(ref, it.copy(lockedAtEpochMs = now)) }
    }

    override suspend fun reopen(ref: String, cursor: Int) {
        checklists.value[ref]?.takeUnless { it.locked }?.let { set(ref, it.copy(completedAtEpochMs = null, cursor = cursor)) }
    }

    override suspend fun updateAnswer(ref: String, answer: ChecklistAnswer): Boolean {
        val checklist = checklists.value[ref]?.takeIf { it.completed && !it.locked } ?: return false
        set(ref, checklist.copy(answers = checklist.answers + (answer.questionId to answer)))
        return true
    }

    override suspend fun answer(ref: String, answer: ChecklistAnswer, nextCursor: Int): Boolean {
        val checklist = checklists.value[ref]?.takeUnless { it.completed } ?: return false
        set(ref, checklist.copy(answers = checklist.answers + (answer.questionId to answer)))
        moveTo(ref, nextCursor)
        return true
    }

    override suspend fun delete(ref: String) {
        checklists.value -= ref
    }

    override suspend fun importPhoto(ref: String, questionId: String, source: InputStream): String {
        val file = newPhotoFile(ref, questionId)
        file.outputStream().use { source.copyTo(it) }
        return file.absolutePath
    }

    override fun newPhotoFile(ref: String, questionId: String): File =
        File.createTempFile("${ref}_$questionId", ".jpg", photoDir)

    override suspend fun deletePhoto(path: String) {
        deletedPhotos += path
    }

    private fun set(ref: String, checklist: Checklist) {
        checklists.value += (ref to checklist)
    }
}

/** Writes an empty file per report and remembers what it was asked for. */
class FakeChecklistReportWriter(private val dir: File) : ChecklistReportWriter {
    val written = mutableListOf<Pair<Checklist, Order?>>()

    override suspend fun write(checklist: Checklist, order: Order?): File {
        written += checklist to order
        return File(dir, "checklist-${checklist.orderReference}.pdf").apply { writeText("%PDF") }
    }
}
