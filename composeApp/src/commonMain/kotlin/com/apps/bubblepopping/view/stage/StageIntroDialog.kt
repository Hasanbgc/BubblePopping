package com.apps.bubblepopping.view.stage

import com.apps.bubblepopping.ui.icons.playIcon
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.data.StageProgress
import com.apps.bubblepopping.stage.StageCatalog
import com.apps.bubblepopping.stage.StageConfig
import com.apps.bubblepopping.ui.components.ButtonColor
import com.apps.bubblepopping.ui.components.ButtonSize
import com.apps.bubblepopping.ui.components.OutlinedText
import com.apps.bubblepopping.ui.components.GameButton
import com.apps.bubblepopping.ui.components.EnamelTone
import com.apps.bubblepopping.ui.components.GameDialog
import com.apps.bubblepopping.ui.components.ResourcePill
import com.apps.bubblepopping.ui.components.formatCount
import com.apps.bubblepopping.ui.components.goldInset
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.BubbleTheme
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing

/**
 * Shown over the stage map when a stage is tapped, built to be read in one
 * glance: stars earned, a one-line story, the goal and bonus stars in one box,
 * the best score once played, and Play.
 */
@Composable
fun StageIntroDialog(
    stage: StageConfig,
    progress: StageProgress?,
    onPlay: () -> Unit,
    onDismiss: () -> Unit,
) {
    GameDialog(title = "Stage ${stage.id}", onDismiss = onDismiss) {
        stage.boss?.let { boss ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Image(GameIcons.Crown, contentDescription = null, modifier = Modifier.size(BOSS_ICON))
                Text("BOSS · ${boss.name}", style = GameTheme.type.label, color = GameColors.Gold)
            }
        }
        StarRow(earned = progress?.stars ?: 0, total = stage.maxStars, starSize = STAR_SIZE)
        Text(
            text = stage.introText,
            style = GameTheme.type.body,
            color = GameColors.TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = STORY_LINES,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )

        // "GOAL" centred just above the box holding the goal, then the bonus stars.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Image(GameIcons.Target, contentDescription = null, modifier = Modifier.size(GOAL_TITLE_ICON))
                OutlinedText("GOAL", GameTheme.type.headline, color = GameColors.Gold)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .goldInset()
                    .padding(horizontal = Spacing.m, vertical = Spacing.xs),
            ) {
                ObjectiveRow(description = stage.goal.description, progress = null, isBonus = false, goalIcon = goalIcon(stage.goal))
                stage.bonusGoals.forEach { objective ->
                    ObjectiveRow(description = objective.description, progress = null, isBonus = true)
                }
            }
        }

        // The best score, once the stage has been played.
        progress?.bestScore?.takeIf { it > 0 }?.let { best ->
            ResourcePill(GameIcons.Star, "Best ${formatCount(best)}", tint = GameColors.VioletTop, tone = EnamelTone.VIOLET)
        }

        GameButton(
            text = "PLAY",
            onClick = onPlay,
            // Same green PLAY as everywhere else.
            color = ButtonColor.GREEN,
            size = ButtonSize.LARGE,
            icon = playIcon(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private val BOSS_ICON = 28.dp
private val GOAL_TITLE_ICON = 26.dp
private val STAR_SIZE = 38.dp
/** The story is one short line or two; anything longer is cut, so the card stays a glance. */
private const val STORY_LINES = 2

@Preview
@Composable
fun StageIntroDialogPreview() {
    BubbleTheme {
        StageIntroDialog(
            stage = StageCatalog.stage(3),
            progress = StageProgress(3, stars = 2, bestScore = 44),
            onPlay = {},
            onDismiss = {},
        )
    }
}
