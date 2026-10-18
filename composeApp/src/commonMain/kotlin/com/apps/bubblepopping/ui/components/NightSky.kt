package com.apps.bubblepopping.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.ui.theme.GameColors
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** One star, in fractions of the canvas so any screen size works. */
private class Star(val x: Float, val y: Float, val radiusDp: Float, val phase: Float, val bright: Boolean)

/**
 * The calm background behind menu screens (everything but Home): a night sky
 * that deepens to navy at the top and glows sky blue towards the bottom, with
 * soft coloured nebulae, a faint swaying aurora ribbon, a twinkling starfield (a
 * few bright stars glint), and now and then a shooting star. Quiet on purpose, so
 * glass cards and text are what the eye lands on.
 *
 * The gradient and nebulae never change, so they are painted once into an image.
 * [animated]: the stars twinkle and the aurora sways on the screen's [ambientTime]
 * (drawn only while drawing, so the sky never recomposes the screen). Otherwise
 * the stars and aurora are still, and only the shooting star moves.
 */
@Composable
fun NightSky(modifier: Modifier = Modifier, animated: Boolean = true) {
    if (animated) AnimatedNightSky(modifier) else StillNightSky(modifier)
}

/**
 * The sky for screens that are read rather than watched. It rests as one still
 * image — stars (each at its own brightness), aurora and all — and every
 * [SHOOTING_STAR_EVERY_MS] comes alive for a moment: a shooting star crosses while
 * the aurora swells out and settles back ([AURORA_SWELL_MS]). Frames are drawn only
 * during that moment, so the screen is still the rest of the time.
 */
@Composable
private fun StillNightSky(modifier: Modifier) {
    val stars = rememberStars()
    // Milliseconds into the current moment of motion; negative while the sky rests.
    val moment = remember { mutableFloatStateOf(-1f) }
    LaunchedEffect(Unit) {
        val momentMs = maxOf(AURORA_SWELL_MS, SHOOTING_STAR_FLIGHT_MS)
        var wait = SHOOTING_STAR_FIRST_MS
        while (true) {
            delay(wait)
            val start = withFrameNanos { it }
            while (moment.floatValue < momentMs) {
                withFrameNanos { now -> moment.floatValue = ((now - start) / 1_000_000f).coerceAtMost(momentMs) }
            }
            moment.floatValue = -1f
            wait = (SHOOTING_STAR_EVERY_MS - momentMs).toLong()
        }
    }

    Spacer(
        // Its own layer, so a moment of motion redraws only the sky, not the screen on top of it.
        modifier.graphicsLayer().drawWithCache {
            val width = ceil(size.width).toInt().coerceAtLeast(1)
            val height = ceil(size.height).toInt().coerceAtLeast(1)
            // Without the aurora and stars, which are drawn live while the aurora moves…
            val base = ImageBitmap(width, height)
            CanvasDrawScope().draw(this, layoutDirection, Canvas(base), size) { drawSkyBase() }
            // …and the whole sky at rest.
            val still = ImageBitmap(width, height)
            CanvasDrawScope().draw(this, layoutDirection, Canvas(still), size) {
                drawImage(base)
                drawAuroraRibbon(0f)
                drawStars(stars, 0f)
            }
            onDrawBehind {
                val ms = moment.floatValue
                if (ms < 0f) {
                    drawImage(still)
                    return@onDrawBehind
                }
                drawImage(base)
                // Out to one side and back, easing in and out.
                val swell = (ms / AURORA_SWELL_MS).coerceAtMost(1f)
                drawAuroraRibbon(sin(swell * PI.toFloat()).let { it * it })
                drawStars(stars, 0f)
                val flight = ms / SHOOTING_STAR_FLIGHT_MS
                if (flight <= 1f) drawShootingStar(flight * SHOOTING_STAR_SHARE)
            }
        }
    )
}

