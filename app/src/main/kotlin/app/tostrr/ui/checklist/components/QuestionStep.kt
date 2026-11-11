package app.tostrr.ui.checklist.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.tostrr.R
import app.tostrr.domain.checklist.AnswerStatus
import app.tostrr.domain.checklist.ChecklistAnswer
import app.tostrr.domain.checklist.ChecklistStep
import app.tostrr.ui.architecture.OnSideEffect
import app.tostrr.ui.checklist.ChecklistViewModel
import app.tostrr.ui.checklist.ChecklistViewModel.Action
import app.tostrr.ui.checklist.ChecklistViewModel.SideEffect
import app.tostrr.ui.theme.TostrrColors
import java.io.File

/** The draft's photo paths, kept across a configuration change or the camera activity recreating the screen. */
private val PhotosSaver = listSaver<List<String>, String>(save = { it }, restore = { it })

@Composable
internal fun ColumnScope.QuestionStep(
    step: ChecklistStep.Question,
    saved: ChecklistAnswer?,
    viewModel: ChecklistViewModel,
    onAnswer: (status: AnswerStatus, comment: String, photos: List<String>, previous: List<String>) -> Unit,
    onBack: () -> Unit,
    /** Changing one answer of a finished checklist: "Revenir" becomes "Annuler". */
    editing: Boolean = false,
) {
    val questionId = step.question.id
    // Draft for this question, seeded with the answer given earlier when coming back to it.
    var comment by rememberSaveable(questionId) { mutableStateOf(saved?.comment.orEmpty()) }
    var photos by rememberSaveable(questionId, stateSaver = PhotosSaver) { mutableStateOf(saved?.photoPaths.orEmpty()) }
    val savedPhotos = saved?.photoPaths.orEmpty()

    fun addPhoto(path: String) {
        photos = photos + path
    }

    /** Takes a photo out of the draft; one the saved answer doesn't have is deleted at once, as nothing else uses it. */
    fun removePhoto(path: String) {
        photos = photos - path
        if (path !in savedPhotos) viewModel.handle(Action.OnDeletePhoto(path))
    }

    val context = LocalContext.current
    // Survives the camera activity recreating this screen.
    var pendingShot by rememberSaveable { mutableStateOf<String?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val path = pendingShot ?: return@rememberLauncherForActivityResult
        pendingShot = null
        if (taken) addPhoto(path) else File(path).delete()
    }
    val pickImages = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        uris.forEach { viewModel.handle(Action.OnImportPhoto(questionId, it)) }
    }
    OnSideEffect(viewModel) { effect ->
        when (effect) {
            is SideEffect.TakePhoto -> if (effect.questionId == questionId) {
                pendingShot = effect.path
                takePicture.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(effect.path)))
            }
            is SideEffect.PhotoImported -> if (effect.questionId == questionId) addPhoto(effect.path)
            else -> Unit
        }
    }

    val total = step.section.questions.size
    Header(tag = stringResource(step.section.titleRes), title = stringResource(step.question.textRes))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.checklist_progress, step.number, total), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = TostrrColors.Muted)
            Text(stringResource(R.string.checklist_percent, step.number * 100 / total), style = MaterialTheme.typography.bodyMedium, color = TostrrColors.Muted)
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
        FieldLabel(stringResource(R.string.checklist_comment))
        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.height(20.dp))
        FieldLabel(stringResource(R.string.checklist_photo))
        PhotosField(
            photos = photos,
            onTakePhoto = { viewModel.handle(Action.OnTakePhoto(questionId)) },
            onPickImage = { pickImages.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onDelete = ::removePhoto,
        )
        Spacer(Modifier.height(20.dp))
        PrimaryButton(stringResource(R.string.checklist_verified_button)) { onAnswer(AnswerStatus.VERIFIED, comment, photos, savedPhotos) }
        Spacer(Modifier.height(12.dp))
        // A problem is said in the comment and shown in the photos above, before tapping this.
        SecondaryButton(stringResource(R.string.checklist_problem_button), leading = Icons.Filled.ReportProblem) {
            onAnswer(AnswerStatus.PROBLEM, comment, photos, savedPhotos)
        }
        Spacer(Modifier.height(12.dp))
        SecondaryButton(stringResource(R.string.checklist_skip), trailing = Icons.Filled.KeyboardDoubleArrowRight) {
            onAnswer(AnswerStatus.SKIPPED, comment, photos, savedPhotos)
        }
        Spacer(Modifier.height(12.dp))
        if (editing) {
            SecondaryButton(stringResource(R.string.common_cancel), onClick = onBack)
        } else {
            SecondaryButton(stringResource(R.string.checklist_previous), leading = Icons.AutoMirrored.Filled.KeyboardArrowLeft, onClick = onBack)
        }
    }
    if (saved != null) {
        Text(
            stringResource(
                when (saved.status) {
                    AnswerStatus.VERIFIED -> R.string.checklist_already_verified
                    AnswerStatus.PROBLEM -> R.string.checklist_already_problem
                    AnswerStatus.SKIPPED -> R.string.checklist_already_skipped
                },
            ),
            Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = TostrrColors.Muted,
            textAlign = TextAlign.Center,
        )
    }
}
