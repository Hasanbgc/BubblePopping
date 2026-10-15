package com.apps.bubblepopping.view.play.component

import com.apps.bubblepopping.ui.components.Ribbon
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.stage.Objective
import com.apps.bubblepopping.stage.ObjectiveProgress
import com.apps.bubblepopping.ui.components.EnamelTone
import com.apps.bubblepopping.ui.components.OutlinedText
import com.apps.bubblepopping.ui.components.enamelPill
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.BubbleTheme
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing

/** Below the HUD in a stage: countdown (if timed) and main-goal progress, as enamel pills. */
@Composable
fun StageGoalBar(
    goal: ObjectiveProgress?,
    timeLeftSec: Int?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        timeLeftSec?.let { TimerPill(it) }
        goal?.let { GoalPill(it) }
    }
}

/** Clock and m:ss on a blue enamel pill; turns red for the last [URGENT_SECONDS]. */
@Composable
private fun TimerPill(secondsLeft: Int) {
    val urgent = secondsLeft <= URGENT_SECONDS
    val tint by animateColorAsState(
        targetValue = if (urgent) GameColors.RedTop else GameColors.PillBubble,
        animationSpec = tween(durationMillis = TINT_FADE_MS),
        label = "timer_tint",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = Modifier
            .height(TIMER_HEIGHT)
            .enamelPill(tint, EnamelTone.LIGHT)
            .padding(start = Spacing.xs, end = Spacing.m),
    ) {
        Image(GameIcons.Clock, contentDescription = null, modifier = Modifier.size(TIMER_ICON))
        OutlinedText(
            text = "${secondsLeft / 60}:${(secondsLeft % 60).toString().padStart(2, '0')}",
            style = GameTheme.type.label,
            color = if (urgent) GameColors.RedTop else GameColors.TextPrimary,
            maxLines = 1,
        )
    }
}

/**
 * The main goal as a compact pill, the same size as the timer: the target icon
 * and the count ("7/15"), with the pill's body filling up in gold left to right
 * as the goal nears. Once met it pops, turns green, and reads "✓ DONE". The goal
 * itself is shown in full by [GoalBanner] when the round starts.
 */
@Composable
private fun GoalPill(goal: ObjectiveProgress) {
    val tint by animateColorAsState(
        targetValue = if (goal.satisfied) GameColors.Success else GameColors.PillBubble,
        animationSpec = tween(durationMillis = TINT_FADE_MS),
        label = "goal_tint",
    )
    val fill by animateFloatAsState(
        targetValue = if (goal.satisfied) 1f else goal.fraction,
        animationSpec = tween(durationMillis = FILL_MS),
        label = "goal_fill",
    )
    val fillColor = if (goal.satisfied) GameColors.Success else GameColors.Gold
    // A quick pop when the goal is met.
    val pop = remember { Animatable(1f) }
    LaunchedEffect(goal.satisfied) {
        if (goal.satisfied) {
            pop.snapTo(DONE_POP_SCALE)
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = Modifier
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            }
            .height(PILL_HEIGHT)
            .widthIn(min = GOAL_MIN_WIDTH)
            .enamelPill(tint, EnamelTone.LIGHT)
            // The fill: gold liquid inside the pill, up to the goal's progress.
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(listOf(fillColor.copy(alpha = 0.75f), fillColor.copy(alpha = 0.45f))),
                    size = size.copy(width = size.width * fill),
                )
            }
            .padding(start = Spacing.xs, end = Spacing.m),
    ) {
        Image(
            imageVector = if (goal.satisfied) GameIcons.Check else GameIcons.Target,
            contentDescription = null,
            modifier = Modifier.size(PILL_ICON),
        )
        OutlinedText(goalCount(goal), GameTheme.type.label, maxLines = 1)
    }
}

/** "7/15", seconds for time goals, "DONE" once met. */
private fun goalCount(goal: ObjectiveProgress): String = when {
    goal.satisfied -> "DONE"
    goal.objective.isConstraint -> "✗"
    goal.objective is Objective.Survive -> "${goal.current}/${goal.target}s"
    else -> "${goal.current}/${goal.target}"
}

/**
 * The stage's goal in full ("Pop 15 bubbles!"), shown big in the middle when the
 * round starts, then shrinking away upwards towards the goal pill.
 */
@Composable
fun GoalBanner(text: String, visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.6f),
        exit = fadeOut() + scaleOut(targetScale = 0.4f, transformOrigin = TransformOrigin(0.5f, -1.5f)),
        modifier = modifier,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            OutlinedText("GOAL", GameTheme.type.label, color = GameColors.Gold)
            Ribbon(text)
        }
    }
}

private val PILL_HEIGHT = 34.dp
private val PILL_ICON = 26.dp
private val GOAL_MIN_WIDTH = 92.dp
private const val FILL_MS = 350
private const val DONE_POP_SCALE = 1.2f
private val TIMER_HEIGHT = PILL_HEIGHT
private val TIMER_ICON = PILL_ICON
private const val URGENT_SECONDS = 10
private const val TINT_FADE_MS = 300

@Preview
@Composable
fun StageGoalBarPreview() {
    BubbleTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.m), modifier = Modifier.padding(Spacing.l)) {
            StageGoalBar(
                goal = ObjectiveProgress(Objective.PopCount(15), current = 7, target = 15, satisfied = false),
                timeLeftSec = 32,
            )
            StageGoalBar(
                goal = ObjectiveProgress(Objective.PopCount(15), current = 15, target = 15, satisfied = true),
                timeLeftSec = 8,
            )
        }
    }
}
