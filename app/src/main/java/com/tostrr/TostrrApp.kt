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
import androidx.glance.appwidget.updateAll
import com.tostrr.widget.OrderWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class TostrrApp : Application(), SingletonImageLoader.Factory {
    lateinit var auth: TeslaAuth
        private set
    lateinit var catalog: OptionCatalog
        private set
    lateinit var repository: OrderRepository
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        auth = TeslaAuth(client, TokenStore(this))
        catalog = OptionCatalog(client, filesDir)
        repository = OrderRepository(TeslaApi(client), auth, catalog, filesDir, getExternalFilesDir("snapshots") ?: java.io.File(filesDir, "snapshots"), OrderNotifier(this) { catalog.codes.value }) {
            scope.launch { OrderWidget().updateAll(this@TostrrApp) }
        }
        if (auth.isLoggedIn()) RefreshScheduler.schedule(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context).build()
}
