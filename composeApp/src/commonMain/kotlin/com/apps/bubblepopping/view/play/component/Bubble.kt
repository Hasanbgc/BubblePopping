package com.apps.bubblepopping.view.play.component


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

enum class BubbleType { NORMAL, POISON, HEART }

/**
 * Represents a single bubble in the game.
 *
 * [x] and [y] are var because they are mutated directly every frame
 * by the ViewModel instead of creating a new copy — keeps the game loop
 * allocation-free and GC-pressure low.
 *
 * [baseX] is the "center column" the bubble oscillates around.
 * Breeze shifts [baseX] over time; the actual [x] is then:
 *   x = baseX + sin(y * frequency + phase) * amplitude
 */
data class Bubble(
    val id: String,
    var x: Float,
    var y: Float,
    val radius: Float,
    val speed: Float,         // pixels per second (upward)
    val amplitude: Float,     // sine-wave swing half-width in pixels
    var baseX: Float,         // center column; drifts with breeze
    val phase: Float,         // sine phase offset so bubbles don't sync
    val color: Color,
    val shimmerAngle: Float,  // degrees — position of the shine highlight
    val type: BubbleType = BubbleType.NORMAL,
)

/**
 * A short-lived burst animation that plays at the position where a
 * bubble was popped.  [progress] goes from 0f → 1f; once it hits 1f
 * the ViewModel removes it from the list.
 */
data class PopAnimation(
    val id: String,
    val x: Float,
    val y: Float,
    val radius: Float,
    val color: Color,
    var progress: Float = 0f,
)

@Composable
fun BubbleSurface(
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    color: Color = Color.Transparent,
    borderColor: Color = Color.White.copy(alpha = 0.55f),
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        color.copy(alpha = 0.15f),
                        color.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(0.25f, 0.18f),
                    radius = 900f
                )
            )
            .border(
                width = 0.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f),
                        borderColor.copy(alpha = 0.4f),
                        Color.White.copy(alpha = 0.15f)
                    )
                ),
                shape = shape
            )
            .drawWithCache {
                onDrawWithContent {
                    drawContent()

                    // Big soft shine
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.55f),
                                Color.White.copy(alpha = 0.18f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.28f, size.height * 0.22f),
                            radius = size.minDimension * 0.38f
                        ),
                        radius = size.minDimension * 0.38f,
                        center = Offset(size.width * 0.28f, size.height * 0.22f)
                    )

                    // Small highlight
                    drawCircle(
                        color = Color.White.copy(alpha = 0.8f),
                        radius = size.minDimension * 0.07f,
                        center = Offset(size.width * 0.28f, size.height * 0.18f)
                    )

                    // Bottom depth shadow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.12f)
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.9f),
                            radius = size.minDimension * 0.7f
                        ),
                        radius = size.minDimension * 0.7f,
                        center = Offset(size.width * 0.5f, size.height * 0.9f)
                    )
                }
            },
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Preview
@Composable
fun BubblePreview(){
    MaterialTheme(){
        BubbleSurface(
            modifier = Modifier.size(50.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color.Green
        )
    }
}