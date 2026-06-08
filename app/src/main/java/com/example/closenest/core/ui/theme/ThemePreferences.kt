package com.example.closenest.core.ui.theme

import android.content.Context

class ThemePreferences(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getThemeMode(): ThemeMode {
        val storedValue = sharedPreferences.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return ThemeMode.entries.firstOrNull { it.name == storedValue } ?: ThemeMode.SYSTEM
    }

    fun setThemeMode(themeMode: ThemeMode) {
        sharedPreferences.edit().putString(KEY_THEME_MODE, themeMode.name).apply()
    }

    private companion object {
        const val PREFS_NAME = "closenest_theme_preferences"
        const val KEY_THEME_MODE = "theme_mode"
    }
}
