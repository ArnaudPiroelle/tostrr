package app.tostrr.domain.appearance

import kotlinx.coroutines.flow.StateFlow

/** The user's display preferences. */
interface AppearanceRepository {
    val themeMode: StateFlow<ThemeMode>

    fun saveThemeMode(mode: ThemeMode)
}
