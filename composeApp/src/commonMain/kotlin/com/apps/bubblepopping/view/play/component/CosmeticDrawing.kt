package com.apps.bubblepopping.view.play.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.apps.bubblepopping.meta.CosmeticKind
import com.apps.bubblepopping.meta.Cosmetics
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// Bubble skins (normal bubbles only)
// ─────────────────────────────────────────────────────────────────────────────

/** The palette for a skin, or null for Classic (which follows the world's palette). */
internal fun skinScheme(skinId: String): BubbleScheme? = SKIN_SCHEMES[skinId]

/** Built once, so the renderer's per-scheme brush cache always gets the same objects. */
private val SKIN_SCHEMES: Map<String, BubbleScheme> = Cosmetics.of(CosmeticKind.SKIN)
    .mapNotNull { skin -> buildSkinScheme(skin.id)?.let { skin.id to it } }
    .toMap()

private fun buildSkinScheme(skinId: String): BubbleScheme? = when (skinId) {
    "skin_cloud" -> NORMAL_SCHEME.copy(
        bodyTint  = Color(0xFFFFFFFF),
        outlineColor = Color.White.copy(alpha = 0.7f),
        arcAStops = rainbow(0xFFFFFFFF, 0xFFE3F2FD, 0xFFBBDEFB, 0xFFFFFFFF),
        arcBStops = rainbow(0xFFE1F5FE, 0xFFFFFFFF, 0xFF90CAF9, 0xFFE3F2FD),
    )
    "skin_storm" -> NORMAL_SCHEME.copy(
        bodyTint  = Color(0xFF90A4AE),
        outlineColor = Color(0xFFCFD8DC).copy(alpha = 0.7f),
        arcAStops = rainbow(0xFFB0BEC5, 0xFF7C4DFF, 0xFFFFEB3B, 0xFF90A4AE),
        arcBStops = rainbow(0xFF546E7A, 0xFFB388FF, 0xFFECEFF1, 0xFFFFF176),
        sparkleColor = Color(0xFFFFF59D),
    )
    "skin_jelly" -> NORMAL_SCHEME.copy(
        bodyTint  = Color(0xFFF8BBD0),
        arcAStops = rainbow(0xFFE040FB, 0xFFFF80AB, 0xFF84FFFF, 0xFFEA80FC),
        arcBStops = rainbow(0xFF7C4DFF, 0xFFFF4081, 0xFF18FFFF, 0xFFB388FF),
        crescentColor = Color(0xFFFF80AB),
    )
    "skin_kraken" -> NORMAL_SCHEME.copy(
        bodyTint  = Color(0xFF004D40),
        outlineColor = Color(0xFF64FFDA).copy(alpha = 0.6f),
        arcAStops = rainbow(0xFF1DE9B6, 0xFF00B8D4, 0xFF7C4DFF, 0xFF64FFDA),
        arcBStops = rainbow(0xFF00BFA5, 0xFF6200EA, 0xFF18FFFF, 0xFF009688),
        crescentColor = Color(0xFFA7FFEB),
    )
    "skin_galaxy" -> NORMAL_SCHEME.copy(
        bodyTint  = Color(0xFF311B92),
        outlineColor = Color(0xFFB388FF).copy(alpha = 0.6f),
        arcAStops = rainbow(0xFF651FFF, 0xFF00E5FF, 0xFFD500F9, 0xFF3D5AFE),
        arcBStops = rainbow(0xFF2979FF, 0xFFAA00FF, 0xFF00B0FF, 0xFF6200EA),
        sparkleColor = Color(0xFFFFF59D),
    )
    "skin_candy" -> NORMAL_SCHEME.copy(
        bodyTint  = Color(0xFFFFCDD2),
        arcAStops = rainbow(0xFFFF5252, 0xFFFFFFFF, 0xFFFF5252, 0xFFFFFFFF),
        arcBStops = rainbow(0xFF69F0AE, 0xFFFFFFFF, 0xFF69F0AE, 0xFFFFFFFF),
        crescentColor = Color(0xFFFFEBEE),
    )
    "skin_neon" -> NORMAL_SCHEME.copy(
        bodyTint  = Color(0xFF00E5FF),
        outlineColor = Color(0xFF00E5FF).copy(alpha = 0.9f),
        arcAStops = rainbow(0xFF00E5FF, 0xFF76FF03, 0xFF00E5FF, 0xFFFF1744),
        arcBStops = rainbow(0xFFFF00FF, 0xFF00E5FF, 0xFFFFEA00, 0xFFFF00FF),
        crescentColor = Color(0xFF00E5FF),
        sparkleColor = Color(0xFFFF00FF),
    )
    "skin_gold" -> NORMAL_SCHEME.copy(
        bodyTint  = Color(0xFFFFD54F),
        outlineColor = Color(0xFFFFE082).copy(alpha = 0.9f),
        arcAStops = rainbow(0xFFFFD700, 0xFFFFF8E1, 0xFFFFB300, 0xFFFFE082),
        arcBStops = rainbow(0xFFFF8F00, 0xFFFFECB3, 0xFFFFD54F, 0xFFFFFFFF),
        crescentColor = Color(0xFFFFF8E1),
        sparkleColor = Color(0xFFFFFDE7),
    )
    "skin_void" -> NORMAL_SCHEME.copy(
        bodyTint  = Color(0xFF0D0221),
        outlineColor = Color(0xFFB388FF).copy(alpha = 0.7f),
        arcAStops = rainbow(0xFF7C4DFF, 0xFF000000, 0xFFE040FB, 0xFF311B92),
        arcBStops = rainbow(0xFF651FFF, 0xFF18FFFF, 0xFF4A148C, 0xFFEA80FC),
        sparkleColor = Color(0xFFE1BEE7),
    )
    "skin_sugar" -> NORMAL_SCHEME.copy(
        bodyTint  = Color(0xFFFFF3E0),
        outlineColor = Color(0xFFFFCC80).copy(alpha = 0.8f),
        arcAStops = rainbow(0xFFFF80AB, 0xFFFFE57F, 0xFF80D8FF, 0xFFFFFFFF),
        arcBStops = rainbow(0xFFFFAB40, 0xFFF8BBD0, 0xFFB388FF, 0xFFFFF8E1),
        crescentColor = Color(0xFFFFFFFF),
        sparkleColor = Color(0xFFFFF59D),
    )
    "skin_phantom" -> NORMAL_SCHEME.copy(
        bodyTint  = Color(0xFFEDE7F6),
        outlineColor = Color.White.copy(alpha = 0.5f),
        arcAStops = rainbow(0xFFD1C4E9, 0xFFFFFFFF, 0xFFB39DDB, 0xFFE8EAF6),
        arcBStops = rainbow(0xFF9FA8DA, 0xFFF3E5F5, 0xFFCE93D8, 0xFFFFFFFF),
        crescentColor = Color(0xFFFFFFFF),
    )
    "skin_titan" -> NORMAL_SCHEME.copy(
        bodyTint  = Color(0xFFB3E5FC),
        outlineColor = Color(0xFFE1F5FE).copy(alpha = 0.95f),
        arcAStops = rainbow(0xFF00B0FF, 0xFFFFFFFF, 0xFF40C4FF, 0xFFB3E5FC),
        arcBStops = rainbow(0xFF0091EA, 0xFFE1F5FE, 0xFF80D8FF, 0xFFFFFFFF),
        crescentColor = Color(0xFFFFFFFF),
        sparkleColor = Color(0xFFFFFFFF),
    )
    "skin_peach" -> NORMAL_SCHEME.copy(
        bodyTint  = Color(0xFFFFE0B2),
        arcAStops = rainbow(0xFFFFAB91, 0xFFFFE0B2, 0xFFFF8A65, 0xFFFFCCBC),
        arcBStops = rainbow(0xFFFFB74D, 0xFFFFF3E0, 0xFFFF7043, 0xFFFFE0B2),
        crescentColor = Color(0xFFFFF3E0),
    )
    "skin_lavender" -> NORMAL_SCHEME.copy(
        bodyTint  = Color(0xFFE1BEE7),
        arcAStops = rainbow(0xFFCE93D8, 0xFFF3E5F5, 0xFFB39DDB, 0xFFE1BEE7),
        arcBStops = rainbow(0xFF9575CD, 0xFFEDE7F6, 0xFFBA68C8, 0xFFD1C4E9),
        crescentColor = Color(0xFFF3E5F5),
    )
    "skin_lava" -> NORMAL_SCHEME.copy(
        bodyTint  = Color(0xFFBF360C),
        outlineColor = Color(0xFFFF6E40).copy(alpha = 0.8f),
        arcAStops = rainbow(0xFFFF3D00, 0xFFFFAB00, 0xFFDD2C00, 0xFFFFD180),
        arcBStops = rainbow(0xFFFF6D00, 0xFFFFE57F, 0xFFD50000, 0xFFFF9E80),
        crescentColor = Color(0xFFFFD180),
        sparkleColor = Color(0xFFFFE57F),
    )
    "skin_sakura" -> NORMAL_SCHEME.copy(
        bodyTint  = Color(0xFFFCE4EC),
        outlineColor = Color(0xFFF8BBD0).copy(alpha = 0.8f),
        arcAStops = rainbow(0xFFF48FB1, 0xFFFFFFFF, 0xFFF06292, 0xFFFCE4EC),
        arcBStops = rainbow(0xFFFF80AB, 0xFFFFF8E1, 0xFFF8BBD0, 0xFFFFFFFF),
        crescentColor = Color(0xFFFFFFFF),
    )
    "skin_pearl" -> NORMAL_SCHEME.copy(
        bodyTint  = Color(0xFFFAFAFA),
        outlineColor = Color(0xFFE0E0E0).copy(alpha = 0.9f),
        arcAStops = rainbow(0xFFFFF8E1, 0xFFE1F5FE, 0xFFFCE4EC, 0xFFF3E5F5),
        arcBStops = rainbow(0xFFE8EAF6, 0xFFFFFDE7, 0xFFE0F7FA, 0xFFFFEBEE),
        crescentColor = Color(0xFFFFFFFF),
        sparkleColor = Color(0xFFFFFFFF),
    )
    "skin_royal" -> NORMAL_SCHEME.copy(
        bodyTint  = Color(0xFF283593),
        outlineColor = Color(0xFFFFD54F).copy(alpha = 0.8f),
        arcAStops = rainbow(0xFF3D5AFE, 0xFFFFD54F, 0xFF651FFF, 0xFFFFE082),
        arcBStops = rainbow(0xFF304FFE, 0xFFFFC400, 0xFF6200EA, 0xFFFFF8E1),
        crescentColor = Color(0xFFFFE082),
        sparkleColor = Color(0xFFFFD54F),
    )
    "skin_diamond" -> NORMAL_SCHEME.copy(
        bodyTint  = Color(0xFFE0F7FA),
        outlineColor = Color.White.copy(alpha = 0.95f),
        arcAStops = rainbow(0xFF18FFFF, 0xFFFFFFFF, 0xFFEA80FC, 0xFFFFFF8D),
        arcBStops = rainbow(0xFF82B1FF, 0xFFFFFFFF, 0xFF84FFFF, 0xFFFF80AB),
        crescentColor = Color(0xFFFFFFFF),
        sparkleColor = Color(0xFFFFFFFF),
    )
    else -> null
}

