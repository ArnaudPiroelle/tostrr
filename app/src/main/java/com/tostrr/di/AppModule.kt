package com.tostrr.di

import android.content.Context
import androidx.work.WorkManager
import com.tostrr.data.ChecklistDao
import com.tostrr.data.TostrrDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import java.util.concurrent.TimeUnit

/**
 * Root Koin module. Our own classes carry their annotation (`@Single`, `@KoinViewModel`, `@KoinWorker`) and are
 * picked up by the scan; the definitions below only cover types we don't construct ourselves.
 */
@Module
@ComponentScan("com.tostrr")
class AppModule {

    @Single
    fun okHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Outlives any screen: used for work started by the sync, such as widget updates. */
    @Single
    fun appScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Built by Room, which generates the implementation. */
    @Single
    fun database(context: Context): TostrrDatabase = TostrrDatabase.create(context)

    /** Only valid once Koin's workManagerFactory() has initialised it, which TostrrApp does at startup. */
    @Single
    fun workManager(context: Context): WorkManager = WorkManager.getInstance(context)

    @Single
    fun checklistDao(database: TostrrDatabase): ChecklistDao = database.checklistDao()
}
