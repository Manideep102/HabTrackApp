package com.example.habtrack.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/** Persists the chosen accent color in SharedPreferences. */
object ThemeStore {
    private const val PREFS = "obsidian_theme"
    private const val KEY_ACCENT = "accent_argb"

    /** Call once at startup (before setContent) to restore the saved accent. */
    fun load(context: Context) {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_ACCENT, Obsidian.AccentTeal.toArgb())
        Obsidian.Accent = Color(saved)
    }

    /** Save + apply a new accent. UI updates immediately. */
    fun saveAccent(context: Context, color: Color) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_ACCENT, color.toArgb()).apply()
        Obsidian.Accent = color
    }
}
