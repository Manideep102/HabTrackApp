package com.example.habtrack.ui.theme

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

    val Accent = Color(0xFF57E6C6)      // luminous teal
    val AccentDim = Color(0x1F57E6C6)   // 12% accent fill
    val AccentBorder = Color(0x3857E6C6)// 22% accent border
}

// Legacy Material palette (kept so nothing else breaks)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
