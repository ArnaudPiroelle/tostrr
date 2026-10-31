package app.tostrr.domain.appearance.usecase

import app.tostrr.domain.appearance.AppearanceRepository
import app.tostrr.domain.appearance.ThemeMode
import org.koin.core.annotation.Factory

/** Saves the app's theme; every screen switches to it at once. */
@Factory
class SetThemeModeUseCase(private val repository: AppearanceRepository) {
    operator fun invoke(mode: ThemeMode) = repository.saveThemeMode(mode)
}
