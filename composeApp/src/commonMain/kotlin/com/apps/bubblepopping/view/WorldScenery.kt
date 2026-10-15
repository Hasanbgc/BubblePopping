package com.apps.bubblepopping.view

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.stage.World
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Scenery painted over a world's colours, shared by the stage map and the play
// screen: Sky — sun glow, fluffy clouds, bubble rings; Ocean — light rays, wave
// lines, rising bubbles; Space — nebulae, stars, a ringed planet; Candy — polka
// dots, sprinkles, lollipop swirls. Placed at seeded spots, so it never moves.

/** Draws [world]'s scenery over [area], placed by [random] (seed it for a stable look). */
internal fun DrawScope.drawScenery(world: World, area: SceneArea, random: Random) {
    when (world) {
        World.SKY -> drawSkyScenery(area, random)
        World.OCEAN -> drawOceanScenery(area, random)
        World.SPACE -> drawSpaceScenery(area, random)
        World.CANDY -> drawCandyScenery(area, random)
    }
}

/**
 * Aurora bands for the Aurora background: three soft, wavy ribbons of green,
 * teal and violet light across the top half, over a sprinkle of stars.
 */
internal fun DrawScope.drawAuroraScenery(area: SceneArea, random: Random) {
    repeat(area.count(14.dp.toPx())) {
        drawCircle(
            Color.White.copy(alpha = 0.3f + random.nextFloat() * 0.4f),
            radius = (0.6f + random.nextFloat() * 0.9f).dp.toPx(),
            center = Offset(area.x(random), area.top + random.nextFloat() * area.height * 0.7f),
        )
    }
    listOf(Color(0x5569F0AE), Color(0x4418FFFF), Color(0x44B388FF)).forEachIndexed { i, tint ->
        val y = area.top + area.height * (0.12f + i * 0.1f)
        val thickness = area.height * 0.09f
        val band = Path().apply {
            moveTo(0f, y)
            val step = area.width / 3f
            for (k in 0 until 3) {
                val up = if ((k + i) % 2 == 0) -thickness else thickness
                quadraticTo(step * k + step / 2f, y + up, step * (k + 1), y)
            }
        }
        drawPath(
            band,
            Brush.verticalGradient(listOf(Color.Transparent, tint, Color.Transparent), startY = y - thickness, endY = y + thickness),
            style = Stroke(width = thickness * 1.6f, cap = StrokeCap.Round),
        )
    }
}

/** A vertical span to decorate, in px. */
internal class SceneArea(val top: Float, val bottom: Float, val width: Float) {
    val height: Float get() = bottom - top
    /** How many decorations to place: one per [every] px of height. */
    fun count(every: Float) = (height / every).toInt().coerceAtLeast(1)
    fun y(random: Random) = top + random.nextFloat() * height
    fun x(random: Random) = random.nextFloat() * width
}

private fun DrawScope.drawSkyScenery(area: SceneArea, random: Random) {
    // Sun glow near the top of the world.
    val sun = Offset(area.width * 0.82f, area.top + area.height * 0.08f)
    drawCircle(
        Brush.radialGradient(listOf(Color(0x66FFF3C4), Color.Transparent), center = sun, radius = area.width * 0.6f),
        radius = area.width * 0.6f,
        center = sun,
    )
    // Fluffy clouds: a cluster of puffs with a lighter top.
    repeat(area.count(260.dp.toPx())) {
        val c = Offset(area.x(random), area.y(random))
        val r = (28 + random.nextInt(26)).dp.toPx()
        drawCloud(c, r)
    }
    // Drifting bubble rings.
    repeat(area.count(110.dp.toPx())) {
        val r = (4 + random.nextInt(10)).dp.toPx()
        drawCircle(Color.White.copy(alpha = 0.22f), radius = r, center = Offset(area.x(random), area.y(random)), style = Stroke(1.5.dp.toPx()))
    }
}

private fun DrawScope.drawCloud(c: Offset, r: Float) {
    val body = Color.White.copy(alpha = 0.13f)
    val top = Color.White.copy(alpha = 0.1f)
    drawCircle(body, r, c)
    drawCircle(body, r * 0.8f, Offset(c.x + r * 0.95f, c.y + r * 0.2f))
    drawCircle(body, r * 0.7f, Offset(c.x - r * 0.9f, c.y + r * 0.25f))
    drawCircle(body, r * 0.6f, Offset(c.x + r * 1.7f, c.y + r * 0.4f))
    drawCircle(top, r * 0.55f, Offset(c.x - r * 0.15f, c.y - r * 0.35f))
}

