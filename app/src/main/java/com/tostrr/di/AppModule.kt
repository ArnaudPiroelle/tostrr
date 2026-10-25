package com.tostrr.di

import com.tostrr.data.di.DataModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/**
 * Root Koin module: includes the data layer's module and scans the app (UI, widgets). Our own classes carry their
 * annotation (`@Single`, `@KoinViewModel`, `@KoinWorker`); the definitions below only cover types we don't
 * construct ourselves.
 */
@Module(includes = [DataModule::class])
@ComponentScan("com.tostrr")
class AppModule {

    /** Outlives any screen: used for work started by the sync, such as widget updates. */
    @Single
    fun appScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
