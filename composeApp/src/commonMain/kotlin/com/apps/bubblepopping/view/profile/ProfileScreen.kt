package com.apps.bubblepopping.view.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.bubblepopping.Res
import com.apps.bubblepopping.data.PlayerStats
import com.apps.bubblepopping.data.ProgressRepository
import com.apps.bubblepopping.ic_avatar
import com.apps.bubblepopping.stage.StageCatalog
import com.apps.bubblepopping.view.ComboColor
import com.apps.bubblepopping.view.ScreenBackgroundColors
import com.apps.bubblepopping.view.StarColor
import com.apps.bubblepopping.view.home.BubbleTopAppBar
import com.apps.bubblepopping.view.home.Difficulty
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.painterResource

@Composable
fun ProfileScreenRoot(
    progressRepository: ProgressRepository,
    onBack: () -> Unit,
) {
    val stats by remember(progressRepository) { progressRepository.stats() }
        .collectAsStateWithLifecycle(PlayerStats())
    val bestScores by remember(progressRepository) {
        combine(
            Difficulty.entries.map { difficulty ->
                progressRepository.bestEndlessScore(difficulty).map { difficulty to it }
            }
        ) { pairs -> pairs.toMap() }
    }.collectAsStateWithLifecycle(emptyMap())
    val totalStars by remember(progressRepository) {
        progressRepository.stageProgress().map { progress -> progress.values.sumOf { it.stars } }
    }.collectAsStateWithLifecycle(0)

    ProfileScreen(stats = stats, bestScores = bestScores, totalStars = totalStars, onBack = onBack)
}

@Composable
fun ProfileScreen(
    stats: PlayerStats,
    bestScores: Map<Difficulty, Int?>,
    totalStars: Int,
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
            BubbleTopAppBar(title = "Profile", onBackPress = onBack)

            Spacer(Modifier.height(16.dp))
            // Replaced by the Google / Facebook photo once sign-in exists.
            Image(
                painter = painterResource(Res.drawable.ic_avatar),
                contentDescription = "Profile picture",
                modifier = Modifier.size(96.dp).clip(CircleShape),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Player",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.height(28.dp))
            SectionTitle("Stats")
            StatRow(label = "Games played", value = stats.gamesPlayed.toString())
            StatRow(label = "Bubbles popped", value = stats.bubblesPopped.toString())
            StatRow(label = "Best combo", value = stats.bestCombo.toString(), valueColor = ComboColor)
            StatRow(
                label = "Stage stars",
                value = "★ $totalStars / ${StageCatalog.maxStars}",
                valueColor = StarColor,
            )

            Spacer(Modifier.height(20.dp))
            SectionTitle("Best scores")
            Difficulty.entries.forEach { difficulty ->
                StatRow(
                    label = difficulty.label,
                    value = bestScores[difficulty]?.toString() ?: "—",
                    valueColor = difficulty.glowColor,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        color = Color.White.copy(alpha = 0.50f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 3.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 6.dp),
    )
}

@Composable
private fun StatRow(label: String, value: String, valueColor: Color = Color.White) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = Color.White, fontSize = 15.sp)
        Text(text = value, color = valueColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Preview
@Composable
fun ProfileScreenPreview() {
    MaterialTheme {
        ProfileScreen(
            stats = PlayerStats(gamesPlayed = 42, bubblesPopped = 1_337, bestCombo = 18),
            bestScores = mapOf(Difficulty.EASY to 120, Difficulty.MEDIUM to 85, Difficulty.HARD to null),
            totalStars = 14,
            onBack = {},
        )
    }
}
