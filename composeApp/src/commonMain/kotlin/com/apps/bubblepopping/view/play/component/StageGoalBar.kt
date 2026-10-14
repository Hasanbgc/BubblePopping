package com.apps.bubblepopping.view.play.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.stage.Objective
import com.apps.bubblepopping.stage.ObjectiveProgress
import com.apps.bubblepopping.ui.components.GameProgressBar
import com.apps.bubblepopping.ui.components.OutlinedText
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.BubbleTheme
import com.apps.bubblepopping.ui.theme.Corners
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing

/** Below the HUD in a stage: countdown (if timed) and main-goal progress. */
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

/** Clock and m:ss; turns red for the last [URGENT_SECONDS]. */
@Composable
private fun TimerPill(secondsLeft: Int) {
    val urgent = secondsLeft <= URGENT_SECONDS
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = Modifier
            .clip(Corners.pill)
            .background(if (urgent) GameColors.RedBottom.copy(alpha = 0.75f) else GameColors.Ink.copy(alpha = 0.5f))
            .border(1.5.dp, GameColors.GlassRim, Corners.pill)
            .padding(horizontal = Spacing.m, vertical = Spacing.xs),
    ) {
        Image(GameIcons.Clock, contentDescription = null, modifier = Modifier.size(20.dp))
        OutlinedText(
            text = "${secondsLeft / 60}:${(secondsLeft % 60).toString().padStart(2, '0')}",
            style = GameTheme.type.label,
        )
    }
}

/** The main goal with its progress bar; turns green once met. */
@Composable
private fun GoalPill(goal: ObjectiveProgress) {
    val background by animateColorAsState(
        targetValue = if (goal.satisfied) GameColors.GreenBottom.copy(alpha = 0.85f) else GameColors.Ink.copy(alpha = 0.5f),
        animationSpec = tween(durationMillis = 300),
        label = "goal_pill_bg",
    )
    Column(
        modifier = Modifier
            .clip(Corners.medium)
            .background(background)
            .border(1.5.dp, GameColors.GlassRim, Corners.medium)
            .padding(horizontal = Spacing.m, vertical = Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Image(if (goal.satisfied) GameIcons.Check else GameIcons.Target, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                text = if (goal.satisfied) goal.objective.description
                       else "${goal.objective.description}  ${goal.current} / ${goal.target}",
                style = GameTheme.type.caption,
                color = Color.White,
            )
        }
        if (!goal.satisfied) GameProgressBar(goal.fraction, modifier = Modifier.width(PROGRESS_BAR_WIDTH), height = 8.dp)
    }
}

private val PROGRESS_BAR_WIDTH = 150.dp
private const val URGENT_SECONDS = 10

@Preview
@Composable
fun StageGoalBarPreview() {
    BubbleTheme {
        StageGoalBar(
            goal = ObjectiveProgress(Objective.PopCount(15), current = 7, target = 15, satisfied = false),
            timeLeftSec = 32,
        )
    }
}
