package com.apps.bubblepopping

/*
// ─────────────────────────────────────────────────────────────────────────────
// Bubble
// ─────────────────────────────────────────────────────────────────────────────

*/
/**
 * Draws a single bubble with a layered, translucent look:
 *
 *  Layer 1 – translucent fill      (alpha ~0.20)  gives body/colour
 *  Layer 2 – outer rim stroke      (alpha ~0.70)  defines the edge
 *  Layer 3 – inner ring stroke     (alpha ~0.25)  adds depth / thickness
 *  Layer 4 – oval shine highlight  (alpha ~0.55)  the classic bubble sheen
 *  Layer 5 – tiny sparkle dot      (alpha ~0.90)  punch of light
 *//*

fun DrawScope.drawBubble(bubble: Bubble) {
    val cx = bubble.x
    val cy = bubble.y
    val r  = bubble.radius
    val center = Offset(cx, cy)

    // ── Layer 1 : translucent fill ──────────────────────────────────────────
    drawCircle(
        color  = bubble.color.copy(alpha = 0.20f),
        radius = r,
        center = center,
    )

    // ── Layer 2 : outer rim ─────────────────────────────────────────────────
    drawCircle(
        color  = bubble.color.copy(alpha = 0.70f),
        radius = r,
        center = center,
        style  = Stroke(width = 1.5.dp.toPx()),
    )

    // ── Layer 3 : inner ring (creates an impression of glass thickness) ─────
    drawCircle(
        color  = bubble.color.copy(alpha = 0.25f),
        radius = r * 0.78f,
        center = center,
        style  = Stroke(width = 0.8.dp.toPx()),
    )

    // ── Layer 4 : oval shine highlight ──────────────────────────────────────
    // The shimmerAngle rotates the highlight slightly per bubble for variety.
    val angleRad = ((bubble.shimmerAngle - 50f) * (PI / 180.0)).toFloat()
    val shineOffX = (r * 0.28f * cos(angleRad))
    val shineOffY = (r * 0.28f * sin(angleRad))
    val shineW = r * 0.42f
    val shineH = r * 0.22f
    drawOval(
        color   = Color.White.copy(alpha = 0.55f),
        topLeft = Offset(cx + shineOffX - shineW / 2f, cy + shineOffY - shineH / 2f),
        size    = Size(shineW, shineH),
    )

    // ── Layer 5 : sparkle dot ───────────────────────────────────────────────
    drawCircle(
        color  = Color.White.copy(alpha = 0.90f),
        radius = r * 0.07f,
        center = Offset(cx + shineOffX, cy + shineOffY),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Pop animation
// ─────────────────────────────────────────────────────────────────────────────

*/
/**
 * Draws the burst animation when a bubble is popped.
 * Three layered effects driven by [anim.progress] (0f → 1f):
 *
 *  1. Expanding ring       – grows outward and fades
 *  2. Particle dots (×8)   – fly outward like water droplets
 *  3. White inner flash    – only visible in the first 30% of the animation
 *//*

fun DrawScope.drawPopAnimation(anim: PopAnimation) {
    val p      = anim.progress
    val alpha  = (1f - p).coerceIn(0f, 1f)
    val center = Offset(anim.x, anim.y)

    // ── 1. Expanding ring ───────────────────────────────────────────────────
    val ringRadius = anim.radius * (1f + p * 2.8f)
    val strokePx   = (2.5f * (1f - p) + 0.5f).dp.toPx()
    drawCircle(
        color  = anim.color.copy(alpha = alpha * 0.75f),
        radius = ringRadius,
        center = center,
        style  = Stroke(width = strokePx),
    )

    // ── 2. Particle dots ────────────────────────────────────────────────────
    val particleCount  = 8
    val dotRadius      = anim.radius * 0.14f * (1f - p * 0.6f)
    val spreadDist     = anim.radius * (1.2f + p * 3.2f)

    for (i in 0 until particleCount) {
        val angle = (i.toFloat() / particleCount) * 2f * PI.toFloat()
        drawCircle(
            color  = anim.color.copy(alpha = alpha * 0.85f),
            radius = dotRadius.coerceAtLeast(0f),
            center = Offset(
                anim.x + cos(angle) * spreadDist,
                anim.y + sin(angle) * spreadDist,
            ),
        )
    }

    // ── 3. Inner white flash (early phase only) ─────────────────────────────
    if (p < 0.30f) {
        val flashAlpha = ((0.30f - p) / 0.30f) * 0.45f
        drawCircle(
            color  = Color.White.copy(alpha = flashAlpha),
            radius = anim.radius * (1f - p * 2f).coerceAtLeast(0f),
            center = center,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Breeze streak lines
// ─────────────────────────────────────────────────────────────────────────────

*/
/**
 * Draws subtle horizontal streaks that drift in the direction of the breeze.
 * Lines are only visible when [breezeForce] is above a small threshold so
 * they appear and disappear smoothly with the force.
 *
 * @param breezeForce  current wind force from the ViewModel (px/s)
 * @param time         elapsed seconds — used to animate the scroll offset
 *//*

fun DrawScope.drawBreezeLines(breezeForce: Float, time: Float) {
    if (abs(breezeForce) < 4f) return

    val normalised = (abs(breezeForce) / 160f).coerceIn(0f, 1f)
    val lineAlpha  = normalised * 0.35f
    val goingRight = breezeForce > 0f
    val sign       = if (goingRight) 1f else -1f

    val lineCount   = 7
    val lineLength  = size.width * 0.12f
    val scrollSpeed = abs(breezeForce) * 0.8f    // px per second

    // Stagger lines vertically
    val verticalStep = size.height / (lineCount + 1)

    for (i in 1..lineCount) {
        val baseY = verticalStep * i
        // Each line has a slight vertical jitter based on its index
        val y     = baseY + (i % 3 - 1) * 12f

        // Scroll the line's start across the canvas; wrap with modulo
        val rawOffset  = (time * scrollSpeed * sign + i * 80f)
        val scrolledX  = rawOffset % size.width
        val startX     = if (goingRight) scrolledX else size.width - scrolledX

        drawLine(
            color       = Color.White.copy(alpha = lineAlpha),
            start       = Offset(startX, y),
            end         = Offset(startX + sign * lineLength, y),
            strokeWidth = 1.dp.toPx(),
        )
    }
}*/

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin


