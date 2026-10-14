package com.apps.bubblepopping.meta

import com.apps.bubblepopping.game.GameMode
import com.apps.bubblepopping.view.home.Difficulty
import com.apps.bubblepopping.view.play.component.BubbleType
import com.apps.bubblepopping.stage.displayName
import kotlin.random.Random

/**
 * Mission types. [cumulative] missions add up across rounds ("Pop 500 bubbles");
 * the others must happen within one round ("Reach a 10 combo").
 */
enum class MissionKind(val cumulative: Boolean) {
    POP_BUBBLES(cumulative = true),
    POP_TYPE(cumulative = false),
    REACH_COMBO(cumulative = false),
    ENDLESS_SCORE(cumulative = false),
    ENDLESS_SURVIVE(cumulative = false),
    REACH_WAVE(cumulative = false),
    STAGE_STARS(cumulative = false),
    CLEAR_STAGES(cumulative = true),
    WIND_SAVES(cumulative = true),
    BOMB_CHAIN(cumulative = false),
    DAILY_CHALLENGE(cumulative = true),
    PLAY_ROUNDS(cumulative = true),
}

/** One active mission. [bubbleType] / [difficulty] narrow some kinds. */
data class Mission(
    val kind: MissionKind,
    val target: Int,
    val reward: Int,
    val progress: Int = 0,
    val bubbleType: BubbleType? = null,
    val difficulty: Difficulty? = null,
) {
    val isComplete: Boolean get() = progress >= target
    val fraction: Float get() = (progress.toFloat() / target).coerceIn(0f, 1f)

    val description: String get() = when (kind) {
        MissionKind.POP_BUBBLES -> "Pop $target bubbles"
        MissionKind.POP_TYPE -> "Pop $target ${bubbleType?.displayName(target) ?: "bubbles"} in one round"
        MissionKind.REACH_COMBO -> "Reach a $target combo"
        MissionKind.ENDLESS_SCORE -> "Score $target in one Endless round"
        MissionKind.ENDLESS_SURVIVE -> "Survive $target s in Endless ${difficulty?.label ?: ""}".trimEnd()
        MissionKind.REACH_WAVE -> "Reach wave $target in Endless"
        MissionKind.STAGE_STARS -> "Earn $target★ on any stage"
        MissionKind.CLEAR_STAGES -> "Clear $target stages"
        MissionKind.WIND_SAVES -> "Blow $target skulls away with the wind"
        MissionKind.BOMB_CHAIN -> "Pop $target bubbles with one bomb"
        MissionKind.DAILY_CHALLENGE -> "Play the Daily Challenge"
        MissionKind.PLAY_ROUNDS -> "Play $target rounds"
    }

    /** This mission after [run]: cumulative kinds add, the others keep their best round. */
    fun advancedBy(run: RunSummary): Mission {
        val value = valueIn(run)
        val next = if (kind.cumulative) progress + value else maxOf(progress, value)
        return copy(progress = next.coerceAtMost(target))
    }

    /** How much [run] counts towards this mission. */
    fun valueIn(run: RunSummary): Int {
        val stats = run.stats
        val endlessDifficulty = when (val mode = run.mode) {
            is GameMode.Endless -> mode.difficulty
            is GameMode.DailyChallenge -> Difficulty.MEDIUM
            else -> null
        }
        val isStage = run.mode is GameMode.Stage || run.mode is GameMode.DailyStage
        return when (kind) {
            MissionKind.POP_BUBBLES -> stats.pops
            MissionKind.POP_TYPE -> bubbleType?.let { stats.popsByType[it] } ?: 0
            MissionKind.REACH_COMBO -> stats.bestCombo
            MissionKind.ENDLESS_SCORE -> if (endlessDifficulty != null) run.score else 0
            MissionKind.ENDLESS_SURVIVE ->
                if (endlessDifficulty != null && (difficulty == null || difficulty == endlessDifficulty)) stats.elapsedSec else 0
            MissionKind.REACH_WAVE -> if (endlessDifficulty != null) stats.wavesCleared + 1 else 0
            MissionKind.STAGE_STARS -> if (isStage) run.stars else 0
            MissionKind.CLEAR_STAGES -> if (isStage && run.stars > 0) 1 else 0
            MissionKind.WIND_SAVES -> stats.windSaves
            MissionKind.BOMB_CHAIN -> stats.bestBombChain
            MissionKind.DAILY_CHALLENGE -> if (run.mode is GameMode.DailyChallenge) 1 else 0
            MissionKind.PLAY_ROUNDS -> 1
        }
    }
}

