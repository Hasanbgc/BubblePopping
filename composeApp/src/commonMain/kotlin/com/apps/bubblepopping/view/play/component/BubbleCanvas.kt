package com.apps.bubblepopping.view.play.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import com.apps.bubblepopping.stage.World
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apps.bubblepopping.game.GameRules
import com.apps.bubblepopping.meta.Cosmetics
import com.apps.bubblepopping.view.BossCrownColor
import com.apps.bubblepopping.view.BossHpFullColor
import com.apps.bubblepopping.view.BossHpLowColor
import com.apps.bubblepopping.view.NearMissColor
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin




// ─────────────────────────────────────────────────────────────────────────────
// Color schemes — one per bubble type
// ─────────────────────────────────────────────────────────────────────────────

internal data class BubbleScheme(
    val bodyTint: Color,
    val outlineColor: Color,
    val arcAStops: List<Color>,
    val arcBStops: List<Color>,
    val crescentColor: Color = Color.White,
    val sparkleColor: Color  = Color.White,
)

internal val NORMAL_SCHEME = BubbleScheme(
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

/**
 * The soap bubble, lit from the top-left. Only the soap film swirls ([rot]);
 * the light and its reflections stay put, as on a real bubble. Layers, back to
 * front:
 *
 * 1. a faint shadow on the scene below,
 * 2. the body: clear in the middle, taking on [BubbleScheme.bodyTint] towards
 *    the rim (glass and film catch more light at a glancing angle),
 * 3. the soap film: a full ring of thin-film colours fading inwards, turned by [rot],
 * 4. thicker, warmer film pooled along the bottom,
 * 5. a crisp rim,
 * 6. the window reflection (top-left), a soft bounce light (bottom-right) and a glint.
 *
 * [wobble] squashes and stretches the bubble a little (positive = wider), so it
 * reads as soft soap. About ten draw calls, with every gradient cached per scheme.
 * Live game bubbles use [drawSoapBubbleSprite] instead: the same layers, drawn
 * once into images.
 */
internal fun DrawScope.drawSoapBubble(
    cx: Float,
    cy: Float,
    r: Float,
    rot: Float,
    scheme: BubbleScheme,
    wobble: Float = 0f,
) {
    // 1. Shadow, a little below — lifts the bubble off the background. Not squashed.
    withTransform({
        translate(cx, cy)
        scale(r, r, pivot = Offset.Zero)
    }) {
        drawBubbleShadow()
    }

    // Everything else is drawn around the origin at unit radius, then moved, sized and squashed.
    withTransform({
        translate(cx, cy)
        scale(scaleX = r * (1f + wobble), scaleY = r * (1f - wobble), pivot = Offset.Zero)
    }) {
        drawBubbleBody(scheme)
        rotate(degrees = rot, pivot = Offset.Zero) { drawBubbleFilm(scheme) }
        drawBubbleFront(scheme)
    }
}

/** Layer 1, at unit radius around the origin: the shadow on the scene below. */
private fun DrawScope.drawBubbleShadow() {
    drawCircle(ShadowBrush, radius = SHADOW_SIZE, center = Offset(0f, SHADOW_DROP))
}

/** Layer 2: the body. */
private fun DrawScope.drawBubbleBody(scheme: BubbleScheme) {
    drawCircle(bubbleBrushes(scheme).body, radius = 1f, center = Offset.Zero)
}

/** Layer 3, the one that swirls: a bright ring at the edge and a wide faint one fading inwards. */
private fun DrawScope.drawBubbleFilm(scheme: BubbleScheme) {
    val film = bubbleBrushes(scheme).film
    drawCircle(film, radius = 0.86f, center = Offset.Zero, style = Stroke(width = 0.26f), alpha = FILM_INNER_ALPHA)
    drawCircle(film, radius = 0.9f, center = Offset.Zero, style = Stroke(width = 0.13f), alpha = FILM_ALPHA)
}

/** Layers 4–6: pooled film, rim and the fixed lights. */
private fun DrawScope.drawBubbleFront(scheme: BubbleScheme) {
    // 4. Thicker, warmer film pooled along the bottom (fixed: gravity, not swirl).
    drawArc(
        brush = bubbleBrushes(scheme).filmPool,
        startAngle = 20f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(-0.88f, -0.88f),
        size = Size(1.76f, 1.76f),
        style = Stroke(width = 0.16f, cap = StrokeCap.Round),
        alpha = POOL_ALPHA,
    )

    // 5. Crisp rim.
    drawCircle(scheme.outlineColor, radius = 0.985f, center = Offset.Zero, style = Stroke(width = RIM_WIDTH))

    // 6. Light, fixed top-left: the curved window reflection…
    drawArc(
        color = Color.White.copy(alpha = WINDOW_ALPHA),
        startAngle = 196f,
        sweepAngle = 58f,
        useCenter = false,
        topLeft = Offset(-0.7f, -0.7f),
        size = Size(1.4f, 1.4f),
        style = Stroke(width = 0.15f, cap = StrokeCap.Round),
    )
    // …a small bright spot beside it…
    drawCircle(Color.White.copy(alpha = 0.9f), radius = 0.075f, center = Offset(-0.18f, -0.62f))
    // …a soft bounce light opposite, tinted by the scheme…
    drawArc(
        color = scheme.crescentColor.copy(alpha = BOUNCE_ALPHA),
        startAngle = 28f,
        sweepAngle = 44f,
        useCenter = false,
        topLeft = Offset(-0.76f, -0.76f),
        size = Size(1.52f, 1.52f),
        style = Stroke(width = 0.07f, cap = StrokeCap.Round),
    )
    // …and a glint right on the rim.
    drawCircle(scheme.sparkleColor.copy(alpha = 0.95f), radius = 0.05f, center = Offset(-0.69f, -0.69f))
}

// ─────────────────────────────────────────────────────────────────────────────
// Sprites: the bubble's layers drawn once into images
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A scheme's bubble as three images, so a live bubble costs three image draws
 * instead of about ten blended gradients: [back] (shadow and body), [film] (turned
 * by the swirl) and [front] (pool, rim, lights). Each is a square [side] pixels
 * wide with the bubble's centre in the middle, drawn at [unitPx] pixels per radius.
 */
private class BubbleSprite(val back: ImageBitmap, val film: ImageBitmap, val front: ImageBitmap, val unitPx: Int) {
    val side = back.width
}

private data class SpriteKey(val scheme: BubbleScheme, val unitPx: Int)

private val spriteCache = HashMap<SpriteKey, BubbleSprite>()

/**
 * The sprite for [scheme] that a bubble of radius [r] pixels is drawn from: by
 * default the largest game bubble's size on this screen, so game bubbles share one
 * sprite that is only ever scaled down; bigger bubbles (menus, shop previews) get
 * one sized up in [SPRITE_STEP_PX] steps. Made on first use, then reused.
 */
private fun DrawScope.bubbleSprite(scheme: BubbleScheme, r: Float): BubbleSprite {
    val gameUnitPx = (GameRules.MAX_RADIUS_DP * density).roundToInt()
    val unitPx = if (r <= gameUnitPx) gameUnitPx else ceil(r / SPRITE_STEP_PX).toInt() * SPRITE_STEP_PX
    return bubbleSpriteAt(scheme, unitPx.coerceIn(SPRITE_MIN_UNIT_PX, SPRITE_MAX_UNIT_PX))
}

private fun bubbleSpriteAt(scheme: BubbleScheme, unitPx: Int): BubbleSprite =
    spriteCache.getOrPut(SpriteKey(scheme, unitPx)) {
        BubbleSprite(
            back = bakeLayer(unitPx) {
                drawBubbleShadow()
                drawBubbleBody(scheme)
            },
            film = bakeLayer(unitPx) { drawBubbleFilm(scheme) },
            front = bakeLayer(unitPx) { drawBubbleFront(scheme) },
            unitPx = unitPx,
        )
    }

/** Draws one unit-radius layer into a new square image, [unitPx] pixels per radius. */
private fun bakeLayer(unitPx: Int, layer: DrawScope.() -> Unit): ImageBitmap {
    // Even, so the centre lands on a whole pixel.
    val side = (ceil(unitPx * SPRITE_EXTENT).toInt() * 2)
    val image = ImageBitmap(side, side)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(side.toFloat(), side.toFloat())) {
        withTransform({
            translate(side / 2f, side / 2f)
            scale(unitPx.toFloat(), unitPx.toFloat(), pivot = Offset.Zero)
        }) { layer() }
    }
    return image
}

