package com.apps.bubblepopping

import androidx.compose.ui.geometry.Offset
import com.apps.bubblepopping.game.ComboTracker
import com.apps.bubblepopping.game.GameEvent
import com.apps.bubblepopping.game.GameOverReason
import com.apps.bubblepopping.game.GameRules
import com.apps.bubblepopping.game.SpawnConfig
import com.apps.bubblepopping.view.BubbleGameViewModel
import com.apps.bubblepopping.view.home.Difficulty
import com.apps.bubblepopping.view.play.component.Bubble
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
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BubbleGameViewModelTest {

    private companion object {
        const val FRAME = 0.016f
        const val WIDTH = 1080f
        const val HEIGHT = 2340f
        const val DENSITY = 2.75f

        val NORMAL_ONLY = SpawnConfig.MEDIUM.copy(typeWeights = mapOf(BubbleType.NORMAL to 1))
        val POISON_ONLY = SpawnConfig.MEDIUM.copy(typeWeights = mapOf(BubbleType.POISON to 1))
        val HEART_ONLY = SpawnConfig.MEDIUM.copy(typeWeights = mapOf(BubbleType.HEART to 1))

        /** Spawns many bubbles quickly so several can be popped within one combo window. */
        val FAST_NORMAL_ONLY = NORMAL_ONLY.copy(spawnInterval = 0.05f, minSpawnInterval = 0.05f)
        val FAST_NORMAL_AND_POISON = FAST_NORMAL_ONLY.copy(
            typeWeights = mapOf(BubbleType.NORMAL to 1, BubbleType.POISON to 1),
        )
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
        spawn: SpawnConfig = SpawnConfig.MEDIUM,
        seed: Long = 42L,
        progress: FakeProgressRepository = FakeProgressRepository(),
        width: Float = WIDTH,
        height: Float = HEIGHT,
        density: Float = DENSITY,
    ) = BubbleGameViewModel(
        difficulty = Difficulty.MEDIUM,
        progressRepository = progress,
        settingsRepository = FakeSettingsRepository(),
        spawn = spawn,
        seed = seed,
    ).apply { setCanvasBounds(width, height, topOffset = 0f, density = density) }

    private fun BubbleGameViewModel.runFor(seconds: Float) {
        repeat((seconds / FRAME).toInt()) { update(FRAME) }
    }

    private fun BubbleGameViewModel.runUntilBubble() {
        while (bubbles.isEmpty()) update(FRAME)
    }

    private fun BubbleGameViewModel.runUntilGameOver(maxSeconds: Float = 120f) {
        var elapsed = 0f
        while (!isGameOver && elapsed < maxSeconds) {
            update(FRAME)
            elapsed += FRAME
        }
    }

    private fun TestScope.collectEvents(viewModel: BubbleGameViewModel): List<GameEvent> {
        val events = mutableListOf<GameEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }
        return events
    }

    // ── Deterministic randomness ────────────────────────────────────────────

    @Test
    fun sameSeed_producesIdenticalRound() {
        val a = newViewModel(seed = 7L).apply { runFor(5f) }
        val b = newViewModel(seed = 7L).apply { runFor(5f) }

        assertTrue(a.bubbles.isNotEmpty())
        assertEquals(a.bubbles.toList(), b.bubbles.toList())
    }

    @Test
    fun restart_withSeed_replaysSameRound() {
        val viewModel = newViewModel(seed = 7L)
        viewModel.runFor(5f)
        val firstRun = viewModel.bubbles.toList()

        viewModel.restartGame()
        viewModel.runFor(5f)

        assertEquals(firstRun, viewModel.bubbles.toList())
    }

    // ── Density independence ────────────────────────────────────────────────

    @Test
    fun roundLooksTheSame_onDifferentScreenDensities() {
        val scale = 3.5f / DENSITY
        val small = newViewModel(seed = 3L)
        val large = newViewModel(
            seed = 3L,
            width = WIDTH * scale,
            height = HEIGHT * scale,
            density = 3.5f,
        )
        small.runFor(4f)
        large.runFor(4f)

        assertEquals(small.bubbles.size, large.bubbles.size)
        small.bubbles.zip(large.bubbles).forEach { (s, l) ->
            assertClose(s.radius / DENSITY, l.radius / 3.5f)
            assertClose(s.x / DENSITY, l.x / 3.5f)
            assertClose(s.y / DENSITY, l.y / 3.5f)
            assertEquals(s.size, l.size)
        }
    }

    // ── Popping & scoring ───────────────────────────────────────────────────

    @Test
    fun poppingNormalBubble_scoresBySize_andEmitsEvents() = runTest {
        val viewModel = newViewModel(spawn = NORMAL_ONLY)
        val events = collectEvents(viewModel)
        viewModel.runUntilBubble()
        val bubble = viewModel.bubbles.first()

        assertTrue(viewModel.tryPop(Offset(bubble.x, bubble.y)))

        assertEquals(bubble.size.points, viewModel.score)
        assertTrue(viewModel.bubbles.isEmpty())
        assertTrue(GameEvent.Tapped(hit = true) in events)
        assertTrue(GameEvent.Popped(bubble, bubble.size.points, combo = 1) in events)
        assertTrue(GameEvent.ComboChanged(combo = 1, previous = 0) in events)
    }

    @Test
    fun tappingEmptySpace_missesWithoutScoring() = runTest {
        val viewModel = newViewModel(spawn = NORMAL_ONLY)
        val events = collectEvents(viewModel)
        viewModel.runUntilBubble()

        assertFalse(viewModel.tryPop(Offset(-500f, -500f)))

        assertEquals(0, viewModel.score)
        assertEquals(listOf<GameEvent>(GameEvent.Tapped(hit = false)), events.filterIsInstance<GameEvent.Tapped>())
    }

    @Test
    fun poppingPoison_endsGame_andRecordsResult() = runTest {
        val progress = FakeProgressRepository()
        val viewModel = newViewModel(spawn = POISON_ONLY, progress = progress)
        val events = collectEvents(viewModel)
        viewModel.runUntilBubble()
        val bubble = viewModel.bubbles.first()

        viewModel.tryPop(Offset(bubble.x, bubble.y))

        assertTrue(viewModel.isGameOver)
        assertTrue(GameEvent.GameOver(GameOverReason.POISON, score = 0, isNewBest = false) in events)
        assertEquals(
            listOf(RecordedGame(Difficulty.MEDIUM, score = 0, bubblesPopped = 1, bestCombo = 0)),
            progress.recordedGames,
        )
    }

    // ── Combos ──────────────────────────────────────────────────────────────

    /** Taps the centre of the first bubble matching [predicate] and returns the Popped event. */
    private fun BubbleGameViewModel.popFirst(
        events: List<GameEvent>,
        predicate: (Bubble) -> Boolean = { true },
    ): GameEvent.Popped {
        val target = bubbles.first(predicate)
        assertTrue(tryPop(Offset(target.x, target.y)))
        return events.filterIsInstance<GameEvent.Popped>().last()
    }

    @Test
    fun quickPops_buildCombo_andMultiplyPoints() = runTest {
        val viewModel = newViewModel(spawn = FAST_NORMAL_ONLY)
        val events = collectEvents(viewModel)
        viewModel.runFor(0.5f)

        val pops = List(3) { viewModel.popFirst(events) }

        assertEquals(listOf(1, 2, 3), pops.map { it.combo })
        assertEquals(pops[2].bubble.size.points * 2, pops[2].points)   // combo 3 → ×2
        assertEquals(pops.sumOf { it.points }, viewModel.score)
        assertEquals(3, viewModel.combo)
        assertEquals(2, viewModel.comboMultiplier)
        assertTrue(viewModel.comboTimeLeft > 0f)
    }

    @Test
    fun combo_expiresAfterWindow() = runTest {
        val viewModel = newViewModel(spawn = FAST_NORMAL_ONLY)
        val events = collectEvents(viewModel)
        viewModel.runFor(0.5f)
        viewModel.popFirst(events)

        viewModel.runFor(ComboTracker.WINDOW_SECONDS + 0.1f)

        assertEquals(0, viewModel.combo)
        assertEquals(0f, viewModel.comboTimeLeft)
        assertTrue(GameEvent.ComboChanged(combo = 0, previous = 1, broken = false) in events)
    }

    @Test
    fun poison_breaksCombo_andBestComboIsRecorded() = runTest {
        val progress = FakeProgressRepository()
        val viewModel = newViewModel(spawn = FAST_NORMAL_AND_POISON, progress = progress)
        val events = collectEvents(viewModel)
        viewModel.runFor(0.6f)

        repeat(2) {
            val pop = viewModel.popFirst(events) { it.type == BubbleType.NORMAL }
            assertEquals(BubbleType.NORMAL, pop.bubble.type)
        }
        val poison = viewModel.popFirst(events) { it.type == BubbleType.POISON }
        assertEquals(BubbleType.POISON, poison.bubble.type)

        assertTrue(GameEvent.ComboChanged(combo = 0, previous = 2, broken = true) in events)
        assertTrue(viewModel.isGameOver)
        assertEquals(2, progress.recordedGames.single().bestCombo)
    }

    @Test
    fun comboMilestone_showsText_andBrieflyFreezesTheGame() = runTest {
        val viewModel = newViewModel(spawn = FAST_NORMAL_ONLY)
        val events = collectEvents(viewModel)
        viewModel.runFor(0.5f)
        repeat(5) { viewModel.popFirst(events) }
        val framesBefore = viewModel.frameCount

        viewModel.update(FRAME)   // inside the hit-stop

        assertTrue(viewModel.floatingTexts.any { it.isMilestone && it.text == "NICE!" })
        assertEquals(framesBefore, viewModel.frameCount)
    }

    @Test
    fun pop_addsFloatingScoreText() = runTest {
        val viewModel = newViewModel(spawn = FAST_NORMAL_ONLY)
        val events = collectEvents(viewModel)
        viewModel.runFor(0.5f)

        val pop = viewModel.popFirst(events)

        assertEquals(listOf("+${pop.points}"), viewModel.floatingTexts.map { it.text })
    }

    @Test
    fun effects_keepAnimatingAfterGameOver() = runTest {
        val viewModel = newViewModel(spawn = POISON_ONLY)
        val events = collectEvents(viewModel)
        viewModel.runUntilBubble()
        viewModel.popFirst(events)
        assertTrue(viewModel.isGameOver)
        assertTrue(viewModel.popAnimations.isNotEmpty())
        assertTrue(viewModel.shakeOffset != Offset.Zero || viewModel.popAnimations.isNotEmpty())

        viewModel.runFor(1f)

        assertTrue(viewModel.popAnimations.isEmpty())
        assertEquals(Offset.Zero, viewModel.shakeOffset)
    }

    // ── Lives ───────────────────────────────────────────────────────────────

    @Test
    fun escapedNormalBubbles_costLives_untilGameOver() = runTest {
        val viewModel = newViewModel(spawn = NORMAL_ONLY)
        val events = collectEvents(viewModel)

        viewModel.runUntilGameOver()

        assertTrue(viewModel.isGameOver)
        assertEquals(0, viewModel.livesLeft)
        assertEquals(GameRules.MAX_LIVES, events.count { it is GameEvent.Missed && it.lifeLost })
        assertEquals(GameOverReason.OUT_OF_LIVES, events.filterIsInstance<GameEvent.GameOver>().single().reason)
    }

    @Test
    fun escapedHeartBubbles_doNotCostLives() = runTest {
        val viewModel = newViewModel(spawn = HEART_ONLY)
        val events = collectEvents(viewModel)

        viewModel.runFor(60f)

        assertFalse(viewModel.isGameOver)
        assertEquals(GameRules.MAX_LIVES, viewModel.livesLeft)
        val missed = events.filterIsInstance<GameEvent.Missed>()
        assertTrue(missed.isNotEmpty())
        assertTrue(missed.none { it.lifeLost })
    }

    // ── Best score ──────────────────────────────────────────────────────────

    @Test
    fun beatingSavedBest_isNewBest() {
        val viewModel = newViewModel(spawn = NORMAL_ONLY, progress = FakeProgressRepository(best = null))
        popOneThenLose(viewModel)

        assertTrue(viewModel.score > 0)
        assertTrue(viewModel.isNewBest)
    }

    @Test
    fun notBeatingSavedBest_isNotNewBest() {
        val viewModel = newViewModel(spawn = NORMAL_ONLY, progress = FakeProgressRepository(best = 1_000))
        popOneThenLose(viewModel)

        assertFalse(viewModel.isNewBest)
    }

    private fun popOneThenLose(viewModel: BubbleGameViewModel) {
        viewModel.runUntilBubble()
        val bubble = viewModel.bubbles.first()
        viewModel.tryPop(Offset(bubble.x, bubble.y))
        viewModel.runUntilGameOver()
        assertTrue(viewModel.isGameOver)
    }

    // ── Ticks & pause ───────────────────────────────────────────────────────

    @Test
    fun tickEvents_emittedOncePerSecond() = runTest {
        val viewModel = newViewModel()
        val events = collectEvents(viewModel)

        viewModel.runFor(3.1f)

        assertEquals(listOf(1, 2, 3), events.filterIsInstance<GameEvent.Tick>().map { it.elapsedSec })
    }

    @Test
    fun paused_gameDoesNotAdvance() {
        val viewModel = newViewModel()
        viewModel.togglePause()

        viewModel.runFor(5f)

        assertTrue(viewModel.bubbles.isEmpty())
        assertEquals(0, viewModel.frameCount)
    }

    // ── SpawnConfig ─────────────────────────────────────────────────────────

    @Test
    fun spawnConfig_rejectsAllZeroWeights() {
        assertFailsWith<IllegalArgumentException> {
            SpawnConfig.MEDIUM.copy(typeWeights = mapOf(BubbleType.NORMAL to 0))
        }
    }

    private fun assertClose(expected: Float, actual: Float, tolerance: Float = 0.01f) {
        assertTrue(abs(expected - actual) <= tolerance, "expected $expected but was $actual")
    }
}
