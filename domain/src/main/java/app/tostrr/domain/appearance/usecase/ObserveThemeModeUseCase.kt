package app.tostrr.domain.appearance.usecase

import app.tostrr.domain.appearance.AppearanceRepository
import app.tostrr.domain.appearance.ThemeMode
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Factory

/** Whether the app follows the phone's theme or is always light or dark. */
@Factory
class ObserveThemeModeUseCase(private val repository: AppearanceRepository) {
    operator fun invoke(): StateFlow<ThemeMode> = repository.themeMode
}
