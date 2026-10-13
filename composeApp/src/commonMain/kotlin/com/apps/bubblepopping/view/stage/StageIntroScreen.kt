package com.apps.bubblepopping.view.stage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.bubblepopping.data.ProgressRepository
import com.apps.bubblepopping.data.StageProgress
import com.apps.bubblepopping.game.GameRules
import com.apps.bubblepopping.stage.StageCatalog
import com.apps.bubblepopping.stage.StageConfig
import com.apps.bubblepopping.view.ScreenBackgroundColors
import com.apps.bubblepopping.view.StageAccentColor
import com.apps.bubblepopping.view.StageCardColor
import com.apps.bubblepopping.view.home.BubbleTopAppBar
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(ScreenBackgroundColors))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BubbleTopAppBar(title = "${stage.world.displayName} World", onBackPress = onBack)
            Spacer(Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(StageCardColor)
                    .padding(24.dp),
            ) {
                Text(
                    text = "STAGE ${stage.id}",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                )
                StarRow(earned = progress?.stars ?: 0, total = stage.maxStars, fontSize = 26.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stage.introText,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(20.dp))
                SectionLabel("GOAL")
                ObjectiveRow(description = stage.goal.description, progress = null, isBonus = false)
                stage.durationSec?.let { seconds ->
                    Text(
                        text = "⏱ $seconds seconds",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(12.dp))
                SectionLabel("BONUS STARS")
                stage.bonusGoals.forEach { objective ->
                    ObjectiveRow(description = objective.description, progress = null, isBonus = true)
                }
                if (stage.lives != GameRules.MAX_LIVES) {
                    Text(
                        text = "❤ ${stage.lives} lives",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
                progress?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Best score  ${it.bestScore}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                    )
                }
            }

            Spacer(Modifier.weight(1f))
            Button(
                onClick = onPlay,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StageAccentColor,
                    contentColor = Color.White,
                ),
                modifier = Modifier
                    .padding(bottom = 32.dp)
                    .height(56.dp)
                    .fillMaxWidth(0.6f),
            ) {
                Text(text = "PLAY", fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.5f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 3.sp,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Preview
@Composable
fun StageIntroScreenPreview() {
    MaterialTheme {
        StageIntroScreen(
            stage = StageCatalog.stage(3),
            progress = StageProgress(3, stars = 2, bestScore = 44),
            onPlay = {},
            onBack = {},
        )
    }
}
