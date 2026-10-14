package com.apps.bubblepopping.view.play.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
    "skin_jelly" -> NORMAL_SCHEME.copy(
        bodyTint  = Color(0xFFF8BBD0),
        arcAStops = rainbow(0xFFE040FB, 0xFFFF80AB, 0xFF84FFFF, 0xFFEA80FC),
        arcBStops = rainbow(0xFF7C4DFF, 0xFFFF4081, 0xFF18FFFF, 0xFFB388FF),
        crescentColor = Color(0xFFFF80AB),
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
    else -> null
}

/** A sample bubble in [skinId] (Classic shows the Sky palette), for the shop. */
fun DrawScope.drawSkinPreview(skinId: String, center: Offset, radius: Float, rotation: Float) {
    drawSoapBubble(center.x, center.y, radius, rotation, skinScheme(skinId) ?: NORMAL_SCHEME)
}

// ─────────────────────────────────────────────────────────────────────────────
// Pop effects
// ─────────────────────────────────────────────────────────────────────────────

/** Draws a pop burst in the equipped [effectId] style. */
fun DrawScope.drawPopAnimation(anim: PopAnimation, effectId: String = Cosmetics.RAINBOW.id) {
    when (effectId) {
        "pop_confetti" -> drawConfettiPop(anim)
        "pop_stars" -> drawShapePop(anim, PopShape.STAR)
        "pop_hearts" -> drawShapePop(anim, PopShape.HEART)
        else -> drawRainbowPop(anim)
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

private enum class PopShape { STAR, HEART }

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

private fun DrawScope.drawHeartShape(center: Offset, r: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y + r * 0.9f)
        cubicTo(center.x - r * 1.4f, center.y - r * 0.1f, center.x - r * 0.6f, center.y - r * 1.1f, center.x, center.y - r * 0.4f)
        cubicTo(center.x + r * 0.6f, center.y - r * 1.1f, center.x + r * 1.4f, center.y - r * 0.1f, center.x, center.y + r * 0.9f)
        close()
    }
    drawPath(path, color)
}
