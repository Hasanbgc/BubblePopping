package com.apps.bubblepopping.view.leaderboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.bubblepopping.data.EndlessRun
import com.apps.bubblepopping.data.ProgressRepository
import com.apps.bubblepopping.data.prefs.GameSettings
import com.apps.bubblepopping.data.prefs.SettingsRepository
import com.apps.bubblepopping.ui.components.GameScreen
import com.apps.bubblepopping.ui.components.GameTabs
import com.apps.bubblepopping.ui.components.GlassPanel
import com.apps.bubblepopping.ui.components.OutlinedText
import com.apps.bubblepopping.ui.components.formatCount
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.BubbleTheme
import com.apps.bubblepopping.ui.theme.Corners
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing
import com.apps.bubblepopping.view.home.Difficulty
import com.apps.bubblepopping.view.meta.Hint

// ─────────────────────────────────────────────────────────────────────────────
// Data model
// ─────────────────────────────────────────────────────────────────────────────

data class LeaderboardEntry(
    val rank: Int,
    val name: String,
    val easyScore: Int?,
    val mediumScore: Int?,
    val hardScore: Int?,
    val isCurrentUser: Boolean = false,
) {
    val totalScore: Int get() = (easyScore ?: 0) + (mediumScore ?: 0) + (hardScore ?: 0)

    /** The difficulty this entry's round was played on (local rounds have exactly one). */
    val difficulty: Difficulty?
        get() = when {
            hardScore != null -> Difficulty.HARD
            mediumScore != null -> Difficulty.MEDIUM
            easyScore != null -> Difficulty.EASY
            else -> null
        }
}

enum class LeaderboardTab(val title: String) { LOCAL("This device"), GLOBAL("Global") }

// ─────────────────────────────────────────────────────────────────────────────
// LeaderboardScreenRoot
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Local: the best Endless rounds played on this device. Global: kept as a tab,
 * shown as "coming soon" until an online backend exists (§8.7).
 */
@Composable
fun LeaderboardScreenRoot(
    progressRepository: ProgressRepository,
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    val runs by remember(progressRepository) { progressRepository.topEndlessRuns(LOCAL_ENTRIES) }
        .collectAsStateWithLifecycle(null)
    val settings by remember(settingsRepository) { settingsRepository.settings }
        .collectAsStateWithLifecycle(GameSettings())
    LeaderboardScreen(runs = runs, playerName = settings.playerName, onBack = onBack)
}

/** One row per round, its score in its difficulty's column. */
fun leaderboardEntries(runs: List<EndlessRun>, playerName: String): List<LeaderboardEntry> =
    runs.mapIndexed { index, run ->
        LeaderboardEntry(
            rank        = index + 1,
            name        = playerName,
            easyScore   = run.score.takeIf { run.difficulty == Difficulty.EASY },
            mediumScore = run.score.takeIf { run.difficulty == Difficulty.MEDIUM },
            hardScore   = run.score.takeIf { run.difficulty == Difficulty.HARD },
        )
    }

