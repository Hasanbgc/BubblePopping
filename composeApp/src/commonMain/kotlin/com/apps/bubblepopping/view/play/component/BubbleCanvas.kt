package com.apps.bubblepopping.view.play.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin




// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun lerpRainbow(t: Float, stops: List<Color>): Color {
    val segments = stops.size - 1
    val scaled   = (t * segments).coerceIn(0f, segments.toFloat())
    val idx      = scaled.toInt().coerceIn(0, segments - 1)
    return lerp(stops[idx], stops[idx + 1], scaled - idx)
}

// ─────────────────────────────────────────────────────────────────────────────
// Color schemes — one per bubble type
// ─────────────────────────────────────────────────────────────────────────────

private data class BubbleScheme(
    val bodyTint: Color,
    val outlineColor: Color,
    val arcAStops: List<Color>,
    val arcBStops: List<Color>,
    val crescentColor: Color = Color.White,
    val sparkleColor: Color  = Color.White,
)

private val NORMAL_SCHEME = BubbleScheme(
    bodyTint     = Color(0xFFD0EEFF),
    outlineColor = Color.White.copy(alpha = 0.45f),
    arcAStops    = listOf(Color.Transparent, Color(0xFFFF80CE), Color(0xFF80D8FF), Color(0xFF69F0AE), Color(0xFFFFFF8D), Color.Transparent),
    arcBStops    = listOf(Color.Transparent, Color(0xFFFF6E40), Color(0xFFEA80FC), Color(0xFF80DEEA), Color(0xFFFF80CE), Color.Transparent),
)

private val HEART_SCHEME = BubbleScheme(
    bodyTint     = Color(0xFFFFD6E7),
    outlineColor = Color(0xFFFFB3C6).copy(alpha = 0.65f),
    arcAStops    = listOf(Color.Transparent, Color(0xFFFF4081), Color(0xFFFF80AB), Color(0xFFFF1744), Color(0xFFF8BBD0), Color.Transparent),
    arcBStops    = listOf(Color.Transparent, Color(0xFFF06292), Color(0xFFE91E63), Color(0xFFFF4D8B), Color(0xFFFF80CE), Color.Transparent),
    crescentColor = Color(0xFFFF80AB),
    sparkleColor  = Color.White,
)

private val POISON_SCHEME = BubbleScheme(
    bodyTint     = Color(0xFF00FF66),
    outlineColor = Color(0xFF39FF14).copy(alpha = 0.55f),
    arcAStops    = listOf(Color.Transparent, Color(0xFF39FF14), Color(0xFFCCFF00), Color(0xFF76FF03), Color(0xFF00E676), Color.Transparent),
    arcBStops    = listOf(Color.Transparent, Color(0xFF1DE9B6), Color(0xFF69F0AE), Color(0xFFB2FF59), Color(0xFF39FF14), Color.Transparent),
    crescentColor = Color(0xFF39FF14),
    sparkleColor  = Color(0xFFB2FF59),
)

// ─────────────────────────────────────────────────────────────────────────────
// Shared soap-bubble renderer (parameterised by color scheme)
// ─────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawSoapBubble(bubble: Bubble, scheme: BubbleScheme) {
    val cx     = bubble.x
    val cy     = bubble.y
    val r      = bubble.radius
    val center = Offset(cx, cy)
    val rot    = bubble.shimmerAngle

    // 1. Ghost fill
    drawCircle(color = scheme.bodyTint.copy(alpha = 0.07f), radius = r, center = center)

    // 2. Thin outline
    drawCircle(
        color  = scheme.outlineColor,
        radius = r,
        center = center,
        style  = Stroke(width = 0.3f.dp.toPx()),
    )

    // 3. Two rainbow arc segments
    val rimWidth       = (r * 0.11f).coerceAtLeast(2.5f)
    val gapFromOutline = 1.2f.dp.toPx()
    val rimR           = r - gapFromOutline - rimWidth * 0.5f
    val rimRect        = Offset(cx - rimR, cy - rimR)
    val rimSize        = Size(rimR * 2f, rimR * 2f)
    val arcSweep       = 130f
    val segCount       = 26
    val segAngle       = arcSweep / segCount

    fun drawRainbowArc(startDeg: Float, stops: List<Color>) {
        for (i in 0 until segCount) {
            drawArc(
                color      = lerpRainbow(i.toFloat() / (segCount - 1), stops),
                startAngle = startDeg + rot + i * segAngle,
                sweepAngle = segAngle + 0.8f,
                useCenter  = false,
                topLeft    = rimRect,
                size       = rimSize,
                style      = Stroke(width = rimWidth, cap = StrokeCap.Butt),
            )
        }
    }
    drawRainbowArc(startDeg = 10f,  stops = scheme.arcAStops)
    drawRainbowArc(startDeg = 190f, stops = scheme.arcBStops)

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
    data class Sparkle(val angleDeg: Float, val sz: Float, val alpha: Float)
    listOf(
        Sparkle(rot - 35f,  r * 0.072f, 0.95f),
        Sparkle(rot + 75f,  r * 0.046f, 0.85f),
        Sparkle(rot + 200f, r * 0.030f, 0.72f),
    ).forEach { sp ->
        val aRad = sp.angleDeg * PI.toFloat() / 180f
        drawCircle(
            color  = scheme.sparkleColor.copy(alpha = sp.alpha),
            radius = sp.sz,
            center = Offset(cx + r * cos(aRad), cy + r * sin(aRad)),
        )
    }
}

