package com.apps.bubblepopping.ui.icons

import androidx.compose.ui.graphics.Color
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
 * The game's icon set. Most icons are the Figma art (SVGs kept in design/icons/),
 * converted to vectors in GameIconArt.kt (Android can't load SVG resources); the rest
 * are drawn here on a 24×24 grid. All are full colour, so draw them with
 * `Image(imageVector = …)` (not a tinted `Icon`).
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

    val Check: ImageVector get() = CheckArt

    val Play: ImageVector by lazy {
        icon("Play") {
            path(fill = SolidColor(Color.White), stroke = InkStroke, strokeLineWidth = OUTLINE, strokeLineJoin = StrokeJoin.Round) {
                moveTo(8f, 4.5f); lineTo(19.5f, 12f); lineTo(8f, 19.5f); close()
            }
        }
    }

    val Pause: ImageVector get() = PauseArt

    val Restart: ImageVector get() = RestartArt

    val Coin: ImageVector get() = CoinArt

    val Star: ImageVector get() = StarArt

    val Heart: ImageVector get() = HeartArt

    val Gear: ImageVector get() = GearArt

    val Trophy: ImageVector get() = TrophyArt

    val Calendar: ImageVector get() = CalendarArt

    val Shop: ImageVector get() = ShopArt

    val Lock: ImageVector get() = LockArt

    val Home: ImageVector get() = HomeArt

    val Person: ImageVector by lazy {
        icon("Person") {
            path(fill = SolidColor(GameColors.BlueTop), stroke = InkStroke, strokeLineWidth = OUTLINE) {
                moveTo(3.5f, 21f); curveTo(3.5f, 15f, 7.5f, 13f, 12f, 13f); curveTo(16.5f, 13f, 20.5f, 15f, 20.5f, 21f); close()
            }
            path(fill = SolidColor(Color(0xFFFFE0C2)), stroke = InkStroke, strokeLineWidth = OUTLINE) { circle(12f, 8f, 4.6f) }
        }
    }

    val Chest: ImageVector get() = ChestArt

    val Target: ImageVector get() = TargetArt

    val Crown: ImageVector get() = CrownArt

    val Music: ImageVector get() = MusicArt

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

    val Vibrate: ImageVector get() = VibrateArt

    val Wind: ImageVector get() = WindArt

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

    val Clock: ImageVector get() = ClockArt

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

    val Skull: ImageVector get() = SkullArt

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
