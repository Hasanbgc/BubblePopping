package com.apps.bubblepopping.meta

import com.apps.bubblepopping.stage.Objective
import com.apps.bubblepopping.stage.StageCatalog
import com.apps.bubblepopping.stage.StageConfig
import com.apps.bubblepopping.stage.World
import com.apps.bubblepopping.view.play.component.BubbleType
import kotlin.random.Random

/**
 * Builds the Daily Stage: a one-minute stage with a random goal, themed on a
 * world. The same day and world always give the same stage (and bubbles).
 */
object DailyStageGenerator {

    /** Daily stages aren't in the catalog; this id marks them. */
    const val DAILY_STAGE_ID = 0
    const val DURATION_SEC = 60

    fun generate(epochDay: Long, world: World): StageConfig {
        val seed = epochDay * SEED_DAY_FACTOR + world.ordinal
        val random = Random(seed)

        // Borrow the bubble mix and speed of one of the world's regular stages.
        val pool = StageCatalog.stages.filter { it.world == world && it.boss == null }
        val spawn = pool[random.nextInt(pool.size)].spawn

        // Special bubbles this mix actually spawns (hearts count too).
        val specials = spawn.typeWeights
            .filter { (type, weight) -> weight > 0 && type != BubbleType.NORMAL && type != BubbleType.POISON }
            .keys.toList()

        val goal: Objective = when (random.nextInt(GOAL_KINDS)) {
            0 -> Objective.ReachScore(random.nextInt(100, 181).roundTo(10))
            1 -> Objective.PopCount(random.nextInt(30, 51).roundTo(5))
            2 -> Objective.ReachCombo(random.nextInt(8, 13))
            3 -> Objective.Survive(random.nextInt(40, 61).roundTo(5))
            else -> specials.takeIf { it.isNotEmpty() }
                ?.let { Objective.PopType(it[random.nextInt(it.size)], random.nextInt(3, 7)) }
                ?: Objective.PopCount(40)
        }
        val bonusScore = when (goal) {
            is Objective.ReachScore -> (goal.target * 3 / 2).roundTo(10)
            else -> random.nextInt(120, 201).roundTo(10)
        }

        return StageConfig(
            id = DAILY_STAGE_ID,
            world = world,
            goal = goal,
            bonusGoals = listOf(Objective.ReachScore(bonusScore), Objective.NoLivesLost),
            spawn = spawn,
            introText = "Today's ${world.displayName} challenge. Clear it for a reward chest!",
            durationSec = (goal as? Objective.Survive)?.seconds ?: DURATION_SEC,
            seed = seed,
        )
    }

    private fun Int.roundTo(step: Int): Int = this / step * step

    private const val GOAL_KINDS = 5
    private const val SEED_DAY_FACTOR = 31L
}
