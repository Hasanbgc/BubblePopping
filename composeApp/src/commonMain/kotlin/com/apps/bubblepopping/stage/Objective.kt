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
        override val description get() = "Reach ${comboArticle(combo)} $combo combo"
    }

    /** Reach a [combo] combo in [times] separate combos. */
    data class ComboTimes(val combo: Int, val times: Int) : Objective {
        override val description get() =
            if (times == 1) "Reach ${comboArticle(combo)} $combo combo" else "Reach ${comboArticle(combo)} $combo combo $times times"
    }

    data class Survive(val seconds: Int) : Objective {
        override val description get() = "Survive $seconds seconds"
    }

    /** One bomb (counting the bombs it sets off) must pop at least [bubbles]. */
    data class BombChain(val bubbles: Int) : Objective {
        override val description get() = "Pop $bubbles bubbles with one bomb"
    }

    /** Lightning bubbles must zap [count] bubbles in all. */
    data class LightningZaps(val count: Int) : Objective {
        override val description get() = "Zap $count bubbles with lightning"
    }

    /** Pop both bubbles of [count] chain pairs in time. */
    data class ChainPairs(val count: Int) : Objective {
        override val description get() = "Pop $count chain pairs"
    }

    /** Pop [count] armored bubbles, however they break. */
    data class CrackArmored(val count: Int) : Objective {
        override val description get() = "Crack $count armored bubbles"
    }

    /** Break [count] armored bubbles with a bomb blast or lightning. */
    data class BreakArmoredWithBlast(val count: Int) : Objective {
        override val description get() = "Break $count armored bubbles with a blast"
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

    data class LivesLostAtMost(val lives: Int) : Objective {
        override val description get() = "Lose at most $lives ${if (lives == 1) "life" else "lives"}"
        override val isConstraint get() = true
    }

    /**
     * At least [percent] % of taps must hit a bubble. Unlike a tap limit this is
     * always reachable: ignoring bubbles costs lives, so every tap is wanted.
     */
    data class Accuracy(val percent: Int) : Objective {
        init {
            require(percent in 1..100)
        }

        override val description get() = "Hit $percent% of your taps"
        override val isConstraint get() = true
    }

    /** Every one of [goals] at once ("Pop 6 bombs and pop 6 golden bubbles"). */
    data class AllOf(val goals: List<Objective>) : Objective {
        init {
            require(goals.size >= 2 && goals.none { it.isConstraint || it is AllOf || it is DefeatBoss }) {
                "AllOf combines two or more plain goals"
            }
        }

        override val description get() =
            goals.first().description + goals.drop(1).joinToString("") { " and " + it.description.replaceFirstChar(Char::lowercase) }
    }

    data class FinishUnder(val seconds: Int) : Objective {
        override val description get() = "Finish within $seconds seconds"
        override val isConstraint get() = true
    }
}

/** "an 8 combo", "an 11 combo", "an 18 combo", "an 80 combo" — otherwise "a". */
private fun comboArticle(combo: Int): String =
    if (combo.toString().startsWith("8") || combo == 11 || combo == 18) "an" else "a"

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
        BubbleType.CLOCK -> "clock bubble"
        BubbleType.DOUBLE -> "×2 bubble"
        BubbleType.LIGHTNING -> "lightning bubble"
        BubbleType.ARMORED -> "armored bubble"
        BubbleType.GHOST -> "ghost bubble"
        BubbleType.CHAIN -> "chain bubble"
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
