package com.apps.bubblepopping.view.play.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.game.BubbleSize
import com.apps.bubblepopping.stage.World
import com.apps.bubblepopping.ui.theme.BubbleTheme
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing
import com.apps.bubblepopping.view.worldMapColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Preview only: the old bubble look beside the new one, to judge the redesign.
// Delete this file once the new look is approved.

private const val OLD_RIM_A_START = 10f
private const val OLD_RIM_B_START = 190f
private const val OLD_RIM_SWEEP = 130f

/**
 * Colour stops for a sweep gradient centred on the origin that paints [stops]
 * evenly along the arc from [startDeg] to [startDeg] + [OLD_RIM_SWEEP]. Outside the
 * arc the gradient holds its end colours, which are transparent.
 */
private fun oldRimColorStops(startDeg: Float, stops: List<Color>): Array<Pair<Float, Color>> =
    Array(stops.size) { i ->
        val t = i.toFloat() / (stops.size - 1)
        (startDeg + t * OLD_RIM_SWEEP) / 360f to stops[i]
    }

/** Sweep-gradient brushes for both rim arcs, built once per scheme. */
private val oldRimBrushCache = HashMap<BubbleScheme, Pair<Brush, Brush>>()

private fun oldRimBrushes(scheme: BubbleScheme): Pair<Brush, Brush> = oldRimBrushCache.getOrPut(scheme) {
    Brush.sweepGradient(*oldRimColorStops(OLD_RIM_A_START, scheme.arcAStops), center = Offset.Zero) to
        Brush.sweepGradient(*oldRimColorStops(OLD_RIM_B_START, scheme.arcBStops), center = Offset.Zero)
}


internal fun DrawScope.drawSoapBubbleClassic(cx: Float, cy: Float, r: Float, rot: Float, scheme: BubbleScheme) {
    val center = Offset(cx, cy)

    // 1. Ghost fill
    drawCircle(color = scheme.bodyTint.copy(alpha = 0.07f), radius = r, center = center)

    // 2. Thin outline
    drawCircle(
        color  = scheme.outlineColor,
        radius = r,
        center = center,
        style  = Stroke(width = 0.3f.dp.toPx()),
    )

    // 3. Two rainbow arc segments. Each is one arc with a cached sweep gradient
    //    (drawn around the origin, then moved and turned into place) — this used
    //    to be 26 solid-colour arcs per segment, 52 draw calls per bubble.
    val rimWidth       = (r * 0.11f).coerceAtLeast(2.5f)
    val gapFromOutline = 1.2f.dp.toPx()
    val rimR           = r - gapFromOutline - rimWidth * 0.5f
    val rimTopLeft     = Offset(-rimR, -rimR)
    val rimSize        = Size(rimR * 2f, rimR * 2f)
    val rimStroke      = Stroke(width = rimWidth, cap = StrokeCap.Butt)
    val brushes        = oldRimBrushes(scheme)
    withTransform({
        translate(cx, cy)
        rotate(degrees = rot, pivot = Offset.Zero)
    }) {
        drawArc(brushes.first, OLD_RIM_A_START, OLD_RIM_SWEEP, useCenter = false, topLeft = rimTopLeft, size = rimSize, style = rimStroke)
        drawArc(brushes.second, OLD_RIM_B_START, OLD_RIM_SWEEP, useCenter = false, topLeft = rimTopLeft, size = rimSize, style = rimStroke)
    }

    // 4. Primary reflection blob
    val blobAngleRad = (rot - 45f) * PI.toFloat() / 180f
    val blobCX       = cx + r * 0.22f * cos(blobAngleRad)
    val blobCY       = cy + r * 0.22f * sin(blobAngleRad)
    val blobW        = r * 0.95f
    val blobH        = r * 0.38f
    val blobPivot    = Offset(blobCX, blobCY)
    rotate(degrees = rot - 30f, pivot = blobPivot) {
        drawOval(
            brush   = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.36f), Color.White.copy(alpha = 0.00f)),
                center = blobPivot,
                radius = blobW * 0.55f,
            ),
            topLeft = Offset(blobCX - blobW / 2f, blobCY - blobH / 2f),
            size    = Size(blobW, blobH),
        )
    }

    // 5. Secondary crescent arc
    val crescentR      = r * 0.60f
    val crescentRect   = Offset(cx - crescentR, cy - crescentR)
    val crescentSz     = Size(crescentR * 2f, crescentR * 2f)
    val crescentStart  = rot + 150f
    val crescentSweep  = 70f
    val crescentStroke = (r * 0.055f).coerceAtLeast(2f)
    drawArc(color = scheme.crescentColor.copy(alpha = 0.22f), startAngle = crescentStart - 6f,               sweepAngle = crescentSweep + 12f,            useCenter = false, topLeft = crescentRect, size = crescentSz, style = Stroke(width = crescentStroke * 2.4f, cap = StrokeCap.Round))
    drawArc(color = scheme.crescentColor.copy(alpha = 0.82f), startAngle = crescentStart,                    sweepAngle = crescentSweep,                  useCenter = false, topLeft = crescentRect, size = crescentSz, style = Stroke(width = crescentStroke,       cap = StrokeCap.Round))
    drawArc(color = scheme.crescentColor.copy(alpha = 0.95f), startAngle = crescentStart + crescentSweep * 0.3f, sweepAngle = crescentSweep * 0.4f,       useCenter = false, topLeft = crescentRect, size = crescentSz, style = Stroke(width = crescentStroke * 0.5f, cap = StrokeCap.Round))

    // 6. Sparkle dots
    val aRad1 = (rot - 35f) * PI.toFloat() / 180f
    drawCircle(
        color  = scheme.sparkleColor.copy(alpha = 0.95f),
        radius = r * 0.072f,
        center = Offset(cx + r * cos(aRad1), cy + r * sin(aRad1)),
    )
    val aRad2 = (rot + 75f) * PI.toFloat() / 180f
    drawCircle(
        color  = scheme.sparkleColor.copy(alpha = 0.85f),
        radius = r * 0.046f,
        center = Offset(cx + r * cos(aRad2), cy + r * sin(aRad2)),
    )
    val aRad3 = (rot + 200f) * PI.toFloat() / 180f
    drawCircle(
        color  = scheme.sparkleColor.copy(alpha = 0.72f),
        radius = r * 0.030f,
        center = Offset(cx + r * cos(aRad3), cy + r * sin(aRad3)),
    )
}

