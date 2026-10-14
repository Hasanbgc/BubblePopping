package com.apps.bubblepopping

import androidx.compose.ui.geometry.Offset
import com.apps.bubblepopping.data.StageProgress
import com.apps.bubblepopping.game.EndlessUnlocks
import com.apps.bubblepopping.game.GameEvent
import com.apps.bubblepopping.game.GameMode
import com.apps.bubblepopping.game.GameOverReason
import com.apps.bubblepopping.game.SpawnConfig
import com.apps.bubblepopping.game.WaveDirector
import com.apps.bubblepopping.stage.Objective
import com.apps.bubblepopping.stage.StageConfig
import com.apps.bubblepopping.stage.World
import com.apps.bubblepopping.view.BubbleGameViewModel
import com.apps.bubblepopping.view.home.Difficulty
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Phase 7: waves, unlocked bubble types and difficulties, and the Endless game over. */
@OptIn(ExperimentalCoroutinesApi::class)
class EndlessModeTest {

    private companion object {
        const val FRAME = 0.016f
        const val WIDTH = 1080f
        const val HEIGHT = 2340f
        const val DENSITY = 2.75f

        /** Hearts never cost a life, so a round of them runs for as long as needed. */
        val HEARTS_ONLY = SpawnConfig.MEDIUM.copy(typeWeights = mapOf(BubbleType.HEART to 1))
        val FAST_NORMAL_ONLY = SpawnConfig.MEDIUM.copy(
            spawnInterval = 0.05f,
            minSpawnInterval = 0.05f,
            typeWeights = mapOf(BubbleType.NORMAL to 1),
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
        spawn: SpawnConfig = HEARTS_ONLY,
        progress: FakeProgressRepository = FakeProgressRepository(),
        difficulty: Difficulty = Difficulty.MEDIUM,
        stage: StageConfig? = null,
        meta: FakeMetaRepository = FakeMetaRepository(progress),
    ) = BubbleGameViewModel(
        mode = stage?.let { GameMode.Stage(it.id) } ?: GameMode.Endless(difficulty),
        progressRepository = progress,
        settingsRepository = FakeSettingsRepository(),
        metaRepository = meta,
        stage = stage,
        spawn = spawn,
        seed = 42L,
    ).apply { setCanvasBounds(WIDTH, HEIGHT, topOffset = 0f, density = DENSITY) }

    private fun BubbleGameViewModel.runFor(seconds: Float) {
        repeat((seconds / FRAME).toInt()) { update(FRAME) }
    }

    private fun TestScope.collectEvents(viewModel: BubbleGameViewModel): List<GameEvent> {
        val events = mutableListOf<GameEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }
        return events
    }

    /** Pops every bubble until the score reaches [target], then lets bubbles escape until the round ends. */
    private fun BubbleGameViewModel.scoreThenDie(target: Int) {
        while (score < target && !isGameOver) {
            update(FRAME)
            bubbles.toList().forEach { tryPop(Offset(it.x, it.y)) }
        }
        while (!isGameOver) update(FRAME)
    }

    // ── Waves ───────────────────────────────────────────────────────────────

    @Test
    fun wave_endsInACalm_thenTheNextWaveStarts() = runTest {
        val viewModel = newViewModel()
        val events = collectEvents(viewModel)

        viewModel.runFor(WaveDirector.WAVE_SECONDS + 0.1f)
        assertTrue(viewModel.isWaveCalm)
        assertEquals(2, viewModel.wave)
        assertTrue(GameEvent.WaveCleared(1) in events)

        viewModel.runFor(WaveDirector.CALM_SECONDS)
        assertFalse(viewModel.isWaveCalm)
        assertTrue(GameEvent.WaveStarted(2) in events)
    }

    @Test
    fun nothingSpawns_duringTheCalm() {
        val viewModel = newViewModel()
        viewModel.runFor(WaveDirector.WAVE_SECONDS + 0.1f)
        val beforeCalm = viewModel.bubbles.map { it.id }.toSet()

        viewModel.runFor(WaveDirector.CALM_SECONDS - 0.3f)

        assertTrue(viewModel.isWaveCalm)
        assertTrue(viewModel.bubbles.all { it.id in beforeCalm })
    }

    @Test
    fun laterWaves_spawnFaster() {
        fun spawnsDuring(viewModel: BubbleGameViewModel, seconds: Float): Int {
            val seen = mutableSetOf<String>()
            repeat((seconds / FRAME).toInt()) {
                viewModel.update(FRAME)
                viewModel.bubbles.mapTo(seen) { it.id }
            }
            return seen.size
        }
        val viewModel = newViewModel()
        val firstWave = spawnsDuring(viewModel, WaveDirector.WAVE_SECONDS)
        viewModel.runFor(WaveDirector.CALM_SECONDS)
        // Waves 2–4 have passed the time limit.
        repeat(2) { viewModel.runFor(WaveDirector.WAVE_SECONDS + WaveDirector.CALM_SECONDS) }
        val fourthWave = spawnsDuring(viewModel, WaveDirector.WAVE_SECONDS)

        assertEquals(4, viewModel.wave)
        assertTrue(fourthWave > firstWave, "wave 4 spawned $fourthWave, wave 1 spawned $firstWave")
    }

