package com.apps.bubblepopping.view.stage

import com.apps.bubblepopping.ui.icons.playIcon
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import com.apps.bubblepopping.game.GameRules
import com.apps.bubblepopping.stage.StageCatalog
import com.apps.bubblepopping.stage.StageConfig
import com.apps.bubblepopping.ui.components.ButtonSize
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
 * the limits and best score as small pills, and Play.
 */
@OptIn(ExperimentalLayoutApi::class)
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
            Text("GOAL", style = GameTheme.type.label, color = GameColors.Gold)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .goldInset()
                    .padding(horizontal = Spacing.m, vertical = Spacing.xs),
            ) {
                ObjectiveRow(description = stage.goal.description, progress = null, isBonus = false)
                stage.bonusGoals.forEach { objective ->
                    ObjectiveRow(description = objective.description, progress = null, isBonus = true)
                }
            }
        }

        // Limits and best score; wraps onto two lines on narrow phones.
        val best = progress?.bestScore?.takeIf { it > 0 }
        val hasLimits = stage.durationSec != null || stage.lives != GameRules.MAX_LIVES
        if (hasLimits || best != null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
                modifier = Modifier.fillMaxWidth(),
            ) {
                stage.durationSec?.let { ResourcePill(GameIcons.Clock, "${it}s", tint = GameColors.OceanTint, tone = EnamelTone.OCEAN) }
                if (stage.lives != GameRules.MAX_LIVES) {
                    ResourcePill(GameIcons.Heart, "${stage.lives} lives", tint = GameColors.PillHeart)
                }
                best?.let { ResourcePill(GameIcons.Star, "Best ${formatCount(it)}", tint = GameColors.VioletTop, tone = EnamelTone.VIOLET) }
            }
        }

        GameButton(
            text = "PLAY",
            onClick = onPlay,
            size = ButtonSize.LARGE,
            icon = playIcon(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private val BOSS_ICON = 28.dp
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
