package app.tostrr.ui.checklist

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import app.tostrr.domain.checklist.AnswerStatus
import app.tostrr.domain.checklist.Checklist
import app.tostrr.domain.checklist.ChecklistAnswer
import app.tostrr.domain.checklist.ChecklistSection
import app.tostrr.domain.checklist.ChecklistStep
import app.tostrr.domain.checklist.DeliveryChecklist
import app.tostrr.ui.common.formatDateTime
import app.tostrr.ui.theme.TostrrColors
import org.koin.androidx.compose.koinViewModel
import java.io.File

@Composable
fun ChecklistScreen(onClose: () -> Unit, viewModel: ChecklistViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    val editingQuestionId by viewModel.editingQuestionId.collectAsState()
    val scrollState = rememberScrollState()
    // Each new screen starts at its top, not where the previous one was scrolled to.
    val screenKey = when (val s = state) {
        is ChecklistState.InProgress -> s.checklist.cursor
        is ChecklistState.Completed -> editingQuestionId
        else -> s
    }
    LaunchedEffect(screenKey) { scrollState.scrollTo(0) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Fermer") }
            Text("Checklist de livraison", style = MaterialTheme.typography.titleMedium)
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (val s = state) {
                ChecklistState.Loading -> Unit
                ChecklistState.NotStarted -> Overview(onStart = viewModel::start, onCancel = onClose)
                is ChecklistState.InProgress -> {
                    val cursor = s.checklist.cursor
                    when (val step = DeliveryChecklist.steps[cursor]) {
                        is ChecklistStep.SectionIntro -> SectionIntro(
                            section = step.section,
                            onStart = { viewModel.moveTo(cursor + 1) },
                            onOpenQuestion = { viewModel.moveTo(DeliveryChecklist.stepIndexOf(it)) },
                            onBack = if (cursor > 0) ({ viewModel.moveTo(cursor - 1) }) else null,
                        )
                        is ChecklistStep.Question -> QuestionStep(
                            step = step,
                            saved = s.checklist.answers[step.question.id],
                            viewModel = viewModel,
                            onAnswer = { status, comment, photo, replaced ->
                                viewModel.answer(cursor, step.question.id, status, comment, photo, replaced)
                            },
                            onBack = { viewModel.moveTo(cursor - 1) },
                        )
                    }
                }
                is ChecklistState.Completed -> {
                    val editing = editingQuestionId?.let { id ->
                        DeliveryChecklist.steps.getOrNull(DeliveryChecklist.stepIndexOf(id)) as? ChecklistStep.Question
                    }
                    if (editing != null) {
                        // Changing one answer of a finished checklist: saving or cancelling goes back to the recap.
                        QuestionStep(
                            step = editing,
                            saved = s.checklist.answers[editing.question.id],
                            viewModel = viewModel,
                            onAnswer = { status, comment, photo, replaced ->
                                viewModel.saveEdit(editing.question.id, status, comment, photo, replaced)
                            },
                            onBack = viewModel::cancelEdit,
                            editing = true,
                        )
                        BackHandler(onBack = viewModel::cancelEdit)
                    } else {
                        Recap(
                            s.checklist,
                            onEdit = { viewModel.reopen(0) },
                            onEditQuestion = viewModel::editQuestion,
                            onDelete = { confirmDelete = true },
                            onClose = onClose,
                        )
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Supprimer la checklist ?",
            text = "Toutes les réponses, commentaires et photos de cette checklist seront effacés.",
            confirm = "Supprimer",
            onConfirm = {
                confirmDelete = false
                viewModel.delete(onDeleted = onClose)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

// --- Overview, shown before the checklist starts ---

@Composable
private fun ColumnScope.Overview(onStart: () -> Unit, onCancel: () -> Unit) {
    Header(tag = "Vérification", title = "Checklist de livraison")
    Text(
        "Contrôlez votre Tesla point par point avant de signer. Pour chaque question : vérifiez ou passez, " +
            "avec un commentaire ou une photo si besoin.",
        Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium,
        color = TostrrColors.Muted,
        textAlign = TextAlign.Center,
    )
    Card {
        DeliveryChecklist.sections.forEachIndexed { i, section ->
            if (i > 0) HorizontalDivider(thickness = 1.dp, color = TostrrColors.Border)
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(section.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                CountPill("${section.questions.size} questions")
            }
        }
        HorizontalDivider(thickness = 1.dp, color = TostrrColors.Border)
        Text(
            "${DeliveryChecklist.questionCount} points de contrôle au total",
            Modifier.fillMaxWidth().padding(top = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = TostrrColors.Muted,
            textAlign = TextAlign.Center,
        )
    }
    PrimaryButton("Commencer", onClick = onStart)
    SecondaryButton("Annuler", onClick = onCancel)
}

// --- Section intro ---

@Composable
private fun ColumnScope.SectionIntro(
    section: ChecklistSection,
    onStart: () -> Unit,
    onOpenQuestion: (questionId: String) -> Unit,
    onBack: (() -> Unit)?,
) {
    var expanded by rememberSaveable(section.id) { mutableStateOf(false) }
    Header(tag = "Vérification", title = "Vérifications « ${section.title} »")
    Card {
        Text(
            "Êtes-vous prêt ? ${section.questions.size} points à contrôler.",
            Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = TostrrColors.Muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Commencer", trailing = Icons.Filled.KeyboardDoubleArrowRight, onClick = onStart)
        Spacer(Modifier.height(16.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, TostrrColors.Border),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().animateContentSize(),
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Voir les vérifications", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Icon(
                        if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TostrrColors.Muted,
                    )
                }
                if (expanded) {
                    Column(Modifier.padding(bottom = 6.dp)) {
                        // A tap jumps straight to that question.
                        section.questions.forEachIndexed { i, question ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenQuestion(question.id) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${i + 1}",
                                    Modifier.width(28.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TostrrColors.Accent,
                                )
                                Text(question.text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = TostrrColors.Muted,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    if (onBack != null) SecondaryButton("Revenir", leading = Icons.AutoMirrored.Filled.KeyboardArrowLeft, onClick = onBack)
}

// --- One question ---

@Composable
private fun ColumnScope.QuestionStep(
    step: ChecklistStep.Question,
    saved: ChecklistAnswer?,
    viewModel: ChecklistViewModel,
    onAnswer: (status: AnswerStatus, comment: String, photo: String?, replaced: String?) -> Unit,
    onBack: () -> Unit,
    /** Changing one answer of a finished checklist: "Revenir" becomes "Annuler". */
    editing: Boolean = false,
) {
    val questionId = step.question.id
    // Draft for this question, seeded with the answer given earlier when coming back to it.
    var comment by rememberSaveable(questionId) { mutableStateOf(saved?.comment.orEmpty()) }
    var photo by rememberSaveable(questionId) { mutableStateOf(saved?.photoPath) }

    /** Sets the draft photo; a previous draft photo nobody else points to is deleted. */
    fun replacePhoto(path: String?) {
        photo?.takeIf { it != saved?.photoPath && it != path }?.let(viewModel::deletePhoto)
        photo = path
    }

    val total = step.section.questions.size
    Header(tag = step.section.title, title = step.question.text)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text("${step.number} / $total", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = TostrrColors.Muted)
            Text("${step.number * 100 / total}%", style = MaterialTheme.typography.bodyMedium, color = TostrrColors.Muted)
        }
        LinearProgressIndicator(
            progress = { step.number.toFloat() / total },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = TostrrColors.Accent,
            trackColor = TostrrColors.Track,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
    Card {
        FieldLabel("Commentaire (facultatif)")
        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.height(20.dp))
        FieldLabel("Photo (facultatif)")
        PhotoField(
            photo = photo,
            newPhotoFile = { viewModel.newPhotoFile(questionId) },
            onImport = { uri -> viewModel.importPhoto(questionId, uri) { replacePhoto(it) } },
            onPhoto = ::replacePhoto,
        )
        Spacer(Modifier.height(20.dp))
        PrimaryButton("J'ai vérifié 👍") { onAnswer(AnswerStatus.VERIFIED, comment, photo, saved?.photoPath) }
        Spacer(Modifier.height(12.dp))
        SecondaryButton("Passer", trailing = Icons.Filled.KeyboardDoubleArrowRight) {
            onAnswer(AnswerStatus.SKIPPED, comment, photo, saved?.photoPath)
        }
        Spacer(Modifier.height(12.dp))
        if (editing) {
            SecondaryButton("Annuler", onClick = onBack)
        } else {
            SecondaryButton("Revenir", leading = Icons.AutoMirrored.Filled.KeyboardArrowLeft, onClick = onBack)
        }
    }
    if (saved != null) {
        Text(
            if (saved.status == AnswerStatus.VERIFIED) "Déjà marqué comme vérifié" else "Déjà passé",
            Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = TostrrColors.Muted,
            textAlign = TextAlign.Center,
        )
    }
}

/** Dashed drop zone with camera and gallery buttons, or the chosen photo with a delete button. */
@Composable
private fun PhotoField(photo: String?, newPhotoFile: () -> File, onImport: (Uri) -> Unit, onPhoto: (String?) -> Unit) {
    val context = LocalContext.current
    // Survives the camera activity recreating this screen.
    var pendingShot by rememberSaveable { mutableStateOf<String?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val path = pendingShot ?: return@rememberLauncherForActivityResult
        pendingShot = null
        if (taken) onPhoto(path) else File(path).delete()
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onImport(uri)
    }
    val accent = TostrrColors.Accent
    val border = TostrrColors.Border

    if (photo != null) {
        Box {
            AsyncImage(
                model = File(photo),
                contentDescription = "Photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(12.dp)),
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            ) {
                IconButton(onClick = { onPhoto(null) }) { Icon(Icons.Filled.Delete, contentDescription = "Supprimer la photo") }
            }
        }
        return
    }

    Column(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = border,
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f))),
                )
            }
            .padding(vertical = 20.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Filled.FileUpload, contentDescription = null, tint = accent, modifier = Modifier.padding(10.dp))
        }
        Text("JPEG · PNG", style = MaterialTheme.typography.labelMedium, color = TostrrColors.Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = {
                val file = newPhotoFile()
                pendingShot = file.absolutePath
                takePicture.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
            }) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Appareil photo")
            }
            TextButton(onClick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Galerie")
            }
        }
    }
}

// --- Recap, once the checklist is done ---

@Composable
private fun ColumnScope.Recap(
    checklist: Checklist,
    onEdit: () -> Unit,
    onEditQuestion: (questionId: String) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    var zoomedPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    val accent = TostrrColors.Accent
    val skipped = checklist.answers.values.count { it.status == AnswerStatus.SKIPPED }

    Header(tag = "Finalisée", title = "Récapitulatif")
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("VÉRIFICATION", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = accent, letterSpacing = 1.sp)
            Text("${checklist.verifiedPercent}%", fontSize = 56.sp, lineHeight = 64.sp, fontWeight = FontWeight.Bold, color = accent)
            Text(
                "${checklist.verifiedCount} / ${DeliveryChecklist.questionCount}",
                style = MaterialTheme.typography.bodyMedium,
                color = TostrrColors.Muted,
            )
        }
    }
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = TostrrColors.Positive.copy(alpha = 0.12f), modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = TostrrColors.Positive, modifier = Modifier.padding(10.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Vérification terminée 🎉", style = MaterialTheme.typography.titleMedium)
                checklist.completedAtEpochMs?.let {
                    Text(formatDateTime(it), style = MaterialTheme.typography.bodyMedium, color = TostrrColors.Muted)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "${checklist.verifiedCount} vérifiés · $skipped passés",
            style = MaterialTheme.typography.bodyMedium,
            color = TostrrColors.Muted,
        )
    }
    DeliveryChecklist.sections.forEach { section ->
        val verified = section.questions.count { checklist.answers[it.id]?.status == AnswerStatus.VERIFIED }
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(section.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                CountPill("$verified / ${section.questions.size}")
            }
            section.questions.forEach { question ->
                RecapRow(
                    question.text,
                    checklist.answers[question.id],
                    onClick = { onEditQuestion(question.id) },
                    onPhoto = { zoomedPhoto = it },
                )
            }
        }
    }
    PrimaryButton("Modifier la checklist", onClick = onEdit)
    SecondaryButton("Supprimer la checklist", leading = Icons.Filled.Delete, onClick = onDelete)
    SecondaryButton("Fermer", onClick = onClose)

    zoomedPhoto?.let { path ->
        Dialog(onDismissRequest = { zoomedPhoto = null }) {
            AsyncImage(
                model = File(path),
                contentDescription = "Photo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { zoomedPhoto = null },
            )
        }
    }
}

