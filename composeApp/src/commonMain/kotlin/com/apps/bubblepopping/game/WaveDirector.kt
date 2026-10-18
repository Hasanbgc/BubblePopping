package com.apps.bubblepopping.game

import kotlin.math.pow

/**
 * Endless mode pacing: [WAVE_SECONDS] of rising pressure, then [CALM_SECONDS]
 * with no new bubbles while a "WAVE N" banner announces the next, harder wave.
 *
 * Replaces the score-based ramp in Endless. Pressure is a "level" that grows
 * by 1 per wave — it rises smoothly through a wave and the next wave starts
 * where the last one peaked. Spawn interval and top speed follow the level,
 * bounded by [base]'s minimum interval and speed cap.
 *
 * Paced stages ([SpawnConfig.inWaves]) use the same scheme with gentler,
 * longer waves: see [forStage].
 */
class WaveDirector(
    private val base: SpawnConfig,
    private val waveSeconds: Float = WAVE_SECONDS,
    private val calmSeconds: Float = CALM_SECONDS,
    private val intervalFactorPerWave: Float = INTERVAL_FACTOR_PER_WAVE,
    private val speedGrowthPerWave: Float = SPEED_GROWTH_PER_WAVE,
) {

    companion object {
        const val WAVE_SECONDS = 20f
        const val CALM_SECONDS = 3f
        /** The spawn interval is multiplied by this per wave level. */
        const val INTERVAL_FACTOR_PER_WAVE = 0.85f
        /** Top speed grows by this fraction of the base speed per wave level. */
        const val SPEED_GROWTH_PER_WAVE = 0.15f

        const val STAGE_WAVE_SECONDS = 25f
        const val STAGE_CALM_SECONDS = 2.5f
        const val STAGE_INTERVAL_FACTOR_PER_WAVE = 0.92f
        const val STAGE_SPEED_GROWTH_PER_WAVE = 0.08f

        /** A paced stage's waves: each 8 % busier and faster, from its start pace up to its top pace. */
        fun forStage(base: SpawnConfig) = WaveDirector(
            base,
            waveSeconds = STAGE_WAVE_SECONDS,
            calmSeconds = STAGE_CALM_SECONDS,
            intervalFactorPerWave = STAGE_INTERVAL_FACTOR_PER_WAVE,
            speedGrowthPerWave = STAGE_SPEED_GROWTH_PER_WAVE,
        )
    }

    /** What changed on an [update], if anything. */
    enum class Change { CALM_STARTED, WAVE_STARTED }

    /** The wave being played, or — during a calm — the one about to start. 1-based. */
    var wave = 1
        private set

    /** True during the break between waves: nothing spawns. */
    var isCalm = false
        private set

    /** Seconds into the current wave or calm. */
    private var clock = 0f

    /** Fraction of the current wave played, 0 → 1 (stays 1 during a calm). */
    val waveProgress: Float
        get() = if (isCalm) 1f else (clock / waveSeconds).coerceIn(0f, 1f)

    /** Pressure level: 0 at the start of wave 1, +1 per wave. */
    val level: Float
        get() = if (isCalm) (wave - 1).toFloat() else (wave - 1) + waveProgress

    val spawnInterval: Float
        get() = (base.spawnInterval * intervalFactorPerWave.pow(level))
            .coerceAtLeast(base.minSpawnInterval)

    val maxSpeed: Float
        get() = (base.maxSpeed * (1f + speedGrowthPerWave * level))
            .coerceAtMost(base.maxSpeedCap)

    /** Advances the clock by [dt] seconds; returns what changed, if anything. */
    fun update(dt: Float): Change? {
        clock += dt
        return when {
            !isCalm && clock >= waveSeconds -> {
                clock -= waveSeconds
                isCalm = true
                wave++
                Change.CALM_STARTED
            }
            isCalm && clock >= calmSeconds -> {
                clock -= calmSeconds
                isCalm = false
                Change.WAVE_STARTED
            }
            else -> null
        }
    }

    fun reset() {
        wave = 1
        isCalm = false
        clock = 0f
    }
}
