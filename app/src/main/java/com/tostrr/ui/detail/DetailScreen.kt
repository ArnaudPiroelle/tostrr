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
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Map
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import com.tostrr.domain.checklist.Checklist
import com.tostrr.domain.checklist.DeliveryChecklist
import com.tostrr.domain.deliverycenter.DeliveryCenter
import com.tostrr.domain.deliverycenter.googleMapsUrl
import com.tostrr.domain.order.Change
import com.tostrr.domain.order.OptionInfo
import com.tostrr.domain.order.Order
import com.tostrr.ui.common.StepBarThickness
import com.tostrr.ui.common.StepDot
import com.tostrr.ui.common.StepDotSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import com.tostrr.ui.common.formatDateTime
import com.tostrr.ui.common.formatShortDate
import com.tostrr.ui.common.formatShortTime
import com.tostrr.ui.theme.TostrrColors

@Composable
fun DetailScreen(
    order: Order,
    history: List<Change>,
    options: Map<String, OptionInfo>,
    checklist: Checklist?,
    deliveryCenter: DeliveryCenter?,
    onOpenChecklist: () -> Unit,
    onBack: () -> Unit,
) {
    val scrollState = rememberScrollState()
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
            Hero(order)
            Column(
                Modifier.padding(16.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Once there is an appointment to prepare for; a checklist already started stays reachable even if
                // the appointment later disappears from Tesla's data.
                if (order.hasAppointment || checklist != null) {
                    ChecklistCard(deliveryDay = order.isDeliveryDay(), checklist = checklist, onOpen = onOpenChecklist)
                }
                Section("Informations de livraison", initiallyExpanded = true) { DeliveryContent(order, deliveryCenter) }
                Section("Progression de la commande", initiallyExpanded = true) { ProgressContent(order) }
                Section("Configuration", initiallyExpanded = false) { ConfigurationContent(order, options) }
                Section("Historique des modifications", initiallyExpanded = false) { HistoryContent(history) }
            }
        }
        // Drawn after the scrolling content so it stays pinned on top of it.
        StickyTopBar(title = "Commande #${order.reference}", scrollOffset = { scrollState.value }, onBack = onBack)
    }
}

/**
 * Entry to the delivery checklist. Highlighted on the day of the appointment, when it is what you want open while
 * inspecting the car; otherwise a quieter card, so it can be prepared or reviewed any day.
 */
@Composable
private fun ChecklistCard(deliveryDay: Boolean, checklist: Checklist?, onOpen: () -> Unit) {
    val answered = checklist?.answers?.size ?: 0
    val (subtitle, action) = when {
        checklist == null -> "Contrôlez votre Tesla point par point avant de signer." to "Commencer la checklist"
        checklist.completed -> "Terminée : ${checklist.verifiedPercent} % des points vérifiés." to "Voir le récapitulatif"
        else -> "En cours : $answered / ${DeliveryChecklist.questionCount} points traités." to "Reprendre la checklist"
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (deliveryDay) TostrrColors.Accent.copy(alpha = 0.12f) else TostrrColors.Surface,
        border = if (deliveryDay) null else BorderStroke(0.5.dp, TostrrColors.Border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = CircleShape, color = TostrrColors.Accent, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Filled.FactCheck,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Column {
                    Text(
                        if (deliveryDay) "C'est le jour de la livraison" else "Checklist de livraison",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TostrrColors.Muted)
                }
            }
            if (deliveryDay || checklist != null) {
                Button(onClick = onOpen, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text(action)
                }
            } else {
                OutlinedButton(onClick = onOpen, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text(action)
                }
            }
        }
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
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(order.modelName, Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
                    if (order.status.isNotBlank()) {
                        Surface(shape = RoundedCornerShape(50), color = TostrrColors.Accent.copy(alpha = 0.2f)) {
                            Text(
                                statusLabel(order.status),
                                Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = TostrrColors.OnSurface,
                            )
                        }
                    }
                }
                Text(
                    "Commande #${order.reference}",
                    style = MaterialTheme.typography.titleMedium,
                    color = TostrrColors.Muted,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    "VIN: ${order.vin ?: "inconnu"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TostrrColors.Muted,
                    modifier = Modifier.padding(top = 12.dp),
                )
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

@Composable
private fun ConfigurationContent(order: Order, options: Map<String, OptionInfo>) {
    val codes = order.visibleOptionCodes
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

/** The delivery centre; a tap opens it in Google Maps, at its exact address when the centre is known. */
@Composable
private fun DeliveryCenterRow(name: String, center: DeliveryCenter?) {
    val uriHandler = LocalUriHandler.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { runCatching { uriHandler.openUri(googleMapsUrl(name, center)) } }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text("Centre de livraison", Modifier.weight(0.45f), style = MaterialTheme.typography.bodyMedium, color = TostrrColors.Muted)
        Column(Modifier.weight(0.55f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Map, contentDescription = null, tint = TostrrColors.Accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Ouvrir dans Google Maps", style = MaterialTheme.typography.bodyMedium, color = TostrrColors.Accent)
            }
        }
    }
}

@Composable
private fun DeliveryContent(order: Order, center: DeliveryCenter?) {
    order.deliveryCenter?.takeIf { it.isNotBlank() }?.let { DeliveryCenterRow(it, center) }
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
    history.take(MAX_HISTORY_ROWS).forEachIndexed { i, change ->
        if (i > 0) HorizontalDivider(thickness = 0.5.dp, color = TostrrColors.Border)
        ChangeRow(change)
    }
    if (history.size > MAX_HISTORY_ROWS) {
        Text(
            "… et ${history.size - MAX_HISTORY_ROWS} autres changements plus anciens",
            Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = TostrrColors.Muted,
        )
    }
}

private const val MAX_HISTORY_ROWS = 100

/** One field-level change between two syncs. A missing old value means the field appeared, a missing new one that it vanished. */
@Composable
private fun ChangeRow(change: Change) {
    val added = change.oldValue == null
    val removed = change.newValue == null
    val positive = TostrrColors.Positive
    val negative = TostrrColors.Negative
    val muted = TostrrColors.Muted
    var showDetail by rememberSaveable { mutableStateOf(false) }

    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.width(74.dp)) {
            Text(formatShortDate(change.timestampEpochMs), style = MaterialTheme.typography.labelSmall, color = muted)
            Text(formatShortTime(change.timestampEpochMs), style = MaterialTheme.typography.labelSmall, color = muted)
        }
        Icon(
            imageVector = when {
                added -> Icons.Filled.Add
                removed -> Icons.Filled.Remove
                else -> Icons.Filled.SwapHoriz
            },
            contentDescription = when {
                added -> "Ajouté"
                removed -> "Supprimé"
                else -> "Modifié"
            },
            modifier = Modifier.size(16.dp),
            tint = muted,
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                breakablePath(change.key),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            // Objects and arrays are summarised ("{9 champs}", "{}") and inspected through the Détail button.
            // Remembered: a structured side can hold up to 120 kB of JSON, too much to parse on every recomposition.
            val oldText = remember(change) { change.oldValue?.let { summarize(it, change.structured) } }
            val newText = remember(change) { change.newValue?.let { summarize(it, change.structured) } }
            when {
                // A scalar that vanished: the key alone says it all. A vanished container shows what it held.
                removed -> if (change.structured) ValueLine(oldText.orEmpty(), negative)
                added -> newText?.takeIf { it != "∅" }?.let { ValueLine(it, positive) }
                // Old and new on separate lines: "old → new" on one line wraps mid-value on a phone.
                else -> Column(Modifier.padding(top = 2.dp)) {
                    Text(
                        oldText.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = negative,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = muted)) { append("→ ") }
                            withStyle(SpanStyle(color = positive)) { append(newText.orEmpty()) }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (change.structured) {
                // Small pill, same shape language as the status badge in the hero.
                AssistChip(
                    onClick = { showDetail = true },
                    // labelLarge is sans-serif; labelMedium is monospace in this theme and makes the pill look like code.
                    label = { Text("Afficher les détails", style = MaterialTheme.typography.labelLarge) },
                    leadingIcon = { Icon(Icons.Filled.DataObject, contentDescription = null, Modifier.size(16.dp)) },
                    shape = RoundedCornerShape(50),
                    border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.padding(top = 8.dp).height(30.dp),
                )
            }
        }
    }
    if (showDetail) ChangeDetailSheet(change) { showDetail = false }
}

