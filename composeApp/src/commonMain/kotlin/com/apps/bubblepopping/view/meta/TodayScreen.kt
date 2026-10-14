package com.apps.bubblepopping.view.meta

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.bubblepopping.data.MetaRepository
import com.apps.bubblepopping.data.ProgressRepository
import com.apps.bubblepopping.data.StageProgress
import com.apps.bubblepopping.game.GameMode
import com.apps.bubblepopping.meta.DailyStageGenerator
import com.apps.bubblepopping.meta.Economy
import com.apps.bubblepopping.meta.Mission
import com.apps.bubblepopping.meta.MissionKind
import com.apps.bubblepopping.meta.PlayerState
import com.apps.bubblepopping.meta.currentEpochDay
import com.apps.bubblepopping.stage.StageCatalog
import com.apps.bubblepopping.stage.World
import com.apps.bubblepopping.ui.components.ButtonColor
import com.apps.bubblepopping.ui.components.CoinAmount
import com.apps.bubblepopping.ui.components.CoinCounter
import com.apps.bubblepopping.ui.components.GameButton
import com.apps.bubblepopping.ui.components.GameProgressBar
import com.apps.bubblepopping.ui.components.GameScreen
import com.apps.bubblepopping.ui.components.GlassPanel
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.BubbleTheme
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing
import kotlinx.coroutines.launch

@Composable
fun TodayScreenRoot(
    metaRepository: MetaRepository,
    progressRepository: ProgressRepository,
    onPlay: (GameMode) -> Unit,
    onBack: () -> Unit,
) {
    val today = remember { currentEpochDay() }
    val scope = rememberCoroutineScope()
    val player by remember(metaRepository) { metaRepository.player() }.collectAsStateWithLifecycle(PlayerState())
    val missions by remember(metaRepository) { metaRepository.missions() }.collectAsStateWithLifecycle(emptyList())
    val progress by remember(progressRepository) { progressRepository.stageProgress() }
        .collectAsStateWithLifecycle(emptyMap())

    LaunchedEffect(metaRepository) { metaRepository.ensureMissions() }

    TodayScreen(
        today = today,
        player = player,
        missions = missions,
        dailyWorld = dailyWorld(progress),
        onClaimLogin = { scope.launch { metaRepository.claimDailyLogin(today) } },
        onPlay = onPlay,
        onBack = onBack,
    )
}

/** The Daily Stage is themed on the player's newest open world. */
fun dailyWorld(progress: Map<Int, StageProgress>): World =
    StageCatalog.worlds.last { it == World.SKY || StageCatalog.isWorldUnlocked(it, progress) }

/** Daily login reward, Daily Challenge, Daily Stage and the three missions. */
@Composable
fun TodayScreen(
    today: Long,
    player: PlayerState,
    missions: List<Mission>,
    dailyWorld: World,
    onClaimLogin: () -> Unit,
    onPlay: (GameMode) -> Unit,
    onBack: () -> Unit,
) {
    GameScreen(title = "Today", onBack = onBack, actions = { CoinCounter() }) {
        LoginPanel(today, player, onClaimLogin)
        DailyChallengePanel(today, player, onPlay)
        DailyStagePanel(today, player, dailyWorld, onPlay)
        MissionsPanel(missions)
    }
}

