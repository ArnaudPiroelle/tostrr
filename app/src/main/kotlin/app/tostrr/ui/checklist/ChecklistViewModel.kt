package app.tostrr.ui.checklist

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.tostrr.domain.checklist.AnswerStatus
import app.tostrr.domain.checklist.Checklist
import app.tostrr.domain.checklist.usecase.AnswerChecklistQuestionUseCase
import app.tostrr.domain.checklist.usecase.CreateChecklistReportUseCase
import app.tostrr.domain.checklist.usecase.CreateChecklistPhotoFileUseCase
import app.tostrr.domain.checklist.usecase.DeleteChecklistPhotoUseCase
import app.tostrr.domain.checklist.usecase.DeleteChecklistUseCase
import app.tostrr.domain.checklist.usecase.ImportChecklistPhotoUseCase
import app.tostrr.domain.checklist.usecase.LockChecklistUseCase
import app.tostrr.domain.checklist.usecase.MoveChecklistUseCase
import app.tostrr.domain.checklist.usecase.ObserveChecklistUseCase
import app.tostrr.domain.checklist.usecase.ReopenChecklistUseCase
import app.tostrr.domain.checklist.usecase.StartChecklistUseCase
import app.tostrr.domain.checklist.usecase.UpdateChecklistAnswerUseCase
import app.tostrr.ui.architecture.BaseAction
import app.tostrr.ui.architecture.BaseSideEffect
import app.tostrr.ui.architecture.BaseState
import app.tostrr.ui.architecture.BaseViewModel
import app.tostrr.ui.checklist.ChecklistViewModel.Action
import app.tostrr.ui.checklist.ChecklistViewModel.SideEffect
import app.tostrr.ui.checklist.ChecklistViewModel.State
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.InjectedParam
import org.koin.android.annotation.KoinViewModel

/** Drives the delivery checklist of one order. */
@KoinViewModel
class ChecklistViewModel(
    /** Given by the screen, from its navigation key. */
    @InjectedParam private val orderReference: String,
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
    private val lockChecklistUseCase: LockChecklistUseCase,
    private val createChecklistReportUseCase: CreateChecklistReportUseCase,
) : BaseViewModel<Action, State, SideEffect>() {

    /** Kept in the saved state, so a question being changed survives the process being killed. */
    private val editingQuestionId = handle.getStateFlow<String?>(KEY_EDITING, null)

    override val state: StateFlow<State> = combine(observeChecklistUseCase(orderReference), editingQuestionId) { checklist, editing ->
        val phase = when {
            checklist == null -> Phase.NotStarted
            checklist.completed -> Phase.Completed(checklist)
            else -> Phase.InProgress(checklist)
        }
        // A locked checklist has no answer to change.
        State(phase, editing.takeUnless { checklist?.locked == true })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), State(editingQuestionId = editingQuestionId.value))

    override suspend fun onHandle(action: Action) {
        when (action) {
            Action.OnStart -> startChecklistUseCase(orderReference)
            is Action.OnMoveTo -> moveChecklistUseCase(orderReference, action.cursor)
            is Action.OnReopen -> reopenChecklistUseCase(orderReference, action.cursor)
            is Action.OnAnswer -> with(action) {
                answerChecklistQuestionUseCase(orderReference, cursor, questionId, status, comment, photoPaths, previousPhotos)
            }
            is Action.OnEditQuestion -> handle[KEY_EDITING] = action.questionId
            Action.OnCancelEdit -> handle[KEY_EDITING] = null
            is Action.OnSaveEdit -> with(action) {
                handle[KEY_EDITING] = null
                updateChecklistAnswerUseCase(orderReference, questionId, status, comment, photoPaths, previousPhotos)
            }
            Action.OnDelete -> {
                deleteChecklistUseCase(orderReference)
                dispatchSideEffect(SideEffect.Deleted)
            }
            is Action.OnTakePhoto -> {
                val file = createChecklistPhotoFileUseCase(orderReference, action.questionId)
                dispatchSideEffect(SideEffect.TakePhoto(action.questionId, file.absolutePath))
            }
            is Action.OnImportPhoto -> {
                val input = context.contentResolver.openInputStream(action.uri) ?: return
                val path = importChecklistPhotoUseCase(orderReference, action.questionId, input)
                dispatchSideEffect(SideEffect.PhotoImported(action.questionId, path))
            }
            is Action.OnDeletePhoto -> deleteChecklistPhotoUseCase(action.path)
            Action.OnLock -> lockChecklistUseCase(orderReference)
            Action.OnOpenReport -> createChecklistReportUseCase(orderReference)?.let { dispatchSideEffect(SideEffect.OpenReport(it.absolutePath)) }
            Action.OnShareReport -> createChecklistReportUseCase(orderReference)?.let { dispatchSideEffect(SideEffect.ShareReport(it.absolutePath)) }
        }
    }

    sealed interface Phase {
        data object Loading : Phase
        data object NotStarted : Phase
        data class InProgress(val checklist: Checklist) : Phase
        data class Completed(val checklist: Checklist) : Phase
    }

    data class State(
        val phase: Phase = Phase.Loading,
        /** Question of a finished checklist being changed on its own, from the recap; null on the recap itself. */
        val editingQuestionId: String? = null,
    ) : BaseState

    sealed class SideEffect : BaseSideEffect {
        data object Deleted : SideEffect()

        /** The file to give the camera for [questionId]'s photo. */
        data class TakePhoto(val questionId: String, val path: String) : SideEffect()

        /** An image picked from the gallery, copied for [questionId]. */
        data class PhotoImported(val questionId: String, val path: String) : SideEffect()

        /** The PDF report of the locked checklist, to show. */
        data class OpenReport(val path: String) : SideEffect()

        /** The PDF report of the locked checklist, to send to another app. */
        data class ShareReport(val path: String) : SideEffect()
    }

    sealed class Action : BaseAction {
        data object OnStart : Action()
        data class OnMoveTo(val cursor: Int) : Action()
        data class OnReopen(val cursor: Int) : Action()

        /**
         * Records the answer on the screen at [cursor] and moves to the next one. [previousPhotos] are the photos of the
         * answer being overwritten; those the new answer no longer has are deleted.
         */
        data class OnAnswer(
            val cursor: Int,
            val questionId: String,
            val status: AnswerStatus,
            val comment: String,
            val photoPaths: List<String>,
            val previousPhotos: List<String>,
        ) : Action()

        data class OnEditQuestion(val questionId: String) : Action()
        data object OnCancelEdit : Action()

        /** Saves the answer changed from the recap and goes back to it; the checklist stays finished. */
        data class OnSaveEdit(
            val questionId: String,
            val status: AnswerStatus,
            val comment: String,
            val photoPaths: List<String>,
            val previousPhotos: List<String>,
        ) : Action()

        data object OnDelete : Action()

        /** Locks the completed checklist; its answers can't change after that. */
        data object OnLock : Action()
        data object OnOpenReport : Action()
        data object OnShareReport : Action()
        data class OnTakePhoto(val questionId: String) : Action()
        data class OnImportPhoto(val questionId: String, val uri: Uri) : Action()
        data class OnDeletePhoto(val path: String) : Action()
    }

    private companion object {
        const val KEY_EDITING = "editingQuestionId"
    }
}
