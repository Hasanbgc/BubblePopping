package com.apps.bubblepopping.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.ui.theme.GameColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The game's icon set, drawn in code — no image files. Each icon is a full-colour
 * vector on a 24×24 grid with the indigo outline used across the UI, so draw
 * them with `Image(imageVector = …)` (not a tinted `Icon`).
 */
object GameIcons {

    private val InkStroke = SolidColor(GameColors.Ink)
    private const val OUTLINE = 1.6f

    val Back: ImageVector by lazy { twoPassStroke("Back") { moveTo(15f, 5f); lineTo(8f, 12f); lineTo(15f, 19f) } }

    val Close: ImageVector by lazy {
        twoPassStroke("Close") {
            moveTo(6.5f, 6.5f); lineTo(17.5f, 17.5f)
            moveTo(17.5f, 6.5f); lineTo(6.5f, 17.5f)
        }
    }

    val Check: ImageVector by lazy {
        twoPassStroke("Check", fill = GameColors.Success) { moveTo(5f, 12.5f); lineTo(10f, 17.5f); lineTo(19f, 7f) }
    }

    val Play: ImageVector by lazy {
        icon("Play") {
            path(fill = SolidColor(Color.White), stroke = InkStroke, strokeLineWidth = OUTLINE, strokeLineJoin = StrokeJoin.Round) {
                moveTo(8f, 4.5f); lineTo(19.5f, 12f); lineTo(8f, 19.5f); close()
            }
        }
    }

    val Pause: ImageVector by lazy {
        icon("Pause") {
            path(fill = SolidColor(Color.White), stroke = InkStroke, strokeLineWidth = OUTLINE) {
                roundRect(5.5f, 4.5f, 10f, 19.5f, 1.5f)
                roundRect(14f, 4.5f, 18.5f, 19.5f, 1.5f)
            }
        }
    }

    val Restart: ImageVector by lazy {
        icon("Restart") {
            val arc: PathBuilder.() -> Unit = {
                moveTo(18.5f, 12f)
                arcTo(6.5f, 6.5f, 0f, true, true, 15.5f, 6.5f)
            }
            path(stroke = InkStroke, strokeLineWidth = 5.4f, strokeLineCap = StrokeCap.Round, pathBuilder = arc)
            path(stroke = SolidColor(Color.White), strokeLineWidth = 3f, strokeLineCap = StrokeCap.Round, pathBuilder = arc)
            path(fill = SolidColor(Color.White), stroke = InkStroke, strokeLineWidth = 1.2f, strokeLineJoin = StrokeJoin.Round) {
                moveTo(13f, 3f); lineTo(19.5f, 5.5f); lineTo(14.5f, 10f); close()
            }
        }
    }

    val Coin: ImageVector by lazy {
        icon("Coin") {
            path(fill = SolidColor(GameColors.GoldBottom), stroke = InkStroke, strokeLineWidth = OUTLINE) { circle(12f, 12f, 9.5f) }
            path(fill = SolidColor(GameColors.GoldTop), stroke = SolidColor(GameColors.GoldLip), strokeLineWidth = 1f) { circle(12f, 12f, 6.3f) }
            path(fill = SolidColor(GameColors.GoldLip)) { star(12f, 12f, 3.6f, 1.6f) }
            path(fill = SolidColor(Color.White.copy(alpha = 0.7f))) { circle(8.2f, 7.8f, 1.3f) }
        }
    }

    val Star: ImageVector by lazy {
        icon("Star") {
            path(fill = SolidColor(GameColors.Gold), stroke = InkStroke, strokeLineWidth = OUTLINE, strokeLineJoin = StrokeJoin.Round) {
                star(12f, 12.6f, 10f, 4.6f)
            }
            path(fill = SolidColor(Color.White.copy(alpha = 0.6f))) { circle(10f, 9.5f, 1.2f) }
        }
    }

