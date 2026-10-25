package com.tostrr

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.tostrr.data.RefreshScheduler
import com.tostrr.data.TeslaAuth
import com.tostrr.di.AppModule
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
        if (get<TeslaAuth>().isLoggedIn()) RefreshScheduler.schedule(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context).build()
}
