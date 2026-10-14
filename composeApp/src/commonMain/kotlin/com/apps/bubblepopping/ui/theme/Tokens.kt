package com.apps.bubblepopping.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * "Bubble Glass" design tokens. Every screen and component takes its colours,
 * spacing and corner sizes from here — never ad-hoc values.
 *
 * Colours are picked from the game's own art: the indigo outline of the logo,
 * the violet ribbon, the gold stars, the sunset pinks of the background.
 */
object GameColors {
    /** Outlines on titles and button text; the darkest colour in the UI. */
    val Ink = Color(0xFF231457)
    val InkSoft = Color(0xFF3B2A84)

    val TextPrimary = Color.White
    val TextSecondary = Color.White.copy(alpha = 0.78f)
    val TextMuted = Color.White.copy(alpha = 0.55f)

    // Glass panels over the sky
    val GlassFill = Color.White.copy(alpha = 0.16f)
    val GlassFillStrong = Color.White.copy(alpha = 0.24f)
    val GlassRim = Color.White.copy(alpha = 0.55f)
    val GlassShine = Color.White.copy(alpha = 0.22f)

    // Dialog body: solid enough to read on any background
    val DialogTop = Color(0xFF5B3FD9)
    val DialogBottom = Color(0xFF3A27A6)

    // Button faces (top → bottom) and their lips
    val BlueTop = Color(0xFF5CC2FF)
    val BlueBottom = Color(0xFF2F7BEA)
    val BlueLip = Color(0xFF1C4AA8)

    val VioletTop = Color(0xFFB57BFF)
    val VioletBottom = Color(0xFF7E3CEB)
    val VioletLip = Color(0xFF5420A8)

    val GreenTop = Color(0xFF7BEA6E)
    val GreenBottom = Color(0xFF33B54A)
    val GreenLip = Color(0xFF1F7A33)

    val GoldTop = Color(0xFFFFE066)
    val GoldBottom = Color(0xFFFFA81F)
    val GoldLip = Color(0xFFC46A00)

    val RedTop = Color(0xFFFF8A8A)
    val RedBottom = Color(0xFFEB3B57)
    val RedLip = Color(0xFFA31D38)

    val GlassButtonTop = Color.White.copy(alpha = 0.30f)
    val GlassButtonBottom = Color.White.copy(alpha = 0.14f)
    val GlassButtonLip = Color(0xFF231457).copy(alpha = 0.45f)

    // Accents
    val Gold = Color(0xFFFFD54F)
    val Pink = Color(0xFFFF6FD8)
    val Success = Color(0xFF7BEA6E)
    val Danger = Color(0xFFFF6B7A)
    val Badge = Color(0xFFFF3D6E)

    /** Darkens the background art behind menus so text stays readable. */
    val Scrim = Color(0xFF14093D)

    // Night sky behind every menu screen except Home: calm and dark so the
    // glass cards and text stand out, glowing sky blue towards the bottom.
    val NightTop = Color(0xFF040A24)
    val NightMiddle = Color(0xFF0A2257)
    val NightBottom = Color(0xFF0F4C91)
    val NightGlow = Color(0xFF3FA9FF)
    val NightGlowCyan = Color(0xFF4FE3FF)
    val NightGlowViolet = Color(0xFF7E5BFF)
    val Starlight = Color(0xFFE8F4FF)
    /** Black shade laid over the night sky, so cards and text are what stands out. */
    val BackgroundShade = Color.Black.copy(alpha = 0.4f)
}

/** The only spacing values used in layouts. */
object Spacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp

    /** Side padding of every screen. */
    val screen = 16.dp
    /** Padding inside panels and dialogs. */
    val panel = 16.dp
}

/** The three corner sizes. */
object Corners {
    val small = RoundedCornerShape(12.dp)
    val medium = RoundedCornerShape(20.dp)
    val large = RoundedCornerShape(28.dp)
    val pill = RoundedCornerShape(50)
}
