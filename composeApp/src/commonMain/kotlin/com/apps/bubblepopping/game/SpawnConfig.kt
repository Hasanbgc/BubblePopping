package com.apps.bubblepopping.game

import com.apps.bubblepopping.view.play.component.BubbleType

/**
 * All spawn and movement tuning for one round.
 *
 * Speeds are fractions of the canvas height per second, so a bubble takes the
 * same time to cross the screen on every device regardless of resolution.
 */
data class SpawnConfig(
    /** Seconds between spawns at score 0. */
    val spawnInterval: Float,
    /** Spawn interval never drops below this, however high the score. */
    val minSpawnInterval: Float = 0.30f,
    /** Seconds shaved off the spawn interval per point scored. */
    val spawnRampPerPoint: Float = 0.005f,
    /** Slowest rise speed (screen heights / second). */
    val minSpeed: Float = 0.030f,
    /** Fastest rise speed at score 0 (screen heights / second). */
    val maxSpeed: Float,
    /** Added to [maxSpeed] per point scored. */
    val speedRampPerPoint: Float = 0.00034f,
    /** [maxSpeed] never grows beyond this. */
    val maxSpeedCap: Float,
    /** Relative spawn weights per bubble type. */
    val typeWeights: Map<BubbleType, Int> = DEFAULT_TYPE_WEIGHTS,
    /**
     * Stages: ramp up in timed waves with a calm between them (like Endless)
     * instead of with the score — see [paced]. Endless always uses waves.
     */
    val inWaves: Boolean = false,
) {
    init {
        require(typeWeights.values.all { it >= 0 } && typeWeights.values.sum() > 0) {
            "typeWeights must contain at least one positive weight"
        }
    }

    companion object {
        val DEFAULT_TYPE_WEIGHTS = mapOf(
            BubbleType.NORMAL to 75,
            BubbleType.POISON to 15,
            BubbleType.HEART to 10,
        )

        val EASY = SpawnConfig(
            spawnInterval = 1.23f,
            maxSpeed = 0.043f,
            maxSpeedCap = 0.096f,
        )

        val MEDIUM = SpawnConfig(
            spawnInterval = 0.85f,
            maxSpeed = 0.066f,
            maxSpeedCap = 0.137f,
        )

        val HARD = SpawnConfig(
            spawnInterval = 0.47f,
            maxSpeed = 0.099f,
            maxSpeedCap = 0.191f,
        )

        /**
         * A stage that starts at [start] and climbs wave by wave towards [top]
         * (see [WaveDirector.forStage]); it never goes beyond [top].
         */
        fun paced(start: Pace, top: Pace = start, typeWeights: Map<BubbleType, Int> = DEFAULT_TYPE_WEIGHTS): SpawnConfig {
            require(top >= start) { "A stage's pace can't fall: $start → $top" }
            return SpawnConfig(
                spawnInterval = start.spawnInterval,
                minSpawnInterval = top.spawnInterval,
                maxSpeed = start.maxSpeed,
                maxSpeedCap = top.maxSpeed,
                typeWeights = typeWeights,
                inWaves = true,
            )
        }
    }
}