private fun DrawScope.drawBubbleIcon(bubble: Bubble, icon: ImageBitmap) {
    val iconSize = bubble.radius * 0.85f
    drawImage(
        image     = icon,
        dstOffset = IntOffset((bubble.x - iconSize / 2f).toInt(), (bubble.y - iconSize / 2f).toInt()),
        dstSize   = IntSize(iconSize.toInt().coerceAtLeast(1), iconSize.toInt().coerceAtLeast(1)),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Public draw API
// ─────────────────────────────────────────────────────────────────────────────

fun DrawScope.drawBubble(bubble: Bubble)                            = drawSoapBubble(bubble, NORMAL_SCHEME)
fun DrawScope.drawPoisonBubble(bubble: Bubble, icon: ImageBitmap)  { drawSoapBubble(bubble, POISON_SCHEME); drawBubbleIcon(bubble, icon) }
fun DrawScope.drawHeartBubble(bubble: Bubble, icon: ImageBitmap)  { drawSoapBubble(bubble, HEART_SCHEME);  drawBubbleIcon(bubble, icon) }

// ─────────────────────────────────────────────────────────────────────────────
// Pop animation
// ─────────────────────────────────────────────────────────────────────────────

fun DrawScope.drawPopAnimation(anim: PopAnimation) {
    val p      = anim.progress
    val alpha  = (1f - p).coerceIn(0f, 1f)
    val center = Offset(anim.x, anim.y)

    val ringR    = anim.radius * (1f + p * 3f)
    val ringRect = Offset(anim.x - ringR, anim.y - ringR)
    val ringSize = Size(ringR * 2f, ringR * 2f)
    val strokeW  = (3f * (1f - p) + 0.5f).dp.toPx()

    val popStops = listOf(
        Color(0xFFFF80CE), Color(0xFF80D8FF), Color(0xFF69F0AE),
        Color(0xFFFFFF8D), Color(0xFFFF80CE),
    )
    for (i in 0 until 36) {
        val t = i.toFloat() / 36f
        drawArc(
            color      = lerpRainbow(t, popStops).copy(alpha = alpha),
            startAngle = t * 360f,
            sweepAngle = 10.5f,
            useCenter  = false,
            topLeft    = ringRect,
            size       = ringSize,
            style      = Stroke(width = strokeW, cap = StrokeCap.Butt),
        )
    }

    val dropColors = listOf(
        Color(0xFF80D8FF), Color(0xFFFF80CE), Color(0xFF69F0AE),
        Color(0xFFFFFF8D), Color(0xFFEA80FC), Color(0xFFFF6E40),
        Color(0xFF80DEEA), Color(0xFFFFAB40),
    )
    val spread    = anim.radius * (1.5f + p * 3.5f)
    val dotRadius = (anim.radius * 0.12f * (1f - p * 0.7f)).coerceAtLeast(0f)
    for (i in 0 until 8) {
        val angle = (i.toFloat() / 8f) * 2f * PI.toFloat()
        drawCircle(
            color  = dropColors[i].copy(alpha = alpha * 0.9f),
            radius = dotRadius,
            center = Offset(anim.x + cos(angle) * spread, anim.y + sin(angle) * spread),
        )
    }

    if (p < 0.25f) {
        drawCircle(
            color  = Color.White.copy(alpha = ((0.25f - p) / 0.25f) * 0.5f),
            radius = anim.radius * (1f - p * 3f).coerceAtLeast(0f),
            center = center,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Breeze lines
// ─────────────────────────────────────────────────────────────────────────────

fun DrawScope.drawBreezeLines(breezeForce: Float, time: Float) {
    if (abs(breezeForce) < 4f) return
    val normalised  = (abs(breezeForce) / 160f).coerceIn(0f, 1f)
    val lineAlpha   = normalised * 0.30f
    val goingRight  = breezeForce > 0f
    val sign        = if (goingRight) 1f else -1f
    val lineLength  = size.width * 0.10f
    val scrollSpeed = abs(breezeForce) * 0.8f
    val vertStep    = size.height / 8f
    for (i in 1..7) {
        val y      = vertStep * i + (i % 3 - 1) * 12f
        val rawOff = time * scrollSpeed * sign + i * 80f
        val startX = if (goingRight) rawOff % size.width else size.width - (rawOff % size.width)
        drawLine(
            color       = Color.White.copy(alpha = lineAlpha),
            start       = Offset(startX, y),
            end         = Offset(startX + sign * lineLength, y),
            strokeWidth = 1.dp.toPx(),
            cap         = StrokeCap.Round,
        )
    }
}