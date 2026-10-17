package com.apps.bubblepopping.view.leaderboard

import com.apps.bubblepopping.ui.components.ButtonColor
import com.apps.bubblepopping.ui.components.candyFace
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.apps.bubblepopping.ui.components.EnamelPanel
import com.apps.bubblepopping.ui.components.PanelDivider
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

/** One ranked player on a difficulty's board. */
data class LeaderboardEntry(
    val rank: Int,
    val name: String,
    val score: Int,
    val isCurrentUser: Boolean = false,
)

// ─────────────────────────────────────────────────────────────────────────────
// LeaderboardScreenRoot
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The global Endless rankings, one tab per difficulty (scores on different
 * difficulties aren't comparable). Until the online backend exists (§8.7) each
 * tab lists the best rounds played on this device.
 */
@Composable
fun LeaderboardScreenRoot(
    progressRepository: ProgressRepository,
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    var difficulty by rememberSaveable { mutableStateOf(Difficulty.EASY) }
    val runs by remember(progressRepository, difficulty) { progressRepository.topEndlessRuns(ENTRIES, difficulty) }
        .collectAsStateWithLifecycle(null)
    val settings by remember(settingsRepository) { settingsRepository.settings }
        .collectAsStateWithLifecycle(GameSettings())
    LeaderboardScreen(
        difficulty = difficulty,
        onDifficultyChange = { difficulty = it },
        runs = runs,
        playerName = settings.playerName,
        onBack = onBack,
    )
}

/** One row per round on this device, best first; they're all the player's own. */
fun leaderboardEntries(runs: List<EndlessRun>, playerName: String): List<LeaderboardEntry> =
    runs.mapIndexed { index, run ->
        LeaderboardEntry(rank = index + 1, name = playerName, score = run.score, isCurrentUser = true)
    }

/** [runs] are [difficulty]'s, best first; null while loading. */
@Composable
fun LeaderboardScreen(
    difficulty: Difficulty,
    onDifficultyChange: (Difficulty) -> Unit,
    runs: List<EndlessRun>?,
    playerName: String,
    onBack: () -> Unit,
) {
    val entries = remember(runs, playerName) { leaderboardEntries(runs.orEmpty(), playerName) }

    // The list scrolls itself, so the screen column doesn't.
    GameScreen(title = "Leaderboard", onBack = onBack, scrollable = false) {
        GameTabs(
            options = Difficulty.entries.map { it.label },
            selected = difficulty.ordinal,
            onSelect = { onDifficultyChange(Difficulty.entries[it]) },
        )
        Hint("Global rankings arrive with online play. For now, these are your best rounds.", Modifier.fillMaxWidth())
        when {
            // Loading (also briefly on a tab switch): nothing, so the old tab's podium doesn't flash.
            runs == null -> Spacer(Modifier.weight(1f))
            entries.isEmpty() -> EmptyMessage(
                icon = GameIcons.Bubble,
                title = "No scores yet",
                text = "Play an Endless round on ${difficulty.label} to set your first score!",
                modifier = Modifier.weight(1f),
            )
            // The top three on a podium; everyone else in one panel below, rows split by dividers.
            else -> Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.l),
                modifier = Modifier.weight(1f),
            ) {
                Podium(entries.take(PODIUM_PLACES))
                val rest = entries.drop(PODIUM_PLACES)
                if (rest.isNotEmpty()) {
                    EnamelPanel(Modifier.fillMaxWidth().weight(1f, fill = false), contentPadding = Spacing.m) {
                        LazyColumn {
                            itemsIndexed(items = rest, key = { _, entry -> entry.rank }) { index, entry ->
                                if (index > 0) PanelDivider()
                                LeaderboardItem(entry)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Podium
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The top three as soft violet cards: 2nd on the left, 1st in the middle (a little
 * taller, with a crown), 3rd on the right. Each card has the player's avatar in a
 * gold, silver or bronze ring with a numbered medal, the name, the difficulty,
 * and the score on a gold pill.
 */
@Composable
private fun Podium(top: List<LeaderboardEntry>) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Podium order: 2nd, 1st, 3rd. A missing place keeps its slot, so 1st stays centred.
        listOf(1, 0, 2).forEach { index ->
            val entry = top.getOrNull(index)
            Box(Modifier.weight(1f), contentAlignment = Alignment.BottomCenter) {
                if (entry != null) PodiumCard(entry)
            }
        }
    }
}

@Composable
private fun PodiumCard(entry: LeaderboardEntry) {
    val first = entry.rank == 1
    val medal = medalColor(entry.rank)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (first) FIRST_CARD_HEIGHT else CARD_HEIGHT)
            // Soft shadow and dark outline, like the other cards.
            .drawBehind {
                val outline = 2.dp.toPx()
                val radius = CARD_CORNER.toPx()
                drawRoundRect(
                    GameColors.ButtonShadow,
                    topLeft = Offset(0f, outline * 2f),
                    size = Size(size.width, size.height + outline),
                    cornerRadius = CornerRadius(radius),
                )
                drawRoundRect(
                    GameColors.ButtonOutline,
                    topLeft = Offset(-outline / 2f, -outline / 2f),
                    size = Size(size.width + outline, size.height + outline),
                    cornerRadius = CornerRadius(radius + outline / 2f),
                    style = Stroke(outline),
                )
            }
            .clip(RoundedCornerShape(CARD_CORNER))
            .background(Brush.verticalGradient(listOf(GameColors.PodiumCardTop, GameColors.PodiumCardBottom)))
            .border(1.5.dp, Brush.verticalGradient(listOf(GameColors.GlassRim, GameColors.PodiumCardRim)), RoundedCornerShape(CARD_CORNER))
            .padding(horizontal = Spacing.xs, vertical = Spacing.s),
    ) {
        if (first) Image(GameIcons.Crown, contentDescription = null, modifier = Modifier.size(CROWN_SIZE))
        PodiumAvatar(entry.rank, medal, if (first) FIRST_AVATAR else AVATAR)
        Text(
            text = entry.name,
            style = GameTheme.type.label,
            color = GameColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Spacing.s),
        )
        Spacer(Modifier.weight(1f))
        // The score on a gold candy pill.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier
                .candyFace(ButtonColor.GOLD, Corners.pill)
                .padding(horizontal = Spacing.s, vertical = 2.dp),
        ) {
            Image(GameIcons.Star, contentDescription = null, modifier = Modifier.size(SCORE_STAR))
            OutlinedText(formatCount(entry.score), GameTheme.type.label, outlineColor = GameColors.GoldLip, outlineWidth = 1.5.dp, maxLines = 1)
        }
    }
}

/** The avatar in a medal-coloured ring, with a numbered medal over its bottom edge. */
@Composable
private fun PodiumAvatar(rank: Int, medal: Color, size: Dp) {
    Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.size(width = size, height = size + MEDAL_BADGE / 2)) {
        // Replaced by the Google / Facebook photo once sign-in exists.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(size)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(GameColors.PodiumAvatarTop, GameColors.PodiumAvatarBottom)))
                .border(AVATAR_RING, Brush.verticalGradient(listOf(lerp(medal, Color.White, 0.4f), lerp(medal, Color.Black, 0.2f))), CircleShape),
        ) {
            Image(GameIcons.Person, contentDescription = null, modifier = Modifier.size(size * 0.62f))
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(MEDAL_BADGE)
                .drawBehind {
                    val outline = 1.5.dp.toPx()
                    drawCircle(GameColors.ButtonOutline, radius = this.size.minDimension / 2f + outline / 2f, style = Stroke(outline))
                }
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(lerp(medal, Color.White, 0.4f), medal, lerp(medal, Color.Black, 0.25f)))),
        ) {
            OutlinedText("$rank", GameTheme.type.label, outlineColor = lerp(medal, Color.Black, 0.55f), outlineWidth = 1.5.dp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LeaderboardItem
// ─────────────────────────────────────────────────────────────────────────────

/** A ranking row: medal or rank, name, difficulty chip, score. */
@Composable
fun LeaderboardItem(entry: LeaderboardEntry) {
    val podium = entry.rank <= 3
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        modifier = Modifier
            .fillMaxWidth()
            // Rows sit inside the leaderboard panel; the top three get a soft medal-coloured wash.
            .clip(Corners.medium)
            .background(if (podium) medalColor(entry.rank).copy(alpha = PODIUM_WASH) else Color.Transparent)
            .padding(horizontal = Spacing.s, vertical = Spacing.xs),
    ) {
        RankBadge(entry.rank)
        Text(
            text = entry.name,
            style = GameTheme.type.label,
            color = GameColors.TextPrimary,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        OutlinedText(
            text = formatCount(entry.score),
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

private fun medalColor(rank: Int): Color = when (rank) {
    1 -> GameColors.Gold
    2 -> Color(0xFFD5DBF0)
    else -> Color(0xFFE09A55)
}

@Composable
private fun EmptyMessage(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        EnamelPanel(Modifier.fillMaxWidth()) {
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

/** How many rounds each difficulty's tab lists. */
private const val ENTRIES = 20
private val RANK_SIZE = 36.dp
private const val PODIUM_PLACES = 3
/** 2nd and 3rd are the same height; 1st is a little taller. */
private val CARD_HEIGHT = 176.dp
private val FIRST_CARD_HEIGHT = 200.dp
private val CARD_CORNER = 18.dp
private val AVATAR = 56.dp
private val FIRST_AVATAR = 62.dp
private val AVATAR_RING = 3.dp
private val MEDAL_BADGE = 24.dp
private val CROWN_SIZE = 24.dp
private val SCORE_STAR = 16.dp
private const val PODIUM_WASH = 0.14f

@Preview
@Composable
fun LeaderboardScreenPreview() {
    BubbleTheme {
        LeaderboardScreen(
            difficulty = Difficulty.MEDIUM,
            onDifficultyChange = {},
            runs = listOf(212, 156, 98, 61).map { EndlessRun(Difficulty.MEDIUM, score = it, playedAt = 0) },
            playerName = "Player",
            onBack = {},
        )
    }
}
