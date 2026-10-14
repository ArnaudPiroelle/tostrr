package com.tostrr

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.tostrr.data.OptionCatalog
import com.tostrr.data.OrderNotifier
import com.tostrr.data.OrderRepository
import com.tostrr.data.RefreshScheduler
import com.tostrr.data.TeslaApi
import com.tostrr.data.TeslaAuth
import com.tostrr.data.TokenStore
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class TostrrApp : Application(), SingletonImageLoader.Factory {
    lateinit var auth: TeslaAuth
        private set
    lateinit var catalog: OptionCatalog
        private set
    lateinit var repository: OrderRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        auth = TeslaAuth(client, TokenStore(this))
        catalog = OptionCatalog(client, filesDir)
        repository = OrderRepository(TeslaApi(client), auth, catalog, filesDir, OrderNotifier(this) { catalog.codes.value })
        if (auth.isLoggedIn()) RefreshScheduler.schedule(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context).build()
}