/**
 * The full sky for screens with motion: the stars twinkle, the aurora sways and a
 * shooting star crosses now and then, all on the screen's [ambientTime].
 */
@Composable
private fun AnimatedNightSky(modifier: Modifier) {
    val stars = rememberStars()
    val time = ambientTime()

    Spacer(
        // Its own layer, so each tick re-records only the sky, not the screen on top of it.
        modifier.graphicsLayer().drawWithCache {
            val sky = ImageBitmap(ceil(size.width).toInt().coerceAtLeast(1), ceil(size.height).toInt().coerceAtLeast(1))
            CanvasDrawScope().draw(this, layoutDirection, Canvas(sky), size) { drawSkyBase() }
            onDrawBehind {
                val seconds = time.value
                // One full twinkle and aurora sway every TWINKLE_MS.
                val t = seconds * 2f * PI.toFloat() * 1000f / TWINKLE_MS
                drawImage(sky)
                drawAuroraRibbon(sin(t))
                drawStars(stars, t)
                drawShootingStar(ambientLoop(seconds, SHOOTING_STAR_EVERY_MS))
            }
        }
    )
}

@Composable
private fun rememberStars(): List<Star> = remember {
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

/** The still part of the sky: the gradient and the nebulae. */
private fun DrawScope.drawSkyBase() {
    // Base: navy at the top → glowing sky blue at the bottom.
    drawRect(
        Brush.verticalGradient(
            0f to GameColors.NightTop,
            0.55f to GameColors.NightMiddle,
            1f to GameColors.NightBottom,
        )
    )
    // Nebulae: soft coloured clouds.
    fun glow(color: Color, alpha: Float, cx: Float, cy: Float, radius: Float) {
        val center = Offset(size.width * cx, size.height * cy)
        drawCircle(
            brush = Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), center = center, radius = size.maxDimension * radius),
            radius = size.maxDimension * radius,
            center = center,
        )
    }
    glow(GameColors.NightGlow, 0.35f, cx = 0.5f, cy = 0.85f, radius = 0.6f)
    glow(GameColors.NightGlowCyan, 0.2f, cx = 0.1f, cy = 0.95f, radius = 0.4f)
    glow(GameColors.NightGlowViolet, 0.22f, cx = 0.92f, cy = 0.14f, radius = 0.38f)
    glow(GameColors.NightNebulaPink, 0.14f, cx = 0.2f, cy = 0.32f, radius = 0.32f)
}

/** Stars, each twinkling on its own phase ([t] in radians); the bright ones glint. */
private fun DrawScope.drawStars(stars: List<Star>, t: Float) {
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
}

/**
 * A faint, wavy ribbon of green-teal light across the top, bent by [swing]
 * (-1 … 1; 0 is the ribbon at rest).
 */
private fun DrawScope.drawAuroraRibbon(swing: Float) {
    val y = size.height * AURORA_HEIGHT
    val thickness = size.height * 0.06f
    val sway = thickness * 0.6f * swing
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
/** One full twinkle (and aurora sway) cycle. */
private const val TWINKLE_MS = 4_000
/** Where the aurora ribbon sits, as a fraction of the height. */
private const val AURORA_HEIGHT = 0.2f
private const val AURORA_ALPHA = 0.22f
private const val SHOOTING_STAR_EVERY_MS = 7_000
/** On a still sky, the first shooting star comes this soon after the screen opens. */
private const val SHOOTING_STAR_FIRST_MS = 2_000L
/** The part of each cycle the shooting star is flying. */
private const val SHOOTING_STAR_SHARE = 0.12f
/** How long the shooting star takes to cross. */
private const val SHOOTING_STAR_FLIGHT_MS = SHOOTING_STAR_EVERY_MS * SHOOTING_STAR_SHARE
/** On a still sky, how long the aurora takes to swell out and settle back. */
private const val AURORA_SWELL_MS = 2_400f

@Preview
@Composable
fun NightSkyPreview() {
    NightSky(Modifier.fillMaxSize())
}
