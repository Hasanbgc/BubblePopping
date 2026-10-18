package com.apps.bubblepopping.meta

import com.apps.bubblepopping.data.PlayerStats
import com.apps.bubblepopping.data.StageProgress
import com.apps.bubblepopping.game.GameMode
import com.apps.bubblepopping.game.RunStats
import com.apps.bubblepopping.stage.StageCatalog
import com.apps.bubblepopping.stage.World
import com.apps.bubblepopping.view.home.Difficulty
import kotlin.random.Random

/** A finished round, as missions, coins and achievements see it. */
data class RunSummary(
    val mode: GameMode,
    val score: Int,
    /** Stage modes: stars earned (0 = failed). 0 otherwise. */
    val stars: Int = 0,
    /** Catalog stages: the stage's best stars before this round. */
    val previousStars: Int = 0,
    val stats: RunStats = RunStats(),
)

/** One line on the result screen: "Score  +12". */
data class RewardLine(val label: String, val coins: Int)

sealed interface ChestReward {
    data class Coins(val amount: Int) : ChestReward
    data class Item(val cosmetic: Cosmetic) : ChestReward
}

/** Everything a round earned. */
data class RunRewards(
    val lines: List<RewardLine> = emptyList(),
    val completedMissions: List<Mission> = emptyList(),
    val chests: List<ChestReward> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
) {
    val coins: Int get() = lines.sumOf { it.coins }
    val isEmpty: Boolean get() = lines.isEmpty() && chests.isEmpty() && achievements.isEmpty()
}

/**
 * Coin amounts and reward rules, aiming at about 150–250 coins per day of normal
 * play so the shop stays a long-term goal (`stage-plan.md` §7).
 */
object Economy {
    /** A stage's 1st, 2nd and 3rd star, each paid the first time it's earned. */
    val STAR_COINS = listOf(10, 10, 15)
    /** A boss stage pays for beating it, and again for its 3rd star. */
    val BOSS_STAR_COINS = listOf(100, 0, 50)
    /** Winning a stage again without a new star. */
    const val REPLAY_COINS = 2
    /** Endless and Daily Challenge: 1 coin per this many points… */
    const val SCORE_PER_COIN = 50
    /** …up to this many per round. */
    const val MAX_SCORE_COINS = 100
    /** Every this many total stars opens a star chest. */
    const val STARS_PER_CHEST = 15
    const val CHEST_COINS = 25
    /** Chance a chest holds a cosmetic the player doesn't own yet (instead of coins). */
    const val CHEST_ITEM_CHANCE = 0.2f

    /** Daily login rewards, day 1 → 7, then the week starts over. Missed days are never lost. */
    val LOGIN_REWARDS = listOf(5, 10, 15, 20, 25, 30, 75)

    /** Coins for the next login, given how many daily rewards were already claimed. */
    fun loginReward(claimedDays: Int): Int = LOGIN_REWARDS[claimedDays % LOGIN_REWARDS.size]

    /** Day 1–7 of the week the next claim falls on. */
    fun loginWeekDay(claimedDays: Int): Int = claimedDays % LOGIN_REWARDS.size + 1

    /** The coin lines a round earns before missions and chests. */
    fun runCoins(run: RunSummary): List<RewardLine> = buildList {
        when (run.mode) {
            is GameMode.Endless, is GameMode.DailyChallenge -> {
                val coins = (run.score / SCORE_PER_COIN).coerceAtMost(MAX_SCORE_COINS)
                if (coins > 0) add(RewardLine("Score ${run.score}", coins))
            }
            is GameMode.Stage -> {
                val isBoss = StageCatalog.stages.firstOrNull { it.id == run.mode.stageId }?.boss != null
                val perStar = if (isBoss) BOSS_STAR_COINS else STAR_COINS
                val newStars = (run.previousStars until run.stars)
                val coins = newStars.sumOf { perStar.getOrElse(it) { 0 } }
                when {
                    coins > 0 -> add(RewardLine("${newStars.count()} new ★", coins))
                    run.stars > 0 -> add(RewardLine("Stage cleared", REPLAY_COINS))
                }
            }
            is GameMode.DailyStage -> Unit // rewarded with a chest instead
        }
    }

    /** How many chests [totalStars] has earned in all. */
    fun chestsEarned(totalStars: Int): Int = totalStars / STARS_PER_CHEST

    /** Opens a chest: sometimes a cosmetic the player lacks, otherwise coins. */
    fun openChest(random: Random, owned: Set<String>): ChestReward {
        val prizes = Cosmetics.all.filter { it.inChests && it.id !in owned }
        return if (prizes.isNotEmpty() && random.nextFloat() < CHEST_ITEM_CHANCE) {
            ChestReward.Item(prizes[random.nextInt(prizes.size)])
        } else {
            ChestReward.Coins(CHEST_COINS)
        }
    }
}

/** What achievements are measured against. */
data class AchievementContext(
    val stats: PlayerStats,
    val stageProgress: Map<Int, StageProgress>,
    val endlessBests: Map<Difficulty, Int?>,
    val missionsCompleted: Int,
)

/** Long-term goals. Each pays [reward] coins once, the moment it's reached. */
enum class Achievement(val title: String, val description: String, val target: Int, val reward: Int) {
    POP_1000("Bubble Buster", "Pop 1,000 bubbles", 1_000, 50),
    POP_10000("Bubble Legend", "Pop 10,000 bubbles", 10_000, 250),
    ROUNDS_50("Regular", "Play 50 rounds", 50, 50),
    COMBO_25("Combo Master", "Reach a 25 combo", 25, 75),
    STARS_100("Star Collector", "Collect 100 stars", 100, 150),
    SKY_PERFECT("Sky High", "Get every star in the Sky world", StageCatalog.stages.filter { it.world == World.SKY }.sumOf { it.maxStars }, 100),
    ALL_BOSSES("Boss Slayer", "Beat every boss", StageCatalog.stages.count { it.boss != null }, 250),
    HARD_300("Hardcore", "Score 300 in Endless Hard", 300, 100),
    MISSIONS_25("Go-Getter", "Complete 25 missions", 25, 100);

    fun progress(context: AchievementContext): Int {
        val stars = context.stageProgress
        val value = when (this) {
            POP_1000, POP_10000 -> context.stats.bubblesPopped.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            ROUNDS_50 -> context.stats.gamesPlayed
            COMBO_25 -> context.stats.bestCombo
            STARS_100 -> stars.values.sumOf { it.stars }
            SKY_PERFECT -> StageCatalog.stages.filter { it.world == World.SKY }.sumOf { stars[it.id]?.stars ?: 0 }
            ALL_BOSSES -> StageCatalog.stages.count { it.boss != null && (stars[it.id]?.stars ?: 0) > 0 }
            HARD_300 -> context.endlessBests[Difficulty.HARD] ?: 0
            MISSIONS_25 -> context.missionsCompleted
        }
        return value.coerceAtMost(target)
    }

    fun isReached(context: AchievementContext): Boolean = progress(context) >= target
}
