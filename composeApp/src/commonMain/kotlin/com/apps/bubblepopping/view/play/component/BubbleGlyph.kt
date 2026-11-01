package com.apps.bubblepopping.view.play.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import com.apps.bubblepopping.Res
import com.apps.bubblepopping.game.BubbleSize
import com.apps.bubblepopping.heart
import com.apps.bubblepopping.skull
import com.apps.bubblepopping.stage.World
import org.jetbrains.compose.resources.painterResource

/**
 * One bubble of [type], drawn exactly as it looks in play, to stand for it in the
 * UI (a stage goal's picture). Smaller [bubbleSize]s draw a smaller bubble on the
 * same floor, so "pop small bubbles" reads at a glance.
 */
@Composable
fun BubbleGlyph(
    type: BubbleType,
    world: World,
    modifier: Modifier = Modifier,
    bubbleSize: BubbleSize = BubbleSize.LARGE,
) {
    val icons = rememberBubbleIcons()
    Canvas(modifier) {
        // A little room around the bubble for its shadow.
        val full = size.minDimension / 2f * FILL
        val r = full * when (bubbleSize) {
            BubbleSize.SMALL -> SMALL_SCALE
            BubbleSize.MEDIUM -> MEDIUM_SCALE
            BubbleSize.LARGE -> 1f
        }
        val bubble = Bubble(
            id = "glyph",
            x = center.x,
            y = center.y + full - r,
            radius = r,
            size = bubbleSize,
            speed = 0f,
            amplitude = 0f,
            baseX = center.x,
            phase = 0f,
            color = Color.Transparent,
            shimmerAngle = SHIMMER_ANGLE,
            type = type,
        )
        drawGameBubble(bubble, world, icons)
    }
}

/** The heart and skull art some bubbles wear, rasterised once. */
@Composable
fun rememberBubbleIcons(): BubbleIcons {
    val heart = rememberIconBitmap(painterResource(Res.drawable.heart))
    val skull = rememberIconBitmap(painterResource(Res.drawable.skull))
    return remember(heart, skull) { BubbleIcons(heart = heart, skull = skull) }
}

// Rasterises a Painter into an ImageBitmap once, then caches it.
// drawImage in a DrawScope is reliable on all Compose Multiplatform targets;
// painter.draw() directly in a Canvas DrawScope is not.
@Composable
private fun rememberIconBitmap(painter: Painter, sizePx: Int = 256): ImageBitmap {
    val density = LocalDensity.current
    return remember(painter, sizePx) {
        val bitmap = ImageBitmap(sizePx, sizePx)
        CanvasDrawScope().draw(
            density         = density,
            layoutDirection = LayoutDirection.Ltr,
            canvas          = Canvas(bitmap),
            size            = Size(sizePx.toFloat(), sizePx.toFloat()),
        ) {
            with(painter) { draw(Size(sizePx.toFloat(), sizePx.toFloat())) }
        }
        bitmap
    }
}

private const val FILL = 0.9f
private const val SMALL_SCALE = 0.6f
private const val MEDIUM_SCALE = 0.8f
private const val SHIMMER_ANGLE = -30f
