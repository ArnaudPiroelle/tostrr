package com.tostrr.data.di

import android.content.Context
import androidx.work.WorkManager
import com.tostrr.data.checklist.ChecklistDao
import com.tostrr.data.checklist.TostrrDatabase
import okhttp3.OkHttpClient
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import java.util.concurrent.TimeUnit

/**
 * Koin module of the data layer. Its classes carry their own annotation and are picked up by the scan; the
 * definitions below only cover types we don't construct ourselves.
 */
@Module
@ComponentScan("com.tostrr.data")
class DataModule {

    @Single
    fun okHttpClient(): OkHttpClient = OkHttpClient.Builder()
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
