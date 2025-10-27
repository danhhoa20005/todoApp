package com.example.appmanagement.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemePreferences {
    private const val PREFS_NAME = "theme_preferences"
    private const val KEY_NIGHT_MODE = "night_mode"

    fun applySavedTheme(context: Context) {
        AppCompatDelegate.setDefaultNightMode(getSavedMode(context))
    }

    fun isDarkMode(context: Context): Boolean {
        return getSavedMode(context) == AppCompatDelegate.MODE_NIGHT_YES
    }

    fun updateDarkMode(context: Context, enabled: Boolean) {
        val mode = if (enabled) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_NIGHT_MODE, mode)
            .apply()

        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun getSavedMode(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_YES)
    }
}
