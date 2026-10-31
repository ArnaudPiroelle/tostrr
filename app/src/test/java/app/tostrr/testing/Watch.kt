package app.tostrr.testing

import app.tostrr.ui.architecture.BaseSideEffect
import app.tostrr.ui.architecture.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/** Subscribes to the state like a screen does, so the use-case flows behind it run; stops with the test. */
@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.watch(viewModel: BaseViewModel<*, *, *>) {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
}

/** The side effects sent from now on, in order. */
@OptIn(ExperimentalCoroutinesApi::class)
fun <E : BaseSideEffect> TestScope.sideEffects(viewModel: BaseViewModel<*, *, E>): List<E> {
    val received = mutableListOf<E>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.sideEffect.collect { received += it } }
    return received
}
