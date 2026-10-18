package com.apps.bubblepopping

import androidx.compose.ui.geometry.Offset
import com.apps.bubblepopping.game.GameEvent
import com.apps.bubblepopping.game.GameMode
import com.apps.bubblepopping.game.GameOverReason
import com.apps.bubblepopping.game.GameRules
import com.apps.bubblepopping.game.SpawnConfig
import com.apps.bubblepopping.stage.BossConfig
import com.apps.bubblepopping.stage.BossTrick
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
        /** Time for a freshly arrived boss to glide down into place. */
        const val BOSS_SETTLE_SECONDS = 2f
        const val SKULL_EVERY = 4
        const val STING_EVERY = 6
        const val STING_SECONDS = 1f
        const val BOMBS = 3

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

    /** A test boss with [hp] per generation, using the same [tricks] in all three phases. */
    private fun boss(vararg hp: Int, tricks: List<BossTrick> = emptyList(), enrageAfterSec: Int? = null) = BossConfig(
        id = "test",
        name = "Test Boss",
        hpPerGeneration = hp.toList(),
        phases = List(BossConfig.PHASES) { tricks },
        enrageAfterSec = enrageAfterSec,
    )

    private fun bossViewModel(
        boss: BossConfig,
        progress: FakeProgressRepository = FakeProgressRepository(),
        bonusGoals: List<Objective> = emptyList(),
        spawn: SpawnConfig = NO_BUBBLES,
    ): BubbleGameViewModel {
        val stage = StageConfig(
            id = 1,
            world = World.SKY,
            goal = Objective.DefeatBoss(boss),
            bonusGoals = bonusGoals,
            spawn = spawn,
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
            // Normal bubbles open the round; the boss arrives, then glides into place.
            runFor(GameRules.BOSS_ENTRY_DELAY_SECONDS + BOSS_SETTLE_SECONDS)
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
        val viewModel = bossViewModel(boss(10))
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
        val viewModel = bossViewModel(boss(3), progress, bonusGoals = listOf(Objective.NoMisses))
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
        val viewModel = bossViewModel(boss(2, 1, 1))
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
    fun cloudKing_throwsASkull_everyFewHits() = runTest {
        val viewModel = bossViewModel(boss(20, tricks = listOf(BossTrick.ThrowSkulls(SKULL_EVERY))))
        collectEvents(viewModel)

        repeat(SKULL_EVERY - 1) { viewModel.tapBoss() }
        assertTrue(viewModel.bubbles.none { it.type == BubbleType.POISON })

        viewModel.tapBoss()
        val skull = viewModel.bubbles.single { it.type == BubbleType.POISON }
        assertTrue(skull.y > viewModel.bosses.single().y)   // sent up from the bottom
    }

    @Test
    fun jellyQueen_stingLocksTaps_withoutCountingMisses() = runTest {
        val viewModel = bossViewModel(boss(20, tricks = listOf(BossTrick.Sting(STING_EVERY, STING_SECONDS))), bonusGoals = listOf(Objective.NoMisses))
        collectEvents(viewModel)
        repeat(STING_EVERY) { viewModel.tapBoss() }
        assertTrue(viewModel.tapLockLeft > 0f)
        val hpAfterSting = viewModel.bosses.single().hp

        assertFalse(viewModel.tapBoss())   // locked
        viewModel.onMissedTap()            // ignored while locked

        assertEquals(hpAfterSting, viewModel.bosses.single().hp)
        assertTrue(viewModel.objectives.last().satisfied)   // NoMisses still intact

        viewModel.runFor(STING_SECONDS + 0.1f)
        assertTrue(viewModel.tapBoss())
    }

    @Test
    fun nebulaCore_dropsBombs_belowAThirdHp_andBombsHurtIt() = runTest {
        val nebula = BossConfig(
            id = "test",
            name = "Test Boss",
            hpPerGeneration = listOf(9),
            phases = listOf(emptyList(), emptyList(), listOf(BossTrick.DropBombs(BOMBS))),
        )
        val viewModel = bossViewModel(nebula)
        val events = collectEvents(viewModel)

        repeat(6) { viewModel.tapBoss() }   // 6 of 9 damage → last third
        val bombs = viewModel.bubbles.filter { it.type == BubbleType.BOMB }
        assertEquals(BOMBS, bombs.size)

        val hpBefore = viewModel.bosses.single().hp
        viewModel.tryPop(Offset(bombs.first().x, bombs.first().y))

        val bossDamage = events.filterIsInstance<GameEvent.BossHit>().drop(6).sumOf { it.damage }
        assertTrue(bossDamage > 0, "bomb blast should reach the boss")
        assertTrue(viewModel.bosses.isEmpty() || viewModel.bosses.single().hp < hpBefore)
    }

    @Test
    fun skullInFrontOfTheBoss_isTappedFirst() = runTest {
        val viewModel = bossViewModel(boss(20, tricks = listOf(BossTrick.ThrowSkulls(SKULL_EVERY))))
        val events = collectEvents(viewModel)
        repeat(SKULL_EVERY) { viewModel.tapBoss() }
        val skull = viewModel.bubbles.single { it.type == BubbleType.POISON }
        // Float it up in front of the boss.
        val boss = viewModel.bosses.single()
        skull.x = boss.x
        skull.y = boss.y

        viewModel.tryPop(Offset(skull.x, skull.y))

        assertTrue(viewModel.isGameOver)
        assertEquals(GameOverReason.POISON, events.filterIsInstance<GameEvent.GameOver>().single().reason)
    }

    @Test
    fun restart_bringsTheBossBack() = runTest {
        val viewModel = bossViewModel(boss(5))
        collectEvents(viewModel)
        viewModel.tapBoss()

        viewModel.restartGame()
        viewModel.update(FRAME)
        assertTrue(viewModel.bosses.isEmpty())   // normal bubbles open the round again

        viewModel.runFor(GameRules.BOSS_ENTRY_DELAY_SECONDS + 0.1f)
        assertEquals(5, viewModel.bosses.single().hp)
        assertEquals(0, viewModel.objectives.first().current)
    }

    @Test
    fun iceShield_blocksTaps_untilAnIceBubbleIsPopped() = runTest {
        val viewModel = bossViewModel(boss(30, tricks = listOf(BossTrick.IceShield(everyHits = 2))))
        collectEvents(viewModel)
        repeat(2) { viewModel.tapBoss() }
        val boss = viewModel.bosses.single()
        assertTrue(boss.shielded)
        val ice = viewModel.bubbles.single { it.type == BubbleType.FREEZE }   // rises with the shield

        val hpShielded = boss.hp
        viewModel.tapBoss()
        assertEquals(hpShielded, boss.hp)

        viewModel.tryPop(Offset(ice.x, ice.y))
        assertFalse(boss.shielded)
        viewModel.tapBoss()
        assertEquals(hpShielded - 1, boss.hp)
    }

    @Test
    fun gust_blowsEveryBubbleToOneSide() = runTest {
        val viewModel = bossViewModel(boss(30, tricks = listOf(BossTrick.Gust(everyHits = 1))))
        collectEvents(viewModel)
        viewModel.tapBoss()
        assertEquals(1f, kotlin.math.abs(viewModel.breeze))
    }

    @Test
    fun bossEnrages_afterItsTime_andThenItsTricksComeTwiceAsOften() = runTest {
        val viewModel = bossViewModel(boss(60, tricks = listOf(BossTrick.ThrowSkulls(SKULL_EVERY)), enrageAfterSec = 5))
        collectEvents(viewModel)
        assertFalse(viewModel.bosses.single().enraged)

        viewModel.runFor(5.1f)
        assertTrue(viewModel.bosses.single().enraged)
        val skullsBefore = viewModel.bubbles.count { it.type == BubbleType.POISON }
        repeat(SKULL_EVERY / 2) { viewModel.tapBoss() }
        assertEquals(skullsBefore + 1, viewModel.bubbles.count { it.type == BubbleType.POISON })
    }

    @Test
    fun bossTotals_includeEverySplitCopy() {
        assertEquals(150, Bosses.CLOUD_KING.totalHp)
        assertEquals(110 + 3 * 35 + 9 * 12, Bosses.GUMBALL_GIANT.totalHp)
    }

    @Test
    fun plates_takeTheirTaps_beforeAnyHp() = runTest {
        val viewModel = bossViewModel(boss(30, tricks = listOf(BossTrick.Plates(count = 2, tapsEach = 3))))
        collectEvents(viewModel)
        val boss = viewModel.bosses.single()
        assertEquals(2, boss.plates)

        repeat(6) { viewModel.tapBoss() }
        assertEquals(0, boss.plates)
        assertEquals(30, boss.hp)
        viewModel.tapBoss()
        assertEquals(29, boss.hp)
    }

    @Test
    fun blastArmor_ignoresTaps_dropsBombs_andBlastsGetThrough() = runTest {
        val armored = BossConfig(
            id = "test",
            name = "Test Boss",
            hpPerGeneration = listOf(30),
            phases = List(BossConfig.PHASES) { listOf(BossTrick.BlastArmor(everySec = 0.5f, bombs = 2)) },
            blastDamage = 5,
        )
        val viewModel = bossViewModel(armored)
        collectEvents(viewModel)
        val boss = viewModel.bosses.single()
        assertTrue(boss.blastOnly)

        viewModel.tapBoss()
        assertEquals(30, boss.hp)

        var waited = 0f
        while (viewModel.bubbles.none { it.type == BubbleType.BOMB } && waited < 2f) {
            viewModel.update(FRAME)
            waited += FRAME
        }
        val bomb = viewModel.bubbles.first { it.type == BubbleType.BOMB }
        bomb.x = boss.x
        bomb.y = boss.y + boss.radius
        bomb.baseX = boss.x
        viewModel.tryPop(Offset(bomb.x, bomb.y))
        assertTrue(boss.hp < 30)
    }

    @Test
    fun healOnEscape_healsTheBoss_whenALifeIsLost() = runTest {
        val healer = BossConfig(
            id = "test",
            name = "Test Boss",
            hpPerGeneration = listOf(30),
            phases = List(BossConfig.PHASES) { listOf(BossTrick.HealOnEscape(hp = 4)) },
        )
        val viewModel = bossViewModel(healer, spawn = SpawnConfig.MEDIUM.copy(spawnInterval = 0.3f, minSpawnInterval = 0.3f, typeWeights = mapOf(BubbleType.NORMAL to 1)))
        collectEvents(viewModel)
        repeat(8) { viewModel.tapBoss() }
        val boss = viewModel.bosses.single()
        val hurt = boss.hp

        while (viewModel.missedCount == 0 && !viewModel.isGameOver) viewModel.update(FRAME)
        assertTrue(boss.hp > hurt)
    }

    @Test
    fun gravity_pullsBubblesTowardsTheBoss() = runTest {
        val viewModel = bossViewModel(
            boss(30, tricks = listOf(BossTrick.Gravity(strength = 2f))),
            spawn = SpawnConfig.MEDIUM.copy(spawnInterval = 0.2f, minSpawnInterval = 0.2f, typeWeights = mapOf(BubbleType.HEART to 1)),
        )
        collectEvents(viewModel)
        val boss = viewModel.bosses.single()
        viewModel.runFor(1f)
        val spread = viewModel.bubbles.map { kotlin.math.abs(it.baseX - boss.x) }.average()
        assertTrue(spread < 1080f / 4f, "bubbles should gather under the boss, spread $spread")
    }
}
