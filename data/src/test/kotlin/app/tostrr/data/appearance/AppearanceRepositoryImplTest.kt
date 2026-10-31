package app.tostrr.data.appearance

import android.content.Context
import app.tostrr.domain.appearance.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AppearanceRepositoryImplTest {
    private val context = RuntimeEnvironment.getApplication()
    private val prefs = context.getSharedPreferences("tostrr_settings", Context.MODE_PRIVATE)

    @Test fun `the app follows the phone until told otherwise`() {
        assertEquals(ThemeMode.SYSTEM, AppearanceRepositoryImpl(context).themeMode.value)
    }

    @Test fun `the chosen theme is applied at once and read back after a restart`() {
        val repository = AppearanceRepositoryImpl(context)
        repository.saveThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repository.themeMode.value)
        assertEquals(ThemeMode.DARK, AppearanceRepositoryImpl(context).themeMode.value)
    }

    @Test fun `following the phone again stores nothing`() {
        val repository = AppearanceRepositoryImpl(context)
        repository.saveThemeMode(ThemeMode.LIGHT)
        repository.saveThemeMode(ThemeMode.SYSTEM)
        assertTrue(prefs.all.isEmpty())
    }

    @Test fun `an unknown stored theme means following the phone`() {
        prefs.edit().putString("theme_mode", "SEPIA").commit()
        assertEquals(ThemeMode.SYSTEM, AppearanceRepositoryImpl(context).themeMode.value)
    }
}
