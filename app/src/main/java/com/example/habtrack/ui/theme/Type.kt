package com.example.habtrack.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Display font ───────────────────────────────────────────────
// The design uses Space Grotesk. To enable it:
//  1. Download Space Grotesk (Regular, Medium, Bold) from fonts.google.com
//  2. Put the .ttf files in app/src/main/res/font/ as:
//       space_grotesk_regular.ttf / space_grotesk_medium.ttf / space_grotesk_bold.ttf
//  3. Uncomment the block below and replace `Display = FontFamily.Default`.
//
// import androidx.compose.ui.text.font.Font
// import com.example.habtrack.R
// val Display = FontFamily(
//     Font(R.font.space_grotesk_regular, FontWeight.Normal),
//     Font(R.font.space_grotesk_medium, FontWeight.Medium),
//     Font(R.font.space_grotesk_bold, FontWeight.Bold),
// )
val Display = FontFamily.Default

val Typography = Typography(
    // Big numerals (hero %, detail numbers)
    displayLarge = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Bold,
        fontSize = 52.sp, lineHeight = 52.sp, letterSpacing = (-1).sp
    ),
    // Screen titles ("today", "analytics")
    headlineMedium = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Bold,
        fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.3).sp
    ),
    // Card titles / habit names
    titleMedium = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Medium,
        fontSize = 15.sp, lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 17.sp
    ),
    // Tracked uppercase micro-labels ("DAILY PROGRESS")
    labelSmall = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Medium,
        fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 1.6.sp
    ),
)