@Composable
private fun LoginPanel(today: Long, player: PlayerState, onClaim: () -> Unit) {
    val canClaim = player.canClaimLogin(today)
    GlassPanel(Modifier.fillMaxWidth(), title = "Daily reward", icon = GameIcons.Calendar) {
        LoginWeek(claimedDays = player.loginDaysClaimed, canClaim = canClaim)
        if (canClaim) {
            GameButton(
                text = "Claim +${Economy.loginReward(player.loginDaysClaimed)}",
                onClick = onClaim,
                color = ButtonColor.GREEN,
                icon = GameIcons.Coin,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Hint("Come back tomorrow! Missed days are never lost.")
        }
    }
}

@Composable
private fun DailyChallengePanel(today: Long, player: PlayerState, onPlay: (GameMode) -> Unit) {
    val best = player.dailyChallengeBest(today)
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        title = "Daily Challenge",
        icon = GameIcons.Bubble,
        trailing = { if (best != null) BestScore(best) },
    ) {
        Body("One Endless round with the same bubbles for every player today. How far can you get?")
        if (best == null) Hint("Not played yet today")
        GameButton(
            text = "Play",
            onClick = { onPlay(GameMode.DailyChallenge(today)) },
            color = ButtonColor.VIOLET,
            icon = GameIcons.Play,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DailyStagePanel(today: Long, player: PlayerState, world: World, onPlay: (GameMode) -> Unit) {
    val stage = remember(today, world) { DailyStageGenerator.generate(today, world) }
    val cleared = player.dailyStageCleared(today)
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        title = "Daily Stage",
        icon = GameIcons.Target,
        trailing = { Text(world.displayName, style = GameTheme.type.label, color = GameColors.TextSecondary) },
    ) {
        GoalLine(GameIcons.Target, stage.goal.description, emphasised = true)
        stage.bonusGoals.forEach { GoalLine(GameIcons.Star, it.description, emphasised = false) }
        if (cleared) {
            GoalLine(GameIcons.Check, "Chest won today — play again for fun", emphasised = false)
        } else {
            GoalLine(GameIcons.Chest, "Clear it to win a reward chest", emphasised = true)
        }
        GameButton(
            text = "Play",
            onClick = { onPlay(GameMode.DailyStage(today, world)) },
            icon = GameIcons.Play,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MissionsPanel(missions: List<Mission>) {
    GlassPanel(Modifier.fillMaxWidth(), title = "Missions", icon = GameIcons.Trophy) {
        if (missions.isEmpty()) Hint("Loading…")
        missions.forEach { mission -> MissionRow(mission) }
        Hint("Completed missions pay out at the end of a round and are replaced by new ones.")
    }
}

@Composable
private fun MissionRow(mission: Mission) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Text(mission.description, style = GameTheme.type.label, color = GameColors.TextPrimary, modifier = Modifier.weight(1f))
            CoinAmount("+${mission.reward}")
        }
        GameProgressBar(mission.fraction, color = if (mission.isComplete) GameColors.Success else GameColors.Gold)
        Text(
            text = "${mission.progress} / ${mission.target}" + if (mission.kind.cumulative) "" else " · in one round",
            style = GameTheme.type.caption,
            color = GameColors.TextMuted,
        )
    }
}

// ── Shared bits for menu screens ────────────────────────────────────────────

/** Normal paragraph text on a panel. */
@Composable
fun Body(text: String, modifier: Modifier = Modifier) {
    Text(text, style = GameTheme.type.body, color = GameColors.TextPrimary, modifier = modifier)
}

/** Small, quieter text: hints and explanations. */
@Composable
fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(text, style = GameTheme.type.caption, color = GameColors.TextSecondary, modifier = modifier)
}

/** An icon and a line of text — goals, perks, notes. */
@Composable
fun GoalLine(icon: ImageVector, text: String, emphasised: Boolean, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Image(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        Text(
            text = text,
            style = if (emphasised) GameTheme.type.label else GameTheme.type.body,
            color = if (emphasised) GameColors.TextPrimary else GameColors.TextSecondary,
        )
    }
}

/** "★ 152" — a best score in a panel title row. */
@Composable
fun BestScore(score: Int, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Image(GameIcons.Star, contentDescription = null, modifier = Modifier.size(20.dp))
        Text("Best $score", style = GameTheme.type.label, color = GameColors.Gold)
    }
}

@Preview
@Composable
fun TodayScreenPreview() {
    BubbleTheme {
        TodayScreen(
            today = 20_000,
            player = PlayerState(coins = 340, loginDaysClaimed = 3, lastLoginDay = 19_999, dailyChallengeDay = 20_000, dailyChallengeBest = 152),
            missions = listOf(
                Mission(MissionKind.POP_BUBBLES, target = 300, reward = 50, progress = 120),
                Mission(MissionKind.REACH_COMBO, target = 10, reward = 40, progress = 7),
                Mission(MissionKind.DAILY_CHALLENGE, target = 1, reward = 30),
            ),
            dailyWorld = World.OCEAN,
            onClaimLogin = {},
            onPlay = {},
            onBack = {},
        )
    }
}
