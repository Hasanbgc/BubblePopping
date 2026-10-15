package com.apps.bubblepopping

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.apps.bubblepopping.audio.GameMusic
import com.apps.bubblepopping.audio.LoopingTrack
import com.apps.bubblepopping.audio.MusicSynth
import com.apps.bubblepopping.data.EndlessRun
import com.apps.bubblepopping.data.prefs.GameSettings
import com.apps.bubblepopping.data.prefs.MAX_PLAYER_NAME_LENGTH
import com.apps.bubblepopping.data.prefs.cleanPlayerName
import com.apps.bubblepopping.game.GameMode
import com.apps.bubblepopping.game.GameRules
import com.apps.bubblepopping.game.SpawnConfig
import com.apps.bubblepopping.game.WaveDirector
import com.apps.bubblepopping.stage.World
import com.apps.bubblepopping.view.BubbleGameViewModel
import com.apps.bubblepopping.view.home.Difficulty
import com.apps.bubblepopping.view.leaderboard.leaderboardEntries
import com.apps.bubblepopping.view.play.component.BubbleType
import com.apps.bubblepopping.view.play.component.NORMAL_SCHEME
import com.apps.bubblepopping.view.play.component.filmRingColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Phase 9: music, accessibility, rendering helpers and small settings rules. */
@OptIn(ExperimentalCoroutinesApi::class)
class PolishTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Music synthesis ─────────────────────────────────────────────────────

    private fun int32(bytes: ByteArray, at: Int) =
        (0 until 4).sumOf { (bytes[at + it].toInt() and 0xFF) shl (8 * it) }

    @Test
    fun synthesisedTrack_isAValidMonoWav() {
        val wav = MusicSynth.track(World.SKY, MusicSynth.Layer.BASE)
        assertEquals("RIFF", wav.decodeToString(0, 4))
        assertEquals("WAVE", wav.decodeToString(8, 12))
        assertEquals(MusicSynth.SAMPLE_RATE, int32(wav, 24))
        val dataBytes = int32(wav, 40)
        assertEquals(MusicSynth.loopSamples(World.SKY) * 2, dataBytes)
        assertEquals(MusicSynth.WAV_HEADER_BYTES + dataBytes, wav.size)
    }

    @Test
    fun layers_haveTheSameLength_soTheyStayInTime() {
        for (world in World.entries) {
            val base = MusicSynth.render(world, MusicSynth.Layer.BASE)
            val intense = MusicSynth.render(world, MusicSynth.Layer.INTENSE)
            assertEquals(base.size, intense.size, "$world")
        }
    }

    @Test
    fun tracks_areAudible_unclipped_andLoopWithoutAClick() {
        for (world in World.entries) {
            for (layer in MusicSynth.Layer.entries) {
                val samples = MusicSynth.render(world, layer)
                val peak = samples.maxOf { abs(it) }
                assertTrue(peak > 0.1f, "$world $layer is nearly silent")
                assertTrue(peak < 1f, "$world $layer clips")
                assertTrue(abs(samples.first()) < 0.01f && abs(samples.last()) < 0.01f, "$world $layer clicks at the loop")
            }
        }
    }

    @Test
    fun synthesis_isDeterministic_andWorldsSoundDifferent() {
        assertContentEquals(
            MusicSynth.track(World.OCEAN, MusicSynth.Layer.INTENSE),
            MusicSynth.track(World.OCEAN, MusicSynth.Layer.INTENSE),
        )
        val lengths = World.entries.map { MusicSynth.loopSamples(it) }.toSet()
        assertTrue(lengths.size > 1, "every world has the same tempo")
    }

    // ── GameMusic ───────────────────────────────────────────────────────────

    private class FakeTrack : LoopingTrack {
        var playing = false
        var currentVolume = -1f
        var plays = 0
        var released = false
        override fun play() { playing = true; plays++ }
        override fun pause() { playing = false }
        override fun setVolume(volume: Float) { currentVolume = volume }
        override fun release() { released = true }
    }

    @Test
    fun music_startsCalm_andTheDrumsFollowTheIntensity() {
        val base = FakeTrack()
        val intense = FakeTrack()
        val music = GameMusic(base, intense)
        assertEquals(GameMusic.BASE_VOLUME, base.currentVolume)
        assertEquals(0f, intense.currentVolume)

        music.play()
        music.play()
        assertTrue(base.playing && intense.playing)
        assertEquals(1, base.plays, "play() twice doesn't restart")

        music.setIntensity(0.5f)
        assertEquals(0.5f * GameMusic.INTENSE_VOLUME, intense.currentVolume)
        music.setIntensity(3f)
        assertEquals(GameMusic.INTENSE_VOLUME, intense.currentVolume)

        music.pause()
        assertFalse(base.playing || intense.playing)
        music.release()
        assertTrue(base.released && intense.released)
    }

    // ── ViewModel: music intensity and larger tap area ──────────────────────

    private fun newViewModel(
        spawn: SpawnConfig = SpawnConfig.MEDIUM.copy(typeWeights = mapOf(BubbleType.HEART to 1)),
        settings: FakeSettingsRepository = FakeSettingsRepository(),
    ) = BubbleGameViewModel(
        mode = GameMode.Endless(Difficulty.MEDIUM),
        progressRepository = FakeProgressRepository(),
        settingsRepository = settings,
        metaRepository = FakeMetaRepository(),
        spawn = spawn,
        seed = 3L,
    ).apply { setCanvasBounds(1080f, 2340f, topOffset = 0f, density = 2.75f) }

    @Test
    fun musicIntensity_risesWithLaterWaves() {
        val viewModel = newViewModel()
        assertEquals(0f, viewModel.musicIntensity)
        repeat(GameRules.MUSIC_FULL_WAVE) {
            repeat(((WaveDirector.WAVE_SECONDS + WaveDirector.CALM_SECONDS) / 0.016f).toInt() + 1) { viewModel.update(0.016f) }
        }
        assertEquals(1f, viewModel.musicIntensity)
    }

    @Test
    fun largerTapArea_reachesFurther() {
        fun tapJustOutside(large: Boolean): Boolean {
            val settings = FakeSettingsRepository().apply { settings.value = GameSettings(largeHitArea = large) }
            val viewModel = newViewModel(settings = settings)
            while (viewModel.bubbles.isEmpty()) viewModel.update(0.016f)
            val bubble = viewModel.bubbles.first()
            val normalReach = maxOf(bubble.radius, GameRules.MIN_HIT_RADIUS_DP * 2.75f)
            return viewModel.tryPop(Offset(bubble.x + normalReach * 1.15f, bubble.y))
        }
        assertFalse(tapJustOutside(large = false))
        assertTrue(tapJustOutside(large = true))
    }

    // ── Rendering helpers ───────────────────────────────────────────────────

    @Test
    fun filmRing_hasNoGaps_andNoSeam() {
        val colors = filmRingColors(NORMAL_SCHEME)
        assertTrue(colors.size >= 3)
        assertTrue(colors.all { it.alpha > 0f })
        assertEquals(colors.first(), colors.last())
    }

    // ── Small rules ─────────────────────────────────────────────────────────

    @Test
    fun playerName_isTrimmed_shortened_andNeverBlank() {
        assertEquals("Ana", cleanPlayerName("  Ana  "))
        assertEquals(MAX_PLAYER_NAME_LENGTH, cleanPlayerName("x".repeat(40)).length)
        assertEquals(GameSettings.DEFAULT_PLAYER_NAME, cleanPlayerName("   "))
    }

    @Test
    fun leaderboard_putsEachScoreInItsDifficultyColumn() {
        val entries = leaderboardEntries(
            listOf(EndlessRun(Difficulty.HARD, 200, 0), EndlessRun(Difficulty.EASY, 150, 0)),
            playerName = "Ana",
        )
        assertEquals(listOf(1, 2), entries.map { it.rank })
        assertEquals(200, entries[0].hardScore)
        assertNull(entries[0].easyScore)
        assertEquals(150, entries[1].easyScore)
        assertTrue(entries.all { it.name == "Ana" })
    }

    @Test
    fun resetProgress_clearsRepositories() = kotlinx.coroutines.test.runTest {
        val settings = FakeSettingsRepository()
        settings.setTutorialSeen(true)
        settings.setLargeHitArea(true)
        settings.resetProgressSettings()
        assertFalse(settings.settings.value.tutorialSeen)
        assertTrue(settings.settings.value.largeHitArea, "accessibility settings survive a reset")
    }
}
