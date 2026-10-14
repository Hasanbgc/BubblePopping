package com.apps.bubblepopping.meta

import com.apps.bubblepopping.data.PlayerStats
import com.apps.bubblepopping.data.StageProgress
import com.apps.bubblepopping.game.EndlessUnlocks
import com.apps.bubblepopping.game.GameMode
import com.apps.bubblepopping.view.home.Difficulty
import kotlin.random.Random

/** Coins, daily state and counters. */
data class PlayerState(
    val coins: Int = 0,
    val loginDaysClaimed: Int = 0,
    val lastLoginDay: Long = -1,
    val chestsOpened: Int = 0,
    val missionsCompleted: Int = 0,
    val dailyChallengeDay: Long = -1,
    val dailyChallengeBest: Int = 0,
    val dailyStageClearedDay: Long = -1,
) {
    fun canClaimLogin(today: Long): Boolean = lastLoginDay < today
    /** Today's Daily Challenge best, or null if not played today. */
    fun dailyChallengeBest(today: Long): Int? = dailyChallengeBest.takeIf { dailyChallengeDay == today }
    fun dailyStageCleared(today: Long): Boolean = dailyStageClearedDay == today
}

/** All meta progression, as stored. */
data class MetaState(
    val player: PlayerState = PlayerState(),
    val missions: List<Mission> = emptyList(),
    /** Cosmetics bought or won from chests (free and boss items are derived, not stored). */
    val purchased: Set<String> = emptySet(),
    val achievements: Set<Achievement> = emptySet(),
)

/** Game progress the rewards depend on, read after the round was saved. */
data class ProgressSnapshot(
    val stageProgress: Map<Int, StageProgress> = emptyMap(),
    val stats: PlayerStats = PlayerStats(),
    val endlessBests: Map<Difficulty, Int?> = emptyMap(),
)

/** The rules of meta progression, as pure functions over [MetaState]. */
object MetaRules {

    fun missionContext(snapshot: ProgressSnapshot) = MissionContext(
        unlockedTypes = EndlessUnlocks.unlockedTypes(snapshot.stageProgress),
        hardUnlocked = EndlessUnlocks.isUnlocked(
            Difficulty.HARD,
            snapshot.endlessBests[Difficulty.MEDIUM],
            snapshot.stageProgress,
        ),
    )

    /** Tops the missions back up to three. */
    fun withMissions(state: MetaState, snapshot: ProgressSnapshot, random: Random): MetaState =
        if (state.missions.size >= MissionCatalog.ACTIVE_COUNT) state
        else state.copy(missions = MissionCatalog.fill(random, missionContext(snapshot), state.missions))

    /**
     * Pays out a finished round: score / new-star coins, daily results,
     * missions (completed ones are paid and replaced), star chests and achievements.
     */
    fun applyRun(
        state: MetaState,
        run: RunSummary,
        snapshot: ProgressSnapshot,
        random: Random,
    ): Pair<MetaState, RunRewards> {
        var player = state.player
        val lines = Economy.runCoins(run).toMutableList()
        val chests = mutableListOf<ChestReward>()
        val purchased = state.purchased.toMutableSet()

        fun openChest(label: String) {
            val reward = Economy.openChest(random, Cosmetics.owned(purchased, snapshot.stageProgress))
            chests += reward
            when (reward) {
                is ChestReward.Coins -> lines += RewardLine(label, reward.amount)
                is ChestReward.Item -> purchased += reward.cosmetic.id
            }
        }

        // Daily modes
        when (val mode = run.mode) {
            is GameMode.DailyChallenge -> player = player.copy(
                dailyChallengeDay = mode.epochDay,
                dailyChallengeBest = if (player.dailyChallengeDay == mode.epochDay) {
                    maxOf(player.dailyChallengeBest, run.score)
                } else {
                    run.score
                },
            )
            is GameMode.DailyStage -> if (run.stars > 0 && player.dailyStageClearedDay != mode.epochDay) {
                player = player.copy(dailyStageClearedDay = mode.epochDay)
                openChest("Daily Stage chest")
            }
            is GameMode.Endless, is GameMode.Stage -> Unit
        }

        // Missions: advance, pay and replace the completed ones.
        val context = missionContext(snapshot)
        val completed = mutableListOf<Mission>()
        val missions = MissionCatalog.fill(random, context, state.missions)
            .map { it.advancedBy(run) }
            .toMutableList()
        for (i in missions.indices) {
            val mission = missions[i]
            if (!mission.isComplete) continue
            completed += mission
            lines += RewardLine("Mission: ${mission.description}", mission.reward)
            val otherKinds = missions.filterIndexed { j, _ -> j != i }.mapTo(mutableSetOf()) { it.kind }
            missions[i] = MissionCatalog.generate(random, context, otherKinds + mission.kind)
        }
        player = player.copy(missionsCompleted = player.missionsCompleted + completed.size)

        // Star chests: one per 10 total stars.
        val totalStars = snapshot.stageProgress.values.sumOf { it.stars }
        while (player.chestsOpened < Economy.chestsEarned(totalStars)) {
            player = player.copy(chestsOpened = player.chestsOpened + 1)
            openChest("Star chest")
        }

        // Achievements
        val achievementContext = AchievementContext(
            stats = snapshot.stats,
            stageProgress = snapshot.stageProgress,
            endlessBests = snapshot.endlessBests,
            missionsCompleted = player.missionsCompleted,
        )
        val newAchievements = Achievement.entries.filter {
            it !in state.achievements && it.isReached(achievementContext)
        }
        newAchievements.forEach { lines += RewardLine("Achievement: ${it.title}", it.reward) }

        val rewards = RunRewards(lines, completed, chests, newAchievements)
        player = player.copy(coins = player.coins + rewards.coins)
        val newState = MetaState(
            player = player,
            missions = missions,
            purchased = purchased,
            achievements = state.achievements + newAchievements,
        )
        return newState to rewards
    }

    /** Claims today's login reward; null if it was already claimed today. */
    fun claimLogin(state: MetaState, today: Long): Pair<MetaState, Int>? {
        val player = state.player
        if (!player.canClaimLogin(today)) return null
        val coins = Economy.loginReward(player.loginDaysClaimed)
        val claimed = player.copy(
            coins = player.coins + coins,
            loginDaysClaimed = player.loginDaysClaimed + 1,
            lastLoginDay = today,
        )
        return state.copy(player = claimed) to coins
    }

    /** Buys [cosmetic]; null if it isn't for sale, is already owned, or costs too much. */
    fun buy(state: MetaState, cosmetic: Cosmetic, progress: Map<Int, StageProgress>): MetaState? {
        val price = cosmetic.price ?: return null
        if (cosmetic.id in Cosmetics.owned(state.purchased, progress)) return null
        if (state.player.coins < price) return null
        return state.copy(
            player = state.player.copy(coins = state.player.coins - price),
            purchased = state.purchased + cosmetic.id,
        )
    }
}
