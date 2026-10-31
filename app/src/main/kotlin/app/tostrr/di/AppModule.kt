package app.tostrr.di

import app.tostrr.data.di.DataModule
import app.tostrr.domain.di.DomainModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/**
 * Root Koin module: includes the domain (use cases) and data (repositories) modules and scans the app (UI,
 * widgets). Our own classes carry their annotation (`@Single`, `@Factory`, `@KoinViewModel`, `@KoinWorker`); the
 * definitions below only cover types we don't construct ourselves.
 */
@Module(includes = [DomainModule::class, DataModule::class])
@ComponentScan("app.tostrr")
class AppModule {

    /** Outlives any screen: used for work started by the sync, such as widget updates. */
    @Single
    fun appScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
