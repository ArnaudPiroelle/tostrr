package app.tostrr.ui.checklist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.domain.checklist.Checklist
import app.tostrr.domain.checklist.ChecklistStep
import app.tostrr.domain.checklist.DeliveryChecklist
import app.tostrr.ui.architecture.OnSideEffect
import app.tostrr.ui.architecture.watchAsState
import app.tostrr.ui.checklist.ChecklistViewModel.Action
import app.tostrr.ui.checklist.ChecklistViewModel.Phase
import app.tostrr.ui.checklist.ChecklistViewModel.SideEffect
import app.tostrr.ui.checklist.components.ConfirmDialog
import app.tostrr.ui.checklist.components.Overview
import app.tostrr.ui.checklist.components.QuestionStep
import app.tostrr.ui.checklist.components.Recap
import app.tostrr.ui.checklist.components.SectionIntro
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ChecklistScreen(
    orderReference: String,
    onClose: () -> Unit,
    viewModel: ChecklistViewModel = koinViewModel { parametersOf(orderReference) },
) {
    val state by viewModel.watchAsState()
    val editingQuestionId = state.editingQuestionId
    val scrollState = rememberScrollState()
    // Each new screen starts at its top, not where the previous one was scrolled to.
    val screenKey = when (val s = state.phase) {
        is Phase.InProgress -> s.checklist.cursor
        is Phase.Completed -> editingQuestionId
        else -> s
    }
    LaunchedEffect(screenKey) { scrollState.scrollTo(0) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    OnSideEffect(viewModel) { effect ->
        if (effect == SideEffect.Deleted) onClose()
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_close)) }
            Text(stringResource(R.string.checklist_title), style = MaterialTheme.typography.titleMedium)
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
            when (val s = state.phase) {
                Phase.Loading -> Unit
                Phase.NotStarted -> Overview(onStart = { viewModel.handle(Action.OnStart) }, onCancel = onClose)
                is Phase.InProgress -> {
                    val cursor = s.checklist.cursor
                    when (val step = DeliveryChecklist.steps[cursor]) {
                        is ChecklistStep.SectionIntro -> SectionIntro(
                            section = step.section,
                            onStart = { viewModel.handle(Action.OnMoveTo(cursor + 1)) },
                            onOpenQuestion = { viewModel.handle(Action.OnMoveTo(DeliveryChecklist.stepIndexOf(it))) },
                            onBack = if (cursor > 0) ({ viewModel.handle(Action.OnMoveTo(cursor - 1)) }) else null,
                        )
                        is ChecklistStep.Question -> QuestionStep(
                            step = step,
                            saved = s.checklist.answers[step.question.id],
                            viewModel = viewModel,
                            onAnswer = { status, comment, photo, replaced ->
                                viewModel.handle(Action.OnAnswer(cursor, step.question.id, status, comment, photo, replaced))
                            },
                            onBack = { viewModel.handle(Action.OnMoveTo(cursor - 1)) },
                        )
                    }
                }
                is Phase.Completed -> {
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
                                viewModel.handle(Action.OnSaveEdit(editing.question.id, status, comment, photo, replaced))
                            },
                            onBack = { viewModel.handle(Action.OnCancelEdit) },
                            editing = true,
                        )
                        BackHandler { viewModel.handle(Action.OnCancelEdit) }
                    } else {
                        Recap(
                            s.checklist,
                            onEdit = { viewModel.handle(Action.OnReopen(0)) },
                            onEditQuestion = { viewModel.handle(Action.OnEditQuestion(it)) },
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
            title = stringResource(R.string.checklist_delete_title),
            text = stringResource(R.string.checklist_delete_text),
            confirm = stringResource(R.string.checklist_delete_confirm),
            onConfirm = {
                confirmDelete = false
                viewModel.handle(Action.OnDelete)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
