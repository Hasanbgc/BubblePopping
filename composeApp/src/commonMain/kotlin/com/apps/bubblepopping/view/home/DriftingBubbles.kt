package com.apps.bubblepopping.view.home

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.meta.Cosmetics
import com.apps.bubblepopping.ui.components.ambientTime
import com.apps.bubblepopping.view.play.component.drawSkinPreview
import kotlin.math.sin
import kotlin.random.Random

/** One decorative bubble; positions are fractions of the canvas, so any size works. */
private class Drifter(
    val x: Float,
    val startY: Float,
    val radiusDp: Float,
    /** Canvas heights per second. */
    val speed: Float,
    val swayPhase: Float,
    val spin: Float,
)

/**
 * Soap bubbles rising slowly behind the menus, wrapping from top to bottom
 * forever, faded to [alpha]. Purely decorative — it doesn't take touches. Moves on
 * the screen's [ambientTime] and drawn from the bubble sprites.
 */
@Composable
fun DriftingBubbles(modifier: Modifier = Modifier, count: Int = BUBBLE_COUNT, alpha: Float = 1f) {
    val drifters = remember(count) {
        val random = Random(SEED)
        List(count) {
            Drifter(
                x = random.nextFloat(),
                startY = random.nextFloat(),
                radiusDp = MIN_RADIUS_DP + random.nextFloat() * (MAX_RADIUS_DP - MIN_RADIUS_DP),
                speed = MIN_SPEED + random.nextFloat() * (MAX_SPEED - MIN_SPEED),
                swayPhase = random.nextFloat() * 6.28f,
                spin = (random.nextFloat() - 0.5f) * 40f,
            )
        }
    }
    val clock = ambientTime()

    // Its own layer, so each tick re-records only the bubbles, not the screen on top of them.
    Canvas(modifier.graphicsLayer()) {
        val time = clock.value
        for (d in drifters) {
            val r = d.radiusDp.dp.toPx()
            // Rise and wrap. The span leaves room for the bubble to clear the
            // edge fully: y = span is just below the bottom, y = 0 just above the top.
            val span = 1f + 2 * r / size.height
            val y = ((d.startY * span - d.speed * time) % span + span) % span
            val cy = y * size.height - r
            val cx = d.x * size.width + sin(time * SWAY_SPEED + d.swayPhase) * SWAY_DP.dp.toPx()
            drawSkinPreview(Cosmetics.CLASSIC.id, Offset(cx, cy), r, rotation = time * d.spin, alpha = alpha)
        }
    }
}

private const val BUBBLE_COUNT = 14
private const val SEED = 7L
private const val MIN_RADIUS_DP = 10f
private const val MAX_RADIUS_DP = 34f
private const val MIN_SPEED = 0.025f
private const val MAX_SPEED = 0.06f
private const val SWAY_SPEED = 0.8f
private const val SWAY_DP = 14f
