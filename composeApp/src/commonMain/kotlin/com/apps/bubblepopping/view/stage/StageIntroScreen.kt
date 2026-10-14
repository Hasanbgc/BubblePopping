package com.apps.bubblepopping.view.stage

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.bubblepopping.data.ProgressRepository
import com.apps.bubblepopping.data.StageProgress
import com.apps.bubblepopping.game.GameRules
import com.apps.bubblepopping.stage.StageCatalog
import com.apps.bubblepopping.stage.StageConfig
import com.apps.bubblepopping.ui.components.ButtonSize
import com.apps.bubblepopping.ui.components.GameButton
import com.apps.bubblepopping.ui.components.GameScreen
import com.apps.bubblepopping.ui.components.GlassPanel
import com.apps.bubblepopping.ui.components.OutlinedText
import com.apps.bubblepopping.ui.components.StarCounter
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.BubbleTheme
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing
import com.apps.bubblepopping.view.meta.BestScore
import com.apps.bubblepopping.view.meta.GoalLine
import kotlinx.coroutines.flow.map

/** Shown before a stage starts: what to do, the bonus objectives, and Play. */
@Composable
fun StageIntroScreenRoot(
    stage: StageConfig,
    progressRepository: ProgressRepository,
    onPlay: () -> Unit,
    onBack: () -> Unit,
) {
    val progress by remember(progressRepository, stage.id) {
        progressRepository.stageProgress().map { it[stage.id] }
    }.collectAsStateWithLifecycle(null)

    StageIntroScreen(stage = stage, progress = progress, onPlay = onPlay, onBack = onBack)
}

@Composable
fun StageIntroScreen(
    stage: StageConfig,
    progress: StageProgress?,
    onPlay: () -> Unit,
    onBack: () -> Unit,
) {
    GameScreen(
        title = "${stage.world.displayName} World",
        onBack = onBack,
        actions = { StarCounter() },
        horizontalAlignment = Alignment.CenterHorizontally,
        bottomBar = {
            GameButton(
                text = "PLAY",
                onClick = onPlay,
                size = ButtonSize.LARGE,
                icon = GameIcons.Play,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xxl, vertical = Spacing.l),
            )
        },
    ) {
        GlassPanel(Modifier.fillMaxWidth(), strong = true) {
            // Title block
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
                modifier = Modifier.fillMaxWidth(),
            ) {
                stage.boss?.let { boss ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Image(GameIcons.Crown, contentDescription = null, modifier = Modifier.size(28.dp))
                        Text("BOSS · ${boss.name}", style = GameTheme.type.label, color = GameColors.Gold)
                    }
                }
                OutlinedText("STAGE ${stage.id}", GameTheme.type.display, outlineWidth = 3.dp)
                StarRow(earned = progress?.stars ?: 0, total = stage.maxStars, starSize = 34.dp)
                Text(stage.introText, style = GameTheme.type.body, color = GameColors.TextPrimary, textAlign = TextAlign.Center)
            }
        }

        GlassPanel(Modifier.fillMaxWidth(), title = "Goal", icon = GameIcons.Target) {
            ObjectiveRow(description = stage.goal.description, progress = null, isBonus = false)
            stage.durationSec?.let { GoalLine(GameIcons.Clock, "$it seconds", emphasised = false) }
            if (stage.lives != GameRules.MAX_LIVES) {
                GoalLine(GameIcons.Heart, "${stage.lives} lives", emphasised = false)
            }
        }

        GlassPanel(
            modifier = Modifier.fillMaxWidth(),
            title = "Bonus stars",
            icon = GameIcons.Star,
            trailing = { progress?.let { BestScore(it.bestScore) } },
        ) {
            stage.bonusGoals.forEach { objective ->
                ObjectiveRow(description = objective.description, progress = null, isBonus = true)
            }
        }
    }
}

@Preview
@Composable
fun StageIntroScreenPreview() {
    BubbleTheme {
        StageIntroScreen(
            stage = StageCatalog.stage(3),
            progress = StageProgress(3, stars = 2, bestScore = 44),
            onPlay = {},
            onBack = {},
        )
    }
}
