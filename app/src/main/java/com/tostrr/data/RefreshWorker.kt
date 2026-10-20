package com.tostrr.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tostrr.TostrrApp
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/** Background sync: updates the cached orders and records what changed. */
class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as TostrrApp
        if (!app.auth.isLoggedIn()) return Result.success()
        return try {
            app.repository.refresh()
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: AuthException) {
            // Session gone or refresh rejected: nothing to retry. The foreground flow detects it and logs out.
            warn(app, expired = e is SessionExpiredException)
            Result.success()
        } catch (e: Exception) {
            // Network or Tesla-side hiccup: let WorkManager retry with backoff.
            warn(app, expired = false)
            Result.retry()
        }
    }

    /** An expired session is reported at once; any other failure only once it has lasted [STALE_AFTER_MS]. */
    private fun warn(app: TostrrApp, expired: Boolean) {
        val age = app.repository.lastSyncAgeMs()
        if (expired || age == null || age > STALE_AFTER_MS) app.repository.notifier?.notifySyncProblem(expired, age)
    }

    private companion object {
        val STALE_AFTER_MS = TimeUnit.HOURS.toMillis(6)
    }
}

object RefreshScheduler {
    private const val WORK_NAME = "orders_refresh"

    /** Idempotent: keeps the existing schedule if one is already enqueued. */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<RefreshWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