/**
 * [drawSoapBubble] from the scheme's sprite: the same bubble, swirl and wobble in
 * three image draws. The wobble squashes the shadow too, which is too faint to see.
 * [alpha] fades each layer, for bubbles kept in the background.
 */
internal fun DrawScope.drawSoapBubbleSprite(
    cx: Float,
    cy: Float,
    r: Float,
    rot: Float,
    scheme: BubbleScheme,
    wobble: Float = 0f,
    alpha: Float = 1f,
) {
    val sprite = bubbleSprite(scheme, r)
    val half = sprite.side / 2
    val topLeft = IntOffset(-half, -half)
    val size = IntSize(sprite.side, sprite.side)
    withTransform({
        translate(cx, cy)
        scale(r * (1f + wobble) / sprite.unitPx, r * (1f - wobble) / sprite.unitPx, pivot = Offset.Zero)
    }) {
        drawImage(sprite.back, dstOffset = topLeft, dstSize = size, alpha = alpha, filterQuality = SPRITE_FILTER)
        rotate(degrees = rot, pivot = Offset.Zero) {
            drawImage(sprite.film, dstOffset = topLeft, dstSize = size, alpha = alpha, filterQuality = SPRITE_FILTER)
        }
        drawImage(sprite.front, dstOffset = topLeft, dstSize = size, alpha = alpha, filterQuality = SPRITE_FILTER)
    }
}