/**
 * A sample bubble in [skinId] (Classic shows the Sky palette), for the shop and the
 * menus' drifting bubbles. Drawn from the bubble sprites; [alpha] fades it.
 */
fun DrawScope.drawSkinPreview(skinId: String, center: Offset, radius: Float, rotation: Float, alpha: Float = 1f) {
    drawSoapBubbleSprite(center.x, center.y, radius, rotation, skinScheme(skinId) ?: NORMAL_SCHEME, alpha = alpha)
}

// ─────────────────────────────────────────────────────────────────────────────
// Pop effects
// ─────────────────────────────────────────────────────────────────────────────

/** Draws a pop burst in the equipped [effectId] style. */
fun DrawScope.drawPopAnimation(anim: PopAnimation, effectId: String = Cosmetics.RAINBOW.id) {
    // The bubble's own burst (flash, droplets, film shards), then the equipped style on top.
    drawPopBurst(anim)
    when (effectId) {
        "pop_confetti" -> drawConfettiPop(anim)
        "pop_stars" -> drawShapePop(anim, PopShape.STAR)
        "pop_hearts" -> drawShapePop(anim, PopShape.HEART)
        "pop_snow" -> drawShapePop(anim, PopShape.SNOWFLAKE)
        "pop_sparks" -> drawSparksPop(anim)
        "pop_bubbles" -> drawBubblesPop(anim)
        "pop_fireworks" -> drawFireworksPop(anim)
        else -> drawRainbowPop(anim)
    }
}

