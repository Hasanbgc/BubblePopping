package com.apps.bubblepopping.view.play.component

import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.apps.bubblepopping.ui.icons.playIcon
import com.apps.bubblepopping.ui.components.GameProgressBar
import com.apps.bubblepopping.ui.components.candyFace
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.layout.Layout
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.ui.components.ButtonColor
import com.apps.bubblepopping.ui.components.GameIconButton
import com.apps.bubblepopping.ui.components.OutlinedText
import com.apps.bubblepopping.ui.components.enamelPill
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.BubbleTheme
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing

/** Height of the HUD content row (below the status bar). Bubbles escape at its bottom edge. */
internal val HUD_CONTENT_HEIGHT: Dp = 56.dp

/**
 * The in-game HUD: lives and power-ups on the left, the score capsule (with the combo
 * bar) in the middle, the pause button on the right. A soft indigo fade sits
 * behind it so it reads over any background.
 */
@Composable
fun GameHud(
    score: Int,
    lives: Int,
    isPaused: Boolean,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier,
    comboTimeLeft: () -> Float = { 0f },
    comboMultiplier: Int = 1,
    shieldActive: Boolean = false,
    freezeSecondsLeft: Int = 0,
    doubleSecondsLeft: Int = 0,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0.00f to GameColors.Scrim.copy(alpha = 0.85f),
                    0.60f to GameColors.Scrim.copy(alpha = 0.55f),
                    1.00f to Color.Transparent,
                )
            ),
    ) {
        HudRow(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(HUD_CONTENT_HEIGHT)
                .padding(horizontal = Spacing.screen),
            start = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                ) {
                    LivesView(lives = lives)
                    PowerUpBadges(shieldActive = shieldActive, freezeSecondsLeft = freezeSecondsLeft, doubleSecondsLeft = doubleSecondsLeft)
                }
            },
            center = {
                ScoreView(
                    score = score,
                    comboTimeLeft = comboTimeLeft,
                    comboMultiplier = comboMultiplier,
                )
            },
            end = {
                GameIconButton(
                    icon = if (isPaused) playIcon() else rememberVectorPainter(GameIcons.Pause),
                    contentDescription = if (isPaused) "Resume" else "Pause",
                    onClick = onPlayPauseClick,
                    color = ButtonColor.BLUE,
                    size = PAUSE_BUTTON_SIZE,
                )
            },
        )
    }
}

/** A heart in a rose enamel pill with the lives left. */
@Composable
fun LivesView(lives: Int, modifier: Modifier = Modifier) {
    HudPill(GameColors.PillHeart, modifier.semantics { contentDescription = "$lives lives" }) {
        Image(
            imageVector = GameIcons.Heart,
            contentDescription = null,
            modifier = Modifier.size(HUD_ICON).alpha(if (lives > 0) 1f else 0.4f),
        )
        OutlinedText("×$lives", GameTheme.type.label)
    }
}

/**
 * A shield badge while a shield is ready; a snowflake with [freezeSecondsLeft] while frozen;
 * "×2" with [doubleSecondsLeft] while points count double.
 */
@Composable
fun PowerUpBadges(shieldActive: Boolean, freezeSecondsLeft: Int, modifier: Modifier = Modifier, doubleSecondsLeft: Int = 0) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        modifier = modifier,
    ) {
        if (shieldActive) {
            HudPill(GameColors.PillBubble, Modifier.semantics { contentDescription = "Shield ready" }, endPadding = Spacing.xs) {
                Image(GameIcons.Shield, contentDescription = null, modifier = Modifier.size(HUD_ICON))
            }
        }
        if (freezeSecondsLeft > 0) {
            val seconds = freezeSecondsLeft
            HudPill(GameColors.PillFreeze, Modifier.semantics { contentDescription = "Frozen, $seconds seconds left" }) {
                Image(GameIcons.Snowflake, contentDescription = null, modifier = Modifier.size(HUD_ICON))
                OutlinedText("$seconds", GameTheme.type.label, color = GameColors.FreezeText)
            }
        }
        if (doubleSecondsLeft > 0) {
            val seconds = doubleSecondsLeft
            HudPill(GameColors.PillHeart, Modifier.semantics { contentDescription = "Double points, $seconds seconds left" }) {
                OutlinedText("×2", GameTheme.type.label)
                OutlinedText("$seconds", GameTheme.type.label)
            }
        }
    }
}

/**
 * A HUD pill: the same enamel pill as the coin and star counters, tinted by what
 * it shows. The icon sits inside on the left; [endPadding] is smaller for an icon alone.
 */
@Composable
private fun HudPill(
    tint: Color,
    modifier: Modifier = Modifier,
    endPadding: Dp = Spacing.m,
    content: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = modifier
            .height(HUD_PILL_HEIGHT)
            .enamelPill(tint)
            .padding(start = Spacing.xs, end = endPadding),
    ) { content() }
}

