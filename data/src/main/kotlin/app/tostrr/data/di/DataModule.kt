package app.tostrr.data.di

import android.content.Context
import androidx.work.WorkManager
import app.tostrr.data.checklist.ChecklistDao
import app.tostrr.data.checklist.TostrrDatabase
import okhttp3.Cache
import okhttp3.OkHttpClient
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Koin module of the data layer. Its classes carry their own annotation and are picked up by the scan; the
 * definitions below only cover types we don't construct ourselves.
 */
@Module
@ComponentScan("app.tostrr.data")
class DataModule {

    /**
     * With an HTTP cache, which follows each server's headers: the ship tracking site and GitHub send an ETag, so they
     * are asked again with `If-None-Match` and answer 304 while nothing changed; the Tesla API answers `no-store`, so
     * none of it is written there.
     */
    @Single
    fun okHttpClient(context: Context): OkHttpClient = OkHttpClient.Builder()
        .cache(Cache(File(context.cacheDir, "http"), 4L * 1024 * 1024))
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Built by Room, which generates the implementation. */
    @Single
    fun database(context: Context): TostrrDatabase = TostrrDatabase.create(context)

    @Single
    fun checklistDao(database: TostrrDatabase): ChecklistDao = database.checklistDao()

    /** Only valid once Koin's workManagerFactory() has initialised it, which the app does at startup. */
    @Single
    fun workManager(context: Context): WorkManager = WorkManager.getInstance(context)
}
