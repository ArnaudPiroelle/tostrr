package app.tostrr.data.layout

import android.content.Context
import app.tostrr.domain.layout.BlockLayout
import app.tostrr.domain.layout.DetailLayoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

/** Keeps the order detail layout as JSON in shared preferences; the default layout is stored as nothing at all. */
@Single
class DetailLayoutRepositoryImpl(context: Context) : DetailLayoutRepository {
    private val prefs = context.getSharedPreferences("tostrr_layouts", Context.MODE_PRIVATE)

    private val _layout = MutableStateFlow(load())
    override val layout: StateFlow<BlockLayout> = _layout.asStateFlow()

    override fun save(layout: BlockLayout) {
        _layout.value = layout
        prefs.edit().apply {
            if (layout.isDefault) remove(KEY_DETAIL) else putString(KEY_DETAIL, json.encodeToString(StoredLayout(layout.order, layout.hidden, layout.collapsed, layout.shown)))
        }.apply()
    }

    /** A missing or unreadable value means the default layout. */
    private fun load(): BlockLayout = prefs.getString(KEY_DETAIL, null)
        ?.let { runCatching { json.decodeFromString<StoredLayout>(it) }.getOrNull() }
        ?.let { BlockLayout(it.order, it.hidden, it.collapsed, it.shown) }
        ?: BlockLayout()

    @Serializable
    private data class StoredLayout(
        val order: List<String> = emptyList(),
        val hidden: List<String> = emptyList(),
        val collapsed: List<String> = emptyList(),
        val shown: List<String> = emptyList(),
    )

    private companion object {
        const val KEY_DETAIL = "order_detail"
        val json = Json { ignoreUnknownKeys = true }
    }
}
