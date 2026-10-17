package com.apps.bubblepopping

import androidx.compose.ui.geometry.Offset
import com.apps.bubblepopping.game.GameEvent
import com.apps.bubblepopping.game.GameMode
import com.apps.bubblepopping.game.GameOverReason
import com.apps.bubblepopping.game.GameRules
import com.apps.bubblepopping.game.SpawnConfig
import com.apps.bubblepopping.meta.MetaRules
import com.apps.bubblepopping.meta.MetaState
import com.apps.bubblepopping.meta.PlayerState
import com.apps.bubblepopping.stage.Objective
import com.apps.bubblepopping.stage.StageConfig
import com.apps.bubblepopping.stage.World
import com.apps.bubblepopping.view.BubbleGameViewModel
import com.apps.bubblepopping.view.home.Difficulty
import com.apps.bubblepopping.view.play.component.BubbleType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReviveTest {

    private companion object {
        const val FRAME = 0.016f
        const val WIDTH = 1080f
        const val HEIGHT = 2340f
        const val DENSITY = 2.75f
        const val RICH = 120

        val NORMAL_ONLY = SpawnConfig.MEDIUM.copy(typeWeights = mapOf(BubbleType.NORMAL to 1))
        val POISON_ONLY = SpawnConfig.MEDIUM.copy(typeWeights = mapOf(BubbleType.POISON to 1))
        val GOLDEN_ONLY = SpawnConfig.MEDIUM.copy(typeWeights = mapOf(BubbleType.GOLDEN to 1))
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(
        coins: Int = RICH,
        spawn: SpawnConfig = NORMAL_ONLY,
        stage: StageConfig? = null,
        progress: FakeProgressRepository = FakeProgressRepository(),
        meta: FakeMetaRepository = FakeMetaRepository(progress),
    ): Pair<BubbleGameViewModel, FakeMetaRepository> {
        meta.state.value = MetaState(player = PlayerState(coins = coins))
        val viewModel = BubbleGameViewModel(
            mode = stage?.let { GameMode.Stage(it.id) } ?: GameMode.Endless(Difficulty.MEDIUM),
            progressRepository = progress,
            settingsRepository = FakeSettingsRepository(),
            metaRepository = meta,
            stage = stage,
            spawn = stage?.spawn ?: spawn,
            seed = 42L,
        ).apply { setCanvasBounds(WIDTH, HEIGHT, topOffset = 0f, density = DENSITY) }
        return viewModel to meta
    }

    private fun BubbleGameViewModel.runFor(seconds: Float) {
        repeat((seconds / FRAME).toInt()) { update(FRAME) }
    }

    private fun BubbleGameViewModel.runUntilGameOver(maxSeconds: Float = 120f) {
        var elapsed = 0f
        while (!isGameOver && elapsed < maxSeconds) {
            update(FRAME)
            elapsed += FRAME
        }
        assertTrue(isGameOver, "round should have ended")
    }

    private fun BubbleGameViewModel.popFirstSkull() {
        while (bubbles.none { it.type == BubbleType.POISON }) update(FRAME)
        val skull = bubbles.first { it.type == BubbleType.POISON }
        tryPop(Offset(skull.x, skull.y))
    }

    private fun lostStage() = StageConfig(
        id = 1,
        world = World.SKY,
        goal = Objective.ReachScore(1_000_000),
        bonusGoals = emptyList(),
        // Golden bubbles cost no lives, so only the timer can end it.
        spawn = GOLDEN_ONLY,
        introText = "",
        durationSec = 3,
    )

    @Test
    fun spendCoins_takesCoinsOnlyWhenAffordable() {
        val state = MetaState(player = PlayerState(coins = 60))
        assertEquals(10, MetaRules.spendCoins(state, 50)?.player?.coins)
        assertNull(MetaRules.spendCoins(state, 61))
    }

    @Test
    fun withoutEnoughCoins_theRoundEndsAndSavesAsBefore() {
        val progress = FakeProgressRepository()
        val (viewModel, meta) = newViewModel(coins = GameRules.REVIVE_COST - 1, progress = progress)
        viewModel.runUntilGameOver()

        assertNull(viewModel.reviveOffer)
        assertEquals(1, meta.finishedRuns.size)
        assertEquals(1, progress.recordedGames.size)
    }

    @Test
    fun aLossWithCoins_offersARevive_andSavesNothingYet() = runTest {
        val progress = FakeProgressRepository()
        val (viewModel, meta) = newViewModel(progress = progress)
        val events = mutableListOf<GameEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.collect { events += it } }
        viewModel.runUntilGameOver()

        val offer = assertNotNull(viewModel.reviveOffer)
        assertEquals(GameOverReason.OUT_OF_LIVES, offer.reason)
        assertEquals(GameRules.REVIVE_COST, offer.cost)
        assertTrue(meta.finishedRuns.isEmpty())
        assertTrue(progress.recordedGames.isEmpty())
        assertTrue(events.none { it is GameEvent.GameOver })
        assertTrue(events.any { it is GameEvent.ReviveOffered })
        job.cancel()
    }

    @Test
    fun revivingOutOfLives_paysAndGivesLivesBack() {
        val (viewModel, meta) = newViewModel()
        viewModel.runUntilGameOver()
        viewModel.revive()

        assertFalse(viewModel.isGameOver)
        assertNull(viewModel.reviveOffer)
        assertTrue(viewModel.hasRevived)
        assertEquals(GameRules.REVIVE_LIVES, viewModel.livesLeft)
        assertEquals(RICH - GameRules.REVIVE_COST, meta.state.value.player.coins)
        assertTrue(meta.finishedRuns.isEmpty())
    }

    @Test
    fun revivingAfterASkull_clearsEverySkullOnScreen() {
        val (viewModel, _) = newViewModel(spawn = POISON_ONLY)
        viewModel.runFor(2f)
        viewModel.popFirstSkull()
        assertEquals(GameOverReason.POISON, viewModel.reviveOffer?.reason)

        viewModel.revive()
        assertFalse(viewModel.isGameOver)
        assertTrue(viewModel.bubbles.none { it.type == BubbleType.POISON })
    }

    @Test
    fun duringTheGracePeriod_skullsAndEscapesAreHarmless() {
        val (viewModel, _) = newViewModel(spawn = POISON_ONLY)
        viewModel.popFirstSkull()
        viewModel.revive()

        viewModel.popFirstSkull()
        assertFalse(viewModel.isGameOver)
        assertTrue(viewModel.reviveGraceLeft > 0f)
    }

    @Test
    fun decliningEndsTheRoundOnce_withoutTakingCoins() {
        val progress = FakeProgressRepository()
        val (viewModel, meta) = newViewModel(progress = progress)
        viewModel.runUntilGameOver()
        val coinsBefore = meta.state.value.player.coins
        viewModel.declineRevive()
        viewModel.declineRevive()

        assertTrue(viewModel.isGameOver)
        assertNull(viewModel.reviveOffer)
        assertEquals(GameOverReason.OUT_OF_LIVES, viewModel.gameOverReason)
        assertEquals(1, meta.finishedRuns.size)
        assertEquals(1, progress.recordedGames.size)
        assertNotNull(viewModel.rewards)
        // Rewards may add coins; nothing was taken.
        assertTrue(meta.state.value.player.coins >= coinsBefore)
    }

    @Test
    fun theOfferRunsOut_andTheRoundEnds() {
        val (viewModel, meta) = newViewModel()
        viewModel.runUntilGameOver()
        viewModel.runFor(GameRules.REVIVE_OFFER_SECONDS - 1f)
        assertNotNull(viewModel.reviveOffer)

        viewModel.runFor(1.5f)
        assertNull(viewModel.reviveOffer)
        assertEquals(1, meta.finishedRuns.size)
    }

    @Test
    fun onlyOneRevivePerRound() {
        val (viewModel, meta) = newViewModel(coins = RICH * 2)
        viewModel.runUntilGameOver()
        viewModel.revive()
        viewModel.runUntilGameOver()

        assertNull(viewModel.reviveOffer)
        assertEquals(1, meta.finishedRuns.size)
    }

    @Test
    fun restartingAllowsAReviveAgain() {
        val (viewModel, _) = newViewModel(coins = RICH * 2)
        viewModel.runUntilGameOver()
        viewModel.revive()
        viewModel.runUntilGameOver()
        viewModel.restartGame()

        assertFalse(viewModel.hasRevived)
        viewModel.runUntilGameOver()
        assertNotNull(viewModel.reviveOffer)
    }

    @Test
    fun aLostTimedStage_revivesWithExtraTime() {
        val progress = FakeProgressRepository()
        val (viewModel, _) = newViewModel(stage = lostStage(), progress = progress)
        viewModel.runUntilGameOver()
        assertEquals(GameOverReason.TIME_UP, viewModel.reviveOffer?.reason)
        assertNull(viewModel.stageResult)

        viewModel.revive()
        assertFalse(viewModel.isGameOver)
        assertEquals(GameRules.REVIVE_EXTRA_SECONDS, viewModel.timeLeftSec)

        viewModel.runFor(GameRules.REVIVE_EXTRA_SECONDS - 1f)
        assertFalse(viewModel.isGameOver)
        viewModel.runUntilGameOver()
        assertNotNull(viewModel.stageResult)
        assertEquals(1, progress.recordedStages.size)
    }
}
