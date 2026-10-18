package com.apps.bubblepopping.meta

import com.apps.bubblepopping.game.EndlessUnlocks
import com.apps.bubblepopping.stage.Objective
import com.apps.bubblepopping.stage.StageConfig
import com.apps.bubblepopping.view.play.component.BubbleType
import kotlinx.serialization.Serializable

/**
 * A head start bought on a stage's card, for that one attempt. They cost coins
 * for now; each will become "watch an ad" later (`stage-plan.md` §6).
 */
@Serializable
enum class Booster(val title: String, val description: String, val price: Int) {
    EXTRA_LIFE("+1 life", "Start with one extra life", 80),
    SHIELD("Shield", "Start with a shield up", 60),
    ICE("Ice", "An ice bubble rises right away", 40),
    CLOCK("Clock", "15 more seconds on the timer", 40),
    LIGHTNING("Lightning", "A lightning bubble rises right away", 80);

    /** Whether this booster is offered before [stage]: each joins once its bubble has been met. */
    fun isOffered(stage: StageConfig): Boolean {
        if (stage.id < FIRST_STAGE) return false
        return when (this) {
            EXTRA_LIFE -> true
            SHIELD -> introduced(BubbleType.SHIELD, stage)
            ICE -> introduced(BubbleType.FREEZE, stage)
            LIGHTNING -> introduced(BubbleType.LIGHTNING, stage)
            // Only where more time helps: a timer to add to, and not a "survive" goal.
            CLOCK -> introduced(BubbleType.CLOCK, stage) && stage.durationSec != null &&
                stage.allObjectives.none { it is Objective.Survive }
        }
    }

    companion object {
        /** The first stages teach the game without boosters. */
        const val FIRST_STAGE = 5
        const val CLOCK_SECONDS = 15
        /** The ice and lightning boosters' bubbles rise this long into the round. */
        const val ICE_DELAY_SECONDS = 1.5f

        fun offeredFor(stage: StageConfig): List<Booster> = entries.filter { it.isOffered(stage) }

        fun price(boosters: Set<Booster>): Int = boosters.sumOf { it.price }
    }
}

private fun introduced(type: BubbleType, stage: StageConfig): Boolean =
    EndlessUnlocks.introStageId(type)?.let { stage.id >= it } == true
