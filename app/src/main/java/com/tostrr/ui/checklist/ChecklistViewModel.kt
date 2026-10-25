package com.tostrr.ui.checklist

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tostrr.data.ChecklistRepository
import com.tostrr.domain.AnswerStatus
import com.tostrr.domain.Checklist
import com.tostrr.domain.ChecklistAnswer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import java.io.File

sealed interface ChecklistState {
    data object Loading : ChecklistState
    data object NotStarted : ChecklistState
    data class InProgress(val checklist: Checklist) : ChecklistState
    data class Completed(val checklist: Checklist) : ChecklistState
}

/** Drives the delivery checklist of the order in the `ref` route argument. */
@KoinViewModel
class ChecklistViewModel(
    /** The application context, to read picked images. */
    private val context: Context,
    private val repository: ChecklistRepository,
    private val handle: SavedStateHandle,
) : ViewModel() {
    val orderReference: String = checkNotNull(handle["ref"])

    val state: StateFlow<ChecklistState> = repository.observe(orderReference)
        .map { checklist ->
            when {
                checklist == null -> ChecklistState.NotStarted
                checklist.completed -> ChecklistState.Completed(checklist)
                else -> ChecklistState.InProgress(checklist)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChecklistState.Loading)

    fun start() {
        viewModelScope.launch { repository.start(orderReference) }
    }

    fun moveTo(cursor: Int) {
        viewModelScope.launch { repository.moveTo(orderReference, cursor) }
    }

    fun reopen(cursor: Int) {
        viewModelScope.launch { repository.reopen(orderReference, cursor) }
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
        val answer = newAnswer(questionId, status, comment, photoPath)
        handle[KEY_EDITING] = null
        viewModelScope.launch {
            repository.updateAnswer(orderReference, answer)
            if (replacedPhoto != null && replacedPhoto != photoPath) repository.deletePhoto(replacedPhoto)
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.delete(orderReference)
            onDeleted()
        }
    }

    /**
     * Records the answer on the screen at [cursor] and moves to the next one. [replacedPhoto] is the photo of the
     * answer being overwritten, deleted once the new answer no longer points to it.
     */
    fun answer(cursor: Int, questionId: String, status: AnswerStatus, comment: String, photoPath: String?, replacedPhoto: String?) {
        val answer = newAnswer(questionId, status, comment, photoPath)
        viewModelScope.launch {
            repository.answer(orderReference, answer, cursor + 1)
            if (replacedPhoto != null && replacedPhoto != photoPath) repository.deletePhoto(replacedPhoto)
        }
    }

    fun newPhotoFile(questionId: String): File = repository.newPhotoFile(orderReference, questionId)

    /** Copies an image picked from the gallery and hands back its path. */
    fun importPhoto(questionId: String, uri: Uri, onImported: (String) -> Unit) {
        viewModelScope.launch {
            val input = context.contentResolver.openInputStream(uri) ?: return@launch
            onImported(repository.importPhoto(orderReference, questionId, input))
        }
    }

    fun deletePhoto(path: String) {
        viewModelScope.launch { repository.deletePhoto(path) }
    }

    private fun newAnswer(questionId: String, status: AnswerStatus, comment: String, photoPath: String?) = ChecklistAnswer(
        questionId = questionId,
        status = status,
        comment = comment.trim().ifEmpty { null },
        photoPath = photoPath,
        answeredAtEpochMs = System.currentTimeMillis(),
    )

    private companion object {
        const val KEY_EDITING = "editingQuestionId"
    }
}
