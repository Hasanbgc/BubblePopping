package com.apps.bubblepopping

import com.apps.bubblepopping.game.SpawnConfig
import com.apps.bubblepopping.game.WaveDirector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WaveDirectorTest {

    private val base = SpawnConfig.MEDIUM

    /** Advances in small steps, returning every change in order. */
    private fun WaveDirector.advance(seconds: Float): List<WaveDirector.Change> {
        val changes = mutableListOf<WaveDirector.Change>()
        repeat((seconds / STEP).toInt()) { update(STEP)?.let(changes::add) }
        return changes
    }

    @Test
    fun startsInWaveOne_atBasePace() {
        val director = WaveDirector(base)

        assertEquals(1, director.wave)
        assertFalse(director.isCalm)
        assertEquals(base.spawnInterval, director.spawnInterval, TOLERANCE)
        assertEquals(base.maxSpeed, director.maxSpeed, TOLERANCE)
    }

    @Test
    fun waveEnds_inACalm_thenTheNextWaveStarts() {
        val director = WaveDirector(base)

        assertEquals(listOf(WaveDirector.Change.CALM_STARTED), director.advance(WaveDirector.WAVE_SECONDS + 0.1f))
        assertTrue(director.isCalm)
        assertEquals(2, director.wave)

        assertEquals(listOf(WaveDirector.Change.WAVE_STARTED), director.advance(WaveDirector.CALM_SECONDS))
        assertFalse(director.isCalm)
        assertEquals(2, director.wave)
    }

    @Test
    fun pressureRisesWithinAWave_andCarriesIntoTheNext() {
        val director = WaveDirector(base)
        val start = director.spawnInterval
        director.advance(WaveDirector.WAVE_SECONDS / 2)
        val middle = director.spawnInterval
        director.advance(WaveDirector.WAVE_SECONDS / 2 - 0.1f)
        val peak = director.spawnInterval

        assertTrue(middle < start)
        assertTrue(peak < middle)

        // Wave 2 starts where wave 1 peaked, not back at the start.
        director.advance(0.2f + WaveDirector.CALM_SECONDS)
        assertEquals(peak, director.spawnInterval, 0.01f)
        assertTrue(director.maxSpeed > base.maxSpeed)
    }

    @Test
    fun pace_neverPassesTheConfigLimits() {
        val director = WaveDirector(base)
        director.advance(30 * (WaveDirector.WAVE_SECONDS + WaveDirector.CALM_SECONDS))

        assertEquals(base.minSpawnInterval, director.spawnInterval, TOLERANCE)
        assertEquals(base.maxSpeedCap, director.maxSpeed, TOLERANCE)
    }

    @Test
    fun reset_returnsToWaveOne() {
        val director = WaveDirector(base)
        director.advance(WaveDirector.WAVE_SECONDS + 1f)
        director.reset()

        assertEquals(1, director.wave)
        assertFalse(director.isCalm)
        assertEquals(0f, director.waveProgress)
        assertNull(director.update(STEP))
    }

    private companion object {
        const val STEP = 0.016f
        const val TOLERANCE = 0.0001f
    }
}
