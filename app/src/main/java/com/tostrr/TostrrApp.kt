package com.tostrr

import android.app.Application
import androidx.glance.appwidget.updateAll
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
import com.tostrr.widget.CountdownWidget
import com.tostrr.widget.OrderWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.File
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
        repository = OrderRepository(
            api = TeslaApi(client),
            auth = auth,
            catalog = catalog,
            dir = filesDir,
            // Readable over adb without root, so real payloads can be pulled for analysis.
            archiveDir = getExternalFilesDir("snapshots") ?: File(filesDir, "snapshots"),
            notifier = OrderNotifier(this) { catalog.codes.value },
            onRefreshed = {
                scope.launch {
                    OrderWidget().updateAll(this@TostrrApp)
                    CountdownWidget().updateAll(this@TostrrApp)
                }
            },
        )
        if (auth.isLoggedIn()) RefreshScheduler.schedule(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context).build()
}
