package app.tostrr.data.layout

import android.content.Context
import app.tostrr.domain.layout.BlockLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DetailLayoutRepositoryImplTest {
    private val context = RuntimeEnvironment.getApplication()
    private val prefs = context.getSharedPreferences("tostrr_layouts", Context.MODE_PRIVATE)

    @Test fun `nothing saved means the default layout`() {
        assertEquals(BlockLayout(), DetailLayoutRepositoryImpl(context).layout.value)
    }

    @Test fun `a saved layout is read back after a restart`() {
        val layout = BlockLayout(order = listOf("payment", "delivery"), hidden = listOf("history"), collapsed = listOf("vehicle"), shown = listOf("progress"))
        val repository = DetailLayoutRepositoryImpl(context)
        repository.save(layout)
        assertEquals(layout, repository.layout.value)
        assertEquals(layout, DetailLayoutRepositoryImpl(context).layout.value)
    }

    @Test fun `going back to the default layout stores nothing`() {
        val repository = DetailLayoutRepositoryImpl(context)
        repository.save(BlockLayout(hidden = listOf("history")))
        repository.save(BlockLayout())
        assertTrue(prefs.all.isEmpty())
    }

    @Test fun `an unreadable value means the default layout`() {
        prefs.edit().putString("order_detail", "{not json").commit()
        assertEquals(BlockLayout(), DetailLayoutRepositoryImpl(context).layout.value)
    }

    @Test fun `a value written by a version with fewer fields still reads`() {
        prefs.edit().putString("order_detail", """{"hidden":["history"],"extra":1}""").commit()
        assertEquals(BlockLayout(hidden = listOf("history")), DetailLayoutRepositoryImpl(context).layout.value)
    }
}
