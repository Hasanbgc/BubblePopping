package com.apps.bubblepopping

import com.apps.bubblepopping.data.StageProgress
import com.apps.bubblepopping.game.EndlessUnlocks
import com.apps.bubblepopping.game.SpawnConfig
import com.apps.bubblepopping.stage.StageCatalog
import com.apps.bubblepopping.view.home.Difficulty
import com.apps.bubblepopping.view.play.component.BubbleType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EndlessUnlocksTest {

    private fun cleared(vararg stageIds: Int) =
        stageIds.associateWith { StageProgress(stageId = it, stars = 1, bestScore = 10) }

    @Test
    fun easyAndMedium_areAlwaysOpen() {
        assertTrue(EndlessUnlocks.isUnlocked(Difficulty.EASY, mediumBest = null, progress = emptyMap()))
        assertTrue(EndlessUnlocks.isUnlocked(Difficulty.MEDIUM, mediumBest = null, progress = emptyMap()))
    }

    @Test
    fun hard_unlocksWithAMediumScore() {
        val target = EndlessUnlocks.HARD_UNLOCK_MEDIUM_SCORE
        assertFalse(EndlessUnlocks.isUnlocked(Difficulty.HARD, mediumBest = null, progress = emptyMap()))
        assertFalse(EndlessUnlocks.isUnlocked(Difficulty.HARD, mediumBest = target - 1, progress = emptyMap()))
        assertTrue(EndlessUnlocks.isUnlocked(Difficulty.HARD, mediumBest = target, progress = emptyMap()))
    }

    @Test
    fun hard_unlocksByBeatingTheSkyBoss() {
        val bossStage = EndlessUnlocks.HARD_UNLOCK_STAGE_ID
        assertEquals(10, bossStage)
        assertTrue(EndlessUnlocks.isUnlocked(Difficulty.HARD, mediumBest = null, progress = cleared(bossStage)))
        assertFalse(EndlessUnlocks.isUnlocked(Difficulty.HARD, mediumBest = null, progress = cleared(bossStage - 1)))
    }

    @Test
    fun everySpecialType_isIntroducedBySomeStage() {
        for (type in EndlessUnlocks.SPECIAL_TYPE_WEIGHTS.keys) {
            assertNotNull(EndlessUnlocks.introStageId(type), "$type never appears in a stage")
        }
    }

    @Test
    fun specialTypes_joinEndlessOnceTheirStageIsCleared() {
        val base = SpawnConfig.DEFAULT_TYPE_WEIGHTS
        assertEquals(base, EndlessUnlocks.typeWeights(base, emptyMap()))

        val goldenStage = EndlessUnlocks.introStageId(BubbleType.GOLDEN)!!
        val weights = EndlessUnlocks.typeWeights(base, cleared(goldenStage))
        assertEquals(setOf(BubbleType.GOLDEN), EndlessUnlocks.unlockedTypes(cleared(goldenStage)))
        assertTrue((weights[BubbleType.GOLDEN] ?: 0) > 0)
        assertEquals(base[BubbleType.NORMAL], weights[BubbleType.NORMAL])
    }

    @Test
    fun clearingEveryStage_unlocksEveryType() {
        val all = cleared(*StageCatalog.stages.map { it.id }.toIntArray())
        assertEquals(EndlessUnlocks.SPECIAL_TYPE_WEIGHTS.keys, EndlessUnlocks.unlockedTypes(all))
    }
}
