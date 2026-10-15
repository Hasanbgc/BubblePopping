package com.apps.bubblepopping.view.play

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.apps.bubblepopping.stage.World
import com.apps.bubblepopping.view.SceneArea
import com.apps.bubblepopping.view.drawAuroraScenery
import com.apps.bubblepopping.view.drawScenery
import com.apps.bubblepopping.view.playBackgroundColors
import com.apps.bubblepopping.view.worldMapColors
import kotlin.random.Random

/**
 * Behind a round: the world's colours with its scenery (the same as on the stage
 * map, kept faint so the bubbles stay the focus) and a soft vignette framing
 * the play area. Outside stages, an equipped background cosmetic ([backgroundId])
 * brings its own colours and scenery: Sunset — sun and clouds, Midnight — stars,
 * Aurora — aurora bands. Static, so it's drawn once, not every frame.
 */
@Composable
fun PlayBackdrop(world: World, backgroundId: String?, modifier: Modifier = Modifier) {
    val colors = backgroundId?.let(::playBackgroundColors) ?: worldMapColors(world)
    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(colors))
        }
        Canvas(Modifier.fillMaxSize().alpha(SCENERY_ALPHA)) {
            val area = SceneArea(0f, size.height, size.width)
            val random = Random(SCENERY_SEED + world.ordinal)
            when (backgroundId) {
                "bg_sunset" -> drawScenery(World.SKY, area, random)
                "bg_midnight" -> drawScenery(World.SPACE, area, random)
                "bg_aurora" -> drawAuroraScenery(area, random)
                else -> drawScenery(world, area, random)
            }
        }
        // Vignette: darker towards the edges, so the eye rests on the middle.
        Canvas(Modifier.fillMaxSize()) {
            val reach = size.maxDimension * 0.75f
            drawRect(
                Brush.radialGradient(
                    0.55f to Color.Transparent,
                    1f to Color.Black.copy(alpha = VIGNETTE_ALPHA),
                    center = Offset(size.width / 2f, size.height * 0.45f),
                    radius = reach,
                )
            )
        }
    }
}

/** The scenery stays in the background: softer than on the map. */
private const val SCENERY_ALPHA = 0.7f
private const val VIGNETTE_ALPHA = 0.35f
private const val SCENERY_SEED = 31

@Preview
@Composable
fun PlayBackdropPreview() {
    PlayBackdrop(world = World.OCEAN, backgroundId = null)
}
