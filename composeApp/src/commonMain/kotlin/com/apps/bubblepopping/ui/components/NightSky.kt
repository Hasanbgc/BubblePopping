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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.ui.theme.GameColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** One star, in fractions of the canvas so any screen size works. */
private class Star(val x: Float, val y: Float, val radiusDp: Float, val phase: Float, val bright: Boolean)

/**
 * The calm background behind menu screens (everything but Home): a night sky
 * that deepens to navy at the top and glows sky blue towards the bottom, with
 * slowly drifting nebulae, a faint aurora ribbon, a twinkling starfield (a few
 * bright stars glint), and now and then a shooting star. Quiet on purpose, so
 * glass cards and text are what the eye lands on.
 *
 * All motion is read while drawing, so the sky never recomposes the screen.
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
                radiusDp = 0.5f + random.nextFloat() * 1.3f,
                phase = random.nextFloat() * 2f * PI.toFloat(),
                bright = random.nextFloat() < BRIGHT_STAR_SHARE,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "night_sky")
    val time = transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(TWINKLE_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "twinkle",
    )
    val shooting = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(SHOOTING_STAR_EVERY_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "shooting_star",
    )

    Canvas(modifier) {
        val t = time.value
        // Base: navy at the top → glowing sky blue at the bottom.
        drawRect(
            Brush.verticalGradient(
                0f to GameColors.NightTop,
                0.55f to GameColors.NightMiddle,
                1f to GameColors.NightBottom,
            )
        )
        // Nebulae: soft coloured clouds, drifting very slowly.
        fun glow(color: Color, alpha: Float, cx: Float, cy: Float, radius: Float) {
            val center = Offset(size.width * cx, size.height * cy)
            drawCircle(
                brush = Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), center = center, radius = size.maxDimension * radius),
                radius = size.maxDimension * radius,
                center = center,
            )
        }
        val drift = sin(t) * NEBULA_DRIFT
        glow(GameColors.NightGlow, 0.35f, cx = 0.5f, cy = 0.85f, radius = 0.6f)
        glow(GameColors.NightGlowCyan, 0.2f, cx = 0.1f + drift, cy = 0.95f, radius = 0.4f)
        glow(GameColors.NightGlowViolet, 0.22f, cx = 0.92f - drift, cy = 0.14f, radius = 0.38f)
        glow(GameColors.NightNebulaPink, 0.14f, cx = 0.2f - drift, cy = 0.32f, radius = 0.32f)

        drawAuroraRibbon(t)

        // Stars, each twinkling on its own phase; the bright ones glint.
        for (star in stars) {
            val twinkle = 0.45f + 0.55f * (0.5f + 0.5f * sin(t + star.phase))
            val center = Offset(star.x * size.width, star.y * size.height)
            val r = star.radiusDp.dp.toPx() * if (star.bright) 1.4f else 1f
            drawCircle(GameColors.Starlight.copy(alpha = twinkle * STAR_ALPHA), radius = r, center = center)
            if (star.bright) {
                val glint = r * 4f * twinkle
                val glintColor = GameColors.Starlight.copy(alpha = twinkle * 0.7f)
                val w = 1.dp.toPx()
                drawLine(glintColor, Offset(center.x - glint, center.y), Offset(center.x + glint, center.y), strokeWidth = w, cap = StrokeCap.Round)
                drawLine(glintColor, Offset(center.x, center.y - glint), Offset(center.x, center.y + glint), strokeWidth = w, cap = StrokeCap.Round)
            }
        }

        drawShootingStar(shooting.value)
    }
}

/** A faint, wavy ribbon of green-teal light across the top, swaying gently. */
private fun DrawScope.drawAuroraRibbon(t: Float) {
    val y = size.height * AURORA_HEIGHT
    val thickness = size.height * 0.06f
    val sway = thickness * 0.6f * sin(t)
    val ribbon = Path().apply {
        moveTo(-size.width * 0.1f, y + sway)
        quadraticTo(size.width * 0.3f, y - thickness - sway, size.width * 0.55f, y)
        quadraticTo(size.width * 0.8f, y + thickness + sway, size.width * 1.1f, y - sway)
    }
    drawPath(
        ribbon,
        Brush.verticalGradient(
            listOf(Color.Transparent, GameColors.NightAurora.copy(alpha = AURORA_ALPHA), Color.Transparent),
            startY = y - thickness * 1.6f,
            endY = y + thickness * 1.6f,
        ),
        style = Stroke(width = thickness * 2f, cap = StrokeCap.Round),
    )
}

/**
 * Once per cycle ([progress] 0 → 1) a shooting star streaks across the upper
 * sky during the first [SHOOTING_STAR_SHARE] of it; the rest of the time the sky is still.
 */
private fun DrawScope.drawShootingStar(progress: Float) {
    if (progress > SHOOTING_STAR_SHARE) return
    val p = progress / SHOOTING_STAR_SHARE
    val start = Offset(size.width * 0.85f, size.height * 0.08f)
    val angle = (155f * PI / 180f).toFloat()
    val travel = size.width * 0.7f
    val head = Offset(start.x + cos(angle) * travel * p, start.y + sin(angle) * travel * p)
    val tailLength = size.width * 0.18f
    val tail = Offset(head.x - cos(angle) * tailLength, head.y - sin(angle) * tailLength)
    // Fades in and out over its flight.
    val alpha = sin(p * PI.toFloat())
    drawLine(
        Brush.linearGradient(listOf(Color.Transparent, GameColors.Starlight.copy(alpha = alpha)), start = tail, end = head),
        start = tail,
        end = head,
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawCircle(GameColors.Starlight.copy(alpha = alpha), radius = 2.dp.toPx(), center = head)
}

private const val STAR_COUNT = 70
private const val STAR_SEED = 11L
private const val STAR_ALPHA = 0.8f
private const val BRIGHT_STAR_SHARE = 0.08f
/** One full twinkle (and nebula drift) cycle. */
private const val TWINKLE_MS = 4_000
/** How far the nebulae drift, as a fraction of the width. */
private const val NEBULA_DRIFT = 0.04f
/** Where the aurora ribbon sits, as a fraction of the height. */
private const val AURORA_HEIGHT = 0.2f
private const val AURORA_ALPHA = 0.22f
private const val SHOOTING_STAR_EVERY_MS = 7_000
/** The part of each cycle the shooting star is flying. */
private const val SHOOTING_STAR_SHARE = 0.12f

@Preview
@Composable
fun NightSkyPreview() {
    NightSky(Modifier.fillMaxSize())
}
