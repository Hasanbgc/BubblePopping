package com.apps.bubblepopping

import androidx.compose.ui.geometry.Offset
import com.apps.bubblepopping.game.GameEvent
import com.apps.bubblepopping.game.GameMode
import com.apps.bubblepopping.game.GameOverReason
import com.apps.bubblepopping.game.GameRules
import com.apps.bubblepopping.game.SpawnConfig
import com.apps.bubblepopping.stage.BossConfig
import com.apps.bubblepopping.stage.BossGimmick
import com.apps.bubblepopping.stage.Bosses
import com.apps.bubblepopping.stage.Objective
import com.apps.bubblepopping.stage.StageConfig
import com.apps.bubblepopping.stage.World
import com.apps.bubblepopping.view.BubbleGameViewModel
import com.apps.bubblepopping.view.play.component.BubbleType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BossTest {

    private companion object {
        const val FRAME = 0.016f
        const val DENSITY = 2.75f

        /** Practically no regular bubbles, so taps always land on the boss. */
        val NO_BUBBLES = SpawnConfig.MEDIUM.copy(
            spawnInterval = 1_000f,
            minSpawnInterval = 1_000f,
            typeWeights = mapOf(BubbleType.NORMAL to 1),
        )
    }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun boss(gimmick: BossGimmick, vararg hp: Int) =
        BossConfig(id = "test", name = "Test Boss", gimmick = gimmick, hpPerGeneration = hp.toList())

    private fun bossViewModel(
        boss: BossConfig,
        progress: FakeProgressRepository = FakeProgressRepository(),
        bonusGoals: List<Objective> = emptyList(),
    ): BubbleGameViewModel {
        val stage = StageConfig(
            id = 1,
            world = World.SKY,
            goal = Objective.DefeatBoss(boss),
            bonusGoals = bonusGoals,
            spawn = NO_BUBBLES,
            introText = "",
        )
        return BubbleGameViewModel(
            mode = GameMode.Stage(stage.id),
            progressRepository = progress,
            settingsRepository = FakeSettingsRepository(),
            metaRepository = FakeMetaRepository(),
            stage = stage,
            seed = 1L,
        ).apply {
            setCanvasBounds(1080f, 2340f, topOffset = 200f, density = DENSITY)
            update(FRAME)   // the boss appears on the first frame
        }
    }

    private fun BubbleGameViewModel.tapBoss(): Boolean {
        val boss = bosses.last()
        return tryPop(Offset(boss.x, boss.y))
    }

    private fun BubbleGameViewModel.runFor(seconds: Float) {
        repeat((seconds / FRAME).toInt()) { update(FRAME) }
    }

    private fun TestScope.collectEvents(viewModel: BubbleGameViewModel): List<GameEvent> {
        val events = mutableListOf<GameEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }
        return events
    }

    @Test
    fun bossAppears_andTapsDealDamage_andShrinkIt() = runTest {
        val viewModel = bossViewModel(boss(BossGimmick.SPLITS, 10))
        val events = collectEvents(viewModel)
        val boss = viewModel.bosses.single()
        val fullRadius = boss.radius

        assertTrue(viewModel.tapBoss())

        assertEquals(9, boss.hp)
        assertTrue(boss.radius < fullRadius)
        assertTrue(events.any { it is GameEvent.BossHit && it.damage == 1 })
        assertEquals(1, viewModel.objectives.first().current)
        assertTrue(viewModel.score > 0)
    }

    @Test
    fun defeatingTheBoss_playsCelebration_thenWinsTheStage() = runTest {
        val progress = FakeProgressRepository()
        val viewModel = bossViewModel(boss(BossGimmick.SPLITS, 3), progress, bonusGoals = listOf(Objective.NoMisses))
        val events = collectEvents(viewModel)

        repeat(3) { viewModel.tapBoss() }

        assertTrue(viewModel.bosses.isEmpty())
        assertTrue(events.any { it is GameEvent.BossDefeated })
        assertTrue(viewModel.confetti.isNotEmpty())
        assertFalse(viewModel.isGameOver)   // slow-motion celebration first

        viewModel.runFor(GameRules.BOSS_CELEBRATION_SECONDS + 0.1f)

        assertTrue(viewModel.isGameOver)
        assertEquals(GameOverReason.GOAL_COMPLETE, events.filterIsInstance<GameEvent.GameOver>().single().reason)
        assertEquals(2, viewModel.stageResult?.stars)
        assertEquals(2, progress.recordedStages.single().stars)
    }

    @Test
    fun splittingBoss_splitsIntoThree_thenNine() = runTest {
        val viewModel = bossViewModel(boss(BossGimmick.SPLITS, 2, 1, 1))
        collectEvents(viewModel)

        repeat(2) { viewModel.tapBoss() }
        assertEquals(3, viewModel.bosses.size)
        assertTrue(viewModel.bosses.all { it.generation == 1 })

        // 3 copies × 1 HP, each splitting into 3 × 1 HP: 12 more taps in any order.
        var taps = 0
        var sawThirdGeneration = false
        while (viewModel.bosses.isNotEmpty() && taps < 50) {
            viewModel.tapBoss()
            taps++
            sawThirdGeneration = sawThirdGeneration || viewModel.bosses.any { it.generation == 2 }
        }

        assertEquals(12, taps)
        assertTrue(sawThirdGeneration)
    }

    @Test
    fun cloudKing_throwsASkull_everyThirdHit() = runTest {
        val viewModel = bossViewModel(boss(BossGimmick.THROWS_SKULLS, 20))
        collectEvents(viewModel)

        repeat(GameRules.SKULL_EVERY_HITS - 1) { viewModel.tapBoss() }
        assertTrue(viewModel.bubbles.none { it.type == BubbleType.POISON })

        viewModel.tapBoss()
        val skull = viewModel.bubbles.single { it.type == BubbleType.POISON }
        assertTrue(skull.y < viewModel.bosses.single().y)   // thrown out of the top
    }

    @Test
    fun jellyQueen_stingLocksTaps_withoutCountingMisses() = runTest {
        val viewModel = bossViewModel(boss(BossGimmick.STINGS, 20), bonusGoals = listOf(Objective.NoMisses))
        collectEvents(viewModel)
        repeat(GameRules.STING_EVERY_HITS) { viewModel.tapBoss() }
        assertTrue(viewModel.tapLockLeft > 0f)
        val hpAfterSting = viewModel.bosses.single().hp

        assertFalse(viewModel.tapBoss())   // locked
        viewModel.onMissedTap()            // ignored while locked

        assertEquals(hpAfterSting, viewModel.bosses.single().hp)
        assertTrue(viewModel.objectives.last().satisfied)   // NoMisses still intact

        viewModel.runFor(GameRules.STING_SECONDS + 0.1f)
        assertTrue(viewModel.tapBoss())
    }

    @Test
    fun nebulaCore_dropsBombs_belowAThirdHp_andBombsHurtIt() = runTest {
        val viewModel = bossViewModel(boss(BossGimmick.DROPS_BOMBS, 9))
        val events = collectEvents(viewModel)

        repeat(6) { viewModel.tapBoss() }   // 6 of 9 damage → last third
        val bombs = viewModel.bubbles.filter { it.type == BubbleType.BOMB }
        assertEquals(GameRules.NEBULA_BOMB_COUNT, bombs.size)

        val hpBefore = viewModel.bosses.single().hp
        viewModel.tryPop(Offset(bombs.first().x, bombs.first().y))

        val bossDamage = events.filterIsInstance<GameEvent.BossHit>().drop(6).sumOf { it.damage }
        assertTrue(bossDamage > 0, "bomb blast should reach the boss")
        assertTrue(viewModel.bosses.isEmpty() || viewModel.bosses.single().hp < hpBefore)
    }

    @Test
    fun skullInFrontOfTheBoss_isTappedFirst() = runTest {
        val viewModel = bossViewModel(boss(BossGimmick.THROWS_SKULLS, 20))
        val events = collectEvents(viewModel)
        repeat(GameRules.SKULL_EVERY_HITS) { viewModel.tapBoss() }
        val skull = viewModel.bubbles.single { it.type == BubbleType.POISON }

        viewModel.tryPop(Offset(skull.x, skull.y))

        assertTrue(viewModel.isGameOver)
        assertEquals(GameOverReason.POISON, events.filterIsInstance<GameEvent.GameOver>().single().reason)
    }

    @Test
    fun restart_bringsTheBossBack() = runTest {
        val viewModel = bossViewModel(boss(BossGimmick.SPLITS, 5))
        collectEvents(viewModel)
        viewModel.tapBoss()

        viewModel.restartGame()
        viewModel.update(FRAME)

        assertEquals(5, viewModel.bosses.single().hp)
        assertEquals(0, viewModel.objectives.first().current)
    }

    @Test
    fun bossTotals_includeEverySplitCopy() {
        assertEquals(20, Bosses.CLOUD_KING.totalHp)
        assertEquals(12 + 3 * 4 + 9 * 1, Bosses.GUMBALL_GIANT.totalHp)
    }
}
