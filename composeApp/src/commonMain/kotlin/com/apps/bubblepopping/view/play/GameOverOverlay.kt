package com.apps.bubblepopping.view.play

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
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
                label?.let { Text(it, style = GameTheme.type.label, color = GameColors.Gold) }
                reasonText(reason)?.let { Text(it, style = GameTheme.type.body, color = GameColors.TextSecondary) }

                // Wave reached
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Image(GameIcons.Bubble, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("Reached wave $wave", style = GameTheme.type.label, color = GameColors.TextSecondary)
                }

                OutlinedText(
                    text = formatCount(countedScore.value.toInt()),
                    style = GameTheme.type.display.copy(fontSize = GameTheme.type.display.fontSize * SCORE_SCALE),
                    outlineWidth = 4.dp,
                )

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
                Verdict(score, bestScore, isNewBest, popIn.heightIn(min = VERDICT_HEIGHT))

                if (unlockedDifficulty != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                        modifier = popIn
                            .clip(Corners.pill)
                            .background(GameColors.GreenBottom)
                            .border(2.dp, Color.White.copy(alpha = 0.6f), Corners.pill)
                            .padding(horizontal = Spacing.l, vertical = Spacing.xs),
                    ) {
                        Image(GameIcons.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
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

/** "NEW BEST!" with stars, "X from your best!", or nothing on a first round. */
@Composable
private fun Verdict(score: Int, bestScore: Int?, isNewBest: Boolean, modifier: Modifier) {
    when {
        isNewBest -> Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Image(GameIcons.Star, contentDescription = null, modifier = Modifier.size(28.dp))
            OutlinedText("NEW BEST!", GameTheme.type.title, color = GameColors.Gold, outlineWidth = 3.dp)
            Image(GameIcons.Star, contentDescription = null, modifier = Modifier.size(28.dp))
        }
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
private val VERDICT_HEIGHT = 36.dp

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
