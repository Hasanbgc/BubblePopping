package com.apps.bubblepopping

import com.apps.bubblepopping.stage.Objective
import com.apps.bubblepopping.stage.ObjectiveProgress
import com.apps.bubblepopping.stage.compactCount
import com.apps.bubblepopping.stage.hudReadout
import com.apps.bubblepopping.stage.starMeterStates
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The HUD's stars: one per objective, each showing its own progress. */
class StarMeterTest {

    private val survive = Objective.Survive(120)
    private val lives = Objective.LivesLostAtMost(2)
    private val score = Objective.ReachScore(3100)

    @Test
    fun eachStar_fillsWithItsOwnObjective_inAnyOrder() {
        val states = starMeterStates(
            listOf(
                ObjectiveProgress(survive, current = 60, target = 120, satisfied = false),
                ObjectiveProgress(lives, current = 1, target = 2, satisfied = true),
                ObjectiveProgress(score, current = 3100, target = 3100, satisfied = true),
            )
        )
        assertEquals(0.5f, states[0].fill)
        // A constraint is full while it holds; a bonus goal can finish before the main one.
        assertEquals(1f, states[1].fill)
        assertEquals(1f, states[2].fill)
    }

    @Test
    fun bonusStars_lightOnlyOnceTheMainGoalIsMet() {
        val bonusesMet = listOf(
            ObjectiveProgress(lives, current = 0, target = 2, satisfied = true),
            ObjectiveProgress(score, current = 3100, target = 3100, satisfied = true),
        )
        val before = starMeterStates(listOf(ObjectiveProgress(survive, 119, 120, satisfied = false)) + bonusesMet)
        assertTrue(before.none { it.lit })
        val after = starMeterStates(listOf(ObjectiveProgress(survive, 120, 120, satisfied = true)) + bonusesMet)
        assertTrue(after.all { it.lit })
    }

    @Test
    fun aBrokenConstraint_isEmptyAndMarked() {
        val states = starMeterStates(
            listOf(
                ObjectiveProgress(survive, current = 120, target = 120, satisfied = true),
                ObjectiveProgress(lives, current = 3, target = 2, satisfied = false),
            )
        )
        assertTrue(states[1].broken)
        assertEquals(0f, states[1].fill)
        assertFalse(states[1].lit)
        assertFalse(states[0].broken)
    }

    @Test
    fun readouts_areShort() {
        assertEquals("75/120s", ObjectiveProgress(survive, 75, 120, satisfied = false).hudReadout())
        assertEquals("3/2", ObjectiveProgress(lives, 3, 2, satisfied = false).hudReadout())
        assertEquals("240/3.1k", ObjectiveProgress(score, 240, 3100, satisfied = false).hudReadout())
        assertEquals("3.1k/3.1k", ObjectiveProgress(score, 5000, 3100, satisfied = true).hudReadout())
        assertEquals("92%", ObjectiveProgress(Objective.Accuracy(80), 92, 80, satisfied = true).hudReadout())
    }

    @Test
    fun shortReadouts_dropAGoalsTarget_butKeepALimit() {
        assertEquals("75s", ObjectiveProgress(survive, 75, 120, satisfied = false).hudReadout(short = true))
        assertEquals("240", ObjectiveProgress(score, 240, 3100, satisfied = false).hudReadout(short = true))
        // "1" alone wouldn't say how many lives may still go.
        assertEquals("1/2", ObjectiveProgress(lives, 1, 2, satisfied = true).hudReadout(short = true))
    }

    @Test
    fun compactCount_shortensThousands() {
        assertEquals("240", compactCount(240))
        assertEquals("3k", compactCount(3000))
        assertEquals("3.1k", compactCount(3100))
        assertEquals("12k", compactCount(12_500))
    }
}
