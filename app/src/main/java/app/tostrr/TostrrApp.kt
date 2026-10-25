package app.tostrr

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.util.DebugLogger
import app.tostrr.di.AppModule
import app.tostrr.domain.sync.usecase.StartBackgroundSyncUseCase
import app.tostrr.widget.WidgetRefresher
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import org.koin.ksp.generated.module

class TostrrApp : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TostrrApp)
            // Builds RefreshWorker through Koin; the default WorkManager initializer is removed in the manifest.
            workManagerFactory()
            modules(AppModule().module)
        }
        get<WidgetRefresher>().start()
        get<StartBackgroundSyncUseCase>()()
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
        // Logs every request and why it failed, under the "RealImageLoader" tag (debug builds only).
        .apply { if (BuildConfig.DEBUG) logger(DebugLogger()) }
        .build()
}
