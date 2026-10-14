package com.apps.bubblepopping

import androidx.compose.ui.geometry.Offset
import com.apps.bubblepopping.data.StageProgress
import com.apps.bubblepopping.game.EndlessUnlocks
import com.apps.bubblepopping.game.GameMode
import com.apps.bubblepopping.meta.DailyStageGenerator
import com.apps.bubblepopping.stage.World
import com.apps.bubblepopping.view.BubbleGameViewModel
import com.apps.bubblepopping.view.home.Difficulty
import com.apps.bubblepopping.view.play.component.BubbleType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Phase 8 in the game itself: daily modes, run summaries and rewards. */
@OptIn(ExperimentalCoroutinesApi::class)
class DailyModesTest {

    private companion object {
        const val FRAME = 0.016f
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
        mode: GameMode,
        progress: FakeProgressRepository = FakeProgressRepository(),
        meta: FakeMetaRepository = FakeMetaRepository(progress),
    ) = BubbleGameViewModel(
        mode = mode,
        progressRepository = progress,
        settingsRepository = FakeSettingsRepository(),
        metaRepository = meta,
    ).apply { setCanvasBounds(1080f, 2340f, topOffset = 0f, density = 2.75f) }

    private fun BubbleGameViewModel.runFor(seconds: Float) {
        repeat((seconds / FRAME).toInt()) { update(FRAME) }
    }

    private fun BubbleGameViewModel.playUntilOver() {
        var elapsed = 0f
        while (!isGameOver && elapsed < 200f) {
            update(FRAME)
            elapsed += FRAME
        }
    }

    @Test
    fun dailyChallenge_isTheSameRoundAllDay() {
        val a = newViewModel(GameMode.DailyChallenge(20_000)).apply { runFor(5f) }
        val b = newViewModel(GameMode.DailyChallenge(20_000)).apply { runFor(5f) }
        val tomorrow = newViewModel(GameMode.DailyChallenge(20_001)).apply { runFor(5f) }

        assertTrue(a.bubbles.isNotEmpty())
        assertEquals(a.bubbles.toList(), b.bubbles.toList())
        assertNotEquals(a.bubbles.toList(), tomorrow.bubbles.toList())
    }

    @Test
    fun dailyChallenge_ignoresBubblesUnlockedInStages() {
        val progress = FakeProgressRepository().apply {
            val golden = EndlessUnlocks.introStageId(BubbleType.GOLDEN)!!
            stages.value = mapOf(golden to StageProgress(golden, stars = 3, bestScore = 10))
        }
        val viewModel = newViewModel(GameMode.DailyChallenge(20_000), progress)
        val seen = mutableSetOf<BubbleType>()
        repeat((40f / FRAME).toInt()) {
            viewModel.update(FRAME)
            viewModel.bubbles.mapTo(seen) { it.type }
        }
        assertTrue(BubbleType.GOLDEN !in seen)
    }

    @Test
    fun dailyStage_playsTheGeneratedStage_andIsNotSavedAsAStage() {
        val progress = FakeProgressRepository()
        val meta = FakeMetaRepository(progress)
        val mode = GameMode.DailyStage(20_000, World.OCEAN)
        val viewModel = newViewModel(mode, progress, meta)

        assertEquals(DailyStageGenerator.generate(20_000, World.OCEAN), viewModel.stage)
        viewModel.playUntilOver()

        assertTrue(viewModel.isGameOver)
        assertNotNull(viewModel.stageResult)
        assertTrue(progress.recordedStages.isEmpty())
        assertEquals(1, progress.dailyGames)
        assertEquals(mode, meta.finishedRuns.single().mode)
    }

    @Test
    fun finishedRound_isPaidOut_withItsStats() {
        val meta = FakeMetaRepository()
        val viewModel = newViewModel(GameMode.Endless(Difficulty.MEDIUM), meta = meta)
        // Pop for a while, then let bubbles escape.
        repeat((20f / FRAME).toInt()) {
            viewModel.update(FRAME)
            viewModel.bubbles.filter { it.type == BubbleType.NORMAL }.forEach { viewModel.tryPop(Offset(it.x, it.y)) }
        }
        viewModel.playUntilOver()

        val run = meta.finishedRuns.single()
        assertEquals(viewModel.score, run.score)
        assertTrue(run.stats.pops > 0)
        assertTrue(run.stats.elapsedSec >= 20)
        val rewards = assertNotNull(viewModel.rewards)
        assertEquals(viewModel.score / 10, rewards.lines.first { it.label.startsWith("Score") }.coins)
        assertEquals(rewards.coins, meta.state.value.player.coins)

        viewModel.restartGame()
        assertEquals(null, viewModel.rewards)
    }
}
