package app.tostrr.di

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.work.WorkerParameters
import app.tostrr.ui.checklist.ChecklistViewModel
import app.tostrr.ui.detail.DetailViewModel
import app.tostrr.ui.navigation.Screen
import app.tostrr.ui.ship.ShipViewModel
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.ksp.generated.module
import org.koin.test.verify.definition
import org.koin.test.verify.injectedParameters
import org.koin.test.verify.verify

/**
 * Checks that every definition of the app, domain and data modules can be built: the compile-time Koin check runs
 * per Gradle module, so it can't see that a use case's repository interface is implemented in :data.
 */
class KoinGraphTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `every dependency has a definition`() {
        AppModule().module.verify(
            // Supplied by Android / Koin at runtime: androidContext(), WorkManager and each screen's saved state.
            extraTypes = listOf(Context::class, WorkerParameters::class, SavedStateHandle::class),
            // Passed by the screen with parametersOf().
            injections = injectedParameters(
                definition<ChecklistViewModel>(String::class),
                definition<DetailViewModel>(String::class),
                definition<ShipViewModel>(Screen.Ship::class),
            ),
        )
    }
}