// ─────────────────────────────────────────────────────────────────────────────
// Bubble
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Draws a realistic iridescent soap bubble.
 *
 * Layer breakdown (back to front):
 *  1. Ghost fill               — barely-there body
 *  2. Iridescent outer rim     — sweep gradient (pink→blue→green→yellow→pink)
 *  3. Inner rim glow           — second sweep, shifted hue, makes rim look thick
 *  4. Large soft reflection    — the big white smear seen on real soap bubbles
 *  5. Secondary crescent       — smaller reflection on the opposite side
 *  6. Sparkle dots             — 3 crisp white points of light on the rim
 */
/*fun DrawScope.drawBubble(bubble: Bubble) {
    val cx     = bubble.x
    val cy     = bubble.y
    val r      = bubble.radius
    val center = Offset(cx, cy)

    // shimmerAngle rotates the rainbow position so no two bubbles look alike
    val rotDeg = bubble.shimmerAngle

    // ── 1. Ghost fill ────────────────────────────────────────────────────────
    drawCircle(
        color  = Color(0xFFD0E8FF).copy(alpha = 0.07f),
        radius = r,
        center = center,
    )

    // ── 2. Iridescent outer rim ──────────────────────────────────────────────
    val rimWidth = (r * 0.13f).coerceAtLeast(2.5f)

    val iridOuter = Brush.sweepGradient(
        colorStops = arrayOf(
            0.00f to Color(0xFFFF80CE),   // pink-magenta
            0.15f to Color(0xFF80D8FF),   // sky blue
            0.30f to Color(0xFF69F0AE),   // mint green
            0.45f to Color(0xFFFFFF8D),   // warm yellow
            0.60f to Color(0xFFFF6E40),   // orange
            0.75f to Color(0xFFEA80FC),   // lavender
            0.90f to Color(0xFF80DEEA),   // cyan
            1.00f to Color(0xFFFF80CE),   // back to pink — seamless loop
        ),
        center = center,
    )

    rotate(degrees = rotDeg, pivot = center) {
        drawCircle(
            brush  = iridOuter,
            radius = r,
            center = center,
            style  = Stroke(width = rimWidth),
        )
    }

    // ── 3. Inner rim glow ────────────────────────────────────────────────────
    val iridInner = Brush.sweepGradient(
        colorStops = arrayOf(
            0.00f to Color(0xFF80D8FF).copy(alpha = 0.55f),
            0.25f to Color(0xFF69F0AE).copy(alpha = 0.45f),
            0.50f to Color(0xFFFF80CE).copy(alpha = 0.55f),
            0.75f to Color(0xFFFFFF8D).copy(alpha = 0.40f),
            1.00f to Color(0xFF80D8FF).copy(alpha = 0.55f),
        ),
        center = center,
    )

    rotate(degrees = rotDeg + 40f, pivot = center) {
        drawCircle(
            brush  = iridInner,
            radius = r - rimWidth * 0.5f,
            center = center,
            style  = Stroke(width = rimWidth * 0.5f),
        )
    }

    // ── 4. Large soft reflection blob ────────────────────────────────────────
    val blobAngleRad = ((rotDeg - 40f) * PI.toFloat() / 180f)
    val blobOffX     = r * 0.22f * cos(blobAngleRad)
    val blobOffY     = r * 0.22f * sin(blobAngleRad)
    val blobW        = r * 1.05f
    val blobH        = r * 0.45f
    val blobCenter   = Offset(cx + blobOffX, cy + blobOffY)

    rotate(degrees = rotDeg - 35f, pivot = blobCenter) {
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.38f),
                    Color.White.copy(alpha = 0.00f),
                ),
                center = blobCenter,
                radius = blobW * 0.6f,
            ),
            topLeft = Offset(blobCenter.x - blobW / 2f, blobCenter.y - blobH / 2f),
            size    = Size(blobW, blobH),
        )
    }

    // ── 5. Secondary crescent reflection ─────────────────────────────────────
    val secAngleRad = blobAngleRad + PI.toFloat()
    val secOffX     = r * 0.42f * cos(secAngleRad)
    val secOffY     = r * 0.42f * sin(secAngleRad)
    val secCenter   = Offset(cx + secOffX, cy + secOffY)
    val secW        = r * 0.55f
    val secH        = r * 0.18f

    rotate(degrees = rotDeg + 145f, pivot = secCenter) {
        drawOval(
            color   = Color.White.copy(alpha = 0.18f),
            topLeft = Offset(secCenter.x - secW / 2f, secCenter.y - secH / 2f),
            size    = Size(secW, secH),
        )
    }

    // ── 6. Sparkle dots ──────────────────────────────────────────────────────
    // Three crisp white dots placed on the rim at varying angles
    val sparkleDefs = listOf(
        Triple(rotDeg - 30f,  0.88f, 0.07f),   // main sparkle
        Triple(rotDeg + 80f,  0.85f, 0.045f),  // secondary
        Triple(rotDeg + 200f, 0.80f, 0.030f),  // tertiary (small)
    )
    for ((angleDeg, radiusFrac, sizeFrac) in sparkleDefs) {
        val aRad = angleDeg * PI.toFloat() / 180f
        drawCircle(
            color  = Color.White.copy(alpha = 0.92f),
            radius = r * sizeFrac,
            center = Offset(
                cx + r * radiusFrac * cos(aRad),
                cy + r * radiusFrac * sin(aRad),
            ),
        )
    }
}*/

