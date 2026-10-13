package com.apps.bubblepopping.view.play

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apps.bubblepopping.stage.StageResult
import com.apps.bubblepopping.view.StageAccentColor
import com.apps.bubblepopping.view.StageCardColor
import com.apps.bubblepopping.view.StarColor
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
) {
    var shownStars by remember(result) { mutableIntStateOf(0) }
    val countedScore = remember(result) { Animatable(0f) }

    LaunchedEffect(result) {
        delay(STAR_START_DELAY_MS)
        for (star in 1..result.stars) {
            shownStars = star
            onStarShown(star)
            delay(STAR_INTERVAL_MS)
        }
    }
    LaunchedEffect(result) {
        countedScore.animateTo(score.toFloat(), tween(durationMillis = SCORE_COUNT_MS))
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.background(Color.Black.copy(alpha = 0.72f)),
    ) {
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
                text = "STAGE $stageId",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                letterSpacing = 3.sp,
            )
            Text(
                text = when {
                    !result.won -> "STAGE FAILED"
                    isBossStage -> "BOSS DEFEATED!"
                    else -> "STAGE CLEAR!"
                },
                color = if (result.won) StarColor else Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(8.dp))
            AnimatedStars(shown = shownStars, total = maxStars)

            Spacer(Modifier.height(8.dp))
            Text(
                text = countedScore.value.toInt().toString(),
                color = Color.White,
                fontSize = 44.sp,
                fontWeight = FontWeight.Light,
            )
            when {
                isNewBest -> Text("NEW BEST!", color = StarColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                bestScore != null -> Text("BEST  $bestScore", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            }

            Spacer(Modifier.height(12.dp))
            result.objectives.forEachIndexed { index, progress ->
                ObjectiveRow(
                    description = progress.objective.description,
                    progress = progress,
                    isBonus = index > 0,
                )
            }
            // A failed goal that was nearly reached gets a nudge to retry.
            result.objectives.firstOrNull()
                ?.takeIf { !result.won && !it.objective.isConstraint && it.fraction >= SO_CLOSE_FRACTION }
                ?.let {
                    Text(
                        text = "${it.current} / ${it.target} — so close!",
                        color = StarColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

            Spacer(Modifier.height(20.dp))
            val showNext = result.won && onNext != null
            if (result.won && onNext != null) ResultButton("Next", primary = true, onClick = onNext)
            ResultButton("Retry", primary = !showNext, onClick = onRetry)
            if (onStages != null) ResultButton("Stages", primary = false, onClick = onStages)
        }
    }
}

@Composable
private fun AnimatedStars(shown: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(total) { index ->
            val earned = index < shown
            // Each star springs in from small when it is awarded.
            val scale by animateFloatAsState(
                targetValue = if (earned) 1f else 0.7f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "star_$index",
            )
            Text(
                text = "★",
                fontSize = 44.sp,
                color = if (earned) StarColor else StarColor.copy(alpha = 0.2f),
                modifier = Modifier.scale(scale),
            )
        }
    }
}

@Composable
private fun ResultButton(text: String, primary: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) StageAccentColor else Color.White.copy(alpha = 0.12f),
            contentColor = Color.White,
        ),
        modifier = Modifier
            .padding(top = 8.dp)
            .height(50.dp)
            .fillMaxWidth(0.7f),
    ) {
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

private const val STAR_START_DELAY_MS = 400L
private const val STAR_INTERVAL_MS = 350L
private const val SCORE_COUNT_MS = 900
/** Failed goals at least this far along show "so close!". */
private const val SO_CLOSE_FRACTION = 0.6f
