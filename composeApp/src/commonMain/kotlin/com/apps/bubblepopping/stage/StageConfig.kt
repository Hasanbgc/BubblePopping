package com.apps.bubblepopping.stage

import com.apps.bubblepopping.game.GameRules
import com.apps.bubblepopping.game.SpawnConfig

/**
 * One hand-designed stage. Stages are data: adding a stage never needs new game logic.
 *
 * How a stage ends:
 * - **Untimed** ([durationSec] null): as soon as [goal] is met. Bonus objectives
 *   must therefore be constraints (checked at that moment).
 * - **Timed**: when time runs out, or earlier once every objective is met.
 * - Either way, running out of lives or popping poison ends it immediately.
 *
 * Stars: 1 for [goal], plus 1 per met [bonusGoals] entry. 0 stars = failed.
 */
data class StageConfig(
    val id: Int,
    val world: World,
    val goal: Objective,
    val bonusGoals: List<Objective>,
    val spawn: SpawnConfig,
    val introText: String,
    val durationSec: Int? = null,
    val lives: Int = GameRules.MAX_LIVES,
    /** Fixed seed → the same bubbles on every attempt. Null = random each time. */
    val seed: Long? = null,
    /**
     * Roughly how long the stage is meant to take, in seconds (`stage-plan.md` §2.1).
     * The stage simulator test checks that a good player can't win much faster.
     * Null = not yet designed to a length.
     */
    val designSec: Int? = null,
) {
    val allObjectives: List<Objective> get() = listOf(goal) + bonusGoals
    val maxStars: Int get() = 1 + bonusGoals.size

    /** The boss to beat, for boss stages (goal = [Objective.DefeatBoss]). */
    val boss: BossConfig? get() = (goal as? Objective.DefeatBoss)?.boss
}