// ─────────────────────────────────────────────────────────────────────────────
// Pop animation
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Expanding iridescent ring + water-droplet particles when a bubble pops.
 */
/*fun DrawScope.drawPopAnimation(anim: PopAnimation) {
    val p      = anim.progress   // 0f → 1f
    val alpha  = (1f - p).coerceIn(0f, 1f)
    val center = Offset(anim.x, anim.y)

    // Expanding iridescent ring
    val ringRadius = anim.radius * (1f + p * 3f)
    val strokePx   = (3f * (1f - p) + 0.5f).dp.toPx()

    drawCircle(
        brush = Brush.sweepGradient(
            colors = listOf(
                Color(0xFFFF80CE).copy(alpha = alpha),
                Color(0xFF80D8FF).copy(alpha = alpha),
                Color(0xFF69F0AE).copy(alpha = alpha),
                Color(0xFFFFFF8D).copy(alpha = alpha),
                Color(0xFFFF80CE).copy(alpha = alpha),
            ),
            center = center,
        ),
        radius = ringRadius,
        center = center,
        style  = Stroke(width = strokePx),
    )

    // Water-droplet particles
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

    // Inner white flash (first 25% only)
    if (p < 0.25f) {
        val flashAlpha = ((0.25f - p) / 0.25f) * 0.5f
        drawCircle(
            color  = Color.White.copy(alpha = flashAlpha),
            radius = anim.radius * (1f - p * 3f).coerceAtLeast(0f),
            center = center,
        )
    }
}*/

