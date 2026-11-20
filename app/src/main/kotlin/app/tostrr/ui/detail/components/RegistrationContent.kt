package app.tostrr.ui.detail.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.tostrr.R
import app.tostrr.domain.order.Order
import app.tostrr.domain.order.RegistrationStage
import app.tostrr.domain.order.RegistrationTodo

/** Where the registration stands, whose name it is in, and what is left to do while the order is under way. */
@Composable
internal fun RegistrationContent(order: Order) {
    val info = order.registrationInfo
    KeyValue(stringResource(R.string.detail_registration_stage), info.stage?.let { stringResource(it.labelRes) })
    KeyValue(stringResource(R.string.detail_registration_holder), info.holder)
    KeyValue(stringResource(R.string.detail_registration_co_holder), info.coHolder)
    KeyValue(stringResource(R.string.detail_registration_expected), order.expectedRegistration)
    KeyValue(stringResource(R.string.detail_registration_plate), order.licensePlate)
    // One row per thing to do, the label on the first only, so wrapped items don't run together.
    if (order.awaitingDelivery) {
        info.todo.forEachIndexed { i, todo ->
            KeyValue(if (i == 0) stringResource(R.string.detail_registration_todo) else "", stringResource(todo.labelRes))
        }
    }
}

@get:StringRes
private val RegistrationStage.labelRes: Int
    get() = when (this) {
        RegistrationStage.DETAILS_NEEDED -> R.string.detail_registration_stage_details_needed
        RegistrationStage.DETAILS_SENT -> R.string.detail_registration_stage_details_sent
        RegistrationStage.PREPARING_DOCUMENTS -> R.string.detail_registration_stage_preparing
        RegistrationStage.AWAITING_PROOF -> R.string.detail_registration_stage_awaiting_proof
        RegistrationStage.DOCUMENTS_SENT -> R.string.detail_registration_stage_documents_sent
        RegistrationStage.AWAITING_PLATE -> R.string.detail_registration_stage_awaiting_plate
        RegistrationStage.REGISTERED -> R.string.detail_registration_stage_registered
    }

@get:StringRes
private val RegistrationTodo.labelRes: Int
    get() = when (this) {
        RegistrationTodo.DRIVING_LICENCE -> R.string.detail_registration_todo_licence
        RegistrationTodo.CO_OWNER -> R.string.detail_registration_todo_co_owner
        RegistrationTodo.IDENTITY -> R.string.detail_registration_todo_identity
    }
