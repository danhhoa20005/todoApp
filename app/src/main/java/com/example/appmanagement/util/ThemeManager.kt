package com.example.appmanagement.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * Centralizes logic for persisting and applying the application's dark/light theme.
 * Dark mode is treated as the default to match the app's existing visual style.
 */
object ThemeManager {
    private const val PREFS_NAME = "theme_preferences"
    private const val KEY_IS_DARK = "is_dark_mode"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isDarkMode(context: Context): Boolean =
        prefs(context).getBoolean(KEY_IS_DARK, true)

    fun applySavedTheme(context: Context) {
        apply(isDarkMode(context))
    }

    fun setDarkMode(context: Context, isDark: Boolean) {
        if (isDarkMode(context) == isDark) {
            apply(isDark)
            return
        }
        prefs(context).edit().putBoolean(KEY_IS_DARK, isDark).apply()
        apply(isDark)
    }

    private fun apply(isDark: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
