package com.apps.bubblepopping.view.play.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.ui.components.ButtonColor
import com.apps.bubblepopping.ui.components.GameIconButton
import com.apps.bubblepopping.ui.components.OutlinedText
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.BubbleTheme
import com.apps.bubblepopping.ui.theme.Corners
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing
import kotlin.math.ceil

/** Height of the HUD content row (below the status bar). Bubbles escape at its bottom edge. */
internal val HUD_CONTENT_HEIGHT: Dp = 56.dp

/**
 * The in-game HUD: lives and power-ups on the left, the score (with the combo
 * ring) in the middle, the pause button on the right. A soft indigo fade sits
 * behind it so it reads over any background.
 */
@Composable
fun GameHud(
    score: Int,
    lives: Int,
    isPaused: Boolean,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier,
    comboTimeLeft: Float = 0f,
    comboMultiplier: Int = 1,
    shieldActive: Boolean = false,
    freezeTimeLeft: Float = 0f,
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(HUD_CONTENT_HEIGHT)
                .padding(horizontal = Spacing.screen),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                ) {
                    LivesView(lives = lives)
                    PowerUpBadges(shieldActive = shieldActive, freezeTimeLeft = freezeTimeLeft)
                }
                GameIconButton(
                    icon = if (isPaused) GameIcons.Play else GameIcons.Pause,
                    contentDescription = if (isPaused) "Resume" else "Pause",
                    onClick = onPlayPauseClick,
                    color = ButtonColor.BLUE,
                    size = PAUSE_BUTTON_SIZE,
                )
            }
            ScoreView(
                score = score,
                comboTimeLeft = comboTimeLeft,
                comboMultiplier = comboMultiplier,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

/** A heart on a glass pill with the lives left. */
@Composable
fun LivesView(lives: Int, modifier: Modifier = Modifier) {
    HudPill(modifier.semantics { contentDescription = "$lives lives" }) {
        Image(
            imageVector = GameIcons.Heart,
            contentDescription = null,
            modifier = Modifier.size(24.dp).alpha(if (lives > 0) 1f else 0.4f),
        )
        OutlinedText("×$lives", GameTheme.type.label)
    }
}

/** A shield badge while a shield is ready; a snowflake with seconds left while frozen. */
@Composable
fun PowerUpBadges(shieldActive: Boolean, freezeTimeLeft: Float, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = modifier,
    ) {
        if (shieldActive) {
            HudPill(Modifier.semantics { contentDescription = "Shield ready" }) {
                Image(GameIcons.Shield, contentDescription = null, modifier = Modifier.size(24.dp))
            }
        }
        if (freezeTimeLeft > 0f) {
            val seconds = ceil(freezeTimeLeft).toInt()
            HudPill(Modifier.semantics { contentDescription = "Frozen, $seconds seconds left" }) {
                Image(GameIcons.Snowflake, contentDescription = null, modifier = Modifier.size(22.dp))
                OutlinedText("$seconds", GameTheme.type.label, color = Color(0xFFB3E5FC))
            }
        }
    }
}

@Composable
private fun HudPill(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = modifier
            .clip(Corners.pill)
            .background(GameColors.Ink.copy(alpha = 0.5f))
            .border(1.5.dp, GameColors.GlassRim, Corners.pill)
            .padding(horizontal = Spacing.s, vertical = 2.dp),
    ) { content() }
}

/**
 * Centered score. The number springs towards [score] on each increase. While a
 * combo runs, a gold ring around it drains as [comboTimeLeft] goes 1 → 0, and
 * the active multiplier ("×3") shows beside it.
 */
@Composable
fun ScoreView(
    score: Int,
    modifier: Modifier = Modifier,
    comboTimeLeft: Float = 0f,
    comboMultiplier: Int = 1,
) {
    val displayed by animateIntAsState(
        targetValue = score,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "score",
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(COMBO_RING_RADIUS * 2)
                .drawBehind {
                    if (comboTimeLeft <= 0f) return@drawBehind
                    val radius = COMBO_RING_RADIUS.toPx() - 2.dp.toPx()
                    val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    drawCircle(GameColors.Ink.copy(alpha = 0.5f), radius, center, style = Stroke(width = 6.dp.toPx()))
                    drawArc(
                        color = GameColors.Gold,
                        startAngle = -90f,
                        sweepAngle = 360f * comboTimeLeft,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = stroke,
                    )
                }
                .semantics { contentDescription = "Score $score" },
        ) {
            OutlinedText(displayed.toString(), GameTheme.type.title, outlineWidth = 3.dp, maxLines = 1)
        }
        if (comboMultiplier > 1 && comboTimeLeft > 0f) {
            OutlinedText(
                text = "×$comboMultiplier",
                style = GameTheme.type.headline,
                color = GameColors.Gold,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = COMBO_RING_RADIUS + Spacing.l),
            )
        }
    }
}

private val COMBO_RING_RADIUS = 30.dp
private val PAUSE_BUTTON_SIZE = 44.dp

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
                comboTimeLeft = 0.6f,
                comboMultiplier = 3,
                shieldActive = true,
                freezeTimeLeft = 2.2f,
            )
        }
    }
}
