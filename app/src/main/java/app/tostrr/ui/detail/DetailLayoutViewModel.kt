package app.tostrr.ui.detail

import androidx.lifecycle.ViewModel
import app.tostrr.domain.layout.BlockLayout
import app.tostrr.domain.layout.DetailBlock
import app.tostrr.domain.layout.usecase.MoveDetailBlockUseCase
import app.tostrr.domain.layout.usecase.ObserveDetailLayoutUseCase
import app.tostrr.domain.layout.usecase.ResetDetailLayoutUseCase
import app.tostrr.domain.layout.usecase.ToggleDetailBlockExpandedUseCase
import app.tostrr.domain.layout.usecase.ToggleDetailBlockUseCase
import kotlinx.coroutines.flow.StateFlow
import org.koin.android.annotation.KoinViewModel

/** The order, visibility and collapsed state of the order detail blocks, as left by the user. */
@KoinViewModel
class DetailLayoutViewModel(
    observeDetailLayoutUseCase: ObserveDetailLayoutUseCase,
    private val moveDetailBlockUseCase: MoveDetailBlockUseCase,
    private val toggleDetailBlockUseCase: ToggleDetailBlockUseCase,
    private val resetDetailLayoutUseCase: ResetDetailLayoutUseCase,
    private val toggleDetailBlockExpandedUseCase: ToggleDetailBlockExpandedUseCase,
) : ViewModel() {
    val layout: StateFlow<BlockLayout> = observeDetailLayoutUseCase()

    fun move(block: DetailBlock, delta: Int) = moveDetailBlockUseCase(block, delta)

    fun toggle(block: DetailBlock) = toggleDetailBlockUseCase(block)

    fun reset() = resetDetailLayoutUseCase()

    fun toggleExpanded(block: DetailBlock) = toggleDetailBlockExpandedUseCase(block)
}
