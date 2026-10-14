package com.apps.bubblepopping.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.ui.theme.GameColors
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** One star, in fractions of the canvas so any screen size works. */
private class Star(val x: Float, val y: Float, val radiusDp: Float, val phase: Float)

/**
 * The calm background behind menu screens (everything but Home): a night sky
 * that deepens to navy at the top and glows sky blue towards the bottom, with
 * soft blue / cyan / violet glows and a few slowly twinkling stars. Quiet on
 * purpose, so glass cards and text are what the eye lands on.
 */
@Composable
fun NightSky(modifier: Modifier = Modifier) {
    val stars = remember {
        val random = Random(STAR_SEED)
        List(STAR_COUNT) {
            Star(
                x = random.nextFloat(),
                // Mostly in the darker top two thirds, where they show best.
                y = random.nextFloat() * random.nextFloat(),
                radiusDp = 0.6f + random.nextFloat() * 1.4f,
                phase = random.nextFloat() * 2f * PI.toFloat(),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "night_sky")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(TWINKLE_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "twinkle",
    )

    Canvas(modifier) {
        // Base: navy at the top → glowing sky blue at the bottom.
        drawRect(
            Brush.verticalGradient(
                0f to GameColors.NightTop,
                0.55f to GameColors.NightMiddle,
                1f to GameColors.NightBottom,
            )
        )
        // Soft glows: a big sky-blue one low in the middle, cyan bottom-left, violet top-right.
        fun glow(color: Color, alpha: Float, cx: Float, cy: Float, radius: Float) = drawCircle(
            brush = Brush.radialGradient(
                listOf(color.copy(alpha = alpha), Color.Transparent),
                center = Offset(size.width * cx, size.height * cy),
                radius = size.maxDimension * radius,
            ),
            radius = size.maxDimension * radius,
            center = Offset(size.width * cx, size.height * cy),
        )
        glow(GameColors.NightGlow, 0.35f, cx = 0.5f, cy = 0.85f, radius = 0.6f)
        glow(GameColors.NightGlowCyan, 0.18f, cx = 0.1f, cy = 1.0f, radius = 0.4f)
        glow(GameColors.NightGlowViolet, 0.16f, cx = 0.95f, cy = 0.12f, radius = 0.35f)

        // Stars, each twinkling on its own phase.
        for (star in stars) {
            val twinkle = 0.45f + 0.55f * (0.5f + 0.5f * sin(time + star.phase))
            drawCircle(
                color = GameColors.Starlight.copy(alpha = twinkle * STAR_ALPHA),
                radius = star.radiusDp.dp.toPx(),
                center = Offset(star.x * size.width, star.y * size.height),
            )
        }
    }
}

private const val STAR_COUNT = 40
private const val STAR_SEED = 11L
private const val STAR_ALPHA = 0.8f
/** One full twinkle cycle. */
private const val TWINKLE_MS = 4_000

@Preview
@Composable
fun NightSkyPreview() {
    NightSky(Modifier.fillMaxSize())
}
