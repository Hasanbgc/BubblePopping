package com.apps.bubblepopping.view.play

import com.apps.bubblepopping.ui.components.goldInset
import com.apps.bubblepopping.ui.components.candyFace
import com.apps.bubblepopping.ui.components.ResourcePill
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.game.GameOverReason
import com.apps.bubblepopping.meta.RunRewards
import com.apps.bubblepopping.ui.components.ButtonColor
import com.apps.bubblepopping.ui.components.ButtonSize
import com.apps.bubblepopping.ui.components.DialogButtons
import com.apps.bubblepopping.ui.components.GameButton
import com.apps.bubblepopping.ui.components.GameDialogCard
import com.apps.bubblepopping.ui.components.OutlinedText
import com.apps.bubblepopping.ui.components.OverlayScrim
import com.apps.bubblepopping.ui.components.formatCount
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.BubbleTheme
import com.apps.bubblepopping.ui.theme.Corners
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing
import com.apps.bubblepopping.view.home.Difficulty
import kotlinx.coroutines.delay

/**
 * End of an Endless round. Waits for the slow-motion drift, then the card pops
 * in; the score counts up, then "NEW BEST!" or "X from your best!" lands.
 * Play Again is the big, first button.
 */
@Composable
fun GameOverOverlay(
    score: Int,
    bestScore: Int?,
    isNewBest: Boolean,
    reason: GameOverReason?,
    wave: Int,
    unlockedDifficulty: Difficulty?,
    /** Coins and other rewards; null while they are being paid out. */
    rewards: RunRewards?,
    onRestart: () -> Unit,
    onHome: (() -> Unit)?,
    modifier: Modifier = Modifier,
    appearDelayMs: Long = APPEAR_DELAY_MS,
    /** Small line above the title, e.g. "DAILY CHALLENGE". */
    label: String? = null,
) {
    var visible by remember { mutableStateOf(false) }
    var verdictShown by remember { mutableStateOf(false) }
    val countedScore = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(appearDelayMs)
        visible = true
        countedScore.animateTo(score.toFloat(), tween(durationMillis = SCORE_COUNT_MS))
        verdictShown = true
    }

    AnimatedVisibility(visible = visible, enter = fadeIn(tween(FADE_IN_MS)), modifier = modifier) {
        OverlayScrim {
            GameDialogCard(title = "Game Over", onClose = null) {
                label?.let { OutlinedText(it, GameTheme.type.label, color = GameColors.Gold) }
                reasonText(reason)?.let {
                    Text(it, style = GameTheme.type.body, color = GameColors.TextSecondary, textAlign = TextAlign.Center)
                }
                ResourcePill(GameIcons.Bubble, "Wave $wave", tint = GameColors.VioletTop)

                // Verdict springs in once the count-up finishes; its space is kept so nothing jumps.
                val verdictScale by animateFloatAsState(
                    targetValue = if (verdictShown) 1f else 0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "verdict_scale",
                )
                val popIn = Modifier.graphicsLayer {
                    scaleX = verdictScale
                    scaleY = verdictScale
                    alpha = verdictScale.coerceIn(0f, 1f)
                }

                ScorePlaque(
                    score = formatCount(countedScore.value.toInt()),
                    celebrate = isNewBest && verdictShown,
                )
                Verdict(score, bestScore, isNewBest, popIn.heightIn(min = VERDICT_HEIGHT))

                if (unlockedDifficulty != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                        modifier = popIn
                            .candyFace(ButtonColor.GREEN, Corners.pill)
                            .padding(start = Spacing.m, end = Spacing.l, top = Spacing.xs, bottom = Spacing.xs),
                    ) {
                        Image(GameIcons.Lock, contentDescription = null, modifier = Modifier.size(TAG_ICON))
                        OutlinedText("${unlockedDifficulty.label.uppercase()} UNLOCKED!", GameTheme.type.label, outlineColor = GameColors.GreenLip)
                    }
                }

                RewardsSummary(rewards)

                DialogButtons {
                    GameButton(
                        text = "Play Again",
                        onClick = onRestart,
                        size = ButtonSize.LARGE,
                        icon = GameIcons.Restart,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (onHome != null) {
                        GameButton(text = "Home", onClick = onHome, color = ButtonColor.GLASS, icon = GameIcons.Home, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

/**
 * The final score in a gold-framed well, under a small "SCORE" label. With
 * [celebrate] (a new best) the number turns gold and soft gold rays turn slowly
 * behind it.
 */
@Composable
internal fun ScorePlaque(score: String, celebrate: Boolean) {
    val rays by animateFloatAsState(
        targetValue = if (celebrate) 1f else 0f,
        animationSpec = tween(RAYS_FADE_MS),
        label = "rays",
    )
    // Read only while drawing, so the turning rays never recompose the card.
    val spin = rememberInfiniteTransition(label = "plaque").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(RAYS_TURN_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "spin",
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .goldInset()
            .drawBehind {
                if (rays <= 0f) return@drawBehind
                // Gold rays from the centre, slowly turning.
                val reach = size.maxDimension
                rotate(spin.value) {
                    for (i in 0 until RAY_COUNT) {
                        val start = i * 360f / RAY_COUNT
                        drawArc(
                            brush = Brush.radialGradient(
                                listOf(GameColors.Gold.copy(alpha = 0.35f * rays), Color.Transparent),
                                center = center,
                                radius = reach / 2f,
                            ),
                            startAngle = start,
                            sweepAngle = 360f / RAY_COUNT / 2f,
                            useCenter = true,
                            topLeft = Offset(center.x - reach / 2f, center.y - reach / 2f),
                            size = Size(reach, reach),
                        )
                    }
                }
            }
            .padding(vertical = Spacing.m),
    ) {
        Text("SCORE", style = GameTheme.type.caption, color = GameColors.Gold)
        OutlinedText(
            text = score,
            style = GameTheme.type.display.copy(fontSize = GameTheme.type.display.fontSize * SCORE_SCALE),
            color = if (celebrate) GameColors.Gold else GameColors.TextPrimary,
            outlineWidth = 4.dp,
            maxLines = 1,
        )
    }
}

/** "NEW BEST!" on a gold candy tag between two stars, "X from your best!", or nothing on a first round. */
@Composable
private fun Verdict(score: Int, bestScore: Int?, isNewBest: Boolean, modifier: Modifier) {
    when {
        isNewBest -> NewBestTag(modifier)
        bestScore == null || bestScore <= 0 -> Row(modifier) {}
        else -> Text(
            text = if (score == bestScore) "You tied your best!" else "${formatCount(bestScore - score)} from your best!",
            style = GameTheme.type.headline,
            color = GameColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = modifier,
        )
    }
}

private fun reasonText(reason: GameOverReason?): String? = when (reason) {
    GameOverReason.POISON -> "You popped a skull!"
    GameOverReason.OUT_OF_LIVES -> "Too many bubbles got away"
    GameOverReason.TIME_UP, GameOverReason.GOAL_COMPLETE, null -> null
}

/** Lets the slow-motion drift play before the card appears. */
private const val APPEAR_DELAY_MS = 900L
private const val FADE_IN_MS = 250
private const val SCORE_COUNT_MS = 800
private const val SCORE_SCALE = 1.4f
private val VERDICT_HEIGHT = 40.dp
private val VERDICT_STAR = 28.dp
private val TAG_ICON = 22.dp
private const val RAY_COUNT = 12
private const val RAYS_FADE_MS = 500
private const val RAYS_TURN_MS = 12_000

@Preview
@Composable
fun GameOverOverlayPreview() {
    BubbleTheme {
        GameOverOverlay(
            score = 138,
            bestScore = 150,
            isNewBest = false,
            reason = GameOverReason.OUT_OF_LIVES,
            wave = 4,
            unlockedDifficulty = Difficulty.HARD,
            rewards = null,
            onRestart = {},
            onHome = {},
            appearDelayMs = 0,
        )
    }
}

/** "NEW BEST!" on a gold candy tag between two stars. Shared with the stage result card. */
@Composable
internal fun NewBestTag(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Image(GameIcons.Star, contentDescription = null, modifier = Modifier.size(VERDICT_STAR))
        Box(
            Modifier
                .candyFace(ButtonColor.GOLD, Corners.pill)
                .padding(horizontal = Spacing.l, vertical = Spacing.xs),
        ) {
            OutlinedText("NEW BEST!", GameTheme.type.headline, outlineColor = GameColors.GoldLip, outlineWidth = 2.5.dp, maxLines = 1)
        }
        Image(GameIcons.Star, contentDescription = null, modifier = Modifier.size(VERDICT_STAR))
    }
}