private fun DrawScope.drawOceanScenery(area: SceneArea, random: Random) {
    // Light rays slanting down from the surface.
    repeat(5) { i ->
        val x = area.width * (0.05f + i * 0.22f + random.nextFloat() * 0.06f)
        val w = area.width * (0.06f + random.nextFloat() * 0.05f)
        val length = area.height * 0.55f
        val ray = Path().apply {
            moveTo(x, area.top)
            lineTo(x + w, area.top)
            lineTo(x + w * 2.6f + length * 0.18f, area.top + length)
            lineTo(x + length * 0.18f, area.top + length)
            close()
        }
        drawPath(ray, Brush.verticalGradient(listOf(Color(0x33E0F7FA), Color.Transparent), startY = area.top, endY = area.top + length))
    }
    // Gentle wave lines.
    val wave = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    repeat(area.count(180.dp.toPx())) {
        val y = area.y(random)
        val x0 = area.x(random) - area.width * 0.2f
        val span = area.width * (0.3f + random.nextFloat() * 0.25f)
        val amp = 5.dp.toPx()
        val path = Path().apply {
            moveTo(x0, y)
            var x = x0
            val step = span / 4f
            repeat(4) { k ->
                val up = if (k % 2 == 0) -amp else amp
                quadraticTo(x + step / 2f, y + up, x + step, y)
                x += step
            }
        }
        drawPath(path, Color.White.copy(alpha = 0.12f), style = wave)
    }
    // Rising bubbles, each with a little highlight.
    repeat(area.count(70.dp.toPx())) {
        val r = (3 + random.nextInt(9)).dp.toPx()
        val c = Offset(area.x(random), area.y(random))
        drawCircle(Color.White.copy(alpha = 0.25f), radius = r, center = c, style = Stroke(1.5.dp.toPx()))
        drawCircle(Color.White.copy(alpha = 0.45f), radius = r * 0.25f, center = Offset(c.x - r * 0.35f, c.y - r * 0.35f))
    }
}

private fun DrawScope.drawSpaceScenery(area: SceneArea, random: Random) {
    // Nebula glows.
    listOf(Color(0x40B388FF), Color(0x33FF80CE), Color(0x3380D8FF)).forEach { tint ->
        val c = Offset(area.x(random), area.y(random))
        val r = area.width * (0.45f + random.nextFloat() * 0.3f)
        drawCircle(Brush.radialGradient(listOf(tint, Color.Transparent), center = c, radius = r), radius = r, center = c)
    }
    // Stars: many small dots, a few brighter ones with a cross glint.
    repeat(area.count(9.dp.toPx())) {
        val c = Offset(area.x(random), area.y(random))
        val bright = random.nextFloat() < 0.08f
        val r = if (bright) 1.8.dp.toPx() else (0.6f + random.nextFloat() * 0.9f).dp.toPx()
        drawCircle(Color.White.copy(alpha = 0.35f + random.nextFloat() * 0.5f), radius = r, center = c)
        if (bright) {
            val glint = r * 3.5f
            drawLine(Color.White.copy(alpha = 0.6f), Offset(c.x - glint, c.y), Offset(c.x + glint, c.y), strokeWidth = 1.dp.toPx(), cap = StrokeCap.Round)
            drawLine(Color.White.copy(alpha = 0.6f), Offset(c.x, c.y - glint), Offset(c.x, c.y + glint), strokeWidth = 1.dp.toPx(), cap = StrokeCap.Round)
        }
    }
    // A ringed planet near the top.
    val planet = Offset(area.width * 0.2f, area.top + area.height * 0.15f)
    val pr = 30.dp.toPx()
    drawCircle(
        Brush.linearGradient(listOf(Color(0xFFB39DDB), Color(0xFF5E35B1)), start = Offset(planet.x - pr, planet.y - pr), end = Offset(planet.x + pr, planet.y + pr)),
        radius = pr,
        center = planet,
        alpha = 0.75f,
    )
    rotate(-18f, pivot = planet) {
        drawOval(
            Color(0xAAE1BEE7),
            topLeft = Offset(planet.x - pr * 1.8f, planet.y - pr * 0.35f),
            size = Size(pr * 3.6f, pr * 0.7f),
            style = Stroke(3.dp.toPx()),
        )
    }
}

private val SPRINKLE_COLORS = listOf(Color(0xFFFFF59D), Color(0xFF80DEEA), Color(0xFFFFFFFF), Color(0xFFB9F6CA), Color(0xFFFFCC80))

private fun DrawScope.drawCandyScenery(area: SceneArea, random: Random) {
    // Soft polka dots.
    repeat(area.count(60.dp.toPx())) {
        drawCircle(Color.White.copy(alpha = 0.08f), radius = (6 + random.nextInt(10)).dp.toPx(), center = Offset(area.x(random), area.y(random)))
    }
    // Sprinkles: short rounded strokes at random angles.
    val sprinkle = 9.dp.toPx()
    repeat(area.count(28.dp.toPx())) {
        val c = Offset(area.x(random), area.y(random))
        val a = random.nextFloat() * 2f * PI.toFloat()
        val d = Offset(cos(a) * sprinkle / 2f, sin(a) * sprinkle / 2f)
        drawLine(SPRINKLE_COLORS[random.nextInt(SPRINKLE_COLORS.size)].copy(alpha = 0.5f), c - d, c + d, strokeWidth = 3.5.dp.toPx(), cap = StrokeCap.Round)
    }
    // Lollipop swirls: a soft disc with a pink spiral stripe.
    repeat(area.count(420.dp.toPx())) {
        val c = Offset(area.x(random), area.y(random))
        val r = (20 + random.nextInt(14)).dp.toPx()
        drawCircle(Color.White.copy(alpha = 0.16f), radius = r, center = c)
        for (k in 0 until 3) {
            val rr = r * (0.35f + k * 0.25f)
            drawArc(
                Color(0x55FF4081),
                startAngle = k * 120f,
                sweepAngle = 220f,
                useCenter = false,
                topLeft = Offset(c.x - rr, c.y - rr),
                size = Size(rr * 2f, rr * 2f),
                style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}

