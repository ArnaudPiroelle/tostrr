package com.tostrr.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tostrr.data.auth.AuthException
import com.tostrr.data.auth.SessionExpiredException
import com.tostrr.data.auth.TeslaAuth
import com.tostrr.data.notification.OrderNotifier
import com.tostrr.data.order.OrderRepository
import kotlinx.coroutines.CancellationException
import org.koin.android.annotation.KoinWorker
import org.koin.core.annotation.Single
import java.util.concurrent.TimeUnit

/** Background sync: updates the cached orders and records what changed. */
@KoinWorker
class RefreshWorker(
    context: Context,
    params: WorkerParameters,
    private val auth: TeslaAuth,
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
class RefreshScheduler(private val workManager: WorkManager) {

    /** Idempotent: keeps the existing schedule if one is already enqueued. */
    fun schedule() {
        val request = PeriodicWorkRequestBuilder<RefreshWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    private companion object {
        const val WORK_NAME = "orders_refresh"
    }
}
