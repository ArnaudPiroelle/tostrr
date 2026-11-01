package app.tostrr.domain.layout

import kotlinx.coroutines.flow.StateFlow

/** How the user arranged the order detail screen. */
interface DetailLayoutRepository {
    val layout: StateFlow<BlockLayout>

    fun save(layout: BlockLayout)
}
