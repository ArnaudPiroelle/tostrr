package app.tostrr.ui.ship.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.tostrr.domain.shipping.Voyage
import app.tostrr.ui.common.GlobeVoyage
import app.tostrr.ui.common.ShipGlobe
import app.tostrr.ui.common.candidateColor
import app.tostrr.ui.common.toGlobe
import app.tostrr.ui.ship.ShipViewModel.State
import app.tostrr.ui.theme.TostrrColors

/**
 * The globe, free to turn and zoom, with a back arrow floating over it, above the ship's bottom sheet: it opens part way, on its header and arrival, and
 * is pulled up for the rest.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShipContent(state: State, voyage: Voyage, onClose: () -> Unit) {
    val color = candidateColor(state.candidateIndex, TostrrColors.Accent)
    val globe = remember(voyage, color) { GlobeVoyage(listOf(voyage.toGlobe(color))) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val peek = maxHeight * PEEK_SHARE
        val globeHeight = maxHeight - peek + SHEET_OVERLAP
        BottomSheetScaffold(
            sheetContent = { ShipSheet(state, voyage, color) },
            sheetPeekHeight = peek,
            sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            sheetContainerColor = TostrrColors.Surface,
            sheetDragHandle = { BottomSheetDefaults.DragHandle(color = TostrrColors.Muted) },
            containerColor = TostrrColors.Background,
        ) {
            Box {
                // Above the sheet while it peeks, its rounded top lying over the globe's lower edge.
                ShipGlobe(globe, Modifier.fillMaxWidth().height(globeHeight).testTag(GLOBE_TAG))
                ShipBackButton(onClose, Modifier.statusBarsPadding().padding(16.dp))
            }
        }
    }
}

internal const val GLOBE_TAG = "ship_globe"
private const val PEEK_SHARE = 0.44f
private val SHEET_OVERLAP = 24.dp
