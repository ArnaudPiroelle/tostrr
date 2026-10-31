package app.tostrr.ui.architecture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * A screen's ViewModel: the screen reads one [state], sends [Action]s through [handle] and reacts to one-off
 * [sideEffect]s (navigation, launching another app...).
 */
abstract class BaseViewModel<A : BaseAction, S : BaseState, E : BaseSideEffect> : ViewModel() {

    private val internalSideEffect = MutableSharedFlow<E>()

    abstract val state: StateFlow<S>
    val sideEffect = internalSideEffect.asSharedFlow()

    protected abstract suspend fun onHandle(action: A)

    fun handle(action: A) = viewModelScope.launch {
        onHandle(action)
    }

    protected fun dispatchSideEffect(sideEffect: E) = viewModelScope.launch {
        internalSideEffect.emit(sideEffect)
    }
}

/** A [BaseViewModel] that owns its state, starting from [initialState] and changed through [updateState]. */
abstract class BaseViewModel2<A : BaseAction, S : BaseState, E : BaseSideEffect>(initialState: S) :
    BaseViewModel<A, S, E>() {

    private val internalState = MutableStateFlow(initialState)

    override val state = internalState.asStateFlow()

    protected fun updateState(reducer: (S) -> S) {
        internalState.value = reducer(state.value)
    }
}

interface BaseAction
interface BaseState
interface BaseSideEffect

object NoSideEffect : BaseSideEffect
object NoAction : BaseAction

/** The state, collected only while the screen is at least started. */
@Composable
fun <A : BaseAction, S : BaseState, E : BaseSideEffect> BaseViewModel<A, S, E>.watchAsState(): State<S> {
    return state.collectAsStateWithLifecycle()
}

@Composable
fun <A : BaseAction, S : BaseState, E : BaseSideEffect> OnSideEffect(
    viewModel: BaseViewModel<A, S, E>,
    block: suspend CoroutineScope.(sideEffect: E) -> Unit
) {
    LaunchedEffect(viewModel.sideEffect) {
        viewModel.sideEffect.collect {
            block(it)
        }
    }
}