/**
 * The HUD's three slots: [start] on the left, [end] on the right, and [center]
 * centred on the screen — unless the sides are wide (e.g. several power-ups),
 * then it slides over so there is always at least [HUD_GAP] between it and
 * either side. Everything is vertically centred.
 */
@Composable
private fun HudRow(
    start: @Composable () -> Unit,
    center: @Composable () -> Unit,
    end: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(
        contents = listOf(start, center, end),
        modifier = modifier,
    ) { (startMeasurables, centerMeasurables, endMeasurables), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val gap = HUD_GAP.roundToPx()
        val endPlaceables = endMeasurables.map { it.measure(loose) }
        val endWidth = endPlaceables.maxOfOrNull { it.width } ?: 0
        val startPlaceables = startMeasurables.map {
            it.measure(loose.copy(maxWidth = (constraints.maxWidth - endWidth - gap).coerceAtLeast(0)))
        }
        val startWidth = startPlaceables.maxOfOrNull { it.width } ?: 0
        val centerPlaceables = centerMeasurables.map { it.measure(loose) }
        val centerWidth = centerPlaceables.maxOfOrNull { it.width } ?: 0
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        // Centred if it fits; otherwise pushed clear of the wider side.
        val minX = if (startWidth > 0) startWidth + gap else 0
        val maxX = width - centerWidth - (if (endWidth > 0) endWidth + gap else 0)
        val centerX = ((width - centerWidth) / 2).coerceAtMost(maxX).coerceAtLeast(minX)
        layout(width, height) {
            startPlaceables.forEach { it.place(0, (height - it.height) / 2) }
            centerPlaceables.forEach { it.place(centerX, (height - it.height) / 2) }
            endPlaceables.forEach { it.place(width - it.width, (height - it.height) / 2) }
        }
    }
}

/**
 * The score on a gold enamel capsule. The number springs towards [score] on
 * each increase. While a combo runs, a gold bar under the capsule drains as
 * [comboTimeLeft] goes 1 → 0 (read only while laying out the bar, so the
 * draining never recomposes the HUD), and a gold candy badge with the multiplier
 * ("×3") sits on the capsule's corner. The bar's space is always kept, so the
 * capsule never jumps.
 */
@Composable
fun ScoreView(
    score: Int,
    modifier: Modifier = Modifier,
    comboTimeLeft: () -> Float = { 0f },
    comboMultiplier: Int = 1,
) {
    val displayed by animateIntAsState(
        targetValue = score,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "score",
    )
    val currentComboTimeLeft by rememberUpdatedState(comboTimeLeft)
    val comboRunning by remember { derivedStateOf { currentComboTimeLeft() > 0f } }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(COMBO_BAR_GAP),
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = if (comboRunning && comboMultiplier > 1) "Score $score, combo times $comboMultiplier" else "Score $score"
        },
    ) {
        Box {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(SCORE_HEIGHT)
                    .widthIn(min = SCORE_MIN_WIDTH)
                    .enamelPill(GameColors.Gold)
                    .padding(horizontal = Spacing.l),
            ) {
                OutlinedText(displayed.toString(), GameTheme.type.headline, outlineWidth = 2.5.dp, maxLines = 1)
            }
            if (comboRunning && comboMultiplier > 1) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = COMBO_BADGE / 2, y = -COMBO_BADGE / 3)
                        .size(COMBO_BADGE)
                        .drawBehind {
                            val outline = 2.dp.toPx()
                            drawCircle(GameColors.ButtonOutline, radius = size.minDimension / 2f + outline / 2f, style = Stroke(outline))
                        }
                        .candyFace(ButtonColor.GOLD, CircleShape),
                ) {
                    OutlinedText("×$comboMultiplier", GameTheme.type.caption, outlineColor = GameColors.GoldLip, outlineWidth = 1.5.dp, maxLines = 1)
                }
            }
        }
        GameProgressBar(
            fraction = comboTimeLeft,
            height = COMBO_BAR_HEIGHT,
            modifier = Modifier
                .width(COMBO_BAR_WIDTH)
                .alpha(if (comboRunning) 1f else 0f),
        )
    }
}

private val PAUSE_BUTTON_SIZE = 44.dp
/** Least space between the score and the pills or pause button. */
private val HUD_GAP = 12.dp
private val SCORE_HEIGHT = 36.dp
private val SCORE_MIN_WIDTH = 88.dp
private val COMBO_BADGE = 28.dp
private val COMBO_BAR_WIDTH = 64.dp
private val COMBO_BAR_HEIGHT = 7.dp
private val COMBO_BAR_GAP = 3.dp
private val HUD_PILL_HEIGHT = 34.dp
private val HUD_ICON = 26.dp

@Preview
@Composable
fun GameHudPreview() {
    BubbleTheme {
        Box(Modifier.background(Color(0xFF4A2380))) {
            GameHud(
                score = 1240,
                lives = 3,
                isPaused = false,
                onPlayPauseClick = {},
                comboTimeLeft = { 0.6f },
                comboMultiplier = 3,
                shieldActive = true,
                freezeSecondsLeft = 3,
            )
        }
    }
}
