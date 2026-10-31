package app.tostrr.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.tostrr.data.notification.OrderNotifier
import app.tostrr.domain.auth.AuthRepository
import app.tostrr.domain.error.AuthException
import app.tostrr.domain.error.SessionExpiredException
import app.tostrr.domain.order.OrderRepository
import app.tostrr.domain.sync.SyncInterval
import app.tostrr.domain.sync.SyncScheduler
import kotlinx.coroutines.CancellationException
import org.koin.android.annotation.KoinWorker
import org.koin.core.annotation.Single
import java.util.concurrent.TimeUnit

/** Background sync: updates the cached orders and records what changed. */
@KoinWorker
class RefreshWorker(
    context: Context,
    params: WorkerParameters,
    private val auth: AuthRepository,
    private val repository: OrderRepository,
    private val notifier: OrderNotifier,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!auth.isLoggedIn()) return Result.success()
        return try {
            repository.refresh()
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: AuthException) {
            // Session gone or refresh rejected: nothing to retry. The foreground flow detects it and logs out.
            warn(expired = e is SessionExpiredException)
            Result.success()
        } catch (e: Exception) {
            // Network or Tesla-side hiccup: let WorkManager retry with backoff.
            warn(expired = false)
            Result.retry()
        }
    }

    /** An expired session is reported at once; any other failure only once it has lasted [STALE_AFTER_MS]. */
    private fun warn(expired: Boolean) {
        val age = repository.lastSyncAgeMs()
        if (expired || age == null || age > STALE_AFTER_MS) notifier.notifySyncProblem(expired, age)
    }

    private companion object {
        val STALE_AFTER_MS = TimeUnit.HOURS.toMillis(6)
    }
}

@Single
class RefreshScheduler(private val workManager: WorkManager) : SyncScheduler {

    /** Idempotent: [ExistingPeriodicWorkPolicy.UPDATE] keeps an enqueued schedule, only changing its period if needed. */
    override fun schedule(interval: SyncInterval) {
        val request = PeriodicWorkRequestBuilder<RefreshWorker>(interval.minutes, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    override fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    private companion object {
        const val WORK_NAME = "orders_refresh"
    }
}