/** One question of the recap; a tap opens it alone to change its answer. */
@Composable
private fun RecapRow(text: String, answer: ChecklistAnswer?, onClick: () -> Unit, onPhoto: (String) -> Unit) {
    val (icon, tint, label) = when (answer?.status) {
        AnswerStatus.VERIFIED -> Triple(Icons.Filled.CheckCircle, TostrrColors.Positive, "Vérifié")
        AnswerStatus.SKIPPED -> Triple(Icons.Filled.SkipNext, TostrrColors.Muted, "Passé")
        null -> Triple(Icons.Filled.RadioButtonUnchecked, TostrrColors.Muted, "Non traité")
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text, style = MaterialTheme.typography.bodyMedium)
            answer?.comment?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = TostrrColors.Muted, fontStyle = FontStyle.Italic)
            }
        }
        answer?.photoPath?.let { path ->
            Spacer(Modifier.width(10.dp))
            AsyncImage(
                model = File(path),
                contentDescription = "Photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).clickable { onPhoto(path) },
            )
        }
    }
}

// --- Shared pieces ---

/** Centred pill tag above a large title, as at the top of every checklist screen. */
@Composable
private fun Header(tag: String, title: String) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = RoundedCornerShape(50), color = TostrrColors.Accent.copy(alpha = 0.12f)) {
            Text(
                tag.uppercase(),
                Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                // Kept to one line: the longest section name is too wide for the pill at a larger size.
                style = MaterialTheme.typography.labelLarge,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = TostrrColors.Accent,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = TostrrColors.Surface,
        border = BorderStroke(1.dp, TostrrColors.Border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

@Composable
private fun CountPill(text: String) {
    Surface(shape = RoundedCornerShape(50), color = TostrrColors.SurfaceHigh) {
        Text(
            text,
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = TostrrColors.Muted,
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, Modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun PrimaryButton(text: String, trailing: ImageVector? = null, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp)) {
        Text(text, fontWeight = FontWeight.Bold)
        if (trailing != null) {
            Spacer(Modifier.width(6.dp))
            Icon(trailing, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SecondaryButton(
    text: String,
    leading: ImageVector? = null,
    trailing: ImageVector? = null,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, TostrrColors.Border),
    ) {
        if (leading != null) {
            Icon(leading, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = TostrrColors.OnSurface)
        if (trailing != null) {
            Spacer(Modifier.width(6.dp))
            Icon(trailing, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}
