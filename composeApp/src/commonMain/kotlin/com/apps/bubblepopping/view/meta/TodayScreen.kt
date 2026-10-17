package com.apps.bubblepopping.view.meta

import com.apps.bubblepopping.ui.icons.playIcon
import androidx.compose.foundation.layout.padding
import com.apps.bubblepopping.view.stage.ObjectiveRow
import com.apps.bubblepopping.view.stage.goalIcon
import com.apps.bubblepopping.ui.components.goldInset
import com.apps.bubblepopping.ui.components.formatCount
import com.apps.bubblepopping.ui.components.ResourcePill
import com.apps.bubblepopping.ui.components.PanelDivider
import com.apps.bubblepopping.ui.components.OutlinedText
import com.apps.bubblepopping.ui.components.EnamelTone
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
import com.apps.bubblepopping.ui.components.CoinCounter
import com.apps.bubblepopping.ui.components.GameButton
import com.apps.bubblepopping.ui.components.GameProgressBar
import com.apps.bubblepopping.ui.components.GameScreen
import com.apps.bubblepopping.ui.components.EnamelPanel
import com.apps.bubblepopping.ui.components.PanelSectionTitle
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
    // Today's day of the week: the one to claim, or the one just claimed.
    val day = if (canClaim || player.loginDaysClaimed == 0) Economy.loginWeekDay(player.loginDaysClaimed)
    else (player.loginDaysClaimed - 1) % Economy.LOGIN_REWARDS.size + 1
    EnamelPanel(Modifier.fillMaxWidth()) {
        PanelSectionTitle(
            "Daily reward",
            trailing = { OutlinedText("Day $day/7", GameTheme.type.label, color = GameColors.Gold) },
        )
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
            Hint("Come back tomorrow! Missed days are never lost.", Modifier.fillMaxWidth())
        }
    }
}

/** Today's events stand out from the rest of the screen on the light enamel. */
@Composable
private fun DailyChallengePanel(today: Long, player: PlayerState, onPlay: (GameMode) -> Unit) {
    val best = player.dailyChallengeBest(today)
    EnamelPanel(Modifier.fillMaxWidth(), tone = EnamelTone.LIGHT) {
        PanelSectionTitle(
            "Daily Challenge",
            trailing = { if (best != null) ResourcePill(GameIcons.Star, "Best ${formatCount(best)}", tint = GameColors.PillStar) },
        )
        Body("One Endless round with the same bubbles for every player today. How far can you get?")
        if (best == null) Hint("Not played yet today")
        GameButton(
            text = "PLAY",
            onClick = { onPlay(GameMode.DailyChallenge(today)) },
            // Same green PLAY as the Home mode cards.
            color = ButtonColor.GREEN,
            icon = playIcon(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DailyStagePanel(today: Long, player: PlayerState, world: World, onPlay: (GameMode) -> Unit) {
    val stage = remember(today, world) { DailyStageGenerator.generate(today, world) }
    val cleared = player.dailyStageCleared(today)
    EnamelPanel(Modifier.fillMaxWidth(), tone = EnamelTone.LIGHT) {
        PanelSectionTitle(
            "Daily Stage",
            trailing = { ResourcePill(GameIcons.Target, world.displayName, tint = GameColors.OceanTint) },
        )
        // The goal and bonus stars in a gold-framed box, like the stage card.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .goldInset()
                .padding(horizontal = Spacing.m, vertical = Spacing.xs),
        ) {
            ObjectiveRow(description = stage.goal.description, progress = null, isBonus = false, goalIcon = goalIcon(stage.goal))
            stage.bonusGoals.forEach { ObjectiveRow(description = it.description, progress = null, isBonus = true) }
        }
        if (cleared) {
            GoalLine(GameIcons.Check, "Chest won today — play again for fun", emphasised = false)
        } else {
            GoalLine(GameIcons.Chest, "Clear it to win a reward chest", emphasised = true)
        }
        GameButton(
            text = "PLAY",
            onClick = { onPlay(GameMode.DailyStage(today, world)) },
            color = ButtonColor.GREEN,
            icon = playIcon(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MissionsPanel(missions: List<Mission>) {
    EnamelPanel(Modifier.fillMaxWidth()) {
        PanelSectionTitle(
            "Missions",
            trailing = {
                if (missions.isNotEmpty()) {
                    OutlinedText("${missions.count { it.isComplete }}/${missions.size}", GameTheme.type.label, color = GameColors.Gold)
                }
            },
        )
        if (missions.isEmpty()) Hint("Loading…")
        missions.forEachIndexed { index, mission ->
            if (index > 0) PanelDivider()
            MissionRow(mission)
        }
        Hint("Completed missions pay out at the end of a round and are replaced by new ones.")
    }
}

/** A mission: its task and coin reward, a progress bar, and the count. Done missions get a tick and a green bar. */
@Composable
private fun MissionRow(mission: Mission) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            if (mission.isComplete) Image(GameIcons.Check, contentDescription = "Done", modifier = Modifier.size(MISSION_ICON))
            Text(mission.description, style = GameTheme.type.label, color = GameColors.TextPrimary, modifier = Modifier.weight(1f))
            ResourcePill(GameIcons.Coin, "+${mission.reward}", tint = GameColors.PillCoin)
        }
        GameProgressBar(mission.fraction, color = if (mission.isComplete) GameColors.Success else GameColors.Gold)
        Text(
            text = "${mission.progress} / ${mission.target}" + if (mission.kind.cumulative) "" else " · in one round",
            style = GameTheme.type.caption,
            color = GameColors.TextMuted,
        )
    }
}

private val MISSION_ICON = 22.dp

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