/** Before / after on each world's background, three sizes each, then the special bubbles. */
@Preview(heightDp = 1100)
@Composable
fun BubbleShowcasePreview() {
    BubbleTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
            modifier = Modifier.background(GameColors.NightTop).padding(Spacing.s),
        ) {
            World.entries.forEach { world ->
                Text("${world.name}: before | after", style = GameTheme.type.label, color = Color.White)
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(Brush.verticalGradient(worldMapColors(world))),
                ) {
                    val half = size.width / 2f
                    SIZES.forEachIndexed { i, (fraction, radiusDp) ->
                        val r = radiusDp.dp.toPx()
                        val y = size.height * (0.3f + 0.2f * i)
                        drawSoapBubbleClassic(half * fraction, y, r, rot = 40f + i * 70f, scheme = NORMAL_SCHEME)
                    }
                    drawLine(Color.White.copy(alpha = 0.3f), Offset(half, 0f), Offset(half, size.height))
                    SIZES.forEachIndexed { i, (fraction, radiusDp) ->
                        val r = radiusDp.dp.toPx()
                        val y = size.height * (0.3f + 0.2f * i)
                        drawGameBubble(previewBubble(half + half * fraction, y, r, BubbleType.NORMAL, i), world, PreviewIcons)
                    }
                }
            }
            Text("Special bubbles (after)", style = GameTheme.type.label, color = Color.White)
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Brush.verticalGradient(worldMapColors(World.SKY))),
            ) {
                val specials = listOf(BubbleType.POISON, BubbleType.HEART, BubbleType.GOLDEN, BubbleType.FREEZE, BubbleType.BOMB, BubbleType.SPLITTER, BubbleType.SHIELD)
                val step = size.width / specials.size
                specials.forEachIndexed { i, type ->
                    drawGameBubble(previewBubble(step * (i + 0.5f), size.height / 2f, 24.dp.toPx(), type, i), World.SKY, PreviewIcons)
                }
            }
            Text("Pop burst (after): 10%, 30%, 55%, 80%", style = GameTheme.type.label, color = Color.White)
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Brush.verticalGradient(worldMapColors(World.SKY))),
            ) {
                val stages = listOf(0.1f, 0.3f, 0.55f, 0.8f)
                val step = size.width / stages.size
                stages.forEachIndexed { i, progress ->
                    drawPopAnimation(
                        PopAnimation(id = "preview_pop", x = step * (i + 0.5f), y = size.height * 0.4f, radius = 18.dp.toPx(), color = Color.White, progress = progress),
                    )
                }
            }
        }
    }
}

/** (x as a fraction of the half, radius in dp) for a large, medium and small bubble. */
private val SIZES = listOf(0.3f to 34f, 0.68f to 24f, 0.42f to 15f)

private fun previewBubble(x: Float, y: Float, r: Float, type: BubbleType, seed: Int) = Bubble(
    id = "preview_$seed",
    x = x,
    y = y,
    radius = r,
    size = BubbleSize.LARGE,
    speed = 0f,
    amplitude = 0f,
    baseX = x,
    phase = seed * 1.3f,
    color = Color.White,
    shimmerAngle = seed * 70f,
    type = type,
)

/** Heart and skull art isn't needed for the bubbles shown here. */
private val PreviewIcons = BubbleIcons(heart = ImageBitmap(1, 1), skull = ImageBitmap(1, 1))
