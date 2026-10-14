package com.tostrr.ui.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import com.tostrr.data.OptionInfo
import com.tostrr.ui.common.StepBarThickness
import com.tostrr.ui.common.StepDot
import com.tostrr.ui.common.StepDotSize
import com.tostrr.domain.Change
import com.tostrr.domain.Order
import com.tostrr.ui.common.formatDateTime
import com.tostrr.ui.theme.TostrrColors

@Composable
fun DetailScreen(order: Order, history: List<Change>, options: Map<String, OptionInfo>, onBack: () -> Unit) {
    val scrollState = rememberScrollState()
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
            Hero(order)
            Column(
                Modifier.padding(16.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Section("Informations de livraison", initiallyExpanded = true) { DeliveryContent(order) }
                Section("Progression de la commande", initiallyExpanded = true) { ProgressContent(order) }
                Section("Configuration", initiallyExpanded = false) { ConfigurationContent(order, options) }
                Section("Historique des modifications", initiallyExpanded = false) { HistoryContent(history) }
            }
        }
        // Drawn after the scrolling content so it stays pinned on top of it.
        StickyTopBar(title = "Commande #${order.reference}", scrollOffset = { scrollState.value }, onBack = onBack)
    }
}

/** Scroll distance (px) over which the top bar goes from transparent to fully opaque. */
private const val TOP_BAR_FADE_PX = 240f

/** Pinned top bar: transparent over the hero, then blends into the screen colour as content scrolls under it. */
@Composable
private fun StickyTopBar(title: String, scrollOffset: () -> Int, onBack: () -> Unit) {
    val background = MaterialTheme.colorScheme.background
    // Alpha is read while drawing, so scrolling doesn't recompose the bar.
    fun progress() = (scrollOffset() / TOP_BAR_FADE_PX).coerceIn(0f, 1f)

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .drawBehind { drawRect(background.copy(alpha = progress())) }
                .statusBarsPadding()
                .height(TOP_BAR_HEIGHT),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
            }
            Text(
                title,
                Modifier.graphicsLayer { alpha = progress() },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
        }
        // Separates the opaque bar from the screen background; fades in with the bar so the hero stays clean.
        HorizontalDivider(
            Modifier.graphicsLayer { alpha = progress() },
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

private val TOP_BAR_HEIGHT = 48.dp

private val HERO_IMAGE_HEIGHT = 175.dp
private const val HERO_IMAGE_ZOOM = 1.7f

@Composable
private fun Hero(order: Order) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(TostrrColors.Accent.copy(alpha = 0.10f), MaterialTheme.colorScheme.background),
                ),
            ),
    ) {
        Column(Modifier.statusBarsPadding().padding(bottom = 8.dp)) {
            // Room for the pinned top bar (StickyTopBar); the car starts right below it.
            Spacer(Modifier.height(TOP_BAR_HEIGHT))
            AsyncImage(
                model = order.imageUrl,
                contentDescription = order.modelName,
                // The compositor image has transparent margins on every side (the car fills ~half of it).
                // Zoom it from the centre and let the box crop the empty edges.
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HERO_IMAGE_HEIGHT)
                    .clipToBounds()
                    .graphicsLayer(scaleX = HERO_IMAGE_ZOOM, scaleY = HERO_IMAGE_ZOOM),
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.Center,
            )
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(order.modelName, style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Commande #${order.reference}",
                    style = MaterialTheme.typography.titleMedium,
                    color = TostrrColors.Muted,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (order.status.isNotBlank()) {
                        Surface(shape = RoundedCornerShape(50), color = TostrrColors.Accent.copy(alpha = 0.2f)) {
                            Text(
                                statusLabel(order.status),
                                Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = TostrrColors.OnSurface,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        "VIN: ${order.vin ?: "inconnu"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TostrrColors.Muted,
                    )
                }
                Text(
                    "Mise à jour ${formatDateTime(order.fetchedAtEpochMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TostrrColors.Muted,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

private fun statusLabel(status: String): String = when (status) {
    "BOOKED" -> "Commandée"
    "DELIVERED" -> "Livrée"
    "CANCELLED" -> "Annulée"
    else -> status
}

@Composable
private fun Section(title: String, initiallyExpanded: Boolean, content: @Composable () -> Unit) {
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = TostrrColors.Surface,
        border = BorderStroke(0.5.dp, TostrrColors.Border),
        modifier = Modifier.fillMaxWidth().animateContentSize(),
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Icon(
                    if (expanded) Icons.Filled.Remove else Icons.Filled.Add,
                    contentDescription = null,
                    tint = TostrrColors.Muted,
                )
            }
            if (expanded) Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) { content() }
        }
    }
}

