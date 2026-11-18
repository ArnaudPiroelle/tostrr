package app.tostrr.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/** The picker is exported for the launcher, so any app can start it: it only ever configures a widget of this app. */
@RunWith(RobolectricTestRunner::class)
class OrderWidgetConfigActivityTest {

    private fun start(widgetId: Int?): Activity {
        val intent = Intent(RuntimeEnvironment.getApplication(), OrderWidgetConfigActivity::class.java)
        widgetId?.let { intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, it) }
        return Robolectric.buildActivity(OrderWidgetConfigActivity::class.java, intent).create().get()
    }

    @Test fun `a widget id that isn't one of this app's widgets closes it, unconfigured`() {
        val activity = start(4242)
        assertTrue(activity.isFinishing)
        assertEquals(Activity.RESULT_CANCELED, shadowOf(activity).resultCode)
    }

    @Test fun `no widget id closes it, unconfigured`() {
        val activity = start(null)
        assertTrue(activity.isFinishing)
        assertEquals(Activity.RESULT_CANCELED, shadowOf(activity).resultCode)
    }
}