/** How far a sprite reaches from the centre, in radii: the shadow hangs lowest (0.22 + 1.05). */
private const val SPRITE_EXTENT = 1.3f
private const val SPRITE_MIN_UNIT_PX = 24
/** Caps a sprite at about 420 px square, whatever the screen density; bigger bubbles are scaled up. */
private const val SPRITE_MAX_UNIT_PX = 160
/** Sprite sizes for bubbles bigger than a game bubble, in pixels per radius. */
private const val SPRITE_STEP_PX = 32
/** Mipmapped, so small bubbles scaled far down keep a clean rim. */
private val SPRITE_FILTER = FilterQuality.Medium

/**
 * The soap film's colours around the ring: both of the scheme's rainbows without
 * their transparent ends, then back to the first colour so the ring has no seam.
 */
internal fun filmRingColors(scheme: BubbleScheme): List<Color> {
    val film = (scheme.arcAStops + scheme.arcBStops).filter { it.alpha > 0f }.ifEmpty { listOf(Color.White) }
    return film + film.first()
}

/** The gradients a scheme needs, made once (at unit radius, around the origin) and reused. */
private class BubbleBrushes(val body: Brush, val film: Brush, val filmPool: Brush)

private val bubbleBrushCache = HashMap<BubbleScheme, BubbleBrushes>()

private fun bubbleBrushes(scheme: BubbleScheme): BubbleBrushes = bubbleBrushCache.getOrPut(scheme) {
    val filmStops = filmRingColors(scheme)
    val film = filmStops.dropLast(1)
    BubbleBrushes(
        body = Brush.radialGradient(
            0f to Color.Transparent,
            0.55f to scheme.bodyTint.copy(alpha = 0.05f),
            0.88f to scheme.bodyTint.copy(alpha = 0.2f),
            1f to scheme.bodyTint.copy(alpha = 0.38f),
            center = Offset.Zero,
            radius = 1f,
        ),
        film = Brush.sweepGradient(filmStops, center = Offset.Zero),
        // Warm film colours along the bottom: the second half of the palette.
        filmPool = Brush.horizontalGradient(
            film.takeLast((film.size / 2).coerceAtLeast(2)).let { if (it.size < 2) it + it else it },
            startX = -0.9f,
            endX = 0.9f,
        ),
    )
}

private const val SHADOW_ALPHA = 0.16f
private const val SHADOW_SIZE = 1.05f
/** The shadow at unit radius, shared by every bubble. */
private val ShadowBrush = Brush.radialGradient(
    listOf(Color.Black.copy(alpha = SHADOW_ALPHA), Color.Transparent),
    center = Offset.Zero,
    radius = SHADOW_SIZE,
)
/** How far below its centre the shadow falls, in radii. */
private const val SHADOW_DROP = 0.22f
private const val FILM_ALPHA = 0.7f
private const val FILM_INNER_ALPHA = 0.22f
private const val POOL_ALPHA = 0.45f
private const val WINDOW_ALPHA = 0.78f
private const val BOUNCE_ALPHA = 0.45f
/** The crisp outer edge, in radii. */
private const val RIM_WIDTH = 0.03f

