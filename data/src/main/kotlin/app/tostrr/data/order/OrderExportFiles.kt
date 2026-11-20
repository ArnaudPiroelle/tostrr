package app.tostrr.data.order

import android.content.Context
import app.tostrr.domain.order.OrderExportWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.io.File

/** Exports go to `cacheDir/exports`, which the app's FileProvider shares; one file per order, the last one kept. */
@Single
class OrderExportFiles(private val context: Context) : OrderExportWriter {
    private val dir get() = File(context.cacheDir, "exports")

    override suspend fun write(reference: String, json: String): File = withContext(Dispatchers.IO) {
        File(dir.apply { mkdirs() }, "order-${reference.filter(Char::isLetterOrDigit)}.json").apply { writeText(json) }
    }

    override fun clear() {
        dir.deleteRecursively()
    }
}
