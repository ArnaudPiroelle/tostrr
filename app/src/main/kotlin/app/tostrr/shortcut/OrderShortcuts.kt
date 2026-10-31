package app.tostrr.shortcut

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import app.tostrr.R
import app.tostrr.domain.order.Order
import app.tostrr.domain.order.usecase.ObserveLatestAwaitingDeliveryUseCase
import app.tostrr.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

/**
 * Keeps one launcher shortcut per recent order not delivered yet, each opening that order's detail screen.
 * The "Commandes" shortcut is static (res/xml/shortcuts.xml).
 */
@Single
class OrderShortcuts(
    private val context: Context,
    private val scope: CoroutineScope,
    private val observeLatestAwaitingDeliveryUseCase: ObserveLatestAwaitingDeliveryUseCase,
) {

    /** Starts mirroring the orders into shortcuts for the life of the process; called once at startup. */
    fun start() {
        scope.launch {
            observeLatestAwaitingDeliveryUseCase(MAX_ORDER_SHORTCUTS)
                // Every sync emits new Order objects; only what the shortcuts show is worth a (rate-limited) update.
                .distinctUntilChangedBy { orders -> orders.map { it.reference to it.modelName } }
                .collect { orders -> ShortcutManagerCompat.setDynamicShortcuts(context, orders.mapIndexed(::shortcut)) }
        }
    }

    private fun shortcut(rank: Int, order: Order): ShortcutInfoCompat =
        ShortcutInfoCompat.Builder(context, "order_${order.reference}")
            // Some launchers (Pixel's) only ever show the short label, in a popup about 16 characters wide: the
            // reference has to fit there next to the model, hence its short name.
            .setShortLabel("${shortModelName(order.modelName)} · ${order.reference}")
            .setLongLabel(context.getString(R.string.shortcut_order_long, order.modelName, order.reference))
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_order))
            .setIntent(Intent(Intent.ACTION_VIEW, MainActivity.orderUri(order.reference), context, MainActivity::class.java))
            .setRank(rank)
            .build()

    /** "Model 3" -> "M3", "Cybertruck" -> "CT", as owners abbreviate them. */
    private fun shortModelName(modelName: String): String = when {
        modelName.startsWith("Model ") -> "M" + modelName.removePrefix("Model ")
        modelName == "Cybertruck" -> "CT"
        else -> modelName
    }

    private companion object {
        /** With the static one, four shortcuts: what most launchers show. */
        const val MAX_ORDER_SHORTCUTS = 3
    }
}
