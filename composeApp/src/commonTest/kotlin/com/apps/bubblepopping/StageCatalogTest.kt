package com.apps.bubblepopping

import com.apps.bubblepopping.data.StageProgress
import com.apps.bubblepopping.stage.Objective
import com.apps.bubblepopping.stage.StageCatalog
import com.apps.bubblepopping.stage.World
import com.apps.bubblepopping.view.play.component.BubbleType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Guards the hand-written stage data against mistakes that would make stages unwinnable. */
class StageCatalogTest {

    private val stages = StageCatalog.stages

    @Test
    fun ids_areSequentialFromOne() {
        assertEquals((1..stages.size).toList(), stages.map { it.id })
    }

    @Test
    fun mainGoal_isNeverAConstraint() {
        stages.forEach { stage ->
            assertFalse(stage.goal.isConstraint, "Stage ${stage.id}: main goal must be reachable, not a constraint")
        }
    }

    @Test
    fun untimedStages_onlyHaveConstraintBonuses() {
        // Untimed stages end the moment the goal is met, so a bonus goal could never be reached.
        stages.filter { it.durationSec == null }.forEach { stage ->
            stage.bonusGoals.forEach { bonus ->
                assertTrue(bonus.isConstraint, "Stage ${stage.id}: bonus '${bonus.description}' can't be reached")
            }
        }
    }

    @Test
    fun timedStages_fitTheirClock() {
        stages.forEach { stage ->
            val duration = stage.durationSec
            stage.allObjectives.forEach { objective ->
                when (objective) {
                    is Objective.Survive -> assertTrue(duration != null && objective.seconds <= duration, "Stage ${stage.id}")
                    is Objective.FinishUnder -> assertNull(duration, "Stage ${stage.id}: FinishUnder needs an untimed stage")
                    else -> Unit
                }
            }
            if (duration != null) assertTrue(duration > 0, "Stage ${stage.id}")
        }
    }

    @Test
    fun targets_arePositive_andStagesHaveLives() {
        stages.forEach { stage ->
            assertTrue(stage.lives > 0, "Stage ${stage.id}")
            stage.allObjectives.forEach { objective ->
                val target = when (objective) {
                    is Objective.ReachScore -> objective.target
                    is Objective.PopCount -> objective.count
                    is Objective.PopType -> objective.count
                    is Objective.PopSize -> objective.count
                    is Objective.ReachCombo -> objective.combo
                    is Objective.Survive -> objective.seconds
                    is Objective.BombChain -> objective.bubbles
                    is Objective.WindSaves -> objective.count
                    is Objective.DefeatBoss -> objective.boss.totalHp
                    is Objective.MaxTaps -> objective.taps
                    is Objective.FinishUnder -> objective.seconds
                    Objective.NoMisses, Objective.NoLivesLost -> 1
                }
                assertTrue(target > 0, "Stage ${stage.id}: ${objective.description}")
            }
        }
    }

    @Test
    fun everyWorld_endsWithItsBoss() {
        StageCatalog.worlds.forEach { world ->
            val last = stages.last { it.world == world }
            assertTrue(last.boss != null, "$world should end with a boss stage")
            assertEquals(1, stages.count { it.world == world && it.boss != null }, "$world: one boss")
        }
    }

    @Test
    fun objectives_onlyNeedBubblesTheStageSpawns() {
        stages.forEach { stage ->
            val spawns = stage.spawn.typeWeights.filterValues { it > 0 }.keys
            stage.allObjectives.forEach { objective ->
                val needed = when (objective) {
                    is Objective.PopType -> objective.type
                    is Objective.BombChain -> BubbleType.BOMB
                    is Objective.WindSaves -> BubbleType.POISON
                    else -> null
                }
                if (needed != null) {
                    assertTrue(needed in spawns, "Stage ${stage.id}: '${objective.description}' but no $needed spawns")
                }
            }
        }
    }

    @Test
    fun everyWorld_hasStages_andTheirStarsCanUnlockTheNext() {
        assertEquals(World.entries.toList(), StageCatalog.worlds)
        World.entries.zipWithNext { world, next ->
            val starsUpToWorld = stages.filter { it.world.ordinal <= world.ordinal }.sumOf { it.maxStars }
            assertTrue(starsUpToWorld >= next.starsToUnlock, "$next needs more stars than exist before it")
        }
    }

    @Test
    fun unlocking_followsStarsOnPreviousStage() {
        assertTrue(StageCatalog.isUnlocked(1, emptyMap()))
        assertFalse(StageCatalog.isUnlocked(2, emptyMap()))
        assertTrue(StageCatalog.isUnlocked(2, mapOf(1 to StageProgress(1, stars = 1, bestScore = 5))))
        assertFalse(StageCatalog.isUnlocked(3, mapOf(1 to StageProgress(1, stars = 3, bestScore = 5))))
    }

    @Test
    fun currentStage_isFirstOpenStageWithoutStars() {
        assertEquals(1, StageCatalog.currentStageId(emptyMap()))

        val cleared = mapOf(
            1 to StageProgress(1, stars = 3, bestScore = 1),
            2 to StageProgress(2, stars = 1, bestScore = 1),
        )
        assertEquals(3, StageCatalog.currentStageId(cleared))
    }

    @Test
    fun currentStage_isLastStageOnceEverythingIsCleared() {
        val allCleared = stages.associate { it.id to StageProgress(it.id, stars = it.maxStars, bestScore = 1) }

        assertEquals(stages.last().id, StageCatalog.currentStageId(allCleared))
    }

    @Test
    fun nextWorld_staysLocked_withoutEnoughStars() {
        // Every Sky and Ocean stage cleared with just 1 star = 20 stars: enough for Ocean, not Space.
        val oneStarEach = stages.filter { it.world.ordinal <= World.OCEAN.ordinal }
            .associate { it.id to StageProgress(it.id, stars = 1, bestScore = 1) }
        val firstSpaceStage = stages.first { it.world == World.SPACE }.id

        assertFalse(StageCatalog.isUnlocked(firstSpaceStage, oneStarEach))
        assertEquals(firstSpaceStage - 1, StageCatalog.currentStageId(oneStarEach))
    }

    @Test
    fun worlds_unlockWithTotalStars() {
        val starsNeeded = World.OCEAN.starsToUnlock
        val justShort = (1 until starsNeeded).associateWith { StageProgress(it, stars = 1, bestScore = 1) }
        val enough = (1..starsNeeded).associateWith { StageProgress(it, stars = 1, bestScore = 1) }

        assertTrue(StageCatalog.isWorldUnlocked(World.SKY, emptyMap()))
        assertFalse(StageCatalog.isWorldUnlocked(World.OCEAN, justShort))
        assertTrue(StageCatalog.isWorldUnlocked(World.OCEAN, enough))
    }

    @Test
    fun worldStarRequirements_increase() {
        val requirements = World.entries.map { it.starsToUnlock }
        assertEquals(requirements.sorted(), requirements)
        assertEquals(0, requirements.first())
    }

    @Test
    fun next_returnsFollowingStageOrNull() {
        assertEquals(2, StageCatalog.next(1)?.id)
        assertNull(StageCatalog.next(stages.last().id))
    }
}