/** How much a bubble's film turns as it rises, in degrees per pixel. */
private const val SWIRL_PER_PX = 0.12f
/** Squash-and-stretch: how far, and how many waves per pixel risen. */
private const val WOBBLE_AMOUNT = 0.035f
private const val WOBBLE_PER_PX = 0.02f

/**
 * Draws a live bubble. It has no clock of its own, so its height drives the
 * motion: as it rises, the film swirls and it gently squashes and stretches,
 * each bubble out of step with the others.
 */
private fun DrawScope.drawSoapBubble(bubble: Bubble, scheme: BubbleScheme) =
    drawSoapBubbleSprite(
        cx = bubble.x,
        cy = bubble.y,
        r = bubble.radius,
        rot = bubble.shimmerAngle - bubble.y * SWIRL_PER_PX,
        scheme = scheme,
        wobble = WOBBLE_AMOUNT * sin(bubble.y * WOBBLE_PER_PX + bubble.phase),
    )

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

internal fun rainbow(vararg colors: Long) = listOf(Color.Transparent) + colors.map { Color(it) } + Color.Transparent

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

/** Normal bubbles take on each world's palette (built once per world). */
private fun normalScheme(world: World): BubbleScheme = WORLD_SCHEMES.getValue(world)

private val WORLD_SCHEMES: Map<World, BubbleScheme> = World.entries.associateWith { world -> buildNormalScheme(world) }

