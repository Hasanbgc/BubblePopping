package com.apps.bubblepopping.view.profile

import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import com.apps.bubblepopping.ui.components.sunkenWell
import com.apps.bubblepopping.ui.components.enamelPill
import com.apps.bubblepopping.ui.components.EnamelTone
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
import com.apps.bubblepopping.ui.components.DialogButtons
import com.apps.bubblepopping.ui.components.GameButton
import com.apps.bubblepopping.ui.components.GameDialog
import com.apps.bubblepopping.ui.components.GameIconButton
import com.apps.bubblepopping.ui.components.GameProgressBar
import com.apps.bubblepopping.ui.components.GameScreen
import com.apps.bubblepopping.ui.components.GameTextField
import com.apps.bubblepopping.ui.components.EnamelPanel
import com.apps.bubblepopping.ui.components.PanelDivider
import com.apps.bubblepopping.ui.components.PanelSectionTitle
import com.apps.bubblepopping.ui.components.OutlinedText
import com.apps.bubblepopping.ui.components.ResourcePill
import com.apps.bubblepopping.ui.components.formatCount
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.BubbleTheme
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

        // Stats, best scores and achievements share one panel, split by dividers.
        EnamelPanel(Modifier.fillMaxWidth()) {
            PanelSectionTitle("Stats")
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                StatTile(GameIcons.Play, formatCount(stats.gamesPlayed), "Rounds played", Modifier.weight(1f))
                StatTile(GameIcons.Bubble, formatCount(stats.bubblesPopped.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()), "Bubbles popped", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                StatTile(GameIcons.Star, "${stats.bestCombo}", "Best combo", Modifier.weight(1f))
                StatTile(GameIcons.Check, "${player.missionsCompleted}", "Missions done", Modifier.weight(1f))
            }

            PanelDivider()
            PanelSectionTitle("Best scores")
            Difficulty.entries.forEach { difficulty -> BestScoreRow(difficulty, bestScores[difficulty]) }

            PanelDivider()
            PanelSectionTitle(
                "Achievements",
                trailing = {
                    Text(
                        "${unlockedAchievements.size} / ${Achievement.entries.size}",
                        style = GameTheme.type.label,
                        color = GameColors.TextSecondary,
                    )
                },
            )
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

/**
 * The hero card: the avatar in a gold ring, the name with an edit button, and
 * the coin / star counters, on the light enamel so it leads the screen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayerHeader(name: String, coins: Int, stars: Int, onEditName: () -> Unit) {
    EnamelPanel(Modifier.fillMaxWidth(), tone = EnamelTone.LIGHT) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.l)) {
            // Replaced by the Google / Facebook photo once sign-in exists.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(AVATAR_SIZE)
                    .drawBehind {
                        val outline = 2.dp.toPx()
                        drawCircle(GameColors.ButtonOutline, radius = size.minDimension / 2f + outline / 2f, style = Stroke(outline))
                    }
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(GameColors.PodiumAvatarTop, GameColors.PodiumAvatarBottom)))
                    .border(AVATAR_RING, Brush.verticalGradient(listOf(GameColors.GoldTop, GameColors.GoldLip)), CircleShape),
            ) {
                Image(GameIcons.Person, contentDescription = "Profile picture", modifier = Modifier.size(AVATAR_SIZE * 0.66f))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    OutlinedText(name, GameTheme.type.title, outlineWidth = 3.dp, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                    GameIconButton(GameIcons.Pencil, contentDescription = "Edit name", onClick = onEditName, color = ButtonColor.VIOLET, size = EDIT_BUTTON)
                }
                // Wraps onto two lines on narrow phones.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    ResourcePill(GameIcons.Coin, formatCount(coins), tint = GameColors.PillCoin)
                    ResourcePill(GameIcons.Star, "$stars/${StageCatalog.maxStars}", tint = GameColors.PillStar)
                }
            }
        }
    }
}

/** One number with an icon and a label, in a well sunk into the Stats section. */
@Composable
private fun StatTile(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = modifier
            .sunkenWell()
            .padding(vertical = Spacing.m, horizontal = Spacing.s),
    ) {
        Image(icon, contentDescription = null, modifier = Modifier.size(STAT_ICON))
        OutlinedText(value, GameTheme.type.headline, color = GameColors.Gold, maxLines = 1)
        Hint(label)
    }
}

/** A difficulty on its own tinted pill, and its best score on the right. */
@Composable
private fun BestScoreRow(difficulty: Difficulty, best: Int?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(DIFFICULTY_PILL_HEIGHT)
                .enamelPill(difficulty.glowColor)
                .padding(horizontal = Spacing.m),
        ) {
            OutlinedText(difficulty.label, GameTheme.type.label, maxLines = 1)
        }
        Spacer(Modifier.weight(1f))
        if (best != null) {
            OutlinedText(formatCount(best), GameTheme.type.headline, color = GameColors.Gold, maxLines = 1)
        } else {
            Text("—", style = GameTheme.type.headline, color = GameColors.TextMuted)
        }
    }
}

/**
 * An achievement in a sunken row: a trophy (reached, washed green, with a tick)
 * or a lock with progress and the coin reward on a pill.
 */
@Composable
private fun AchievementRow(achievement: Achievement, progress: Int, unlocked: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        modifier = Modifier
            .fillMaxWidth()
            .sunkenWell(tint = if (unlocked) GameColors.Success else null)
            .padding(Spacing.m),
    ) {
        Image(
            imageVector = if (unlocked) GameIcons.Trophy else GameIcons.Lock,
            contentDescription = null,
            modifier = Modifier.size(ACHIEVEMENT_ICON).alpha(if (unlocked) 1f else LOCKED_ALPHA),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(achievement.title, style = GameTheme.type.label, color = GameColors.TextPrimary)
            Hint(achievement.description)
            if (!unlocked) {
                GameProgressBar(progress.toFloat() / achievement.target, height = 10.dp)
                Text("$progress / ${achievement.target}", style = GameTheme.type.caption, color = GameColors.TextMuted)
            }
        }
        if (unlocked) {
            Image(GameIcons.Check, contentDescription = "Reached", modifier = Modifier.size(CHECK_ICON))
        } else {
            ResourcePill(GameIcons.Coin, "+${achievement.reward}", tint = GameColors.PillCoin)
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
private val AVATAR_RING = 4.dp
private val EDIT_BUTTON = 34.dp
private val STAT_ICON = 30.dp
private val DIFFICULTY_PILL_HEIGHT = 32.dp
private val ACHIEVEMENT_ICON = 36.dp
private val CHECK_ICON = 28.dp
private const val LOCKED_ALPHA = 0.6f

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
