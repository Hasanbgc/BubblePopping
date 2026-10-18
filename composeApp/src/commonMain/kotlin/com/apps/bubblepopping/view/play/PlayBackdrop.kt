package com.apps.bubblepopping.view.play

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.tooling.preview.Preview
import com.apps.bubblepopping.stage.World
import com.apps.bubblepopping.view.SceneArea
import com.apps.bubblepopping.view.drawAuroraScenery
import com.apps.bubblepopping.view.drawScenery
import com.apps.bubblepopping.view.playBackgroundColors
import com.apps.bubblepopping.view.worldMapColors
import kotlin.math.ceil
import kotlin.random.Random

/**
 * Behind a round: the world's colours with its scenery (the same as on the stage
 * map, kept faint so the bubbles stay the focus), a soft vignette framing the
 * play area and a shade towards the bottom. Outside stages, an equipped
 * background cosmetic ([backgroundId]) brings its own colours and scenery:
 * Sunset — sun and clouds, Midnight — stars, Aurora — aurora bands.
 *
 * It never changes during a round, so it is painted once into an image and each
 * frame draws just that image, not several full-screen gradients.
 */
@Composable
fun PlayBackdrop(world: World, backgroundId: String?, modifier: Modifier = Modifier) {
    Spacer(
        modifier.fillMaxSize().drawWithCache {
            val image = ImageBitmap(ceil(size.width).toInt().coerceAtLeast(1), ceil(size.height).toInt().coerceAtLeast(1))
            CanvasDrawScope().draw(this, layoutDirection, Canvas(image), size) {
                drawBackdrop(world, backgroundId)
            }
            onDrawBehind { drawImage(image) }
        }
    )
}

private fun DrawScope.drawBackdrop(world: World, backgroundId: String?) {
    val colors = backgroundId?.let(::playBackgroundColors) ?: worldMapColors(world)
    drawRect(Brush.verticalGradient(colors))

    // The scenery, faded as one layer so overlapping shapes don't show through each other.
    drawIntoCanvas { canvas ->
        canvas.saveLayer(Rect(Offset.Zero, size), Paint().apply { alpha = SCENERY_ALPHA })
        val area = SceneArea(0f, size.height, size.width)
        val random = Random(SCENERY_SEED + world.ordinal)
        when (backgroundId) {
            "bg_sunset" -> drawScenery(World.SKY, area, random)
            "bg_midnight" -> drawScenery(World.SPACE, area, random)
            "bg_aurora" -> drawAuroraScenery(area, random)
            else -> drawScenery(world, area, random)
        }
        canvas.restore()
    }

    // Vignette: darker towards the edges, so the eye rests on the middle.
    val reach = size.maxDimension * 0.75f
    drawRect(
        Brush.radialGradient(
            0.55f to Color.Transparent,
            1f to Color.Black.copy(alpha = VIGNETTE_ALPHA),
            center = Offset(size.width / 2f, size.height * 0.45f),
            radius = reach,
        )
    )
    // A little darker towards the bottom.
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = BOTTOM_SHADE_ALPHA))))
}

/** The scenery stays in the background: softer than on the map. */
private const val SCENERY_ALPHA = 0.7f
private const val VIGNETTE_ALPHA = 0.35f
private const val BOTTOM_SHADE_ALPHA = 0.2f
private const val SCENERY_SEED = 31

@Preview
@Composable
fun PlayBackdropPreview() {
    PlayBackdrop(world = World.OCEAN, backgroundId = null)
}