@Composable
private fun ValueLine(text: String, color: Color) {
    Text(
        text,
        Modifier.padding(top = 2.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Short human summary of a change side: scalars as-is, containers as "{n champs}" / "[n éléments]" or "{}" / "[]". */
private fun summarize(value: String, structured: Boolean): String {
    if (!structured) return value.ifEmpty { "∅" }
    return when (val el = runCatching { Json.parseToJsonElement(value) }.getOrNull()) {
        is JsonObject -> if (el.isEmpty()) "{}" else "{${el.size} champs}"
        is JsonArray -> if (el.isEmpty()) "[]" else "[${el.size} éléments]"
        // Scalar side of a structured change, or a container too large to have been stored whole.
        else -> when {
            value.isEmpty() -> "∅"
            value.startsWith("{") -> "{…}"
            value.startsWith("[") -> "[…]"
            else -> value
        }
    }
}

/** Zero-width spaces after the dots let a long key path wrap at a segment boundary. */
private fun breakablePath(path: String) = path.replace(".", ".\u200B")

private val PrettyJson = Json { prettyPrint = true }

private fun prettyJson(text: String): String =
    runCatching { PrettyJson.encodeToString(JsonElement.serializer(), Json.parseToJsonElement(text)) }.getOrDefault(text)

/** Bottom sheet showing the JSON that changed, before and after. Opens half-height, drags up to full, swipes down to close. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangeDetailSheet(change: Change, onDismiss: () -> Unit) {
    val hasOld = change.oldValue != null
    val hasNew = change.newValue != null
    var showAfter by rememberSaveable { mutableStateOf(!hasOld) }
    val lines = remember(change, showAfter) {
        prettyJson((if (showAfter) change.newValue else change.oldValue).orEmpty()).lines()
    }
    val color = if (showAfter) TostrrColors.Positive else TostrrColors.Negative

    // Opens half-height; drag up to expand to full screen.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    // Full screen: no drag handle (the × closes it), and the content starts below the status bar.
    val expanded = sheetState.targetValue == SheetValue.Expanded

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = if (expanded) null else ({ BottomSheetDefaults.DragHandle() }),
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .then(if (expanded) Modifier.statusBarsPadding() else Modifier)
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    breakablePath(change.key),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Fermer") }
            }
            if (hasOld && hasNew) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !showAfter, onClick = { showAfter = false }, label = { Text("Avant") })
                    FilterChip(selected = showAfter, onClick = { showAfter = true }, label = { Text("Après") })
                }
            }
            LazyColumn(
                Modifier.weight(1f).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            ) {
                items(lines) { line ->
                    Text(
                        line,
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp),
                        color = color,
                    )
                }
            }
        }
    }
}
