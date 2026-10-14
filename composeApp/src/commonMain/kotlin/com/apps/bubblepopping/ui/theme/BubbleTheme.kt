package com.apps.bubblepopping.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.apps.bubblepopping.view.BalooFontFamily

/**
 * The six text styles. Titles are drawn with an outline (see `OutlinedText`);
 * everything uses Baloo 2.
 */
@Immutable
data class GameTypography(
    /** Big moments: "STAGE CLEAR!", "WAVE 4". */
    val display: TextStyle,
    /** Screen and dialog titles. */
    val title: TextStyle,
    /** Panel titles, big numbers in lists. */
    val headline: TextStyle,
    /** Normal text. */
    val body: TextStyle,
    /** Buttons, chips, small emphasis. */
    val label: TextStyle,
    /** Hints and fine print. */
    val caption: TextStyle,
)

private fun gameTypography(font: FontFamily) = GameTypography(
    display = TextStyle(fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = 1.sp),
    title = TextStyle(fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = 0.5.sp),
    headline = TextStyle(fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    body = TextStyle(fontFamily = font, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    label = TextStyle(fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = 0.5.sp),
    caption = TextStyle(fontFamily = font, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 17.sp),
)

val LocalGameTypography = staticCompositionLocalOf<GameTypography> {
    error("GameTypography is only available inside BubbleTheme")
}

/** Shortcut: `GameTheme.type.title`. */
object GameTheme {
    val type: GameTypography
        @Composable get() = LocalGameTypography.current
}

/**
 * Wraps the whole app. Material components that remain (text fields, the odd
 * system dialog) pick up Baloo and the game's colours too.
 */
@Composable
fun BubbleTheme(content: @Composable () -> Unit) {
    val font = BalooFontFamily()
    val type = gameTypography(font)
    val material = Typography().let { base ->
        base.copy(
            displayLarge = base.displayLarge.copy(fontFamily = font),
            displayMedium = base.displayMedium.copy(fontFamily = font),
            displaySmall = base.displaySmall.copy(fontFamily = font),
            headlineLarge = base.headlineLarge.copy(fontFamily = font),
            headlineMedium = base.headlineMedium.copy(fontFamily = font),
            headlineSmall = base.headlineSmall.copy(fontFamily = font),
            titleLarge = type.title.copy(fontSize = 22.sp),
            titleMedium = base.titleMedium.copy(fontFamily = font),
            titleSmall = base.titleSmall.copy(fontFamily = font),
            bodyLarge = type.body,
            bodyMedium = base.bodyMedium.copy(fontFamily = font),
            bodySmall = base.bodySmall.copy(fontFamily = font),
            labelLarge = type.label,
            labelMedium = base.labelMedium.copy(fontFamily = font),
            labelSmall = base.labelSmall.copy(fontFamily = font),
        )
    }
    val colors = darkColorScheme(
        primary = GameColors.BlueBottom,
        onPrimary = GameColors.TextPrimary,
        secondary = GameColors.VioletBottom,
        tertiary = GameColors.Gold,
        background = GameColors.Scrim,
        surface = GameColors.DialogBottom,
        surfaceContainerHigh = GameColors.DialogTop,
        onSurface = GameColors.TextPrimary,
        onSurfaceVariant = GameColors.TextSecondary,
        outline = GameColors.GlassRim,
        error = GameColors.Danger,
    )
    MaterialTheme(colorScheme = colors, typography = material) {
        CompositionLocalProvider(LocalGameTypography provides type, content = content)
    }
}