    @Test
    fun stages_haveNoWaves() = runTest {
        val stage = StageConfig(
            id = 1,
            world = World.SKY,
            goal = Objective.Survive(60),
            bonusGoals = emptyList(),
            spawn = HEARTS_ONLY,
            introText = "",
            durationSec = 60,
        )
        val viewModel = newViewModel(stage = stage)
        val events = collectEvents(viewModel)

        viewModel.runFor(WaveDirector.WAVE_SECONDS + 1f)

        assertEquals(1, viewModel.wave)
        assertFalse(viewModel.isWaveCalm)
        assertTrue(events.none { it is GameEvent.WaveCleared || it is GameEvent.WaveStarted })
    }

    @Test
    fun restart_returnsToWaveOne() {
        val viewModel = newViewModel()
        viewModel.runFor(WaveDirector.WAVE_SECONDS + 0.1f)
        viewModel.restartGame()

        assertEquals(1, viewModel.wave)
        assertFalse(viewModel.isWaveCalm)
        assertEquals(0f, viewModel.waveProgress)
    }

    // ── Bubble types unlocked in Stages ─────────────────────────────────────

    private fun typesSeen(viewModel: BubbleGameViewModel, seconds: Float): Set<BubbleType> {
        val seen = mutableSetOf<BubbleType>()
        repeat((seconds / FRAME).toInt()) {
            viewModel.update(FRAME)
            viewModel.bubbles.mapTo(seen) { it.type }
        }
        return seen
    }

    @Test
    fun specialTypes_stayOutUntilUnlocked() {
        assertEquals(setOf(BubbleType.HEART), typesSeen(newViewModel(), seconds = 30f))
    }

    @Test
    fun clearingAStage_addsItsBubbleToEndless() {
        val goldenStage = EndlessUnlocks.introStageId(BubbleType.GOLDEN)!!
        val progress = FakeProgressRepository().apply {
            stages.value = mapOf(goldenStage to StageProgress(goldenStage, stars = 1, bestScore = 10))
        }

        val seen = typesSeen(newViewModel(progress = progress), seconds = 30f)

        assertTrue(BubbleType.GOLDEN in seen)
        assertFalse(BubbleType.BOMB in seen)
    }

    // ── Difficulty unlocks ──────────────────────────────────────────────────

    @Test
    fun reachingTheTargetOnMedium_unlocksHard() {
        val viewModel = newViewModel(spawn = FAST_NORMAL_ONLY, progress = FakeProgressRepository(best = 40))

        viewModel.scoreThenDie(EndlessUnlocks.HARD_UNLOCK_MEDIUM_SCORE)

        assertEquals(Difficulty.HARD, viewModel.unlockedDifficulty)
        viewModel.restartGame()
        assertNull(viewModel.unlockedDifficulty)
    }

    @Test
    fun hardStaysUnannounced_whenAlreadyUnlockedOrBelowTarget() {
        val alreadyOpen = newViewModel(
            spawn = FAST_NORMAL_ONLY,
            progress = FakeProgressRepository(best = EndlessUnlocks.HARD_UNLOCK_MEDIUM_SCORE),
        )
        alreadyOpen.scoreThenDie(EndlessUnlocks.HARD_UNLOCK_MEDIUM_SCORE + 10)
        assertNull(alreadyOpen.unlockedDifficulty)

        val tooLow = newViewModel(spawn = FAST_NORMAL_ONLY)
        tooLow.scoreThenDie(20)
        assertNull(tooLow.unlockedDifficulty)
    }

    @Test
    fun onlyMediumScores_unlockHard() {
        val viewModel = newViewModel(spawn = FAST_NORMAL_ONLY, difficulty = Difficulty.EASY)
        viewModel.scoreThenDie(EndlessUnlocks.HARD_UNLOCK_MEDIUM_SCORE)
        assertNull(viewModel.unlockedDifficulty)
    }

    // ── Game over ───────────────────────────────────────────────────────────

    @Test
    fun gameOver_keepsTheReason_andBubblesDriftInSlowMotion() {
        val viewModel = newViewModel(spawn = FAST_NORMAL_ONLY)
        viewModel.scoreThenDie(5)
        assertEquals(GameOverReason.OUT_OF_LIVES, viewModel.gameOverReason)

        val bubble = viewModel.bubbles.first()
        val startY = bubble.y
        viewModel.runFor(0.2f)
        val driftedY = bubble.y
        assertTrue(driftedY < startY, "bubbles keep rising for a moment")

        viewModel.runFor(1f)
        val settledY = bubble.y
        viewModel.runFor(0.5f)
        assertEquals(settledY, bubble.y, "the drift stops")

        viewModel.restartGame()
        assertNull(viewModel.gameOverReason)
    }

    @Test
    fun newBest_inEndless_throwsConfetti() {
        val viewModel = newViewModel(spawn = FAST_NORMAL_ONLY, progress = FakeProgressRepository(best = 3))
        viewModel.scoreThenDie(10)

        assertTrue(viewModel.isNewBest)
        assertTrue(viewModel.confetti.isNotEmpty())
    }
}
