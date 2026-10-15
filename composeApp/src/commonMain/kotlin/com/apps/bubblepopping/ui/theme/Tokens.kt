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

    val SlateTop = Color(0xFF6A7396)
    val SlateBottom = Color(0xFF424A6E)
    val SlateLip = Color(0xFF262B45)

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
    val NightNebulaPink = Color(0xFFFF6FD8)
    val NightAurora = Color(0xFF4FFFC4)
    /** Black shade laid over the night sky, so cards and text are what stands out. */
    val BackgroundShade = Color.Black.copy(alpha = 0.4f)

    // Buttons: dark outline (matches the icon art) and the soft shadow under them
    val ButtonOutline = Color(0xFF0B0C24)
    val ButtonShadow = Color.Black.copy(alpha = 0.25f)

    // Resource pill tints: each counter is coloured by what it counts
    val PillNeutral = Color(0xFF8E9BFF)
    val PillCoin = Color(0xFFFFA81F)
    val PillStar = Color(0xFFFFD54F)
    val PillBubble = Color(0xFF5CC2FF)
    val PillHeart = Color(0xFFFF6B8A)
    val PillFreeze = Color(0xFF7FDBFF)
    /** Seconds left on the freeze pill. */
    val FreezeText = Color(0xFFB3E5FC)

    // Shop cards: the common tier glow, and the near-white gold of rim glints
    val TierCommon = Color(0xFFB8D4F0)
    val CardGlint = Color(0xFFFFF6CC)

    // Light enamel panels (Endless cards, stage intro): brighter violet-indigo body
    val EnamelLightTop = Color(0xFF6E5CE6)
    val EnamelLightBottom = Color(0xFF4A38B8)

    // Ocean enamel (stage map world name plates): light cyan body and rim
    val EnamelOceanTop = Color(0xFF3FC6D6)
    val EnamelOceanBottom = Color(0xFF178AA6)
    val OceanTint = Color(0xFF8CF0F5)

    // Leaderboard podium: soft mid-violet cards (between the light enamel and the dialogs) and avatar backgrounds
    val PodiumCardTop = Color(0xFF8A78EE)
    val PodiumCardBottom = Color(0xFF5B48C9)
    val PodiumCardRim = Color(0xFF4A38B8)
    val PodiumAvatarTop = Color(0xFFE9E2FF)
    val PodiumAvatarBottom = Color(0xFFB9ABF8)
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