// ─────────────────────────────────────────────────────────────────────────────
// Breeze streak lines
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Subtle horizontal streaks that drift in the direction of the breeze.
 * Only visible while [breezeForce] is above a small threshold.
 */
/*
fun DrawScope.drawBreezeLines(breezeForce: Float, time: Float) {
    if (abs(breezeForce) < 4f) return

    val normalised  = (abs(breezeForce) / 160f).coerceIn(0f, 1f)
    val lineAlpha   = normalised * 0.30f
    val goingRight  = breezeForce > 0f
    val sign        = if (goingRight) 1f else -1f
    val lineCount   = 7
    val lineLength  = size.width * 0.10f
    val scrollSpeed = abs(breezeForce) * 0.8f
    val vertStep    = size.height / (lineCount + 1)

    for (i in 1..lineCount) {
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
}*/


// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Returns an interpolated [Color] at position [t] (0f..1f) across a list of
 * evenly-spaced colour stops.  Used to paint the iridescent rim segments.
 */
/*private fun lerpRainbow(t: Float, stops: List<Color>): Color {
    val segments = stops.size - 1
    val scaled   = (t * segments).coerceIn(0f, segments.toFloat())
    val idx      = scaled.toInt().coerceIn(0, segments - 1)
    val frac     = scaled - idx
    return lerp(stops[idx], stops[idx + 1], frac)
}

// ─────────────────────────────────────────────────────────────────────────────
// Bubble
// ─────────────────────────────────────────────────────────────────────────────

*//**
 * Draws a realistic iridescent soap bubble.
 *
 * Why segmented arcs instead of sweepGradient-stroke?
 * ────────────────────────────────────────────────────
 * Brush.sweepGradient applied as a Stroke has two known rendering problems on
 * Compose Multiplatform:
 *   1. A visible seam / gap at the 0°/360° junction.
 *   2. The gradient is clipped slightly inside the stroke bounds, so it looks
 *      thinner than intended and misaligned with the circle edge.
 *
 * Using 72 × drawArc calls (5° each, with a tiny 0.8° overlap) avoids both
 * problems: each segment is a solid colour, the transitions are smooth because
 * adjacent segments are only 5° apart, and every arc is drawn at exactly
 * the same radius so the rim sits perfectly on the bubble outline.
 *
 * Layer order (back → front):
 *   1. Ghost fill
 *   2. Iridescent rim  — 72 arc segments
 *   3. Bright rim highlight arc  — partial white arc, sells the glass look
 *   4. Primary reflection blob   — large soft white oval (upper area)
 *   5. Secondary crescent arc    — thin arc stroke (lower-opposite area)
 *   6. Sparkle dots              — crisp white points on the rim
 *//*
fun DrawScope.drawBubble(bubble: Bubble) {
    val cx        = bubble.x
    val cy        = bubble.y
    val r         = bubble.radius
    val center    = Offset(cx, cy)
    val rotDeg    = bubble.shimmerAngle   // rotates the rainbow per-bubble

    // Rim stroke is centered exactly on r, so outer edge = r + rimWidth/2,
    // inner edge = r − rimWidth/2.  This is the "outline" of the bubble.
    val rimWidth  = (r * 0.14f).coerceAtLeast(3f)
    val arcRect   = Offset(cx - r, cy - r)
    val arcSize   = Size(r * 2f, r * 2f)

    // ── 1. Ghost fill ────────────────────────────────────────────────────────
    drawCircle(
        color  = Color(0xFFD0EEFF).copy(alpha = 0.06f),
        radius = r,
        center = center,
    )

    // ── 2. Iridescent rim — 72 arc segments ─────────────────────────────────
    // Rainbow colour stops (first == last so the loop closes perfectly)
    val rainbowStops = listOf(
        Color(0xFFFF80CE),   // pink-magenta
        Color(0xFF80D8FF),   // sky blue
        Color(0xFF69F0AE),   // mint green
        Color(0xFFFFFF8D),   // warm yellow
        Color(0xFFFF6E40),   // orange
        Color(0xFFEA80FC),   // lavender
        Color(0xFF80DEEA),   // cyan
        Color(0xFFFF80CE),   // back to pink — seamless loop
    )

    val segCount      = 72
    val segAngle      = 360f / segCount
    val overlapAngle  = 0.8f   // tiny overlap so adjacent segments share an edge

    for (i in 0 until segCount) {
        val t          = i.toFloat() / segCount
        val segColor   = lerpRainbow(t, rainbowStops)
        val startAngle = rotDeg + t * 360f

        drawArc(
            color     = segColor,
            startAngle = startAngle,
            sweepAngle = segAngle + overlapAngle,
            useCenter  = false,
            topLeft    = arcRect,
            size       = arcSize,
            style      = Stroke(width = rimWidth, cap = StrokeCap.Butt),
        )
    }

    // ── 3. Bright rim highlight arc ──────────────────────────────────────────
    // A partial white arc (~110°) layered on top of the rainbow rim.
    // It sits on the same radius so it appears as a bright patch on the rim —
    // exactly the "glint" you see on real soap bubbles.
    val highlightWidth = rimWidth * 0.55f
    drawArc(
        color      = Color.White.copy(alpha = 0.70f),
        startAngle = rotDeg - 80f,
        sweepAngle = 110f,
        useCenter  = false,
        topLeft    = arcRect,
        size       = arcSize,
        style      = Stroke(width = highlightWidth, cap = StrokeCap.Round),
    )

    // ── 4. Primary reflection blob ───────────────────────────────────────────
    // Large, soft white oval in the upper-left quadrant of the bubble.
    // Radial gradient from white (centre) to transparent (edge).
    val blobAngleRad = (rotDeg - 45f) * PI.toFloat() / 180f
    val blobCX       = cx + r * 0.24f * cos(blobAngleRad)
    val blobCY       = cy + r * 0.24f * sin(blobAngleRad)
    val blobW        = r * 1.00f
    val blobH        = r * 0.40f

    // We draw the oval as a radialGradient on an Oval shape.
    // Rotating the DrawScope tilts the reflection naturally.
    val blobPivot = Offset(blobCX, blobCY)
    rotate(
        degrees = rotDeg - 30f, pivot = blobPivot
    ) {
        drawOval(
            brush   = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.40f),
                    Color.White.copy(alpha = 0.00f),
                ),
                center = blobPivot,
                radius = blobW * 0.55f,
            ),
            topLeft = Offset(blobCX - blobW / 2f, blobCY - blobH / 2f),
            size    = Size(blobW, blobH),
        )
    }

    // ── 5. Secondary crescent arc ─────────────────────────────────────────────
    // On real soap bubbles the secondary reflection is a thin curved arc,
    // not an oval.  It sits inside the bubble, roughly opposite the primary,
    // slightly smaller in radius so it looks "inset".
    //
    // We use drawArc with a Stroke (no fill) to get the crescent shape.
    val crescentR        = r * 0.62f           // inset from the rim
    val crescentStartAng = rotDeg + 140f       // opposite side from main highlight
    val crescentSweep    = 75f
    val crescentRect     = Offset(cx - crescentR, cy - crescentR)
    val crescentSize     = Size(crescentR * 2f, crescentR * 2f)

    drawArc(
        color      = Color.White.copy(alpha = 0.28f),
        startAngle = crescentStartAng,
        sweepAngle = crescentSweep,
        useCenter  = false,
        topLeft    = crescentRect,
        size       = crescentSize,
        style      = Stroke(
            width = (r * 0.06f).coerceAtLeast(1.5f),
            cap   = StrokeCap.Round,
        ),
    )

    // Softer, slightly wider version behind it for a glow effect
    drawArc(
        color      = Color.White.copy(alpha = 0.10f),
        startAngle = crescentStartAng - 5f,
        sweepAngle = crescentSweep + 10f,
        useCenter  = false,
        topLeft    = crescentRect,
        size       = crescentSize,
        style      = Stroke(
            width = (r * 0.12f).coerceAtLeast(2.5f),
            cap   = StrokeCap.Round,
        ),
    )

    // ── 6. Sparkle dots ──────────────────────────────────────────────────────
    // Three crisp white points placed exactly on the rim edge (at radius r).
    data class Sparkle(val angleDeg: Float, val size: Float, val alpha: Float)
    val sparkles = listOf(
        Sparkle(rotDeg - 35f,  r * 0.075f, 0.95f),   // main
        Sparkle(rotDeg + 75f,  r * 0.048f, 0.85f),   // secondary
        Sparkle(rotDeg + 195f, r * 0.032f, 0.75f),   // tertiary
    )
    for (sp in sparkles) {
        val aRad = sp.angleDeg * PI.toFloat() / 180f
        drawCircle(
            color  = Color.White.copy(alpha = sp.alpha),
            radius = sp.size,
            center = Offset(cx + r * cos(aRad), cy + r * sin(aRad)),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pop animation
// ─────────────────────────────────────────────────────────────────────────────

*//**
 * Expanding iridescent ring + water-droplet particles.
 *//*
fun DrawScope.drawPopAnimation(anim: PopAnimation) {
    val p      = anim.progress   // 0f → 1f
    val alpha  = (1f - p).coerceIn(0f, 1f)
    val center = Offset(anim.x, anim.y)

    // Expanding multi-segment iridescent ring (same technique as rim)
    val ringR    = anim.radius * (1f + p * 3f)
    val ringRect = Offset(anim.x - ringR, anim.y - ringR)
    val ringSize = Size(ringR * 2f, ringR * 2f)
    val strokeW  = (3f * (1f - p) + 0.5f).dp.toPx()

    val popStops = listOf(
        Color(0xFFFF80CE), Color(0xFF80D8FF), Color(0xFF69F0AE),
        Color(0xFFFFFF8D), Color(0xFFFF80CE),
    )
    val popSegCount = 36
    for (i in 0 until popSegCount) {
        val t     = i.toFloat() / popSegCount
        val color = lerpRainbow(t, popStops).copy(alpha = alpha)
        drawArc(
            color      = color,
            startAngle = t * 360f,
            sweepAngle = 360f / popSegCount + 0.5f,
            useCenter  = false,
            topLeft    = ringRect,
            size       = ringSize,
            style      = Stroke(width = strokeW, cap = StrokeCap.Butt),
        )
    }

    // Water-droplet particles
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

    // Inner white flash (first 25% only)
    if (p < 0.25f) {
        drawCircle(
            color  = Color.White.copy(alpha = ((0.25f - p) / 0.25f) * 0.5f),
            radius = anim.radius * (1f - p * 3f).coerceAtLeast(0f),
            center = center,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Breeze streak lines
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
}*/


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
// Bubble
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Bubble visual spec:
 *
 *  1. Ghost fill         — very subtle, near-transparent body
 *  2. Thin white outline — 0.8dp stroke, white alpha 0.45, at radius r
 *  3. Rainbow rim        — TWO arc segments sitting INSIDE the outline
 *                          with a small gap from it (drawn at r - gap - rimW/2)
 *                          Each arc fades in/out at its tips into the ghost color
 *  4. Primary blob       — large soft white reflection (upper area)
 *  5. Secondary crescent — bright arc, high alpha, opposite side
 *  6. Sparkle dots       — crisp white points on the outline edge
 */
