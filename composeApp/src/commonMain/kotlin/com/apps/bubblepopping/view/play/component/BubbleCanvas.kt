package com.apps.bubblepopping.view.play.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import com.apps.bubblepopping.stage.World
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apps.bubblepopping.game.GameRules
import com.apps.bubblepopping.view.BossCrownColor
import com.apps.bubblepopping.view.BossHpFullColor
import com.apps.bubblepopping.view.BossHpLowColor
import com.apps.bubblepopping.view.NearMissColor
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

private fun DrawScope.drawSoapBubble(bubble: Bubble, scheme: BubbleScheme) =
    drawSoapBubble(bubble.x, bubble.y, bubble.radius, bubble.shimmerAngle, scheme)

private fun DrawScope.drawSoapBubble(cx: Float, cy: Float, r: Float, rot: Float, scheme: BubbleScheme) {
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

private fun DrawScope.drawBubbleIcon(bubble: Bubble, icon: ImageBitmap) {
    val iconSize = bubble.radius * 0.85f
    drawImage(
        image     = icon,
        dstOffset = IntOffset((bubble.x - iconSize / 2f).toInt(), (bubble.y - iconSize / 2f).toInt()),
        dstSize   = IntSize(iconSize.toInt().coerceAtLeast(1), iconSize.toInt().coerceAtLeast(1)),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Special-bubble and per-world schemes
// ─────────────────────────────────────────────────────────────────────────────

private fun rainbow(vararg colors: Long) = listOf(Color.Transparent) + colors.map { Color(it) } + Color.Transparent

private val GOLDEN_SCHEME = BubbleScheme(
    bodyTint      = Color(0xFFFFE082),
    outlineColor  = Color(0xFFFFD54F).copy(alpha = 0.8f),
    arcAStops     = rainbow(0xFFFFD54F, 0xFFFFF59D, 0xFFFFB300, 0xFFFFECB3),
    arcBStops     = rainbow(0xFFFFA000, 0xFFFFE082, 0xFFFFCA28, 0xFFFFF8E1),
    crescentColor = Color(0xFFFFF59D),
)

private val FREEZE_SCHEME = BubbleScheme(
    bodyTint      = Color(0xFFB3E5FC),
    outlineColor  = Color(0xFFE1F5FE).copy(alpha = 0.8f),
    arcAStops     = rainbow(0xFF80D8FF, 0xFFE1F5FE, 0xFF40C4FF, 0xFFB3E5FC),
    arcBStops     = rainbow(0xFF81D4FA, 0xFFFFFFFF, 0xFF4FC3F7, 0xFFE1F5FE),
    crescentColor = Color(0xFFE1F5FE),
)

private val BOMB_SCHEME = BubbleScheme(
    bodyTint      = Color(0xFFFF5252),
    outlineColor  = Color(0xFFFF8A80).copy(alpha = 0.7f),
    arcAStops     = rainbow(0xFFFF1744, 0xFFFF9100, 0xFFFF3D00, 0xFFFFAB40),
    arcBStops     = rainbow(0xFFD50000, 0xFFFF6E40, 0xFFFF5252, 0xFFFFD180),
    crescentColor = Color(0xFFFFAB40),
    sparkleColor  = Color(0xFFFFD180),
)

private val SPLITTER_SCHEME = BubbleScheme(
    bodyTint      = Color(0xFFE1BEE7),
    outlineColor  = Color(0xFFEA80FC).copy(alpha = 0.7f),
    arcAStops     = rainbow(0xFFEA80FC, 0xFFB388FF, 0xFFFF80AB, 0xFFE1BEE7),
    arcBStops     = rainbow(0xFFB388FF, 0xFF80D8FF, 0xFFEA80FC, 0xFFF8BBD0),
    crescentColor = Color(0xFFEA80FC),
)

private val SHIELD_SCHEME = BubbleScheme(
    bodyTint      = Color(0xFFECEFF1),
    outlineColor  = Color(0xFFCFD8DC).copy(alpha = 0.9f),
    arcAStops     = rainbow(0xFFCFD8DC, 0xFFFFFFFF, 0xFF90A4AE, 0xFFECEFF1),
    arcBStops     = rainbow(0xFFB0BEC5, 0xFFECEFF1, 0xFF78909C, 0xFFFFFFFF),
    crescentColor = Color(0xFFFFFFFF),
)

/** Normal bubbles take on each world's palette. */
private fun normalScheme(world: World): BubbleScheme = when (world) {
    World.SKY -> NORMAL_SCHEME
    World.OCEAN -> NORMAL_SCHEME.copy(
        bodyTint  = Color(0xFFB2EBF2),
        arcAStops = rainbow(0xFF18FFFF, 0xFF64FFDA, 0xFF80D8FF, 0xFFA7FFEB),
        arcBStops = rainbow(0xFF00E5FF, 0xFF69F0AE, 0xFF40C4FF, 0xFF84FFFF),
    )
    World.SPACE -> NORMAL_SCHEME.copy(
        bodyTint  = Color(0xFFD1C4E9),
        arcAStops = rainbow(0xFFB388FF, 0xFF8C9EFF, 0xFFEA80FC, 0xFF82B1FF),
        arcBStops = rainbow(0xFF7C4DFF, 0xFF536DFE, 0xFFE040FB, 0xFF448AFF),
    )
    World.CANDY -> NORMAL_SCHEME.copy(
        bodyTint  = Color(0xFFFCE4EC),
        arcAStops = rainbow(0xFFFF80AB, 0xFFFFFF8D, 0xFFFF9E80, 0xFFF8BBD0),
        arcBStops = rainbow(0xFFFF4081, 0xFFFFD180, 0xFFEA80FC, 0xFFFFFF8D),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Vector icons for special bubbles (drawn, not image assets)
// ─────────────────────────────────────────────────────────────────────────────

/** Five-pointed star. */
private fun DrawScope.drawStarIcon(center: Offset, r: Float) {
    val path = Path()
    for (i in 0 until 10) {
        val radius = if (i % 2 == 0) r else r * 0.45f
        val angle = (-90f + i * 36f) * PI.toFloat() / 180f
        val point = Offset(center.x + radius * cos(angle), center.y + radius * sin(angle))
        if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    drawPath(path, Color(0xFFFFC107))
    drawPath(path, Color.White.copy(alpha = 0.8f), style = Stroke(width = r * 0.12f))
}

/** Three crossing strokes with small side branches. */
private fun DrawScope.drawSnowflakeIcon(center: Offset, r: Float) {
    val stroke = r * 0.16f
    for (i in 0 until 3) {
        val angle = (i * 60f) * PI.toFloat() / 180f
        val dx = r * cos(angle)
        val dy = r * sin(angle)
        drawLine(Color.White, Offset(center.x - dx, center.y - dy), Offset(center.x + dx, center.y + dy), stroke, StrokeCap.Round)
    }
    for (i in 0 until 6) {
        val angle = (i * 60f) * PI.toFloat() / 180f
        val tip = Offset(center.x + r * 0.6f * cos(angle), center.y + r * 0.6f * sin(angle))
        for (side in listOf(-1f, 1f)) {
            val branchAngle = angle + side * 0.8f
            drawLine(
                Color.White,
                tip,
                Offset(tip.x + r * 0.3f * cos(branchAngle), tip.y + r * 0.3f * sin(branchAngle)),
                stroke * 0.7f,
                StrokeCap.Round,
            )
        }
    }
}

/** Round bomb with a fuse and a spark. */
private fun DrawScope.drawBombIcon(center: Offset, r: Float) {
    val body = Offset(center.x - r * 0.1f, center.y + r * 0.15f)
    drawCircle(Color(0xFF212121), r * 0.7f, body)
    drawCircle(Color.White.copy(alpha = 0.35f), r * 0.18f, Offset(body.x - r * 0.25f, body.y - r * 0.25f))
    val fuseStart = Offset(body.x + r * 0.45f, body.y - r * 0.5f)
    val fuseEnd = Offset(center.x + r * 0.75f, center.y - r * 0.85f)
    drawLine(Color(0xFF8D6E63), fuseStart, fuseEnd, r * 0.14f, StrokeCap.Round)
    drawCircle(Color(0xFFFFD180), r * 0.2f, fuseEnd)
    drawCircle(Color(0xFFFF6D00), r * 0.11f, fuseEnd)
}

/** Three small bubbles in a triangle. */
private fun DrawScope.drawSplitterIcon(center: Offset, r: Float) {
    for (i in 0 until 3) {
        val angle = (-90f + i * 120f) * PI.toFloat() / 180f
        val dot = Offset(center.x + r * 0.5f * cos(angle), center.y + r * 0.5f * sin(angle))
        drawCircle(Color.White.copy(alpha = 0.25f), r * 0.33f, dot)
        drawCircle(Color.White, r * 0.33f, dot, style = Stroke(width = r * 0.1f))
    }
}

/** Shield badge: flat top, pointed bottom. */
private fun DrawScope.drawShieldIcon(center: Offset, r: Float) {
    val path = Path().apply {
        moveTo(center.x, center.y - r)
        lineTo(center.x + r * 0.8f, center.y - r * 0.65f)
        lineTo(center.x + r * 0.7f, center.y + r * 0.2f)
        quadraticTo(center.x + r * 0.45f, center.y + r * 0.75f, center.x, center.y + r)
        quadraticTo(center.x - r * 0.45f, center.y + r * 0.75f, center.x - r * 0.7f, center.y + r * 0.2f)
        lineTo(center.x - r * 0.8f, center.y - r * 0.65f)
        close()
    }
    drawPath(path, Color(0xFF90A4AE))
    drawPath(path, Color.White, style = Stroke(width = r * 0.12f))
}

// ─────────────────────────────────────────────────────────────────────────────
// Public draw API
// ─────────────────────────────────────────────────────────────────────────────

/** Image icons for the bubble types that use artwork. */
class BubbleIcons(val heart: ImageBitmap, val skull: ImageBitmap)

/** Draws [bubble] with its type's look; normal bubbles use [world]'s palette. */
fun DrawScope.drawGameBubble(bubble: Bubble, world: World, icons: BubbleIcons) {
    val center = Offset(bubble.x, bubble.y)
    val iconRadius = bubble.radius * 0.45f
    when (bubble.type) {
        BubbleType.NORMAL -> drawSoapBubble(bubble, normalScheme(world))
        BubbleType.POISON -> { drawSoapBubble(bubble, POISON_SCHEME); drawBubbleIcon(bubble, icons.skull) }
        BubbleType.HEART -> { drawSoapBubble(bubble, HEART_SCHEME); drawBubbleIcon(bubble, icons.heart) }
        BubbleType.GOLDEN -> { drawSoapBubble(bubble, GOLDEN_SCHEME); drawStarIcon(center, iconRadius) }
        BubbleType.FREEZE -> { drawSoapBubble(bubble, FREEZE_SCHEME); drawSnowflakeIcon(center, iconRadius) }
        BubbleType.BOMB -> { drawSoapBubble(bubble, BOMB_SCHEME); drawBombIcon(center, iconRadius) }
        BubbleType.SPLITTER -> { drawSoapBubble(bubble, SPLITTER_SCHEME); drawSplitterIcon(center, iconRadius) }
        BubbleType.SHIELD -> { drawSoapBubble(bubble, SHIELD_SCHEME); drawShieldIcon(center, iconRadius) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Boss
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A boss: a big soap bubble in the world's colours with a crown, an HP ring,
 * cracks below ⅔ and ⅓ HP, and a white flash + wobble when hit.
 */
fun DrawScope.drawBoss(boss: BossBubble, world: World, time: Float) {
    val flash = (boss.hitFlash / GameRules.BOSS_HIT_FLASH_SECONDS).coerceIn(0f, 1f)
    val wobble = 1f + 0.08f * flash * sin(time * 60f)
    val r = boss.radius * wobble
    val center = Offset(boss.x, boss.y)

    // Body: a more solid version of the world palette so the boss reads as "big and heavy".
    drawCircle(Brush.radialGradient(
        colors = listOf(Color.White.copy(alpha = 0.18f), worldBossTint(world).copy(alpha = 0.35f)),
        center = center,
        radius = r,
    ), r, center)
    drawSoapBubble(boss.x, boss.y, r, rot = time * 20f, scheme = normalScheme(world))

    // Cracks, deterministic per boss.
    if (boss.hpFraction <= 2f / 3f) drawCracks(center, r, seed = boss.id.hashCode(), count = 2)
    if (boss.hpFraction <= 1f / 3f) drawCracks(center, r, seed = boss.id.hashCode() + 7, count = 3)

    // HP ring, green → red.
    val ringR = r + 6.dp.toPx()
    drawCircle(Color.Black.copy(alpha = 0.25f), ringR, center, style = Stroke(width = 5.dp.toPx()))
    drawArc(
        color = lerp(BossHpLowColor, BossHpFullColor, boss.hpFraction),
        startAngle = -90f,
        sweepAngle = 360f * boss.hpFraction,
        useCenter = false,
        topLeft = Offset(center.x - ringR, center.y - ringR),
        size = Size(ringR * 2, ringR * 2),
        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
    )

    if (boss.generation == 0) drawCrown(Offset(center.x, center.y - r), r * 0.45f)

    if (flash > 0f) drawCircle(Color.White.copy(alpha = 0.45f * flash), r, center)
}

private fun worldBossTint(world: World): Color = when (world) {
    World.SKY -> Color(0xFF90CAF9)
    World.OCEAN -> Color(0xFFCE93D8)
    World.SPACE -> Color(0xFF7C4DFF)
    World.CANDY -> Color(0xFFFF80AB)
}

/** Zig-zag cracks from the rim inwards; positions come from [seed] so they don't flicker. */
private fun DrawScope.drawCracks(center: Offset, r: Float, seed: Int, count: Int) {
    val random = kotlin.random.Random(seed)
    repeat(count) {
        val angle = random.nextFloat() * 2f * PI.toFloat()
        var point = Offset(center.x + r * 0.95f * cos(angle), center.y + r * 0.95f * sin(angle))
        val path = Path().apply { moveTo(point.x, point.y) }
        repeat(4) { step ->
            val inward = angle + PI.toFloat() + (random.nextFloat() - 0.5f) * 1.2f
            val length = r * (0.12f + random.nextFloat() * 0.1f) * (1f - step * 0.15f)
            point = Offset(point.x + length * cos(inward), point.y + length * sin(inward))
            path.lineTo(point.x, point.y)
        }
        drawPath(path, Color.White.copy(alpha = 0.75f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

/** Three-pointed crown sitting on top of the boss. */
private fun DrawScope.drawCrown(base: Offset, width: Float) {
    val h = width * 0.6f
    val left = base.x - width / 2
    val path = Path().apply {
        moveTo(left, base.y)
        lineTo(left, base.y - h * 0.6f)
        lineTo(left + width * 0.25f, base.y - h * 0.25f)
        lineTo(base.x, base.y - h)
        lineTo(left + width * 0.75f, base.y - h * 0.25f)
        lineTo(left + width, base.y - h * 0.6f)
        lineTo(left + width, base.y)
        close()
    }
    drawPath(path, BossCrownColor)
    drawPath(path, Color.White.copy(alpha = 0.7f), style = Stroke(width = 1.5f.dp.toPx()))
    drawCircle(Color(0xFFE53935), width * 0.07f, Offset(base.x, base.y - h * 0.3f))
}

/** A spinning confetti scrap, fading out over its last half second. */
fun DrawScope.drawConfetti(scrap: Confetti) {
    val alpha = (scrap.life / 0.5f).coerceIn(0f, 1f)
    rotate(degrees = scrap.rotation, pivot = Offset(scrap.x, scrap.y)) {
        drawRect(
            color = scrap.color.copy(alpha = alpha),
            topLeft = Offset(scrap.x - scrap.size / 2, scrap.y - scrap.size / 4),
            size = Size(scrap.size, scrap.size / 2),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pop animation
// ─────────────────────────────────────────────────────────────────────────────

/** Bursts scale with [PopAnimation.intensity]: higher combos → wider, thicker bursts. */
fun DrawScope.drawPopAnimation(anim: PopAnimation) {
    val p      = anim.progress
    val k      = anim.intensity
    val alpha  = (1f - p).coerceIn(0f, 1f)
    val center = Offset(anim.x, anim.y)

    val ringR    = anim.radius * (1f + p * 3f * k)
    val ringRect = Offset(anim.x - ringR, anim.y - ringR)
    val ringSize = Size(ringR * 2f, ringR * 2f)
    val strokeW  = ((3f * (1f - p) + 0.5f) * k).dp.toPx()

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
    val spread    = anim.radius * (1.5f + p * 3.5f * k)
    val dotRadius = (anim.radius * 0.12f * k * (1f - p * 0.7f)).coerceAtLeast(0f)
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
// Near-miss warning
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Pulsing red ring around a bubble that is about to escape.
 * [time] drives the pulse (seconds).
 */
fun DrawScope.drawNearMissWarning(bubble: Bubble, time: Float) {
    val pulse = 0.5f + 0.5f * sin(time * 12f)
    drawCircle(
        color  = NearMissColor.copy(alpha = 0.35f + 0.45f * pulse),
        radius = bubble.radius + (2f + 2f * pulse).dp.toPx(),
        center = Offset(bubble.x, bubble.y),
        style  = Stroke(width = 2.5f.dp.toPx()),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Floating texts
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Draws a floating score / milestone text centred on its position: pops in,
 * rises, then fades out over its lifetime.
 */
fun DrawScope.drawFloatingText(text: FloatingText, measurer: TextMeasurer, fontFamily: FontFamily) {
    val p = text.progress
    val popIn = (p / 0.15f).coerceIn(0f, 1f)                  // scale up over the first 15%
    val alpha = if (p < 0.6f) 1f else (1f - (p - 0.6f) / 0.4f) // fade over the last 40%
    val baseSize = if (text.isMilestone) 30f else 18f
    val rise = (if (text.isMilestone) 24f else 48f).dp.toPx() * p

    val layout = measurer.measure(
        text = text.text,
        style = TextStyle(
            color = text.color.copy(alpha = alpha.coerceIn(0f, 1f)),
            fontSize = (baseSize * (0.6f + 0.4f * popIn)).sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = fontFamily,
            shadow = Shadow(color = Color.Black.copy(alpha = 0.5f * alpha), blurRadius = 6f),
        ),
    )
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(
            text.x - layout.size.width / 2f,
            text.y - rise - layout.size.height / 2f,
        ),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Breeze lines
// ─────────────────────────────────────────────────────────────────────────────

/** [breeze] is the normalised breeze strength, -1 (full left) to +1 (full right). */
fun DrawScope.drawBreezeLines(breeze: Float, time: Float) {
    if (abs(breeze) < 0.025f) return
    val normalised  = abs(breeze).coerceIn(0f, 1f)
    val lineAlpha   = normalised * 0.30f
    val goingRight  = breeze > 0f
    val sign        = if (goingRight) 1f else -1f
    val lineLength  = size.width * 0.10f
    val scrollSpeed = normalised * size.width * 0.12f
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