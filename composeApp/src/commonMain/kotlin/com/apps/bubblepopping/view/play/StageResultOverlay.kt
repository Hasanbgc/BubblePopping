package com.apps.bubblepopping.view.play

import com.apps.bubblepopping.ui.icons.playIcon
import com.apps.bubblepopping.ui.components.PanelDivider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import com.apps.bubblepopping.ui.components.goldInset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.meta.RunRewards
import com.apps.bubblepopping.stage.Objective
import com.apps.bubblepopping.stage.ObjectiveProgress
import com.apps.bubblepopping.stage.StageResult
import com.apps.bubblepopping.ui.components.ButtonColor
import com.apps.bubblepopping.ui.components.ButtonSize
import com.apps.bubblepopping.ui.components.GameButton
import com.apps.bubblepopping.ui.components.GameDialogCard
import com.apps.bubblepopping.ui.components.OutlinedText
import com.apps.bubblepopping.ui.components.OverlayScrim
import com.apps.bubblepopping.ui.components.formatCount
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.BubbleTheme
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
    /** Coins and other rewards; null while they are being paid out. */
    rewards: RunRewards? = null,
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

    val title = if (result.won) "Congratulations!" else "Stage failed"

    // Laid out to be read in one glance: stars, score + best, goals, rewards, buttons.
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(FADE_IN_MS)), modifier = modifier) {
        OverlayScrim {
            GameDialogCard(title = title, onClose = null) {
                AnimatedStars(shown = shownStars, total = maxStars)

                val counted = countedScore.value.toInt()
                ScoreAndBest(
                    score = formatCount(counted),
                    best = bestScore?.let(::formatCount),
                    isNewBest = isNewBest && counted >= score,
                )

                // Every objective, ticked or crossed.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .goldInset()
                        .padding(horizontal = Spacing.m, vertical = Spacing.xs),
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

                RewardsStrip(rewards)

                // Main action, then the rest as round buttons — like the pause card.
                val showNext = result.won && onNext != null
                if (showNext && onNext != null) {
                    GameButton("Next stage", onClick = onNext, size = ButtonSize.LARGE, icon = playIcon(), modifier = Modifier.fillMaxWidth())
                } else {
                    GameButton("Retry", onClick = onRetry, size = ButtonSize.LARGE, icon = GameIcons.Restart, modifier = Modifier.fillMaxWidth())
                }
                if (showNext || onStages != null) {
                    PanelDivider()
                    RoundActions {
                        if (showNext) RoundAction(GameIcons.Restart, "Retry", ButtonColor.VIOLET, onRetry)
                        if (onStages != null) RoundAction(GameIcons.Home, backLabel, ButtonColor.GLASS, onStages)
                    }
                }
            }
        }
    }
}

/**
 * Score and best side by side in one gold-framed section, split by a thin gold
 * line. On a new best the score turns gold and "NEW BEST!" replaces the best.
 */
@Composable
private fun ScoreAndBest(score: String, best: String?, isNewBest: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .goldInset()
            .padding(vertical = Spacing.s),
    ) {
        ScoreColumn("SCORE", Modifier.weight(1f)) {
            OutlinedText(score, GameTheme.type.title, color = if (isNewBest) GameColors.Gold else GameColors.TextPrimary, outlineWidth = 3.dp, maxLines = 1)
        }
        Box(
            Modifier
                .fillMaxHeight()
                .padding(vertical = Spacing.xs)
                .width(1.dp)
                .background(Brush.verticalGradient(listOf(GameColors.GoldTop.copy(alpha = 0.6f), GameColors.GoldLip.copy(alpha = 0.3f)))),
        )
        ScoreColumn(if (isNewBest) "NEW BEST!" else "BEST", Modifier.weight(1f)) {
            when {
                isNewBest -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Image(GameIcons.Star, contentDescription = null, modifier = Modifier.size(BEST_STAR))
                    OutlinedText(score, GameTheme.type.title, color = GameColors.Gold, outlineWidth = 3.dp, maxLines = 1)
                }
                best != null -> OutlinedText(best, GameTheme.type.title, outlineWidth = 3.dp, maxLines = 1)
                else -> OutlinedText("—", GameTheme.type.title, color = GameColors.TextMuted, outlineWidth = 3.dp)
            }
        }
    }
}

@Composable
private fun ScoreColumn(label: String, modifier: Modifier, value: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(label, style = GameTheme.type.caption, color = GameColors.Gold, maxLines = 1)
        value()
    }
}

/**
 * Three stars in a row, the middle one bigger (all on one baseline, so nothing
 * reaches up into the ribbon). An awarded star springs in with a soft gold glow
 * behind it; a missed one stays faded.
 */
@Composable
private fun AnimatedStars(shown: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { index ->
            val earned = index < shown
            val scale by animateFloatAsState(
                targetValue = if (earned) 1f else 0.75f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "star_$index",
            )
            val isMiddle = total == 3 && index == 1
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(if (isMiddle) BIG_STAR else STAR)
                    .drawBehind {
                        val r = size.minDimension / 2f
                        if (earned) {
                            drawCircle(
                                Brush.radialGradient(listOf(GameColors.Gold.copy(alpha = STAR_GLOW_ALPHA * scale), Color.Transparent), center, r * 1.4f),
                                radius = r * 1.4f,
                            )
                        }
                    },
            ) {
                Image(
                    imageVector = GameIcons.Star,
                    contentDescription = if (earned) "Star earned" else null,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(scale)
                        .alpha(if (earned) 1f else UNEARNED_ALPHA),
                )
            }
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
private const val STAR_GLOW_ALPHA = 0.55f
private val STAR = 44.dp
private val BIG_STAR = 58.dp
private val BEST_STAR = 22.dp

@Preview
@Composable
fun StageResultOverlayPreview() {
    BubbleTheme {
        StageResultOverlay(
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
