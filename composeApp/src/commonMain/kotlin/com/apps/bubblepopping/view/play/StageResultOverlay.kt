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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.meta.RunRewards
import com.apps.bubblepopping.stage.Objective
import com.apps.bubblepopping.stage.ObjectiveProgress
import com.apps.bubblepopping.stage.StageResult
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
import com.apps.bubblepopping.view.stage.ObjectiveRow
import kotlinx.coroutines.delay

/**
 * End of a stage. Stars land one by one ([onStarShown] fires for each, for
 * sound and haptics), the score counts up, and every objective shows ✓ or ✗.
 */
@Composable
fun StageResultOverlay(
    stageId: Int,
    result: StageResult,
    maxStars: Int,
    score: Int,
    bestScore: Int?,
    isNewBest: Boolean,
    onRetry: () -> Unit,
    onStages: (() -> Unit)?,
    onNext: (() -> Unit)?,
    onStarShown: (star: Int) -> Unit,
    modifier: Modifier = Modifier,
    isBossStage: Boolean = false,
    /** Coins and other rewards; null while they are being paid out. */
    rewards: RunRewards? = null,
    /** Line above the stars. */
    stageLabel: String = "STAGE $stageId",
    /** Label of the [onStages] button. */
    backLabel: String = "Stages",
    /** Lets the slow-motion drift play before the card appears. */
    appearDelayMs: Long = APPEAR_DELAY_MS,
) {
    var visible by remember(result) { mutableStateOf(false) }
    var shownStars by remember(result) { mutableIntStateOf(0) }
    val countedScore = remember(result) { Animatable(0f) }

    LaunchedEffect(result) {
        delay(appearDelayMs)
        visible = true
        delay(STAR_START_DELAY_MS)
        for (star in 1..result.stars) {
            shownStars = star
            onStarShown(star)
            delay(STAR_INTERVAL_MS)
        }
    }
    LaunchedEffect(result) {
        delay(appearDelayMs)
        countedScore.animateTo(score.toFloat(), tween(durationMillis = SCORE_COUNT_MS))
    }

    val title = when {
        !result.won -> "Stage failed"
        isBossStage -> "Boss defeated!"
        else -> "Stage clear!"
    }

    AnimatedVisibility(visible = visible, enter = fadeIn(tween(FADE_IN_MS)), modifier = modifier) {
        OverlayScrim {
            GameDialogCard(title = title, onClose = null) {
                Text(stageLabel, style = GameTheme.type.label, color = GameColors.TextSecondary)
                AnimatedStars(shown = shownStars, total = maxStars)

                OutlinedText(formatCount(countedScore.value.toInt()), GameTheme.type.display, outlineWidth = 3.dp)
                when {
                    isNewBest -> OutlinedText("NEW BEST!", GameTheme.type.headline, color = GameColors.Gold)
                    bestScore != null -> Text("Best ${formatCount(bestScore)}", style = GameTheme.type.label, color = GameColors.TextSecondary)
                }

                // Every objective, ticked or crossed, on a darker inset.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(Corners.medium)
                        .background(GameColors.Ink.copy(alpha = 0.35f))
                        .padding(horizontal = Spacing.m, vertical = Spacing.s),
                ) {
                    result.objectives.forEachIndexed { index, progress ->
                        ObjectiveRow(
                            description = progress.objective.description,
                            progress = progress,
                            isBonus = index > 0,
                        )
                    }
                }
                // A failed goal that was nearly reached gets a nudge to retry.
                result.objectives.firstOrNull()
                    ?.takeIf { !result.won && !it.objective.isConstraint && it.fraction >= SO_CLOSE_FRACTION }
                    ?.let { OutlinedText("${it.current} / ${it.target} — so close!", GameTheme.type.label, color = GameColors.Gold) }

                RewardsSummary(rewards)

                DialogButtons {
                    val showNext = result.won && onNext != null
                    if (showNext && onNext != null) {
                        GameButton("Next stage", onClick = onNext, size = ButtonSize.LARGE, icon = GameIcons.Play, modifier = Modifier.fillMaxWidth())
                    }
                    GameButton(
                        text = "Retry",
                        onClick = onRetry,
                        color = if (showNext) ButtonColor.GLASS else ButtonColor.BLUE,
                        size = if (showNext) ButtonSize.MEDIUM else ButtonSize.LARGE,
                        icon = GameIcons.Restart,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (onStages != null) {
                        GameButton(backLabel, onClick = onStages, color = ButtonColor.GLASS, icon = GameIcons.Home, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

/** Three stars; the middle one bigger and raised. Each springs in when awarded. */
@Composable
private fun AnimatedStars(shown: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.Bottom) {
        repeat(total) { index ->
            val earned = index < shown
            val scale by animateFloatAsState(
                targetValue = if (earned) 1f else 0.75f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "star_$index",
            )
            val isMiddle = total == 3 && index == 1
            Image(
                imageVector = GameIcons.Star,
                contentDescription = if (earned) "Star earned" else null,
                modifier = Modifier
                    .offset(y = if (isMiddle) (-Spacing.m) else 0.dp)
                    .size(if (isMiddle) BIG_STAR else STAR)
                    .scale(scale)
                    .alpha(if (earned) 1f else UNEARNED_ALPHA),
            )
        }
    }
}

/** Same pause as the Endless game-over card. */
private const val APPEAR_DELAY_MS = 900L
private const val FADE_IN_MS = 250
private const val STAR_START_DELAY_MS = 400L
private const val STAR_INTERVAL_MS = 350L
private const val SCORE_COUNT_MS = 900
/** Failed goals at least this far along show "so close!". */
private const val SO_CLOSE_FRACTION = 0.6f
private const val UNEARNED_ALPHA = 0.3f
private val STAR = 52.dp
private val BIG_STAR = 68.dp

@Preview
@Composable
fun StageResultOverlayPreview() {
    BubbleTheme {
        StageResultOverlay(
            stageId = 7,
            result = StageResult(
                won = true,
                stars = 2,
                objectives = listOf(
                    ObjectiveProgress(Objective.ReachScore(80), current = 92, target = 80, satisfied = true),
                    ObjectiveProgress(Objective.ReachCombo(6), current = 6, target = 6, satisfied = true),
                    ObjectiveProgress(Objective.NoLivesLost, current = 1, target = 0, satisfied = false),
                ),
            ),
            maxStars = 3,
            score = 92,
            bestScore = 80,
            isNewBest = true,
            onRetry = {},
            onStages = {},
            onNext = {},
            onStarShown = {},
            appearDelayMs = 0,
        )
    }
}
