package com.apps.bubblepopping

import com.apps.bubblepopping.game.GameMode
import com.apps.bubblepopping.game.RunStats
import com.apps.bubblepopping.meta.Mission
import com.apps.bubblepopping.meta.MissionCatalog
import com.apps.bubblepopping.meta.MissionContext
import com.apps.bubblepopping.meta.MissionKind
import com.apps.bubblepopping.meta.RunSummary
import com.apps.bubblepopping.view.home.Difficulty
import com.apps.bubblepopping.view.play.component.BubbleType
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MissionsTest {

    private val endless = GameMode.Endless(Difficulty.MEDIUM)

    private fun run(
        mode: GameMode = endless,
        score: Int = 0,
        stars: Int = 0,
        stats: RunStats = RunStats(),
    ) = RunSummary(mode = mode, score = score, stars = stars, stats = stats)

    @Test
    fun cumulativeMissions_addUpAcrossRounds() {
        val mission = Mission(MissionKind.POP_BUBBLES, target = 100, reward = 30)
        val after = mission
            .advancedBy(run(stats = RunStats(pops = 40)))
            .advancedBy(run(stats = RunStats(pops = 35)))

        assertEquals(75, after.progress)
        assertFalse(after.isComplete)
        assertEquals(100, after.advancedBy(run(stats = RunStats(pops = 90))).progress)
    }

    @Test
    fun oneRoundMissions_keepTheBestRound() {
        val mission = Mission(MissionKind.REACH_COMBO, target = 10, reward = 40)
        val after = mission
            .advancedBy(run(stats = RunStats(bestCombo = 7)))
            .advancedBy(run(stats = RunStats(bestCombo = 4)))

        assertEquals(7, after.progress)
        assertTrue(after.advancedBy(run(stats = RunStats(bestCombo = 12))).isComplete)
    }

    @Test
    fun endlessMissions_ignoreStages_andCountTheDailyChallengeAsMedium() {
        val score = Mission(MissionKind.ENDLESS_SCORE, target = 80, reward = 30)
        assertEquals(0, score.valueIn(run(mode = GameMode.Stage(3), score = 200)))
        assertEquals(120, score.valueIn(run(mode = GameMode.DailyChallenge(1), score = 120)))

        val surviveHard = Mission(MissionKind.ENDLESS_SURVIVE, target = 90, reward = 70, difficulty = Difficulty.HARD)
        assertEquals(0, surviveHard.valueIn(run(stats = RunStats(elapsedSec = 120))))
        assertEquals(
            120,
            surviveHard.valueIn(run(mode = GameMode.Endless(Difficulty.HARD), stats = RunStats(elapsedSec = 120))),
        )
    }

    @Test
    fun stageMissions_countCatalogAndDailyStages() {
        val clear = Mission(MissionKind.CLEAR_STAGES, target = 3, reward = 40)
        assertEquals(1, clear.valueIn(run(mode = GameMode.Stage(2), stars = 1)))
        assertEquals(0, clear.valueIn(run(mode = GameMode.Stage(2), stars = 0)))
        assertEquals(1, clear.valueIn(run(mode = GameMode.DailyStage(1, com.apps.bubblepopping.stage.World.SKY), stars = 2)))
        assertEquals(0, clear.valueIn(run(stars = 3)))
    }

    @Test
    fun popTypeMission_countsOnlyItsType() {
        val golden = Mission(MissionKind.POP_TYPE, target = 3, reward = 40, bubbleType = BubbleType.GOLDEN)
        val stats = RunStats(popsByType = mapOf(BubbleType.GOLDEN to 2, BubbleType.HEART to 5))
        assertEquals(2, golden.valueIn(run(stats = stats)))
        assertEquals("Pop 3 golden bubbles in one round", golden.description)
    }

    @Test
    fun fill_givesThreeMissionsOfDifferentKinds() {
        repeat(50) { seed ->
            val missions = MissionCatalog.fill(Random(seed), MissionContext(), emptyList())
            assertEquals(MissionCatalog.ACTIVE_COUNT, missions.size)
            assertEquals(MissionCatalog.ACTIVE_COUNT, missions.map { it.kind }.toSet().size)
        }
    }

    @Test
    fun newMissions_onlyAskForWhatThePlayerHasUnlocked() {
        val locked = MissionCatalog.templates(MissionContext()).values.flatten()
        assertTrue(locked.none { it.bubbleType != null && it.bubbleType != BubbleType.HEART })
        assertTrue(locked.none { it.difficulty == Difficulty.HARD })
        assertTrue(locked.none { it.kind == MissionKind.BOMB_CHAIN })

        val open = MissionCatalog.templates(
            MissionContext(unlockedTypes = setOf(BubbleType.GOLDEN, BubbleType.BOMB), hardUnlocked = true),
        ).values.flatten()
        assertTrue(open.any { it.bubbleType == BubbleType.GOLDEN })
        assertTrue(open.any { it.difficulty == Difficulty.HARD })
        assertTrue(open.any { it.kind == MissionKind.BOMB_CHAIN })
    }

    @Test
    fun everyTemplate_isSensible() {
        val all = MissionCatalog.templates(
            MissionContext(unlockedTypes = BubbleType.entries.toSet(), hardUnlocked = true),
        ).values.flatten()
        assertEquals(MissionKind.entries.toSet(), all.map { it.kind }.toSet())
        for (mission in all) {
            assertTrue(mission.target > 0 && mission.reward > 0, mission.toString())
            assertTrue(mission.description.isNotBlank())
            assertEquals(0, mission.progress)
        }
    }
}
