package app.tostrr.ui.checklist

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tostrr.domain.checklist.AnswerStatus
import app.tostrr.domain.checklist.Checklist
import app.tostrr.domain.checklist.usecase.AnswerChecklistQuestionUseCase
import app.tostrr.domain.checklist.usecase.CreateChecklistPhotoFileUseCase
import app.tostrr.domain.checklist.usecase.DeleteChecklistPhotoUseCase
import app.tostrr.domain.checklist.usecase.DeleteChecklistUseCase
import app.tostrr.domain.checklist.usecase.ImportChecklistPhotoUseCase
import app.tostrr.domain.checklist.usecase.MoveChecklistUseCase
import app.tostrr.domain.checklist.usecase.ObserveChecklistUseCase
import app.tostrr.domain.checklist.usecase.ReopenChecklistUseCase
import app.tostrr.domain.checklist.usecase.StartChecklistUseCase
import app.tostrr.domain.checklist.usecase.UpdateChecklistAnswerUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import java.io.File

sealed interface ChecklistState {
    data object Loading : ChecklistState
    data object NotStarted : ChecklistState
    data class InProgress(val checklist: Checklist) : ChecklistState
    data class Completed(val checklist: Checklist) : ChecklistState
}

/** Drives the delivery checklist of one order. */
@KoinViewModel
class ChecklistViewModel(
    /** Given by the screen, from its navigation key. */
    @InjectedParam val orderReference: String,
    /** The application context, to read picked images. */
    private val context: Context,
    private val handle: SavedStateHandle,
    observeChecklistUseCase: ObserveChecklistUseCase,
    private val startChecklistUseCase: StartChecklistUseCase,
    private val moveChecklistUseCase: MoveChecklistUseCase,
    private val reopenChecklistUseCase: ReopenChecklistUseCase,
    private val answerChecklistQuestionUseCase: AnswerChecklistQuestionUseCase,
    private val updateChecklistAnswerUseCase: UpdateChecklistAnswerUseCase,
    private val deleteChecklistUseCase: DeleteChecklistUseCase,
    private val createChecklistPhotoFileUseCase: CreateChecklistPhotoFileUseCase,
    private val importChecklistPhotoUseCase: ImportChecklistPhotoUseCase,
    private val deleteChecklistPhotoUseCase: DeleteChecklistPhotoUseCase,
) : ViewModel() {
    val state: StateFlow<ChecklistState> = observeChecklistUseCase(orderReference)
        .map { checklist ->
            when {
                checklist == null -> ChecklistState.NotStarted
                checklist.completed -> ChecklistState.Completed(checklist)
                else -> ChecklistState.InProgress(checklist)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChecklistState.Loading)

    fun start() {
        viewModelScope.launch { startChecklistUseCase(orderReference) }
    }

    fun moveTo(cursor: Int) {
        viewModelScope.launch { moveChecklistUseCase(orderReference, cursor) }
    }

    fun reopen(cursor: Int) {
        viewModelScope.launch { reopenChecklistUseCase(orderReference, cursor) }
    }

    /** Question of a finished checklist being changed on its own, from the recap; null on the recap itself. */
    val editingQuestionId: StateFlow<String?> = handle.getStateFlow(KEY_EDITING, null)

    fun editQuestion(questionId: String) {
        handle[KEY_EDITING] = questionId
    }

    fun cancelEdit() {
        handle[KEY_EDITING] = null
    }

    /** Saves the answer changed from the recap and goes back to it; the checklist stays finished. */
    fun saveEdit(questionId: String, status: AnswerStatus, comment: String, photoPath: String?, replacedPhoto: String?) {
        handle[KEY_EDITING] = null
        viewModelScope.launch {
            updateChecklistAnswerUseCase(orderReference, questionId, status, comment, photoPath, replacedPhoto)
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            deleteChecklistUseCase(orderReference)
            onDeleted()
        }
    }

    /**
     * Records the answer on the screen at [cursor] and moves to the next one. [replacedPhoto] is the photo of the
     * answer being overwritten, deleted once the new answer no longer points to it.
     */
    fun answer(cursor: Int, questionId: String, status: AnswerStatus, comment: String, photoPath: String?, replacedPhoto: String?) {
        viewModelScope.launch {
            answerChecklistQuestionUseCase(orderReference, cursor, questionId, status, comment, photoPath, replacedPhoto)
        }
    }

    fun newPhotoFile(questionId: String): File = createChecklistPhotoFileUseCase(orderReference, questionId)

    /** Copies an image picked from the gallery and hands back its path. */
    fun importPhoto(questionId: String, uri: Uri, onImported: (String) -> Unit) {
        viewModelScope.launch {
            val input = context.contentResolver.openInputStream(uri) ?: return@launch
            onImported(importChecklistPhotoUseCase(orderReference, questionId, input))
        }
    }

    fun deletePhoto(path: String) {
        viewModelScope.launch { deleteChecklistPhotoUseCase(path) }
    }

    private companion object {
        const val KEY_EDITING = "editingQuestionId"
    }
}