/** Golden streaks shoot straight out, thinning as they go. */
private fun DrawScope.drawSparksPop(anim: PopAnimation) {
    val p = anim.progress
    val alpha = (1f - p).coerceIn(0f, 1f)
    val count = (8 * anim.intensity).toInt().coerceIn(8, 16)
    val seed = anim.id.hashCode()
    for (i in 0 until count) {
        val angle = (i.toFloat() / count) * 2f * PI.toFloat() + (seed % 5) * 0.2f
        val reach = anim.radius * (0.7f + p * 2.8f * anim.intensity) * (0.8f + (i % 3) * 0.15f)
        val tail = anim.radius * 0.7f * (1f - p)
        val head = Offset(anim.x + cos(angle) * reach, anim.y + sin(angle) * reach)
        val back = Offset(head.x - cos(angle) * tail, head.y - sin(angle) * tail)
        val color = if (i % 2 == 0) Color(0xFFFFD54F) else Color(0xFFFFF8E1)
        drawLine(color.copy(alpha = alpha), back, head, strokeWidth = anim.radius * 0.12f * (1f - p * 0.7f), cap = StrokeCap.Round)
    }
}

/** Little soap bubbles float out and drift up as they fade. */
private fun DrawScope.drawBubblesPop(anim: PopAnimation) {
    val p = anim.progress
    val alpha = (1f - p).coerceIn(0f, 1f)
    val count = 7
    val seed = anim.id.hashCode()
    for (i in 0 until count) {
        val angle = (i.toFloat() / count) * 2f * PI.toFloat() + (seed % 3)
        val spread = anim.radius * (0.6f + p * 2f * anim.intensity.coerceAtMost(2f))
        val rise = p * anim.radius * 1.6f
        val center = Offset(anim.x + cos(angle) * spread, anim.y + sin(angle) * spread - rise)
        val r = anim.radius * (0.18f + (i % 3) * 0.06f)
        drawCircle(Color.White.copy(alpha = alpha * 0.2f), r, center)
        drawCircle(Color(0xFF80D8FF).copy(alpha = alpha * 0.9f), r, center, style = Stroke(width = r * 0.22f))
        drawCircle(Color.White.copy(alpha = alpha), r * 0.25f, Offset(center.x - r * 0.35f, center.y - r * 0.35f))
    }
}