/** What the player has access to — missions only ask for things they can do. */
data class MissionContext(
    val unlockedTypes: Set<BubbleType> = emptySet(),
    val hardUnlocked: Boolean = false,
)

/** Creates new missions. Always [ACTIVE_COUNT] are active, each of a different kind. */
object MissionCatalog {
    const val ACTIVE_COUNT = 3

    /** A new mission of a kind not in [activeKinds]. */
    fun generate(random: Random, context: MissionContext, activeKinds: Set<MissionKind>): Mission {
        val candidates = templates(context).filterKeys { it !in activeKinds }
        val kinds = candidates.keys.toList()
        val options = candidates.getValue(kinds[random.nextInt(kinds.size)])
        return options[random.nextInt(options.size)]
    }

    /** Fills up to [ACTIVE_COUNT] missions, keeping [current]. */
    fun fill(random: Random, context: MissionContext, current: List<Mission>): List<Mission> {
        val missions = current.toMutableList()
        while (missions.size < ACTIVE_COUNT) {
            missions += generate(random, context, missions.mapTo(mutableSetOf()) { it.kind })
        }
        return missions
    }

    /** Every possible mission, grouped by kind. */
    fun templates(context: MissionContext): Map<MissionKind, List<Mission>> = buildMap {
        put(MissionKind.POP_BUBBLES, tiers(MissionKind.POP_BUBBLES, 150 to 30, 300 to 50, 500 to 80))
        put(
            MissionKind.POP_TYPE,
            buildList {
                add(Mission(MissionKind.POP_TYPE, 3, 30, bubbleType = BubbleType.HEART))
                for ((type, target, reward) in TYPE_MISSIONS) {
                    if (type in context.unlockedTypes) add(Mission(MissionKind.POP_TYPE, target, reward, bubbleType = type))
                }
            },
        )
        put(MissionKind.REACH_COMBO, tiers(MissionKind.REACH_COMBO, 8 to 30, 10 to 40, 15 to 60))
        put(MissionKind.ENDLESS_SCORE, tiers(MissionKind.ENDLESS_SCORE, 80 to 30, 150 to 50, 250 to 80))
        put(
            MissionKind.ENDLESS_SURVIVE,
            buildList {
                add(Mission(MissionKind.ENDLESS_SURVIVE, 60, 40, difficulty = Difficulty.MEDIUM))
                if (context.hardUnlocked) add(Mission(MissionKind.ENDLESS_SURVIVE, 90, 70, difficulty = Difficulty.HARD))
            },
        )
        put(MissionKind.REACH_WAVE, tiers(MissionKind.REACH_WAVE, 3 to 30, 4 to 45, 5 to 60))
        put(MissionKind.STAGE_STARS, tiers(MissionKind.STAGE_STARS, 3 to 50))
        put(MissionKind.CLEAR_STAGES, tiers(MissionKind.CLEAR_STAGES, 3 to 40, 5 to 60))
        put(MissionKind.WIND_SAVES, tiers(MissionKind.WIND_SAVES, 5 to 40))
        if (BubbleType.BOMB in context.unlockedTypes) {
            put(MissionKind.BOMB_CHAIN, tiers(MissionKind.BOMB_CHAIN, 5 to 50))
        }
        put(MissionKind.DAILY_CHALLENGE, tiers(MissionKind.DAILY_CHALLENGE, 1 to 30))
        put(MissionKind.PLAY_ROUNDS, tiers(MissionKind.PLAY_ROUNDS, 5 to 25))
    }

    private fun tiers(kind: MissionKind, vararg targetAndReward: Pair<Int, Int>) =
        targetAndReward.map { (target, reward) -> Mission(kind, target, reward) }

    private val TYPE_MISSIONS = listOf(
        Triple(BubbleType.GOLDEN, 3, 40),
        Triple(BubbleType.FREEZE, 2, 30),
        Triple(BubbleType.BOMB, 2, 30),
        Triple(BubbleType.SPLITTER, 3, 30),
        Triple(BubbleType.SHIELD, 2, 30),
    )
}