fun DrawScope.drawBubble(bubble: Bubble) {
    val cx     = bubble.x
    val cy     = bubble.y
    val r      = bubble.radius
    val center = Offset(cx, cy)
    val rot    = bubble.shimmerAngle   // rotates everything per-bubble

    // ── 1. Ghost fill ────────────────────────────────────────────────────────
    drawCircle(
        color  = Color(0xFFD0EEFF).copy(alpha = 0.07f),
        radius = r,
        center = center,
    )

    // ── 2. Thin white outline ────────────────────────────────────────────────
    // Sits exactly at radius r.  Thin, barely-there — just defines the edge.
    val outlineWidth = 0.3f.dp.toPx()
    drawCircle(
        color  = Color.White.copy(alpha = 0.45f),
        radius = r,
        center = center,
        style  = Stroke(width = outlineWidth),
    )

    // ── 3. Two rainbow arc segments ──────────────────────────────────────────
    // Each arc spans ~130°.  The remaining ~100° on each side is empty (gap).
    // The arcs are drawn at radius (r - gap - rimWidth/2) so they are
    // visually INSIDE the outline with a clear gap between them.
    //
    // Segment A starts at rot +  10°  →  rot + 140°  (upper-left area)
    // Segment B starts at rot + 190°  →  rot + 320°  (lower-right area)
    //
    // Both ends are faded to transparent so they blend smoothly into the ghost.

    val rimWidth   = (r * 0.11f).coerceAtLeast(2.5f)
    val gapFromOutline = 1.2f.dp.toPx()          // gap between outline and rim
    val rimR       = r - gapFromOutline - rimWidth * 0.5f
    val rimRect    = Offset(cx - rimR, cy - rimR)
    val rimSize    = Size(rimR * 2f, rimR * 2f)

    // Ghost color (transparent version of bubble tint) — arcs fade into this
    val ghostColor = Color(0xFFD0EEFF).copy(alpha = 0f)

    // Rainbow colours for the two arcs (slightly different palette per arc)
    val arcAStops = listOf(
        ghostColor,
        Color(0xFFFF80CE),   // pink
        Color(0xFF80D8FF),   // sky blue
        Color(0xFF69F0AE),   // mint
        Color(0xFFFFFF8D),   // yellow
        ghostColor,
    )
    val arcBStops = listOf(
        ghostColor,
        Color(0xFFFF6E40),   // orange
        Color(0xFFEA80FC),   // lavender
        Color(0xFF80DEEA),   // cyan
        Color(0xFFFF80CE),   // pink
        ghostColor,
    )

    val arcSweep = 130f
    val segCount = 26        // segments per arc (5° each)
    val segAngle = arcSweep / segCount

    // Draw one arc using the given colour stops
    fun drawRainbowArc(startDeg: Float, stops: List<Color>) {
        for (i in 0 until segCount) {
            val t     = i.toFloat() / (segCount - 1)
            val color = lerpRainbow(t, stops)
            drawArc(
                color      = color,
                startAngle = startDeg + rot + i * segAngle,
                sweepAngle = segAngle + 0.8f,   // slight overlap — no gaps
                useCenter  = false,
                topLeft    = rimRect,
                size       = rimSize,
                style      = Stroke(width = rimWidth, cap = StrokeCap.Butt),
            )
        }
    }

    drawRainbowArc(startDeg = 10f,  stops = arcAStops)
    drawRainbowArc(startDeg = 190f, stops = arcBStops)

    // ── 4. Primary reflection blob ───────────────────────────────────────────
    val blobAngleRad = (rot - 45f) * PI.toFloat() / 180f
    val blobCX       = cx + r * 0.22f * cos(blobAngleRad)
    val blobCY       = cy + r * 0.22f * sin(blobAngleRad)
    val blobW        = r * 0.95f
    val blobH        = r * 0.38f
    val blobPivot    = Offset(blobCX, blobCY)

   rotate(
        degrees = rot - 30f, pivot = blobPivot
    ) {
        drawOval(
            brush   = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.36f),
                    Color.White.copy(alpha = 0.00f),
                ),
                center = blobPivot,
                radius = blobW * 0.55f,
            ),
            topLeft = Offset(blobCX - blobW / 2f, blobCY - blobH / 2f),
            size    = Size(blobW, blobH),
        )
    }

    // ── 5. Secondary crescent arc — bright ───────────────────────────────────
    // Sits inside the bubble at ~0.60r, on the opposite side from the blob.
    // High alpha so it reads clearly as a reflection.
    val crescentR     = r * 0.60f
    val crescentRect  = Offset(cx - crescentR, cy - crescentR)
    val crescentSize  = Size(crescentR * 2f, crescentR * 2f)
    val crescentStart = rot + 150f
    val crescentSweep = 70f
    val crescentStroke = (r * 0.055f).coerceAtLeast(2f)

    // Glow layer — wider, lower alpha
    drawArc(
        color      = Color.White.copy(alpha = 0.22f),
        startAngle = crescentStart - 6f,
        sweepAngle = crescentSweep + 12f,
        useCenter  = false,
        topLeft    = crescentRect,
        size       = crescentSize,
        style      = Stroke(width = crescentStroke * 2.4f, cap = StrokeCap.Round),
    )

    // Core crescent — bright and sharp
    drawArc(
        color      = Color.White.copy(alpha = 0.82f),
        startAngle = crescentStart,
        sweepAngle = crescentSweep,
        useCenter  = false,
        topLeft    = crescentRect,
        size       = crescentSize,
        style      = Stroke(width = crescentStroke, cap = StrokeCap.Round),
    )

    // Specular centre highlight — the very brightest middle portion
    drawArc(
        color      = Color.White.copy(alpha = 0.95f),
        startAngle = crescentStart + crescentSweep * 0.3f,
        sweepAngle = crescentSweep * 0.4f,
        useCenter  = false,
        topLeft    = crescentRect,
        size       = crescentSize,
        style      = Stroke(width = crescentStroke * 0.5f, cap = StrokeCap.Round),
    )

    // ── 6. Sparkle dots — on the outline edge ────────────────────────────────
    data class Sparkle(val angleDeg: Float, val size: Float, val alpha: Float)
    listOf(
        Sparkle(rot - 35f,  r * 0.072f, 0.95f),
        Sparkle(rot + 75f,  r * 0.046f, 0.85f),
        Sparkle(rot + 200f, r * 0.030f, 0.72f),
    ).forEach { sp ->
        val aRad = sp.angleDeg * PI.toFloat() / 180f
        drawCircle(
            color  = Color.White.copy(alpha = sp.alpha),
            radius = sp.size,
            center = Offset(cx + r * cos(aRad), cy + r * sin(aRad)),
        )
    }
}

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