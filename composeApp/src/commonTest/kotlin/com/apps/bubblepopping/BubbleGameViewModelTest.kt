package com.apps.bubblepopping

import com.apps.bubblepopping.stage.StageCatalog
import com.apps.bubblepopping.meta.Booster
import androidx.compose.ui.geometry.Offset
import com.apps.bubblepopping.game.ComboTracker
import com.apps.bubblepopping.game.GameEvent
import com.apps.bubblepopping.game.GameMode
import com.apps.bubblepopping.game.GameOverReason
import com.apps.bubblepopping.game.GameRules
import com.apps.bubblepopping.game.SpawnConfig
import com.apps.bubblepopping.stage.Objective
import com.apps.bubblepopping.stage.StageConfig
import com.apps.bubblepopping.stage.World
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
import com.apps.bubblepopping.game.BubbleSize
import com.apps.bubblepopping.game.Pace
import com.apps.bubblepopping.game.WaveDirector
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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

    /** [spawn] defaults to the stage's spawn, or MEDIUM in Endless. */
    private fun newViewModel(
        spawn: SpawnConfig? = null,
        seed: Long = 42L,
        progress: FakeProgressRepository = FakeProgressRepository(),
        width: Float = WIDTH,
        height: Float = HEIGHT,
        density: Float = DENSITY,
        stage: StageConfig? = null,
    ) = BubbleGameViewModel(
        mode = stage?.let { GameMode.Stage(it.id) } ?: GameMode.Endless(Difficulty.MEDIUM),
        progressRepository = progress,
        settingsRepository = FakeSettingsRepository(),
        metaRepository = FakeMetaRepository(),
        stage = stage,
        spawn = spawn ?: stage?.spawn ?: SpawnConfig.MEDIUM,
        seed = seed,
    ).apply { setCanvasBounds(width, height, topOffset = 0f, density = density) }

    private fun testStage(
        goal: Objective,
        bonusGoals: List<Objective> = emptyList(),
        durationSec: Int? = null,
        lives: Int = GameRules.MAX_LIVES,
    ) = StageConfig(
        id = 1,
        world = World.SKY,
        goal = goal,
        bonusGoals = bonusGoals,
        spawn = FAST_NORMAL_ONLY,
        introText = "",
        durationSec = durationSec,
        lives = lives,
    )

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

        // A press on empty space isn't a miss yet — it might become a swipe.
        assertFalse(viewModel.tryPop(Offset(-500f, -500f)))
        assertTrue(events.filterIsInstance<GameEvent.Tapped>().isEmpty())

        viewModel.onMissedTap()

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

    // ── Stages ──────────────────────────────────────────────────────────────

    @Test
    fun untimedStage_endsWhenGoalMet_withStarsAndSavedResult() = runTest {
        val progress = FakeProgressRepository()
        val viewModel = newViewModel(
            stage = testStage(goal = Objective.PopCount(2), bonusGoals = listOf(Objective.NoMisses)),
            progress = progress,
        )
        val events = collectEvents(viewModel)
        viewModel.runFor(0.5f)

        viewModel.popFirst(events)
        assertFalse(viewModel.isGameOver)
        viewModel.popFirst(events)

        assertTrue(viewModel.isGameOver)
        assertEquals(GameOverReason.GOAL_COMPLETE, events.filterIsInstance<GameEvent.GameOver>().single().reason)
        val result = assertNotNull(viewModel.stageResult)
        assertTrue(result.won)
        assertEquals(2, result.stars)
        assertEquals(2, progress.recordedStages.single().stars)
        assertEquals(2, progress.stages.value.getValue(1).stars)
    }

    @Test
    fun brokenConstraint_costsABonusStar() = runTest {
        val viewModel = newViewModel(
            stage = testStage(goal = Objective.PopCount(1), bonusGoals = listOf(Objective.NoMisses)),
        )
        val events = collectEvents(viewModel)
        viewModel.runFor(0.5f)

        viewModel.onMissedTap()
        viewModel.popFirst(events)

        assertEquals(1, viewModel.stageResult?.stars)
    }

    @Test
    fun timedStage_failsWhenTimeRunsOutBeforeGoal() = runTest {
        val progress = FakeProgressRepository()
        val viewModel = newViewModel(
            stage = testStage(goal = Objective.ReachScore(10_000), durationSec = 2),
            progress = progress,
        )
        val events = collectEvents(viewModel)
        assertEquals(2, viewModel.timeLeftSec)

        viewModel.runFor(2.1f)

        assertTrue(viewModel.isGameOver)
        assertEquals(0, viewModel.timeLeftSec)
        assertEquals(GameOverReason.TIME_UP, events.filterIsInstance<GameEvent.GameOver>().single().reason)
        assertEquals(false, viewModel.stageResult?.won)
        assertEquals(0, progress.recordedStages.single().stars)
        assertTrue(progress.stages.value.isEmpty())   // a failed stage is not saved as progress
    }

    @Test
    fun timedStage_keepsRunningAfterGoal_untilBonusIsMetToo() = runTest {
        val viewModel = newViewModel(
            stage = testStage(
                goal = Objective.PopCount(1),
                bonusGoals = listOf(Objective.PopCount(3)),
                durationSec = 30,
            ),
        )
        val events = collectEvents(viewModel)
        viewModel.runFor(0.5f)

        viewModel.popFirst(events)
        assertFalse(viewModel.isGameOver)          // goal met, bonus not yet
        assertTrue(viewModel.objectives.first().satisfied)

        repeat(2) { viewModel.popFirst(events) }

        assertTrue(viewModel.isGameOver)           // every objective met → ends early
        assertEquals(2, viewModel.stageResult?.stars)
    }

    @Test
    fun surviveStage_winsWhenTimerEnds() = runTest {
        val viewModel = newViewModel(
            stage = testStage(goal = Objective.Survive(2), durationSec = 2),
            spawn = HEART_ONLY,   // hearts never cost lives
        )
        collectEvents(viewModel)

        viewModel.runFor(2.1f)

        assertEquals(true, viewModel.stageResult?.won)
    }

    @Test
    fun stageLives_comeFromConfig() {
        val viewModel = newViewModel(stage = testStage(goal = Objective.PopCount(100), lives = 2))

        assertEquals(2, viewModel.livesLeft)
    }

    @Test
    fun restart_resetsStageState() = runTest {
        val viewModel = newViewModel(stage = testStage(goal = Objective.PopCount(1), durationSec = 30))
        val events = collectEvents(viewModel)
        viewModel.runFor(0.5f)
        viewModel.popFirst(events)

        viewModel.restartGame()

        assertEquals(null, viewModel.stageResult)
        assertEquals(30, viewModel.timeLeftSec)
        assertEquals(0, viewModel.objectives.first().current)
    }

    // ── Special bubbles ─────────────────────────────────────────────────────

    private fun fastOnly(vararg types: BubbleType) =
        FAST_NORMAL_ONLY.copy(typeWeights = types.associateWith { 1 })

    @Test
    fun golden_isSmall_andWorth25() = runTest {
        val viewModel = newViewModel(spawn = fastOnly(BubbleType.GOLDEN))
        val events = collectEvents(viewModel)
        viewModel.runFor(0.5f)

        val pop = viewModel.popFirst(events)

        assertEquals(BubbleSize.SMALL, pop.bubble.size)
        assertEquals(GameRules.GOLDEN_POINTS, pop.points)
    }

    @Test
    fun freeze_slowsBubbles_forAFewSeconds() = runTest {
        val viewModel = newViewModel(spawn = fastOnly(BubbleType.FREEZE))
        val events = collectEvents(viewModel)
        viewModel.runFor(0.5f)
        viewModel.popFirst(events)
        assertEquals(GameRules.FREEZE_SECONDS, viewModel.freezeTimeLeft)

        val bubble = viewModel.bubbles.first()
        val yBefore = bubble.y
        viewModel.update(FRAME)

        assertEquals(bubble.speed * FRAME * GameRules.FREEZE_SLOWDOWN, yBefore - bubble.y, 0.01f)
        viewModel.runFor(GameRules.FREEZE_SECONDS)
        assertEquals(0f, viewModel.freezeTimeLeft)
    }

    @Test
    fun shield_absorbsTheNextSkull() = runTest {
        val viewModel = newViewModel(spawn = fastOnly(BubbleType.SHIELD, BubbleType.POISON))
        val events = collectEvents(viewModel)
        viewModel.runFor(0.6f)

        viewModel.popFirst(events) { it.type == BubbleType.SHIELD }
        assertTrue(viewModel.shieldActive)
        viewModel.popFirst(events) { it.type == BubbleType.POISON }

        assertFalse(viewModel.isGameOver)
        assertFalse(viewModel.shieldActive)
        assertTrue(GameEvent.ShieldUsed in events)

        viewModel.popFirst(events) { it.type == BubbleType.POISON }
        assertTrue(viewModel.isGameOver)   // no shield left
    }

    @Test
    fun splitter_burstsIntoThreeSmallBubbles() = runTest {
        val viewModel = newViewModel(spawn = fastOnly(BubbleType.SPLITTER))
        val events = collectEvents(viewModel)
        viewModel.runFor(0.3f)
        val before = viewModel.bubbles.size

        val pop = viewModel.popFirst(events)

        assertEquals(before - 1 + GameRules.SPLIT_COUNT, viewModel.bubbles.size)
        val fragments = viewModel.bubbles.filter { abs(it.y - pop.bubble.y) < 0.01f }
        assertEquals(GameRules.SPLIT_COUNT, fragments.size)
        assertTrue(fragments.all { it.type == BubbleType.NORMAL && it.size == BubbleSize.SMALL })
    }

    @Test
    fun bomb_popsEverythingInReach_andChains() = runTest {
        val viewModel = newViewModel(spawn = fastOnly(BubbleType.BOMB, BubbleType.NORMAL))
        val events = collectEvents(viewModel)
        viewModel.runFor(0.5f)
        val bomb = viewModel.bubbles.first { it.type == BubbleType.BOMB }
        val blast = GameRules.BOMB_RADIUS_DP * DENSITY
        val inReach = viewModel.bubbles.filter {
            it !== bomb && hypot(it.x - bomb.x, it.y - bomb.y) - it.radius <= blast
        }
        assertTrue(inReach.isNotEmpty(), "test setup: nothing near the bomb")

        assertTrue(viewModel.tryPop(Offset(bomb.x, bomb.y)))

        assertTrue(viewModel.bubbles.none { it in inReach })
        val chain = events.filterIsInstance<GameEvent.BombExploded>().single().chainSize
        assertTrue(chain >= inReach.size)
        assertFalse(viewModel.isGameOver)
    }

    @Test
    fun bomb_thatCatchesASkull_endsTheRound() = runTest {
        val viewModel = newViewModel(spawn = fastOnly(BubbleType.BOMB, BubbleType.POISON))
        val events = collectEvents(viewModel)
        viewModel.runFor(0.5f)
        val blast = GameRules.BOMB_RADIUS_DP * DENSITY
        val bomb = viewModel.bubbles.first { candidate ->
            candidate.type == BubbleType.BOMB && viewModel.bubbles.any {
                it.type == BubbleType.POISON && hypot(it.x - candidate.x, it.y - candidate.y) - it.radius <= blast
            }
        }

        // Tap the bomb exactly; if a skull is the first match at that point, the round ends either way.
        viewModel.tryPop(Offset(bomb.x, bomb.y))

        assertTrue(viewModel.isGameOver)
        assertEquals(GameOverReason.POISON, events.filterIsInstance<GameEvent.GameOver>().single().reason)
    }

    @Test
    fun wind_blowsSkullsOffScreen() = runTest {
        val viewModel = newViewModel(spawn = fastOnly(BubbleType.POISON))
        val events = collectEvents(viewModel)

        repeat((3f / FRAME).toInt()) {
            viewModel.swipeWind(1f)
            viewModel.update(FRAME)
        }

        assertTrue(events.any { it is GameEvent.WindSave })
        assertFalse(viewModel.isGameOver)
    }

    @Test
    fun wind_keepsOtherBubblesOnScreen() = runTest {
        val viewModel = newViewModel(spawn = FAST_NORMAL_ONLY)
        collectEvents(viewModel)

        repeat((3f / FRAME).toInt()) {
            viewModel.swipeWind(1f)
            viewModel.update(FRAME)
        }

        assertTrue(viewModel.bubbles.isNotEmpty())
        assertTrue(viewModel.bubbles.all { it.baseX <= WIDTH - it.radius + 0.01f })
    }

    @Test
    fun swipeWind_scalesWithDistance_andOnlyStrengthensInSameDirection() {
        val viewModel = newViewModel()

        viewModel.swipeWind(GameRules.FULL_WIND_SWIPE_FRACTION / 2)
        assertEquals(0.5f, viewModel.breeze, 0.001f)

        viewModel.swipeWind(GameRules.FULL_WIND_SWIPE_FRACTION / 10)   // weaker, same way
        assertEquals(0.5f, viewModel.breeze, 0.001f)

        viewModel.swipeWind(-GameRules.FULL_WIND_SWIPE_FRACTION / 10)  // other way
        assertEquals(-0.1f, viewModel.breeze, 0.001f)

        viewModel.swipeWind(5f)
        assertEquals(1f, viewModel.breeze)
    }

    // ── SpawnConfig ─────────────────────────────────────────────────────────

    @Test
    fun spawnConfig_rejectsAllZeroWeights() {
        assertFailsWith<IllegalArgumentException> {
            SpawnConfig.MEDIUM.copy(typeWeights = mapOf(BubbleType.NORMAL to 0))
        }
    }
    // ── Paced stages ────────────────────────────────────────────────────────

    @Test
    fun pacedStage_spawnsNothingDuringItsCalm_andDoesNotAnnounceWaves() = runTest {
        val stage = StageConfig(
            id = 1,
            world = World.SKY,
            goal = Objective.Survive(60),
            bonusGoals = emptyList(),
            // Hearts never cost a life, so the round keeps going.
            spawn = SpawnConfig.paced(Pace.P10, typeWeights = mapOf(BubbleType.HEART to 1)),
            introText = "",
            durationSec = 60,
        )
        val viewModel = newViewModel(stage = stage)
        val events = collectEvents(viewModel)
        val seen = mutableSetOf<String>()
        fun runAndRecord(seconds: Float) = repeat((seconds / FRAME).toInt()) {
            viewModel.update(FRAME)
            viewModel.bubbles.mapTo(seen) { it.id }
        }

        // Into the calm that follows the first wave.
        runAndRecord(WaveDirector.STAGE_WAVE_SECONDS + 0.2f)
        val beforeCalm = seen.size
        runAndRecord(WaveDirector.STAGE_CALM_SECONDS - 0.4f)
        assertEquals(beforeCalm, seen.size, "no bubbles spawn during the calm")

        // Then the next wave spawns again.
        runAndRecord(1f)
        assertTrue(seen.size > beforeCalm)
        assertFalse(events.any { it is GameEvent.WaveCleared || it is GameEvent.WaveStarted })
        assertEquals(1, viewModel.wave)
    }
    // ── Clock and ×2 bubbles ────────────────────────────────────────────────

    @Test
    fun doubleBubble_doublesThePointsThatFollow_forAWhile() {
        val viewModel = newViewModel(spawn = FAST_NORMAL_ONLY.copy(typeWeights = mapOf(BubbleType.NORMAL to 1, BubbleType.DOUBLE to 1)))
        while (viewModel.bubbles.none { it.type == BubbleType.DOUBLE }) viewModel.update(FRAME)
        val double = viewModel.bubbles.first { it.type == BubbleType.DOUBLE }
        viewModel.tryPop(Offset(double.x, double.y))
        assertClose(GameRules.DOUBLE_SECONDS, viewModel.doubleTimeLeft, tolerance = 0.05f)

        // Long enough for the combo to lapse, so only ×2 multiplies.
        viewModel.runFor(ComboTracker.WINDOW_SECONDS + 0.1f)
        while (viewModel.bubbles.none { it.type == BubbleType.NORMAL }) viewModel.update(FRAME)
        val normal = viewModel.bubbles.first { it.type == BubbleType.NORMAL }
        val before = viewModel.score
        viewModel.tryPop(Offset(normal.x, normal.y))
        assertEquals(normal.size.points * GameRules.DOUBLE_MULTIPLIER, viewModel.score - before)

        viewModel.runFor(GameRules.DOUBLE_SECONDS)
        assertEquals(0f, viewModel.doubleTimeLeft)
    }

    @Test
    fun clockBubble_addsTimeToTheStageTimer() {
        val stage = testStage(goal = Objective.ReachScore(1_000), durationSec = 30).copy(
            spawn = FAST_NORMAL_ONLY.copy(typeWeights = mapOf(BubbleType.CLOCK to 1)),
        )
        val viewModel = newViewModel(stage = stage)
        viewModel.runUntilBubble()
        val before = requireNotNull(viewModel.timeLeftSec)
        val clock = viewModel.bubbles.first()
        viewModel.tryPop(Offset(clock.x, clock.y))
        assertEquals(before + GameRules.CLOCK_SECONDS, viewModel.timeLeftSec)
    }
    // ── Boosters ────────────────────────────────────────────────────────────

    private fun boostedViewModel(stage: StageConfig, boosters: Set<Booster>) = BubbleGameViewModel(
        mode = GameMode.Stage(stage.id),
        progressRepository = FakeProgressRepository(),
        settingsRepository = FakeSettingsRepository(),
        metaRepository = FakeMetaRepository(),
        stage = stage,
        seed = 42L,
        boosters = boosters,
    ).apply { setCanvasBounds(WIDTH, HEIGHT, topOffset = 0f, density = DENSITY) }

    @Test
    fun boosters_giveTheirHeadStart_forOneAttemptOnly() {
        val stage = testStage(goal = Objective.ReachScore(1_000), durationSec = 30)
        val viewModel = boostedViewModel(stage, setOf(Booster.EXTRA_LIFE, Booster.SHIELD, Booster.CLOCK, Booster.ICE))

        assertEquals(GameRules.MAX_LIVES + 1, viewModel.maxLives)
        assertTrue(viewModel.shieldActive)
        assertEquals(30 + Booster.CLOCK_SECONDS, viewModel.timeLeftSec)
        viewModel.runFor(Booster.ICE_DELAY_SECONDS + 0.1f)
        assertTrue(viewModel.bubbles.any { it.type == BubbleType.FREEZE })

        // A restart is a fresh attempt, without them.
        viewModel.restartGame()
        assertEquals(GameRules.MAX_LIVES, viewModel.maxLives)
        assertFalse(viewModel.shieldActive)
        assertEquals(30, viewModel.timeLeftSec)
    }

    @Test
    fun boosters_areOfferedOnlyWhereTheyHelp() {
        val early = StageCatalog.stage(Booster.FIRST_STAGE - 1)
        assertTrue(Booster.offeredFor(early).isEmpty())

        val survive = StageCatalog.stages.first { stage -> stage.id >= 11 && stage.allObjectives.any { it is Objective.Survive } }
        assertFalse(Booster.CLOCK in Booster.offeredFor(survive))
        val timedScore = StageCatalog.stages.first { stage -> stage.id >= 11 && stage.durationSec != null && stage.goal is Objective.ReachScore }
        assertTrue(Booster.CLOCK in Booster.offeredFor(timedScore))
        assertTrue(Booster.EXTRA_LIFE in Booster.offeredFor(timedScore))
    }
    // ── Lightning and armored bubbles ───────────────────────────────────────

    @Test
    fun lightning_zapsTheNearestBubbles_butNeverSkullsOrBombs() = runTest {
        val viewModel = newViewModel(spawn = FAST_NORMAL_ONLY.copy(typeWeights = mapOf(BubbleType.NORMAL to 1)))
        val events = collectEvents(viewModel)
        viewModel.runFor(1f)
        val lightning = viewModel.bubbles.first().copy(id = "zap", type = BubbleType.LIGHTNING)
        val skull = viewModel.bubbles.first().copy(id = "skull", x = lightning.x + 1f, type = BubbleType.POISON)
        val bomb = viewModel.bubbles.first().copy(id = "bomb", x = lightning.x + 2f, type = BubbleType.BOMB)
        viewModel.bubbles.add(0, lightning)
        viewModel.bubbles += listOf(skull, bomb)
        val others = viewModel.bubbles.count { it.type == BubbleType.NORMAL }

        viewModel.tryPop(Offset(lightning.x, lightning.y))

        val zapped = events.filterIsInstance<GameEvent.LightningZapped>().single().count
        assertEquals(minOf(GameRules.LIGHTNING_ZAPS, others), zapped)
        assertTrue(skull in viewModel.bubbles && bomb in viewModel.bubbles)
        assertTrue(viewModel.lightningBolts.isNotEmpty())
        assertTrue(events.filterIsInstance<GameEvent.Popped>().count { it.blast } == zapped)
    }

    @Test
    fun armoredBubble_cracksAtTheFirstTap_andPopsAtTheSecond() = runTest {
        val viewModel = newViewModel(spawn = FAST_NORMAL_ONLY.copy(typeWeights = mapOf(BubbleType.ARMORED to 1)))
        val events = collectEvents(viewModel)
        viewModel.runUntilBubble()
        val armored = viewModel.bubbles.first()

        assertTrue(viewModel.tryPop(Offset(armored.x, armored.y)))
        assertTrue(armored.cracked)
        assertTrue(armored in viewModel.bubbles)
        assertTrue(events.any { it == GameEvent.ArmorCracked })
        assertEquals(0, viewModel.score)

        viewModel.tryPop(Offset(armored.x, armored.y))
        assertFalse(armored in viewModel.bubbles)
        assertEquals(GameRules.ARMORED_POINTS, viewModel.score)
    }

    @Test
    fun escapedArmoredBubbles_costLives() {
        val viewModel = newViewModel(spawn = FAST_NORMAL_ONLY.copy(typeWeights = mapOf(BubbleType.ARMORED to 1)))
        viewModel.runUntilGameOver()
        assertEquals(GameOverReason.OUT_OF_LIVES, viewModel.gameOverReason)
    }

    private fun assertClose(expected: Float, actual: Float, tolerance: Float = 0.01f) {
        assertTrue(abs(expected - actual) <= tolerance, "expected $expected but was $actual")
    }
}
