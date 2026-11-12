package app.tostrr.ui.ship

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import app.tostrr.ui.architecture.watchAsState
import app.tostrr.ui.navigation.Screen
import app.tostrr.ui.ship.components.ShipContent
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/** One ship that may carry the car: on a globe the user can turn and zoom, with its voyage under it. */
@Composable
fun ShipScreen(key: Screen.Ship, onClose: () -> Unit, viewModel: ShipViewModel = koinViewModel { parametersOf(key) }) {
    val state by viewModel.watchAsState()
    val voyage = state.voyage

    // The ship no longer carries the car: nothing left to show.
    LaunchedEffect(voyage == null) {
        if (voyage == null) onClose()
    }

    if (voyage != null) ShipContent(state, voyage, onClose)
}