@Composable
private fun KeyValue(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Text(label, Modifier.weight(0.45f), style = MaterialTheme.typography.bodyMedium, color = TostrrColors.Muted)
        Text(value, Modifier.weight(0.55f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Empty(text: String) = Text(text, style = MaterialTheme.typography.bodyMedium, color = TostrrColors.Muted)

/** Internal model markers that the official app doesn't list as options. */
private val HIDDEN_OPTION_CODES = setOf("MDL3", "MDLY", "MDLX", "MDLS")

@Composable
private fun ConfigurationContent(order: Order, options: Map<String, OptionInfo>) {
    val codes = order.optionCodes.map { it.removePrefix("$").uppercase() }
        .filter { it !in HIDDEN_OPTION_CODES }
        .distinct()
        .sorted()
    if (codes.isEmpty()) return Empty("Aucune option disponible.")
    // Light list: description first, code as a quiet tag on the right. No inner frame or dividers.
    codes.forEach { code ->
        val info = options[code]
        Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
            Text(
                info?.label ?: "Description indisponible",
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = if (info != null) TostrrColors.OnSurface else TostrrColors.Muted,
            )
            Text(
                code,
                Modifier.padding(start = 12.dp, top = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = TostrrColors.Muted,
            )
        }
    }
}

@Composable
private fun DeliveryContent(order: Order) {
    KeyValue("Centre de livraison", order.deliveryCenter)
    KeyValue("Rendez-vous", order.appointment)
    KeyValue("Fenêtre de livraison", order.deliveryWindow)
    KeyValue("Arrivée au centre", order.etaToDeliveryCenter)
    KeyValue("Immatriculation prévue", order.expectedRegistration)
    if (listOf(order.deliveryCenter, order.appointment, order.deliveryWindow, order.etaToDeliveryCenter, order.expectedRegistration).all { it.isNullOrBlank() }) {
        Empty("Aucune information de livraison pour le moment.")
    }
}

@Composable
private fun ProgressContent(order: Order) {
    val current = order.currentStepIndex
    val track = TostrrColors.Track
    val accent = TostrrColors.Accent
    val rowHeight = 44.dp
    val dotSize = StepDotSize
    val lastIndex = order.steps.lastIndex

    // The bar is drawn behind the rows, from the first dot to the last; the part up to the current step is filled.
    Column(
        Modifier.drawBehind {
            val x = dotSize.toPx() / 2
            val top = rowHeight.toPx() / 2
            val stroke = StepBarThickness.toPx()
            drawLine(track, Offset(x, top), Offset(x, top + rowHeight.toPx() * lastIndex), stroke, StrokeCap.Round)
            if (current > 0) {
                drawLine(accent, Offset(x, top), Offset(x, top + rowHeight.toPx() * current), stroke, StrokeCap.Round)
            }
        },
    ) {
        order.steps.forEachIndexed { i, step ->
            Row(Modifier.fillMaxWidth().height(rowHeight), verticalAlignment = Alignment.CenterVertically) {
                StepDot(done = step.done, current = i == current)
                Spacer(Modifier.width(14.dp))
                Text(
                    step.label,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (step.done || i == current) TostrrColors.OnSurface else TostrrColors.Muted,
                )
                Text(
                    step.date.orEmpty().replace("\n", " – "),
                    style = MaterialTheme.typography.labelMedium,
                    color = TostrrColors.Muted,
                )
            }
        }
    }
}

@Composable
private fun HistoryContent(history: List<Change>) {
    if (history.isEmpty()) return Empty("Aucun changement détecté depuis la première synchronisation.")
    history.take(100).forEach { c ->
        Column(Modifier.padding(vertical = 6.dp)) {
            Text(c.key, style = MaterialTheme.typography.labelMedium)
            Text(
                "${c.oldValue?.ifEmpty { "∅" } ?: "∅"} → ${c.newValue?.ifEmpty { "∅" } ?: "∅"}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(formatDateTime(c.timestampEpochMs), style = MaterialTheme.typography.labelSmall, color = TostrrColors.Muted)
        }
    }
}
