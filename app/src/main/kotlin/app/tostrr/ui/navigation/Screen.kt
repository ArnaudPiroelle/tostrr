package app.tostrr.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** The app's destinations: the back stack is a list of these, saved across process death. */
@Serializable
sealed interface Screen : NavKey {
    @Serializable
    data object Login : Screen

    @Serializable
    data object Orders : Screen

    @Serializable
    data class OrderDetail(val reference: String) : Screen

    @Serializable
    data class Checklist(val reference: String) : Screen

    /** One of the ships that may carry [reference]'s car, by its [ship] name as the tracking site gives it. */
    @Serializable
    data class Ship(val reference: String, val ship: String) : Screen

    @Serializable
    data object Settings : Screen

    @Serializable
    data object Update : Screen
}
