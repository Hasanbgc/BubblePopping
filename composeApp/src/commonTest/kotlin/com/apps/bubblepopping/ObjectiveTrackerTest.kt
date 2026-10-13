package com.apps.bubblepopping

import androidx.compose.ui.graphics.Color
import com.apps.bubblepopping.game.BubbleSize
import com.apps.bubblepopping.game.GameEvent
import com.apps.bubblepopping.game.ObjectiveTracker
import com.apps.bubblepopping.game.SpawnConfig
import com.apps.bubblepopping.stage.Objective
import com.apps.bubblepopping.stage.StageConfig
import com.apps.bubblepopping.stage.World
import com.apps.bubblepopping.view.play.component.Bubble
import com.apps.bubblepopping.view.play.component.BubbleType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ObjectiveTrackerTest {

    private fun bubble(type: BubbleType = BubbleType.NORMAL, size: BubbleSize = BubbleSize.SMALL) = Bubble(
        id = "b", x = 0f, y = 0f, radius = 10f, size = size, speed = 0f, amplitude = 0f,
        baseX = 0f, phase = 0f, color = Color.White, shimmerAngle = 0f, type = type,
    )

    private fun ObjectiveTracker.pop(type: BubbleType = BubbleType.NORMAL, size: BubbleSize = BubbleSize.SMALL, points: Int = 1) =
        onEvent(GameEvent.Popped(bubble(type, size), points, combo = 1))

    @Test
    fun reachScore_sumsPoints() {
        val tracker = ObjectiveTracker()
        tracker.pop(points = 3)
        tracker.pop(points = 5)

        val progress = tracker.progress(Objective.ReachScore(10))
        assertEquals(8, progress.current)
        assertFalse(progress.satisfied)
        assertEquals(0.8f, progress.fraction)

        tracker.pop(points = 2)
        assertTrue(tracker.isSatisfied(Objective.ReachScore(10)))
    }

    @Test
    fun popCounts_byTypeAndSize_ignorePoison() {
        val tracker = ObjectiveTracker()
        tracker.pop(BubbleType.HEART, BubbleSize.LARGE)
        tracker.pop(BubbleType.NORMAL, BubbleSize.SMALL)
        tracker.pop(BubbleType.POISON, BubbleSize.SMALL)

        assertEquals(2, tracker.progress(Objective.PopCount(10)).current)
        assertEquals(1, tracker.progress(Objective.PopType(BubbleType.HEART, 3)).current)
        assertEquals(1, tracker.progress(Objective.PopSize(BubbleSize.SMALL, 5)).current)
    }

    @Test
    fun reachCombo_usesBestComboSeen() {
        val tracker = ObjectiveTracker()
        tracker.onEvent(GameEvent.ComboChanged(combo = 4, previous = 3))
        tracker.onEvent(GameEvent.ComboChanged(combo = 0, previous = 4, broken = true))

        assertTrue(tracker.isSatisfied(Objective.ReachCombo(4)))
        assertFalse(tracker.isSatisfied(Objective.ReachCombo(5)))
    }

    @Test
    fun bombChain_usesBiggestSingleExplosion() {
        val tracker = ObjectiveTracker()
        tracker.onEvent(GameEvent.BombExploded(chainSize = 4))
        tracker.onEvent(GameEvent.BombExploded(chainSize = 2))

        assertEquals(4, tracker.progress(Objective.BombChain(5)).current)
        assertTrue(tracker.isSatisfied(Objective.BombChain(4)))
    }

    @Test
    fun windSaves_countBlownAwaySkulls() {
        val tracker = ObjectiveTracker()
        repeat(2) { tracker.onEvent(GameEvent.WindSave(bubble(BubbleType.POISON))) }

        assertEquals(2, tracker.progress(Objective.WindSaves(3)).current)
    }

    @Test
    fun survive_andFinishUnder_followTicks() {
        val tracker = ObjectiveTracker()
        tracker.onEvent(GameEvent.Tick(20))

        assertTrue(tracker.isSatisfied(Objective.Survive(20)))
        assertTrue(tracker.isSatisfied(Objective.FinishUnder(20)))

        tracker.onEvent(GameEvent.Tick(21))
        assertFalse(tracker.isSatisfied(Objective.FinishUnder(20)))
    }

    @Test
    fun constraints_startMet_andBreak() {
        val tracker = ObjectiveTracker()
        assertTrue(tracker.isSatisfied(Objective.NoMisses))
        assertTrue(tracker.isSatisfied(Objective.NoLivesLost))
        assertTrue(tracker.isSatisfied(Objective.MaxTaps(2)))

        tracker.onEvent(GameEvent.Tapped(hit = true))
        tracker.onEvent(GameEvent.Tapped(hit = true))
        tracker.onEvent(GameEvent.Missed(bubble(BubbleType.HEART), lifeLost = false))
        assertTrue(tracker.isSatisfied(Objective.NoMisses))
        assertTrue(tracker.isSatisfied(Objective.NoLivesLost))
        assertTrue(tracker.isSatisfied(Objective.MaxTaps(2)))

        tracker.onEvent(GameEvent.Tapped(hit = false))
        tracker.onEvent(GameEvent.Missed(bubble(), lifeLost = true))
        assertFalse(tracker.isSatisfied(Objective.NoMisses))
        assertFalse(tracker.isSatisfied(Objective.NoLivesLost))
        assertFalse(tracker.isSatisfied(Objective.MaxTaps(2)))
    }

    @Test
    fun stars_needMainGoal_thenCountBonuses() {
        val stage = StageConfig(
            id = 1,
            world = World.SKY,
            goal = Objective.PopCount(2),
            bonusGoals = listOf(Objective.NoMisses, Objective.ReachScore(10)),
            spawn = SpawnConfig.EASY,
            introText = "",
        )
        val tracker = ObjectiveTracker()
        tracker.pop(points = 5)
        assertEquals(0, tracker.stars(stage))   // bonuses don't count without the goal

        tracker.pop(points = 5)
        assertEquals(3, tracker.stars(stage))

        tracker.onEvent(GameEvent.Tapped(hit = false))
        assertEquals(2, tracker.stars(stage))
    }
}