    val Heart: ImageVector by lazy {
        icon("Heart") {
            path(fill = SolidColor(Color(0xFFFF4F7B)), stroke = InkStroke, strokeLineWidth = OUTLINE, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 20.5f)
                curveTo(4f, 15f, 2.5f, 11f, 3.5f, 7.8f)
                curveTo(4.6f, 4.3f, 9.3f, 3.6f, 12f, 7.4f)
                curveTo(14.7f, 3.6f, 19.4f, 4.3f, 20.5f, 7.8f)
                curveTo(21.5f, 11f, 20f, 15f, 12f, 20.5f)
                close()
            }
            path(fill = SolidColor(Color.White.copy(alpha = 0.65f))) { circle(7.4f, 8.6f, 1.4f) }
        }
    }

    val Gear: ImageVector by lazy {
        icon("Gear") {
            path(
                fill = SolidColor(Color(0xFFE3E7FF)),
                stroke = InkStroke,
                strokeLineWidth = OUTLINE,
                strokeLineJoin = StrokeJoin.Round,
                pathFillType = PathFillType.EvenOdd,
            ) {
                val teeth = 8
                for (i in 0 until teeth * 4) {
                    // Each tooth: outer, outer, inner, inner — a flat-topped cog.
                    val r = if (i % 4 < 2) 10.2f else 7.6f
                    val angle = (i - 0.5f) / (teeth * 4) * 2f * PI.toFloat()
                    val x = 12f + r * cos(angle)
                    val y = 12f + r * sin(angle)
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
                circle(12f, 12f, 3.2f)
            }
        }
    }

    val Trophy: ImageVector by lazy {
        icon("Trophy") {
            // Handles
            path(stroke = InkStroke, strokeLineWidth = 4.2f, strokeLineCap = StrokeCap.Round) {
                moveTo(6.5f, 6f); curveTo(2f, 6f, 2.5f, 12f, 7.5f, 12f)
                moveTo(17.5f, 6f); curveTo(22f, 6f, 21.5f, 12f, 16.5f, 12f)
            }
            path(stroke = SolidColor(GameColors.GoldBottom), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
                moveTo(6.5f, 6f); curveTo(2f, 6f, 2.5f, 12f, 7.5f, 12f)
                moveTo(17.5f, 6f); curveTo(22f, 6f, 21.5f, 12f, 16.5f, 12f)
            }
            // Cup, stem and base
            path(fill = SolidColor(GameColors.Gold), stroke = InkStroke, strokeLineWidth = OUTLINE, strokeLineJoin = StrokeJoin.Round) {
                moveTo(6f, 3.5f); lineTo(18f, 3.5f); lineTo(17.5f, 9f)
                curveTo(17f, 13f, 14.5f, 14.5f, 12f, 14.5f)
                curveTo(9.5f, 14.5f, 7f, 13f, 6.5f, 9f); close()
            }
            path(fill = SolidColor(GameColors.GoldBottom), stroke = InkStroke, strokeLineWidth = OUTLINE, strokeLineJoin = StrokeJoin.Round) {
                moveTo(10.5f, 14.3f); lineTo(13.5f, 14.3f); lineTo(14f, 17.5f); lineTo(10f, 17.5f); close()
                roundRect(7f, 17.5f, 17f, 21f, 1.2f)
            }
            path(fill = SolidColor(Color.White.copy(alpha = 0.6f))) { roundRect(8.5f, 5f, 10f, 10f, 0.75f) }
        }
    }

    val Calendar: ImageVector by lazy {
        icon("Calendar") {
            path(fill = SolidColor(Color.White), stroke = InkStroke, strokeLineWidth = OUTLINE) { roundRect(3.5f, 5f, 20.5f, 20.5f, 2.5f) }
            path(fill = SolidColor(Color(0xFFFF5A7A)), stroke = InkStroke, strokeLineWidth = OUTLINE) { roundRect(3.5f, 5f, 20.5f, 9.5f, 2.5f) }
            path(fill = SolidColor(GameColors.InkSoft)) {
                for (row in 0..1) for (col in 0..2) circle(8f + col * 4f, 13f + row * 4f, 1f)
            }
            path(stroke = InkStroke, strokeLineWidth = 2.2f, strokeLineCap = StrokeCap.Round) {
                moveTo(8f, 3f); lineTo(8f, 7f); moveTo(16f, 3f); lineTo(16f, 7f)
            }
        }
    }

    val Shop: ImageVector by lazy {
        icon("Shop") {
            path(stroke = InkStroke, strokeLineWidth = 3.6f, strokeLineCap = StrokeCap.Round) {
                moveTo(8.5f, 9f); curveTo(8.5f, 2.5f, 15.5f, 2.5f, 15.5f, 9f)
            }
            path(stroke = SolidColor(Color.White), strokeLineWidth = 1.6f, strokeLineCap = StrokeCap.Round) {
                moveTo(8.5f, 9f); curveTo(8.5f, 2.5f, 15.5f, 2.5f, 15.5f, 9f)
            }
            path(fill = SolidColor(GameColors.Pink), stroke = InkStroke, strokeLineWidth = OUTLINE, strokeLineJoin = StrokeJoin.Round) {
                moveTo(4.5f, 7.5f); lineTo(19.5f, 7.5f); lineTo(18.5f, 20.5f); lineTo(5.5f, 20.5f); close()
            }
            path(fill = SolidColor(Color.White.copy(alpha = 0.5f))) { roundRect(7f, 10f, 8.5f, 17f, 0.75f) }
            path(fill = SolidColor(GameColors.Gold), stroke = InkStroke, strokeLineWidth = 1f) { star(13.5f, 13.8f, 3.2f, 1.5f) }
        }
    }

    val Lock: ImageVector by lazy {
        icon("Lock") {
            path(stroke = InkStroke, strokeLineWidth = 4.4f, strokeLineCap = StrokeCap.Round) {
                moveTo(8f, 11f); lineTo(8f, 8f); curveTo(8f, 3f, 16f, 3f, 16f, 8f); lineTo(16f, 11f)
            }
            path(stroke = SolidColor(Color(0xFFD5DBF0)), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
                moveTo(8f, 11f); lineTo(8f, 8f); curveTo(8f, 3f, 16f, 3f, 16f, 8f); lineTo(16f, 11f)
            }
            path(fill = SolidColor(GameColors.Gold), stroke = InkStroke, strokeLineWidth = OUTLINE) { roundRect(4.5f, 10f, 19.5f, 21f, 2.5f) }
            path(fill = SolidColor(GameColors.Ink)) { circle(12f, 14.5f, 1.6f); roundRect(11.3f, 15f, 12.7f, 18.3f, 0.6f) }
        }
    }

    val Home: ImageVector by lazy {
        icon("Home") {
            path(fill = SolidColor(Color.White), stroke = InkStroke, strokeLineWidth = OUTLINE, strokeLineJoin = StrokeJoin.Round) {
                moveTo(5.5f, 10.5f); lineTo(5.5f, 20.5f); lineTo(18.5f, 20.5f); lineTo(18.5f, 10.5f); close()
            }
            path(fill = SolidColor(Color(0xFFFF5A7A)), stroke = InkStroke, strokeLineWidth = OUTLINE, strokeLineJoin = StrokeJoin.Round) {
                moveTo(2.5f, 11.5f); lineTo(12f, 3f); lineTo(21.5f, 11.5f); close()
            }
            path(fill = SolidColor(GameColors.BlueBottom), stroke = InkStroke, strokeLineWidth = 1.2f) { roundRect(10f, 14.5f, 14f, 20.5f, 1f) }
        }
    }

    val Person: ImageVector by lazy {
        icon("Person") {
            path(fill = SolidColor(GameColors.BlueTop), stroke = InkStroke, strokeLineWidth = OUTLINE) {
                moveTo(3.5f, 21f); curveTo(3.5f, 15f, 7.5f, 13f, 12f, 13f); curveTo(16.5f, 13f, 20.5f, 15f, 20.5f, 21f); close()
            }
            path(fill = SolidColor(Color(0xFFFFE0C2)), stroke = InkStroke, strokeLineWidth = OUTLINE) { circle(12f, 8f, 4.6f) }
        }
    }

    val Chest: ImageVector by lazy {
        icon("Chest") {
            path(fill = SolidColor(Color(0xFFC2773A)), stroke = InkStroke, strokeLineWidth = OUTLINE) { roundRect(3f, 11f, 21f, 20.5f, 2f) }
            path(fill = SolidColor(Color(0xFFE09A55)), stroke = InkStroke, strokeLineWidth = OUTLINE, strokeLineJoin = StrokeJoin.Round) {
                moveTo(3f, 11f); lineTo(3f, 9f); curveTo(3f, 4f, 21f, 4f, 21f, 9f); lineTo(21f, 11f); close()
            }
            path(fill = SolidColor(GameColors.Gold), stroke = InkStroke, strokeLineWidth = 1.2f) {
                roundRect(10f, 9.5f, 14f, 14.5f, 1f)
            }
        }
    }

    val Target: ImageVector by lazy {
        icon("Target") {
            path(fill = SolidColor(Color(0xFFFF5A7A)), stroke = InkStroke, strokeLineWidth = OUTLINE) { circle(12f, 12f, 9.5f) }
            path(fill = SolidColor(Color.White)) { circle(12f, 12f, 6.4f) }
            path(fill = SolidColor(Color(0xFFFF5A7A))) { circle(12f, 12f, 3.4f) }
        }
    }

    val Crown: ImageVector by lazy {
        icon("Crown") {
            path(fill = SolidColor(GameColors.Gold), stroke = InkStroke, strokeLineWidth = OUTLINE, strokeLineJoin = StrokeJoin.Round) {
                moveTo(3f, 7f); lineTo(7.5f, 11.5f); lineTo(12f, 4.5f); lineTo(16.5f, 11.5f); lineTo(21f, 7f)
                lineTo(19.5f, 18.5f); lineTo(4.5f, 18.5f); close()
            }
            path(fill = SolidColor(GameColors.Pink)) { circle(12f, 14f, 1.5f) }
        }
    }

    val Music: ImageVector by lazy {
        icon("Music") {
            path(fill = SolidColor(Color.White), stroke = InkStroke, strokeLineWidth = OUTLINE, strokeLineJoin = StrokeJoin.Round) {
                moveTo(9f, 17f); lineTo(9f, 5.5f); lineTo(19f, 3.5f); lineTo(19f, 15f)
                lineTo(17.5f, 15f); lineTo(17.5f, 6.5f); lineTo(10.5f, 8f); lineTo(10.5f, 17f); close()
            }
            path(fill = SolidColor(GameColors.VioletTop), stroke = InkStroke, strokeLineWidth = OUTLINE) {
                circle(7f, 17.5f, 3f)
                circle(16f, 15.5f, 3f)
            }
        }
    }

    val Sound: ImageVector by lazy {
        icon("Sound") {
            path(fill = SolidColor(Color.White), stroke = InkStroke, strokeLineWidth = OUTLINE, strokeLineJoin = StrokeJoin.Round) {
                moveTo(3.5f, 9f); lineTo(7.5f, 9f); lineTo(12.5f, 4.5f); lineTo(12.5f, 19.5f); lineTo(7.5f, 15f); lineTo(3.5f, 15f); close()
            }
            path(stroke = InkStroke, strokeLineWidth = 3.6f, strokeLineCap = StrokeCap.Round) {
                moveTo(16f, 8.5f); curveTo(17.8f, 10.5f, 17.8f, 13.5f, 16f, 15.5f)
                moveTo(18.8f, 6f); curveTo(22f, 9.5f, 22f, 14.5f, 18.8f, 18f)
            }
            path(stroke = SolidColor(Color.White), strokeLineWidth = 1.6f, strokeLineCap = StrokeCap.Round) {
                moveTo(16f, 8.5f); curveTo(17.8f, 10.5f, 17.8f, 13.5f, 16f, 15.5f)
                moveTo(18.8f, 6f); curveTo(22f, 9.5f, 22f, 14.5f, 18.8f, 18f)
            }
        }
    }

    val Vibrate: ImageVector by lazy {
        icon("Vibrate") {
            path(fill = SolidColor(GameColors.BlueTop), stroke = InkStroke, strokeLineWidth = OUTLINE) { roundRect(7.5f, 3.5f, 16.5f, 20.5f, 2.2f) }
            path(fill = SolidColor(Color.White.copy(alpha = 0.85f))) { roundRect(9f, 6f, 15f, 16.5f, 1f) }
            path(stroke = InkStroke, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round) {
                moveTo(4.5f, 8f); lineTo(4.5f, 16f); moveTo(19.5f, 8f); lineTo(19.5f, 16f)
                moveTo(2f, 10f); lineTo(2f, 14f); moveTo(22f, 10f); lineTo(22f, 14f)
            }
        }
    }

    val Wind: ImageVector by lazy {
        twoPassStroke("Wind") {
            moveTo(3f, 9f); lineTo(14f, 9f); curveTo(18f, 9f, 18f, 4f, 14.5f, 4.5f)
            moveTo(3f, 14f); lineTo(17f, 14f); curveTo(21.5f, 14f, 21.5f, 19.5f, 17.5f, 19f)
        }
    }

    val Bubble: ImageVector by lazy {
        icon("Bubble") {
            path(fill = SolidColor(GameColors.BlueTop.copy(alpha = 0.55f)), stroke = InkStroke, strokeLineWidth = OUTLINE) { circle(12f, 12f, 9f) }
            path(stroke = SolidColor(Color.White), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round) {
                moveTo(7f, 10f); curveTo(7.5f, 7.5f, 9f, 6.5f, 11f, 6.2f)
            }
            path(fill = SolidColor(Color.White)) { circle(15.5f, 16f, 1.1f) }
        }
    }

    val Pencil: ImageVector by lazy {
        icon("Pencil") {
            // Body, tilted 45°: from the tip (bottom left) to the eraser (top right).
            path(fill = SolidColor(GameColors.Gold), stroke = InkStroke, strokeLineWidth = OUTLINE, strokeLineJoin = StrokeJoin.Round) {
                moveTo(4f, 20f); lineTo(5.2f, 15.2f); lineTo(16f, 4.4f); lineTo(19.6f, 8f); lineTo(8.8f, 18.8f); close()
            }
            path(fill = SolidColor(Color(0xFFFF8FB1)), stroke = InkStroke, strokeLineWidth = OUTLINE, strokeLineJoin = StrokeJoin.Round) {
                moveTo(16f, 4.4f); lineTo(17.6f, 2.8f); lineTo(21.2f, 6.4f); lineTo(19.6f, 8f); close()
            }
            path(fill = SolidColor(Color(0xFFFFE0C2)), stroke = InkStroke, strokeLineWidth = 1.2f, strokeLineJoin = StrokeJoin.Round) {
                moveTo(4f, 20f); lineTo(5.2f, 15.2f); lineTo(8.8f, 18.8f); close()
            }
        }
    }

    val Clock: ImageVector by lazy {
        icon("Clock") {
            path(fill = SolidColor(Color.White), stroke = InkStroke, strokeLineWidth = OUTLINE) { circle(12f, 13f, 8.5f) }
            path(fill = SolidColor(Color(0xFFFF5A7A)), stroke = InkStroke, strokeLineWidth = 1.2f) { roundRect(10f, 2f, 14f, 4.6f, 1f) }
            path(stroke = InkStroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
                moveTo(12f, 13f); lineTo(12f, 8.5f); moveTo(12f, 13f); lineTo(15f, 14.8f)
            }
        }
    }

    val Shield: ImageVector by lazy {
        icon("Shield") {
            path(fill = SolidColor(Color(0xFFDCE3F0)), stroke = InkStroke, strokeLineWidth = OUTLINE, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 2.5f); lineTo(20f, 5.5f); lineTo(20f, 11f)
                curveTo(20f, 16f, 16.5f, 19.5f, 12f, 21.5f)
                curveTo(7.5f, 19.5f, 4f, 16f, 4f, 11f); lineTo(4f, 5.5f); close()
            }
            path(fill = SolidColor(GameColors.BlueTop)) {
                moveTo(12f, 5.5f); lineTo(17f, 7.4f); lineTo(17f, 11f)
                curveTo(17f, 14.4f, 14.8f, 16.8f, 12f, 18.3f); close()
            }
        }
    }

    val Snowflake: ImageVector by lazy {
        twoPassStroke("Snowflake", fill = Color(0xFFB3E5FC)) {
            for (i in 0 until 3) {
                val a = i * PI.toFloat() / 3f
                moveTo(12f + 8.5f * cos(a), 12f + 8.5f * sin(a)); lineTo(12f - 8.5f * cos(a), 12f - 8.5f * sin(a))
            }
        }
    }

    /** A hand pointing up — the "tap here" hint. */
    val Tap: ImageVector by lazy {
        icon("Tap") {
            val skin = SolidColor(Color(0xFFFFE0C2))
            path(fill = skin, stroke = InkStroke, strokeLineWidth = OUTLINE, strokeLineJoin = StrokeJoin.Round) {
                roundRect(9.5f, 1.5f, 13.5f, 14f, 2f)
            }
            path(fill = skin, stroke = InkStroke, strokeLineWidth = OUTLINE, strokeLineJoin = StrokeJoin.Round) {
                roundRect(6f, 10f, 19f, 22f, 4.5f)
            }
            path(stroke = InkStroke, strokeLineWidth = 1.2f, strokeLineCap = StrokeCap.Round) {
                moveTo(13.5f, 10.5f); lineTo(13.5f, 13.5f)
                moveTo(16.3f, 11f); lineTo(16.3f, 13.5f)
            }
        }
    }

    val Skull: ImageVector by lazy {
        icon("Skull") {
            path(fill = SolidColor(Color(0xFF7CFF8A)), stroke = InkStroke, strokeLineWidth = OUTLINE, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 3f)
                curveTo(6.5f, 3f, 3.5f, 6.8f, 3.5f, 11f)
                curveTo(3.5f, 13.8f, 5f, 15.6f, 7f, 16.5f)
                lineTo(7f, 20.5f); lineTo(17f, 20.5f); lineTo(17f, 16.5f)
                curveTo(19f, 15.6f, 20.5f, 13.8f, 20.5f, 11f)
                curveTo(20.5f, 6.8f, 17.5f, 3f, 12f, 3f)
                close()
            }
            path(fill = InkStroke) {
                circle(8.6f, 11.2f, 2.3f)
                circle(15.4f, 11.2f, 2.3f)
                moveTo(12f, 13.6f); lineTo(13.3f, 16f); lineTo(10.7f, 16f); close()
            }
            path(stroke = InkStroke, strokeLineWidth = 1.2f) {
                moveTo(10f, 18f); lineTo(10f, 20.5f); moveTo(14f, 18f); lineTo(14f, 20.5f)
            }
        }
    }

    // ── Builders ────────────────────────────────────────────────────────────

    private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply(block).build()

    /** A thick white line with an indigo outline: drawn twice, wide dark then narrow light. */
    private fun twoPassStroke(name: String, fill: Color = Color.White, pathBuilder: PathBuilder.() -> Unit) = icon(name) {
        path(stroke = InkStroke, strokeLineWidth = 5.4f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathBuilder = pathBuilder)
        path(stroke = SolidColor(fill), strokeLineWidth = 3f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathBuilder = pathBuilder)
    }
}

private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
    moveTo(cx - r, cy)
    arcTo(r, r, 0f, false, true, cx + r, cy)
    arcTo(r, r, 0f, false, true, cx - r, cy)
    close()
}

private fun PathBuilder.roundRect(left: Float, top: Float, right: Float, bottom: Float, radius: Float) {
    moveTo(left + radius, top)
    lineTo(right - radius, top)
    arcTo(radius, radius, 0f, false, true, right, top + radius)
    lineTo(right, bottom - radius)
    arcTo(radius, radius, 0f, false, true, right - radius, bottom)
    lineTo(left + radius, bottom)
    arcTo(radius, radius, 0f, false, true, left, bottom - radius)
    lineTo(left, top + radius)
    arcTo(radius, radius, 0f, false, true, left + radius, top)
    close()
}

private fun PathBuilder.star(cx: Float, cy: Float, outer: Float, inner: Float) {
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outer else inner
        val angle = (-90f + i * 36f) * PI.toFloat() / 180f
        val x = cx + r * cos(angle)
        val y = cy + r * sin(angle)
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}