private fun buildNormalScheme(world: World): BubbleScheme = when (world) {
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
internal fun DrawScope.drawStarIcon(center: Offset, r: Float) {
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

/**
 * Draws [bubble] with its type's look. Normal bubbles wear the equipped [skinId],
 * or with the Classic skin [world]'s palette. Special bubbles never change look.
 */
fun DrawScope.drawGameBubble(
    bubble: Bubble,
    world: World,
    icons: BubbleIcons,
    skinId: String = Cosmetics.CLASSIC.id,
    highContrastSkulls: Boolean = false,
) {
    val center = Offset(bubble.x, bubble.y)
    val iconRadius = bubble.radius * 0.45f
    if (bubble.type != BubbleType.NORMAL) {
        // Special bubbles: the film, then a soft glow in their colour behind the icon.
        drawSoapBubble(bubble, specialScheme(bubble.type))
        drawCoreGlow(center, bubble.radius, CORE_GLOW_COLORS.getValue(bubble.type))
    }
    when (bubble.type) {
        BubbleType.NORMAL -> drawSoapBubble(bubble, skinScheme(skinId) ?: normalScheme(world))
        BubbleType.POISON -> {
            drawToxicSwirl(center, bubble.radius, rot = bubble.shimmerAngle - bubble.y * TOXIC_SWIRL_PER_PX)
            drawBubbleIcon(bubble, icons.skull)
            if (highContrastSkulls) drawHazardRing(center, bubble.radius)
        }
        BubbleType.HEART -> drawBubbleIcon(bubble, icons.heart)
        BubbleType.GOLDEN -> drawStarIcon(center, iconRadius)
        BubbleType.FREEZE -> drawSnowflakeIcon(center, iconRadius)
        BubbleType.BOMB -> drawBombIcon(center, iconRadius)
        BubbleType.SPLITTER -> drawSplitterIcon(center, iconRadius)
        BubbleType.SHIELD -> drawShieldIcon(center, iconRadius)
    }
}

/** The film colours of a special bubble (normal bubbles use the skin or world instead). */
private fun specialScheme(type: BubbleType): BubbleScheme = when (type) {
    BubbleType.POISON -> POISON_SCHEME
    BubbleType.HEART -> HEART_SCHEME
    BubbleType.GOLDEN -> GOLDEN_SCHEME
    BubbleType.FREEZE -> FREEZE_SCHEME
    BubbleType.BOMB -> BOMB_SCHEME
    BubbleType.SPLITTER -> SPLITTER_SCHEME
    BubbleType.SHIELD -> SHIELD_SCHEME
    BubbleType.NORMAL -> NORMAL_SCHEME
}

/** The glow behind each special bubble's icon, so its kind reads at a glance. */
private val CORE_GLOW_COLORS: Map<BubbleType, Color> = mapOf(
    BubbleType.POISON to Color(0xFF39FF14),
    BubbleType.HEART to Color(0xFFFF4D8B),
    BubbleType.GOLDEN to Color(0xFFFFD54F),
    BubbleType.FREEZE to Color(0xFF80D8FF),
    BubbleType.BOMB to Color(0xFFFF6E40),
    BubbleType.SPLITTER to Color(0xFFEA80FC),
    BubbleType.SHIELD to Color(0xFFECEFF1),
)

/** Unit-radius glow brushes, one per colour, built once. */
private val coreGlowBrushes = HashMap<Color, Brush>()

/** A soft glow in [color] filling the middle of a bubble, behind its icon. */
private fun DrawScope.drawCoreGlow(center: Offset, r: Float, color: Color) {
    val brush = coreGlowBrushes.getOrPut(color) {
        Brush.radialGradient(
            0f to color.copy(alpha = CORE_GLOW_ALPHA),
            0.6f to color.copy(alpha = CORE_GLOW_ALPHA * 0.35f),
            1f to Color.Transparent,
            center = Offset.Zero,
            radius = 1f,
        )
    }
    withTransform({
        translate(center.x, center.y)
        scale(r * CORE_GLOW_SIZE, r * CORE_GLOW_SIZE, pivot = Offset.Zero)
    }) {
        drawCircle(brush, radius = 1f, center = Offset.Zero)
    }
}

/** Swirling toxic greens inside a skull bubble, turning as it rises: dangerous at a glance. */
private val ToxicSwirlBrush = Brush.sweepGradient(
    listOf(
        Color(0x0039FF14), Color(0x8839FF14), Color(0x00B2FF59),
        Color(0x7700E676), Color(0x0039FF14), Color(0x88CCFF00), Color(0x0039FF14),
    ),
    center = Offset.Zero,
)

private fun DrawScope.drawToxicSwirl(center: Offset, r: Float, rot: Float) {
    withTransform({
        translate(center.x, center.y)
        rotate(rot, pivot = Offset.Zero)
        scale(r, r, pivot = Offset.Zero)
    }) {
        drawCircle(ToxicSwirlBrush, radius = TOXIC_SWIRL_SIZE, center = Offset.Zero, alpha = TOXIC_SWIRL_ALPHA)
    }
}

private const val CORE_GLOW_ALPHA = 0.55f
/** The core glow's reach, in bubble radii. */
private const val CORE_GLOW_SIZE = 0.72f
private const val TOXIC_SWIRL_SIZE = 0.78f
private const val TOXIC_SWIRL_ALPHA = 0.85f
/** The skull's swirl turns faster than normal film, so it looks restless. */
private const val TOXIC_SWIRL_PER_PX = 0.5f

/**
 * Accessibility: a black-and-yellow striped ring, so skulls stand out by shape
 * and contrast rather than by their green colour.
 */
private fun DrawScope.drawHazardRing(center: Offset, r: Float) {
    val ringR = r + HAZARD_GAP_DP.dp.toPx()
    val width = HAZARD_WIDTH_DP.dp.toPx()
    val dash = HAZARD_DASH_DP.dp.toPx()
    drawCircle(Color.Black, radius = ringR, center = center, style = Stroke(width = width))
    drawCircle(
        color = HazardYellow,
        radius = ringR,
        center = center,
        style = Stroke(width = width, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))),
    )
}

private val HazardYellow = Color(0xFFFFEB3B)
private const val HAZARD_GAP_DP = 3f
private const val HAZARD_WIDTH_DP = 4f
private const val HAZARD_DASH_DP = 6f

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

/** Rainbow around the full circle, centred on the origin. */
private val POP_RING_BRUSH = Brush.sweepGradient(
    listOf(Color(0xFFFF80CE), Color(0xFF80D8FF), Color(0xFF69F0AE), Color(0xFFFFFF8D), Color(0xFFFF80CE)),
    center = Offset.Zero,
)

