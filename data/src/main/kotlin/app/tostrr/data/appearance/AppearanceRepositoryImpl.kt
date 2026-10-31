package app.tostrr.data.appearance

import android.content.Context
import app.tostrr.domain.appearance.AppearanceRepository
import app.tostrr.domain.appearance.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

/** Keeps the display settings in shared preferences, next to the sync ones; the default is stored as nothing at all. */
@Single
class AppearanceRepositoryImpl(context: Context) : AppearanceRepository {
    private val prefs = context.getSharedPreferences("tostrr_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        prefs.getString(KEY_THEME_MODE, null)
            ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
            ?: ThemeMode.SYSTEM,
    )
    override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    override fun saveThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().apply {
            if (mode == ThemeMode.SYSTEM) remove(KEY_THEME_MODE) else putString(KEY_THEME_MODE, mode.name)
        }.apply()
    }

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
    }
}
