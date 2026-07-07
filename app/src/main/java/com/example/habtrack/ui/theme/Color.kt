package com.example.habtrack.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// ── Obsidian design tokens ─────────────────────────────────────
object Obsidian {
    val Bg = Color(0xFF0A0C0F)          // app background
    val Surface = Color(0xFF12151A)     // cards
    val Surface2 = Color(0xFF171B21)    // elevated cards
    val Stroke = Color(0x12FFFFFF)      // 7% white borders
    val StrokeSoft = Color(0x0AFFFFFF)  // 4% white dividers

    val TextHi = Color(0xFFE8ECF0)      // primary text
    val TextMid = Color(0xFF8B94A0)     // secondary text
    val TextLow = Color(0xFF5A6470)     // labels / tertiary
    val TextFaint = Color(0xFF3D4550)   // faintest text

    // Curated accent choices (Settings → Accent color)
    val AccentTeal = Color(0xFF57E6C6)
    val AccentViolet = Color(0xFF8B7BFF)
    val AccentAmber = Color(0xFFF2B558)
    val AccentBlue = Color(0xFF6EA8FE)
    val AccentOptions = listOf(
        "Teal" to AccentTeal,
        "Violet" to AccentViolet,
        "Amber" to AccentAmber,
        "Blue" to AccentBlue,
    )

    /** The active accent — observable Compose state; the whole UI
     *  recomposes when the user picks a new color in Settings. */
    var Accent by mutableStateOf(AccentTeal)

    val AccentDim get() = Accent.copy(alpha = 0.12f)     // 12% accent fill
    val AccentBorder get() = Accent.copy(alpha = 0.22f)  // 22% accent border
}

// Legacy Material palette (kept so nothing else breaks)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
