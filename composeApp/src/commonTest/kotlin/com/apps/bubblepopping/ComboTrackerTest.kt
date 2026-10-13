package com.apps.bubblepopping

import com.apps.bubblepopping.game.ComboTracker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ComboTrackerTest {

    @Test
    fun popsWithinWindow_extendCombo() {
        val tracker = ComboTracker(window = 0.8f)

        assertEquals(1, tracker.onPop(0.0f))
        assertEquals(2, tracker.onPop(0.5f))
        assertEquals(3, tracker.onPop(1.2f))
    }

    @Test
    fun popAfterWindow_startsNewCombo() {
        val tracker = ComboTracker(window = 0.8f)
        tracker.onPop(0f)
        tracker.onPop(0.5f)

        assertEquals(1, tracker.onPop(1.5f))
        assertEquals(2, tracker.best)
    }

    @Test
    fun expireIfStale_endsComboOnlyAfterWindow() {
        val tracker = ComboTracker(window = 0.8f)
        tracker.onPop(1f)

        assertFalse(tracker.expireIfStale(1.7f))
        assertEquals(1, tracker.combo)
        assertTrue(tracker.expireIfStale(1.9f))
        assertEquals(0, tracker.combo)
        assertFalse(tracker.expireIfStale(2.5f))   // already expired
    }

    @Test
    fun breakCombo_returnsBrokenCountAndKeepsBest() {
        val tracker = ComboTracker()
        repeat(4) { tracker.onPop(it * 0.1f) }

        assertEquals(4, tracker.breakCombo())
        assertEquals(0, tracker.combo)
        assertEquals(4, tracker.best)
        assertEquals(0, tracker.breakCombo())
    }

    @Test
    fun timeLeftFraction_drainsFromOneToZero() {
        val tracker = ComboTracker(window = 0.8f)
        assertEquals(0f, tracker.timeLeftFraction(0f))

        tracker.onPop(1f)

        assertEquals(1f, tracker.timeLeftFraction(1f))
        assertEquals(0.5f, tracker.timeLeftFraction(1.4f), 0.0001f)
        assertEquals(0f, tracker.timeLeftFraction(2f))
    }

    @Test
    fun reset_clearsComboAndBest() {
        val tracker = ComboTracker()
        tracker.onPop(0f)
        tracker.onPop(0.1f)

        tracker.reset()

        assertEquals(0, tracker.combo)
        assertEquals(0, tracker.best)
        assertEquals(1, tracker.onPop(0.2f))
    }

    @Test
    fun multiplierTiers() {
        assertEquals(listOf(1, 1, 2, 2, 2, 3, 3, 3, 3, 5, 5), (1..11).map(ComboTracker::multiplierFor))
    }

    @Test
    fun milestoneText_onlyAtExactCounts() {
        assertEquals("NICE!", ComboTracker.milestoneText(5))
        assertEquals("GREAT!", ComboTracker.milestoneText(10))
        assertEquals("PERFECT!", ComboTracker.milestoneText(15))
        assertEquals("INSANE!", ComboTracker.milestoneText(25))
        assertNull(ComboTracker.milestoneText(6))
    }
}
