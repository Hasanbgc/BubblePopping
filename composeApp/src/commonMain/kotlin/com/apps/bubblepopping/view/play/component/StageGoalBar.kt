package com.apps.bubblepopping.view.play.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apps.bubblepopping.stage.Objective
import com.apps.bubblepopping.stage.ObjectiveProgress
import com.apps.bubblepopping.view.NearMissColor
import com.apps.bubblepopping.view.ObjectiveMetColor
import com.apps.bubblepopping.view.StarColor

/** Below the HUD in a stage: countdown (if timed) and main-goal progress. */
@Composable
fun StageGoalBar(
    goal: ObjectiveProgress?,
    timeLeftSec: Int?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        timeLeftSec?.let { TimerPill(it) }
        goal?.let { GoalPill(it) }
    }
}

@Composable
private fun TimerPill(secondsLeft: Int) {
    val urgent = secondsLeft <= URGENT_SECONDS
    Text(
        text = "⏱ ${secondsLeft / 60}:${(secondsLeft % 60).toString().padStart(2, '0')}",
        color = if (urgent) NearMissColor else Color.White,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(PillBackground)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun GoalPill(goal: ObjectiveProgress) {
    // Flashes green when the goal is met.
    val background by animateColorAsState(
        targetValue = if (goal.satisfied) ObjectiveMetColor.copy(alpha = 0.85f) else PillBackground,
        animationSpec = tween(durationMillis = 300),
        label = "goal_pill_bg",
    )
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (goal.satisfied) "✓ ${goal.objective.description}"
                   else "${goal.objective.description}  ${goal.current} / ${goal.target}",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (!goal.satisfied) {
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(PROGRESS_BAR_WIDTH)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.2f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(goal.fraction)
                        .background(StarColor),
                )
            }
        }
    }
}

private val PillBackground = Color.Black.copy(alpha = 0.35f)
private val PROGRESS_BAR_WIDTH = 140.dp
private const val URGENT_SECONDS = 10

@Preview
@Composable
fun StageGoalBarPreview() {
    MaterialTheme {
        StageGoalBar(
            goal = ObjectiveProgress(Objective.PopCount(15), current = 7, target = 15, satisfied = false),
            timeLeftSec = 32,
        )
    }
}
