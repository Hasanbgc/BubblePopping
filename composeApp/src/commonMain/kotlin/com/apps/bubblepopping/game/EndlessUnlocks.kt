package com.apps.bubblepopping.game

import com.apps.bubblepopping.data.StageProgress
import com.apps.bubblepopping.stage.StageCatalog
import com.apps.bubblepopping.stage.World
import com.apps.bubblepopping.view.home.Difficulty
import com.apps.bubblepopping.view.play.component.BubbleType

/** What Endless mode has unlocked: difficulties, and bubble types first met in Stages. */
object EndlessUnlocks {

    /** Scoring this on Medium unlocks Hard… */
    const val HARD_UNLOCK_MEDIUM_SCORE = 150

    /** …and so does clearing the last Sky stage (the Cloud King). */
    val HARD_UNLOCK_STAGE_ID: Int = StageCatalog.stages.last { it.world == World.SKY }.id

    /**
     * Endless spawn weights for the special bubbles, next to the base
     * 75 normal / 15 skull / 10 heart. Each joins once it's unlocked.
     */
    val SPECIAL_TYPE_WEIGHTS: Map<BubbleType, Int> = mapOf(
        BubbleType.GOLDEN to 6,
        BubbleType.FREEZE to 4,
        BubbleType.BOMB to 5,
        BubbleType.SPLITTER to 6,
        BubbleType.SHIELD to 3,
    )

    fun isUnlocked(difficulty: Difficulty, mediumBest: Int?, progress: Map<Int, StageProgress>): Boolean =
        when (difficulty) {
            Difficulty.EASY, Difficulty.MEDIUM -> true
            Difficulty.HARD -> (mediumBest ?: 0) >= HARD_UNLOCK_MEDIUM_SCORE ||
                    (progress[HARD_UNLOCK_STAGE_ID]?.stars ?: 0) > 0
        }

    /** How to unlock a locked [difficulty], for its card. */
    fun unlockHint(difficulty: Difficulty): String = when (difficulty) {
        Difficulty.EASY, Difficulty.MEDIUM -> ""
        Difficulty.HARD -> {
            val goal = StageCatalog.stage(HARD_UNLOCK_STAGE_ID).boss?.let { "beat the ${it.name}" }
                ?: "clear stage $HARD_UNLOCK_STAGE_ID"
            "Score $HARD_UNLOCK_MEDIUM_SCORE on Medium\nor $goal"
        }
    }

    /** The first stage that spawns [type] — clearing it unlocks the type in Endless. */
    fun introStageId(type: BubbleType): Int? =
        StageCatalog.stages.firstOrNull { (it.spawn.typeWeights[type] ?: 0) > 0 }?.id

    /** Special bubble types whose introducing stage has at least one star. */
    fun unlockedTypes(progress: Map<Int, StageProgress>): Set<BubbleType> =
        SPECIAL_TYPE_WEIGHTS.keys.filterTo(mutableSetOf()) { type ->
            val stageId = introStageId(type) ?: return@filterTo false
            (progress[stageId]?.stars ?: 0) > 0
        }

    /** [base] weights plus every unlocked special type. */
    fun typeWeights(base: Map<BubbleType, Int>, progress: Map<Int, StageProgress>): Map<BubbleType, Int> =
        base + SPECIAL_TYPE_WEIGHTS.filterKeys { it in unlockedTypes(progress) }
}
