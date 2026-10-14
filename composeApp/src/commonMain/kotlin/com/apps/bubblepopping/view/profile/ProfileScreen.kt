package com.apps.bubblepopping.view.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.bubblepopping.data.MetaRepository
import com.apps.bubblepopping.data.PlayerStats
import com.apps.bubblepopping.data.ProgressRepository
import com.apps.bubblepopping.data.StageProgress
import com.apps.bubblepopping.data.prefs.GameSettings
import com.apps.bubblepopping.data.prefs.MAX_PLAYER_NAME_LENGTH
import com.apps.bubblepopping.data.prefs.SettingsRepository
import com.apps.bubblepopping.meta.Achievement
import com.apps.bubblepopping.meta.AchievementContext
import com.apps.bubblepopping.meta.PlayerState
import com.apps.bubblepopping.stage.StageCatalog
import com.apps.bubblepopping.ui.components.ButtonColor
import com.apps.bubblepopping.ui.components.CoinAmount
import com.apps.bubblepopping.ui.components.DialogButtons
import com.apps.bubblepopping.ui.components.GameButton
import com.apps.bubblepopping.ui.components.GameDialog
import com.apps.bubblepopping.ui.components.GameIconButton
import com.apps.bubblepopping.ui.components.GameProgressBar
import com.apps.bubblepopping.ui.components.GameScreen
import com.apps.bubblepopping.ui.components.GameTextField
import com.apps.bubblepopping.ui.components.GlassPanel
import com.apps.bubblepopping.ui.components.OutlinedText
import com.apps.bubblepopping.ui.components.ResourcePill
import com.apps.bubblepopping.ui.components.formatCount
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.BubbleTheme
import com.apps.bubblepopping.ui.theme.Corners
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing
import com.apps.bubblepopping.view.home.Difficulty
import com.apps.bubblepopping.view.meta.Hint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun ProfileScreenRoot(
    progressRepository: ProgressRepository,
    metaRepository: MetaRepository,
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val stats by remember(progressRepository) { progressRepository.stats() }
        .collectAsStateWithLifecycle(PlayerStats())
    val bestScores by remember(progressRepository) {
        combine(
            Difficulty.entries.map { difficulty ->
                progressRepository.bestEndlessScore(difficulty).map { difficulty to it }
            }
        ) { pairs -> pairs.toMap() }
    }.collectAsStateWithLifecycle(emptyMap())
    val stageProgress by remember(progressRepository) { progressRepository.stageProgress() }
        .collectAsStateWithLifecycle(emptyMap())
    val player by remember(metaRepository) { metaRepository.player() }.collectAsStateWithLifecycle(PlayerState())
    val achievements by remember(metaRepository) { metaRepository.achievements() }
        .collectAsStateWithLifecycle(emptySet())
    val settings by remember(settingsRepository) { settingsRepository.settings }
        .collectAsStateWithLifecycle(GameSettings())

    ProfileScreen(
        playerName = settings.playerName,
        stats = stats,
        bestScores = bestScores,
        stageProgress = stageProgress,
        player = player,
        unlockedAchievements = achievements,
        onRename = { name -> scope.launch { settingsRepository.setPlayerName(name) } },
        onBack = onBack,
    )
}

@Composable
fun ProfileScreen(
    playerName: String,
    stats: PlayerStats,
    bestScores: Map<Difficulty, Int?>,
    stageProgress: Map<Int, StageProgress>,
    player: PlayerState,
    unlockedAchievements: Set<Achievement>,
    onRename: (String) -> Unit,
    onBack: () -> Unit,
) {
    var editingName by remember { mutableStateOf(false) }
    val totalStars = stageProgress.values.sumOf { it.stars }
    val achievementContext = AchievementContext(stats, stageProgress, bestScores, player.missionsCompleted)

    GameScreen(title = "Profile", onBack = onBack) {
        PlayerHeader(
            name = playerName,
            coins = player.coins,
            stars = totalStars,
            onEditName = { editingName = true },
        )

        GlassPanel(Modifier.fillMaxWidth(), title = "Stats", icon = GameIcons.Target) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                StatTile(GameIcons.Play, formatCount(stats.gamesPlayed), "Rounds played", Modifier.weight(1f))
                StatTile(GameIcons.Bubble, formatCount(stats.bubblesPopped.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()), "Bubbles popped", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                StatTile(GameIcons.Star, "${stats.bestCombo}", "Best combo", Modifier.weight(1f))
                StatTile(GameIcons.Check, "${player.missionsCompleted}", "Missions done", Modifier.weight(1f))
            }
        }

        GlassPanel(Modifier.fillMaxWidth(), title = "Best scores", icon = GameIcons.Trophy) {
            Difficulty.entries.forEach { difficulty ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                    Box(Modifier.size(14.dp).clip(CircleShape).background(difficulty.glowColor).border(2.dp, GameColors.GlassRim, CircleShape))
                    Text(difficulty.label, style = GameTheme.type.body, color = GameColors.TextPrimary, modifier = Modifier.weight(1f))
                    val best = bestScores[difficulty]
                    if (best != null) OutlinedText(formatCount(best), GameTheme.type.headline)
                    else Text("—", style = GameTheme.type.headline, color = GameColors.TextMuted)
                }
            }
        }

        GlassPanel(
            modifier = Modifier.fillMaxWidth(),
            title = "Achievements",
            icon = GameIcons.Crown,
            trailing = {
                Text(
                    "${unlockedAchievements.size} / ${Achievement.entries.size}",
                    style = GameTheme.type.label,
                    color = GameColors.TextSecondary,
                )
            },
        ) {
            // Reached ones first, then by how close the rest are.
            Achievement.entries
                .sortedWith(
                    compareByDescending<Achievement> { it in unlockedAchievements }
                        .thenByDescending { it.progress(achievementContext).toFloat() / it.target }
                )
                .forEach { achievement ->
                    AchievementRow(
                        achievement = achievement,
                        progress = achievement.progress(achievementContext),
                        unlocked = achievement in unlockedAchievements,
                    )
                }
        }
    }

    if (editingName) {
        NameDialog(
            current = playerName,
            onSave = { onRename(it); editingName = false },
            onDismiss = { editingName = false },
        )
    }
}