private val FIREWORK_COLORS = listOf(Color(0xFFFF4081), Color(0xFFFFD54F), Color(0xFF40C4FF), Color(0xFFB2FF59), Color(0xFFEA80FC))

/** Two rings of coloured sparks, the outer one later and wider, falling a little. */
private fun DrawScope.drawFireworksPop(anim: PopAnimation) {
    val p = anim.progress
    val seed = anim.id.hashCode()
    for (ring in 0 until 2) {
        val local = ((p - ring * 0.15f) / (1f - ring * 0.15f)).coerceIn(0f, 1f)
        if (local <= 0f) continue
        val alpha = (1f - local).coerceIn(0f, 1f)
        val count = 10 + ring * 4
        val reach = anim.radius * (0.5f + local * (2.2f + ring * 1.2f) * anim.intensity.coerceAtMost(2.5f))
        for (i in 0 until count) {
            val angle = (i.toFloat() / count) * 2f * PI.toFloat() + ring * 0.3f
            val drop = local * local * anim.radius * 0.8f
            val dot = Offset(anim.x + cos(angle) * reach, anim.y + sin(angle) * reach + drop)
            val color = FIREWORK_COLORS[(i + ring + seed).mod(FIREWORK_COLORS.size)].copy(alpha = alpha)
            drawCircle(color, anim.radius * (0.1f - ring * 0.02f), dot)
            drawLine(
                color.copy(alpha = alpha * 0.5f),
                Offset(anim.x + cos(angle) * reach * 0.7f, anim.y + sin(angle) * reach * 0.7f + drop * 0.5f),
                dot,
                strokeWidth = anim.radius * 0.05f,
                cap = StrokeCap.Round,
            )
        }
    }
}

