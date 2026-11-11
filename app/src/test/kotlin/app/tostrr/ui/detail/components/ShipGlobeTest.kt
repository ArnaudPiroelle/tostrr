package app.tostrr.ui.detail.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The globe's gestures, inside a scrolling column as in the order detail. Robolectric drops the second finger of a
 * pinch, so the pinch itself can't be tested here.
 */
@RunWith(RobolectricTestRunner::class)
class ShipGlobeTest {
    @get:Rule val compose = createComposeRule()

    private val voyage = GlobeVoyage(
        listOf(GlobeShip("Glovis Lighthouse", GeoPoint(-30.75, 37.62), 240.0, emptyList(), emptyList(), Color.Red)),
    )
    private val state = GlobeState(voyage.focus)

    private fun show() = compose.setContent {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            // Small enough for the whole globe to fit Robolectric's screen.
            Spacer(Modifier.height(40.dp))
            ShipGlobe(voyage, Modifier.fillMaxWidth().height(340.dp).testTag("globe"), state)
            Spacer(Modifier.height(2000.dp))
        }
    }

    @Test fun `dragging turns the globe`() {
        show()
        val before = state.centerLon

        compose.onNodeWithTag("globe").performTouchInput { swipeLeft() }
        compose.waitForIdle()

        assertTrue("lon ${state.centerLon}", state.centerLon > before + 10)
    }

}