/** The default "Rainbow ring" burst. Scales with [PopAnimation.intensity]: higher combos → wider, thicker bursts. */
internal fun DrawScope.drawRainbowPop(anim: PopAnimation) {
    val p      = anim.progress
    val k      = anim.intensity
    val alpha  = (1f - p).coerceIn(0f, 1f)

    val ringR    = anim.radius * (1f + p * 3f * k)
    val strokeW  = ((3f * (1f - p) + 0.5f) * k).dp.toPx()

    // One circle with a rainbow sweep gradient (was 36 arcs).
    translate(anim.x, anim.y) {
        drawCircle(brush = POP_RING_BRUSH, radius = ringR, center = Offset.Zero, alpha = alpha, style = Stroke(width = strokeW))
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

}


/**
 * Shared by every pop style, drawn under it: the bubble's real burst — a bright
 * flash, a ring of water droplets that fly out and fall, and a few curved shards
 * of soap film spinning away. Bigger with [PopAnimation.intensity].
 */
internal fun DrawScope.drawPopBurst(anim: PopAnimation) {
    val p = anim.progress
    val k = anim.intensity
    val r = anim.radius
    val fade = (1f - p).coerceIn(0f, 1f)
    val seed = anim.id.hashCode()

    // Flash: a soft white glow that fills the bubble and fades fast.
    if (p < FLASH_END) {
        val flash = 1f - p / FLASH_END
        withTransform({
            translate(anim.x, anim.y)
            val s = r * (1f + p * 2f)
            scale(s, s, pivot = Offset.Zero)
        }) {
            drawCircle(FlashBrush, radius = 1f, center = Offset.Zero, alpha = flash * FLASH_ALPHA)
        }
    }

    // Film shards: short curved strips of the bubble's skin, flying out and turning.
    val shardR = r * (1f + p * 1.6f * k)
    val shardStroke = Stroke(width = (r * 0.09f * fade).coerceAtLeast(0.5f), cap = StrokeCap.Round)
    for (i in 0 until SHARD_COUNT) {
        val start = i * (360f / SHARD_COUNT) + (seed % 60) + p * 50f
        drawArc(
            color = SHARD_COLORS[(i + seed).mod(SHARD_COLORS.size)],
            startAngle = start,
            sweepAngle = SHARD_SWEEP * (1f - p * 0.5f),
            useCenter = false,
            topLeft = Offset(anim.x - shardR, anim.y - shardR),
            size = Size(shardR * 2f, shardR * 2f),
            style = shardStroke,
            alpha = fade * fade,
        )
    }

    // Droplets: fly out at slightly different speeds, then gravity pulls them down.
    val dropR = (r * 0.065f * (1f - p * 0.6f)).coerceAtLeast(0.5f)
    for (i in 0 until DROPLET_COUNT) {
        val angle = (i.toFloat() / DROPLET_COUNT) * 2f * PI.toFloat() + (seed % 10) * 0.1f
        val speed = 0.8f + ((i * 37 + seed).mod(10)) * 0.05f
        val reach = r * (0.95f + p * 2.4f * k * speed)
        val x = anim.x + cos(angle) * reach
        val y = anim.y + sin(angle) * reach + p * p * r * DROPLET_FALL
        drawCircle(DropletColor, radius = dropR, center = Offset(x, y), alpha = fade * 0.9f)
        // A tiny highlight on each droplet.
        drawCircle(Color.White, radius = dropR * 0.4f, center = Offset(x - dropR * 0.3f, y - dropR * 0.3f), alpha = fade)
    }
}

private val FlashBrush = Brush.radialGradient(
    listOf(Color.White, Color.White.copy(alpha = 0.5f), Color.Transparent),
    center = Offset.Zero,
    radius = 1f,
)
/** The soap-film colours the shards take. */
private val SHARD_COLORS = listOf(
    Color(0xFFFF80CE), Color(0xFF80D8FF), Color(0xFFFFE57F), Color(0xFFB388FF), Color(0xFF69F0AE),
)
private val DropletColor = Color(0xFFE1F5FE)
private const val FLASH_END = 0.18f
private const val FLASH_ALPHA = 0.75f
private const val SHARD_COUNT = 5
private const val SHARD_SWEEP = 34f
private const val DROPLET_COUNT = 10
/** How far droplets fall by the end, in bubble radii. */
private const val DROPLET_FALL = 1.4f
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
    val fade = alpha.coerceIn(0f, 1f)

    // Laid out once at full size (the measurer caches it); the pop-in scale and the
    // fade are applied while drawing, so no frame lays the text out again.
    val layout = measurer.measure(
        text = text.text,
        style = TextStyle(
            fontSize = baseSize.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = fontFamily,
        ),
    )
    val center = Offset(text.x, text.y - rise)
    scale(0.6f + 0.4f * popIn, pivot = center) {
        drawText(
            textLayoutResult = layout,
            color = text.color.copy(alpha = fade),
            topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f),
            shadow = Shadow(color = Color.Black.copy(alpha = 0.5f * fade), blurRadius = 6f),
        )
    }
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