package com.noah.demo.ui.game

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Colours and type for Button 3, defined explicitly rather than read from
 * MaterialTheme.
 *
 * The app theme enables dynamic colour, which derives the whole palette from the
 * device wallpaper. That is fine for the other screens but would mean this one
 * looked different on every device, so the arcade surface pins its own values.
 *
 * The accents are deliberately not invented: they are the body colours of the four
 * sprites, so the screen is coloured by its own artwork.
 */
object Arcade {
    /** Inside of the tube. Not pure black; a powered CRT still glows faintly. */
    val Void = Color(0xFF07090C)
    val Panel = Color(0xFF0E1218)
    val Bezel = Color(0xFF1A2029)
    val Marquee = Color(0xFFF2F5FF)
    val Dim = Color(0xFF4A5568)
    val Danger = Color(0xFFE8453C)

    val MarqueeText = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        letterSpacing = 6.sp,
    )

    val ScoreText = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = 2.sp,
    )

    val LabelText = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 2.sp,
    )

    val CountdownText = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 76.sp,
        letterSpacing = 4.sp,
    )
}

/** Controls how fast the sequence is replayed. */
enum class Difficulty(val label: String, val flashMs: Long, val gapMs: Long) {
    EASY("EASY", 620L, 280L),
    MEDIUM("MEDIUM", 420L, 180L),
    HARD("HARD", 260L, 110L),
}
