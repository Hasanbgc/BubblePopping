package com.apps.bubblepopping.stage

import com.apps.bubblepopping.game.BubbleSize
import com.apps.bubblepopping.view.play.component.BubbleType

/**
 * Something the player must do in a stage.
 *
 * Goals ("pop 10 bubbles") start unmet and are reached by playing. Constraints
 * ("lose no lives") start met and can only be broken — they are checked when
 * the stage ends, so they only make sense as bonus objectives.
 */
sealed interface Objective {
    /** Text for the stage card, HUD and result screen. */
    val description: String

    /** True for objectives that start met and can only be broken. */
    val isConstraint: Boolean get() = false

    data class ReachScore(val target: Int) : Objective {
        override val description get() = "Score $target points"
    }

    data class PopCount(val count: Int) : Objective {
        override val description get() = "Pop $count bubbles"
    }

    data class PopType(val type: BubbleType, val count: Int) : Objective {
        override val description get() = "Pop $count ${type.displayName(count)}"
    }

    data class PopSize(val size: BubbleSize, val count: Int) : Objective {
        override val description get() = "Pop $count ${size.name.lowercase()} bubbles"
    }

    data class ReachCombo(val combo: Int) : Objective {
        override val description get() = "Reach a $combo combo"
    }

    data class Survive(val seconds: Int) : Objective {
        override val description get() = "Survive $seconds seconds"
    }

    /** One bomb (counting the bombs it sets off) must pop at least [bubbles]. */
    data class BombChain(val bubbles: Int) : Objective {
        override val description get() = "Pop $bubbles bubbles with one bomb"
    }

    data class WindSaves(val count: Int) : Objective {
        override val description get() = "Blow $count skulls away with the wind"
    }

    /** Beat every copy of [boss]; progress is damage dealt out of its total HP. */
    data class DefeatBoss(val boss: BossConfig) : Objective {
        override val description get() = "Defeat the ${boss.name}"
    }

    /** Every tap must hit a bubble. */
    data object NoMisses : Objective {
        override val description get() = "Don't miss a tap"
        override val isConstraint get() = true
    }

    data object NoLivesLost : Objective {
        override val description get() = "Lose no lives"
        override val isConstraint get() = true
    }

    data class MaxTaps(val taps: Int) : Objective {
        override val description get() = "Use at most $taps taps"
        override val isConstraint get() = true
    }

    data class FinishUnder(val seconds: Int) : Objective {
        override val description get() = "Finish within $seconds seconds"
        override val isConstraint get() = true
    }
}

/** "golden bubble" / "golden bubbles" — for objective and mission texts. */
fun BubbleType.displayName(count: Int): String {
    val singular = when (this) {
        BubbleType.NORMAL -> "bubble"
        BubbleType.HEART -> "heart"
        BubbleType.POISON -> "poison bubble"
        BubbleType.GOLDEN -> "golden bubble"
        BubbleType.FREEZE -> "ice bubble"
        BubbleType.BOMB -> "bomb"
        BubbleType.SPLITTER -> "splitter"
        BubbleType.SHIELD -> "shield"
    }
    return if (count == 1) singular else "${singular}s"
}

/** Where an objective stands right now. For constraints, [current] counts the "bad" thing. */
data class ObjectiveProgress(
    val objective: Objective,
    val current: Int,
    val target: Int,
    val satisfied: Boolean,
) {
    /** 0 → 1 progress towards a goal; constraints report 1 while met, 0 once broken. */
    val fraction: Float
        get() = when {
            objective.isConstraint -> if (satisfied) 1f else 0f
            target <= 0 -> 1f
            else -> (current.toFloat() / target).coerceIn(0f, 1f)
        }
}