/** Avatar, name with an edit button, and the coin / star counters. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayerHeader(name: String, coins: Int, stars: Int, onEditName: () -> Unit) {
    GlassPanel(Modifier.fillMaxWidth(), strong = true) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.l)) {
            // Replaced by the Google / Facebook photo once sign-in exists.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(AVATAR_SIZE)
                    .clip(CircleShape)
                    .background(GameColors.Ink.copy(alpha = 0.4f))
                    .border(3.dp, GameColors.GlassRim, CircleShape),
            ) {
                Image(GameIcons.Person, contentDescription = "Profile picture", modifier = Modifier.size(AVATAR_SIZE * 0.7f))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    OutlinedText(name, GameTheme.type.title, outlineWidth = 3.dp, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                    GameIconButton(GameIcons.Pencil, contentDescription = "Edit name", onClick = onEditName, size = 36.dp)
                }
                // Wraps onto two lines on narrow phones.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    ResourcePill(GameIcons.Coin, formatCount(coins))
                    ResourcePill(GameIcons.Star, "$stars/${StageCatalog.maxStars}")
                }
            }
        }
    }
}

/** One number with an icon and a label, in a quarter of the Stats panel. */
@Composable
private fun StatTile(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(Corners.medium)
            .background(GameColors.Ink.copy(alpha = 0.3f))
            .padding(vertical = Spacing.m, horizontal = Spacing.s),
    ) {
        Image(icon, contentDescription = null, modifier = Modifier.size(28.dp))
        OutlinedText(value, GameTheme.type.headline, maxLines = 1)
        Hint(label)
    }
}

@Composable
private fun AchievementRow(achievement: Achievement, progress: Int, unlocked: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        modifier = Modifier
            .fillMaxWidth()
            .clip(Corners.medium)
            .background(if (unlocked) GameColors.Success.copy(alpha = 0.18f) else GameColors.Ink.copy(alpha = 0.3f))
            .padding(Spacing.m),
    ) {
        Image(if (unlocked) GameIcons.Trophy else GameIcons.Lock, contentDescription = null, modifier = Modifier.size(36.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(achievement.title, style = GameTheme.type.label, color = GameColors.TextPrimary)
            Hint(achievement.description)
            if (!unlocked) {
                GameProgressBar(progress.toFloat() / achievement.target, height = 10.dp)
                Text("$progress / ${achievement.target}", style = GameTheme.type.caption, color = GameColors.TextMuted)
            }
        }
        if (unlocked) {
            Image(GameIcons.Check, contentDescription = "Reached", modifier = Modifier.size(28.dp))
        } else {
            CoinAmount("+${achievement.reward}")
        }
    }
}

/** Rename: avatar, text field with a character count, Save / Cancel. */
@Composable
private fun NameDialog(current: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(current) }
    GameDialog(title = "Your name", onDismiss = onDismiss) {
        Image(GameIcons.Person, contentDescription = null, modifier = Modifier.size(64.dp))
        GameTextField(
            value = name,
            onValueChange = { name = it.take(MAX_PLAYER_NAME_LENGTH) },
            placeholder = GameSettings.DEFAULT_PLAYER_NAME,
            onDone = { onSave(name) },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "${name.length} / $MAX_PLAYER_NAME_LENGTH",
            style = GameTheme.type.caption,
            color = GameColors.TextSecondary,
            modifier = Modifier.align(Alignment.End),
        )
        DialogButtons {
            GameButton("Save", onClick = { onSave(name) }, color = ButtonColor.GREEN, modifier = Modifier.fillMaxWidth())
            GameButton("Cancel", onClick = onDismiss, color = ButtonColor.GLASS, modifier = Modifier.fillMaxWidth())
        }
    }
}

private val AVATAR_SIZE = 84.dp

@Preview
@Composable
fun ProfileScreenPreview() {
    BubbleTheme {
        ProfileScreen(
            playerName = "Bubbles",
            stats = PlayerStats(gamesPlayed = 42, bubblesPopped = 1_337, bestCombo = 18),
            bestScores = mapOf(Difficulty.EASY to 120, Difficulty.MEDIUM to 85, Difficulty.HARD to null),
            stageProgress = mapOf(1 to StageProgress(1, 3, 40)),
            player = PlayerState(coins = 560, missionsCompleted = 7),
            unlockedAchievements = setOf(Achievement.POP_1000),
            onRename = {},
            onBack = {},
        )
    }
}