private val CONFETTI_POP_COLORS = listOf(
    Color(0xFFFFD54F), Color(0xFFFF80AB), Color(0xFF80D8FF), Color(0xFF69F0AE),
    Color(0xFFEA80FC), Color(0xFFFFAB40),
)

/** Paper scraps fly out, fall a little and spin. */
private fun DrawScope.drawConfettiPop(anim: PopAnimation) {
    val p = anim.progress
    val alpha = (1f - p).coerceIn(0f, 1f)
    val count = (10 * anim.intensity).toInt().coerceIn(10, 24)
    val seed = anim.id.hashCode()
    val scrap = anim.radius * 0.35f
    for (i in 0 until count) {
        val angle = (i.toFloat() / count) * 2f * PI.toFloat() + seed % 7
        val spread = anim.radius * (0.6f + p * 3f * anim.intensity) * (0.7f + (i % 3) * 0.2f)
        val x = anim.x + cos(angle) * spread
        val y = anim.y + sin(angle) * spread + p * p * anim.radius * 2f
        rotate(degrees = p * 540f + i * 40f, pivot = Offset(x, y)) {
            drawRect(
                color = CONFETTI_POP_COLORS[(i + seed).mod(CONFETTI_POP_COLORS.size)].copy(alpha = alpha),
                topLeft = Offset(x - scrap / 2f, y - scrap / 4f),
                size = Size(scrap, scrap / 2f),
            )
        }
    }
}

private enum class PopShape { STAR, HEART, SNOWFLAKE }

/** Little stars or hearts burst outward and fade. */
private fun DrawScope.drawShapePop(anim: PopAnimation, shape: PopShape) {
    val p = anim.progress
    val alpha = (1f - p).coerceIn(0f, 1f)
    val count = 6
    val size = anim.radius * 0.38f * (1f - p * 0.4f) * anim.intensity.coerceAtMost(2f)
    val spread = anim.radius * (0.8f + p * 2.6f * anim.intensity)
    for (i in 0 until count) {
        val angle = (i.toFloat() / count) * 2f * PI.toFloat() - PI.toFloat() / 2f
        val center = Offset(anim.x + cos(angle) * spread, anim.y + sin(angle) * spread)
        when (shape) {
            PopShape.STAR -> drawStarShape(center, size, Color(0xFFFFD54F).copy(alpha = alpha))
            PopShape.HEART -> drawHeartShape(center, size, Color(0xFFFF4081).copy(alpha = alpha))
            PopShape.SNOWFLAKE -> drawFlakeShape(center, size, Color(0xFFE1F5FE).copy(alpha = alpha))
        }
    }
    if (p < 0.3f) {
        drawCircle(
            color = Color.White.copy(alpha = (0.3f - p) / 0.3f * 0.5f),
            radius = anim.radius * (1f - p * 2f),
            center = Offset(anim.x, anim.y),
        )
    }
}

private fun DrawScope.drawStarShape(center: Offset, r: Float, color: Color) {
    val path = Path()
    for (i in 0 until 10) {
        val radius = if (i % 2 == 0) r else r * 0.45f
        val angle = (-90f + i * 36f) * PI.toFloat() / 180f
        val x = center.x + radius * cos(angle)
        val y = center.y + radius * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

/** A six-armed snowflake. */
private fun DrawScope.drawFlakeShape(center: Offset, r: Float, color: Color) {
    for (i in 0 until 3) {
        val angle = i * PI.toFloat() / 3f
        val dx = r * cos(angle)
        val dy = r * sin(angle)
        drawLine(color, Offset(center.x - dx, center.y - dy), Offset(center.x + dx, center.y + dy), r * 0.22f, StrokeCap.Round)
    }
    drawCircle(color, r * 0.22f, center)
}

private fun DrawScope.drawHeartShape(center: Offset, r: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y + r * 0.9f)
        cubicTo(center.x - r * 1.4f, center.y - r * 0.1f, center.x - r * 0.6f, center.y - r * 1.1f, center.x, center.y - r * 0.4f)
        cubicTo(center.x + r * 0.6f, center.y - r * 1.1f, center.x + r * 1.4f, center.y - r * 0.1f, center.x, center.y + r * 0.9f)
        close()
    }
    drawPath(path, color)
}