@Composable
fun LeaderboardScreen(
    runs: List<EndlessRun>?,
    playerName: String,
    onBack: () -> Unit,
) {
    var activeTab by rememberSaveable { mutableStateOf(LeaderboardTab.LOCAL) }
    val entries = remember(runs, playerName) { leaderboardEntries(runs.orEmpty(), playerName) }

    // The list scrolls itself, so the screen column doesn't.
    GameScreen(title = "Leaderboard", onBack = onBack, scrollable = false) {
        GameTabs(
            options = LeaderboardTab.entries.map { it.title },
            selected = activeTab.ordinal,
            onSelect = { activeTab = LeaderboardTab.entries[it] },
        )
        when {
            activeTab == LeaderboardTab.GLOBAL -> EmptyMessage(
                icon = GameIcons.Crown,
                title = "Coming soon!",
                text = "Global rankings arrive with online play.\nFor now, beat your own best.",
                modifier = Modifier.weight(1f),
            )
            runs != null && entries.isEmpty() -> EmptyMessage(
                icon = GameIcons.Bubble,
                title = "No scores yet",
                text = "Play an Endless round to set your first score!",
                modifier = Modifier.weight(1f),
            )
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = Spacing.s),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                items(items = entries, key = { it.rank }) { entry -> LeaderboardItem(entry) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LeaderboardItem
// ─────────────────────────────────────────────────────────────────────────────

/** A glass row: medal or rank, name, difficulty chip, score. */
@Composable
fun LeaderboardItem(entry: LeaderboardEntry) {
    val podium = entry.rank <= 3
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        modifier = Modifier
            .fillMaxWidth()
            .clip(Corners.medium)
            .background(if (podium) GameColors.GlassFillStrong else GameColors.GlassFill)
            .border(if (podium) 2.dp else 1.dp, if (podium) medalColor(entry.rank) else GameColors.GlassRim.copy(alpha = 0.5f), Corners.medium)
            .padding(horizontal = Spacing.m, vertical = Spacing.s),
    ) {
        RankBadge(entry.rank)
        Text(
            text = entry.name,
            style = GameTheme.type.label,
            color = GameColors.TextPrimary,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        entry.difficulty?.let { DifficultyChip(it) }
        OutlinedText(
            text = formatCount(entry.totalScore),
            style = GameTheme.type.headline,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = Spacing.xs),
        )
    }
}

/** Gold, silver and bronze medals for the top three; a plain number after that. */
@Composable
private fun RankBadge(rank: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(RANK_SIZE)
            .clip(CircleShape)
            .background(
                if (rank <= 3) Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.6f), medalColor(rank)))
                else Brush.verticalGradient(listOf(GameColors.Ink.copy(alpha = 0.4f), GameColors.Ink.copy(alpha = 0.4f)))
            )
            .border(2.dp, if (rank <= 3) GameColors.Ink else GameColors.GlassRim.copy(alpha = 0.5f), CircleShape),
    ) {
        OutlinedText("$rank", GameTheme.type.label)
    }
}

@Composable
private fun DifficultyChip(difficulty: Difficulty) {
    Text(
        text = difficulty.label,
        style = GameTheme.type.caption,
        color = GameColors.TextPrimary,
        modifier = Modifier
            .clip(Corners.pill)
            .background(difficulty.glowColor.copy(alpha = 0.55f))
            .border(1.dp, difficulty.glowColor, Corners.pill)
            .padding(horizontal = Spacing.s, vertical = 2.dp),
    )
}

private fun medalColor(rank: Int): Color = when (rank) {
    1 -> GameColors.Gold
    2 -> Color(0xFFD5DBF0)
    else -> Color(0xFFE09A55)
}

@Composable
private fun EmptyMessage(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        GlassPanel(Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.l),
            ) {
                Image(icon, contentDescription = null, modifier = Modifier.size(64.dp))
                OutlinedText(title, GameTheme.type.title, outlineWidth = 3.dp)
                Hint(text, Modifier.fillMaxWidth().padding(horizontal = Spacing.l))
            }
        }
    }
}

/** How many local rounds the Local tab lists. */
private const val LOCAL_ENTRIES = 20
private val RANK_SIZE = 36.dp

@Preview
@Composable
fun LeaderboardScreenPreview() {
    BubbleTheme {
        LeaderboardScreen(
            runs = listOf(
                EndlessRun(Difficulty.MEDIUM, score = 212, playedAt = 0),
                EndlessRun(Difficulty.HARD, score = 156, playedAt = 0),
                EndlessRun(Difficulty.EASY, score = 98, playedAt = 0),
                EndlessRun(Difficulty.EASY, score = 61, playedAt = 0),
            ),
            playerName = "Player",
            onBack = {},
        )
    }
}
