package com.example.appmanagement.util

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

private const val PREFS_NAME = "app_settings"
private const val KEY_DARK_MODE_ENABLED = "dark_mode_enabled"

object ThemeUtils {

    fun applySavedTheme(context: Context) {
        val shouldUseDarkTheme = isDarkModeSelected(context)
        applyTheme(shouldUseDarkTheme)
    }

    fun applyTheme(useDarkMode: Boolean) {
        val desiredMode = if (useDarkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        if (AppCompatDelegate.getDefaultNightMode() != desiredMode) {
            AppCompatDelegate.setDefaultNightMode(desiredMode)
        }
    }

    fun persistDarkMode(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DARK_MODE_ENABLED, enabled)
            .apply()
    }

    fun isDarkModeSelected(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return if (prefs.contains(KEY_DARK_MODE_ENABLED)) {
            prefs.getBoolean(KEY_DARK_MODE_ENABLED, false)
        } else {
            isSystemInDarkMode(context)
        }
    }

    private fun isSystemInDarkMode(context: Context): Boolean {
        val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }
}
