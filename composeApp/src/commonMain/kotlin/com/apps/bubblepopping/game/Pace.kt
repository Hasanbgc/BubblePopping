package com.apps.bubblepopping.game

/**
 * A stage's speed level, P1 (lively) → P10 (frantic): how often bubbles spawn, in
 * groups of how many, and how fast the fastest rise. See `stage-plan.md` §2.2.
 *
 * Bubbles come in groups so the screen is never empty and pops can be chained into
 * combos: P1 already gives about 1.6 bubbles a second, P10 about 5.5.
 */
enum class Pace(
    /** Seconds between groups. */
    val spawnInterval: Float,
    /** Fastest rise speed, in screen heights per second. */
    val maxSpeed: Float,
    /** Bubbles per group, from [groupMin] to [groupMax]. */
    val groupMin: Int,
    val groupMax: Int,
) {
    P1(0.95f, 0.06f, 1, 2),
    P2(0.85f, 0.065f, 1, 2),
    P3(0.80f, 0.07f, 1, 3),
    P4(0.75f, 0.08f, 1, 3),
    P5(0.70f, 0.09f, 2, 3),
    P6(0.66f, 0.10f, 2, 3),
    P7(0.62f, 0.11f, 2, 3),
    P8(0.60f, 0.115f, 2, 3),
    P9(0.58f, 0.12f, 2, 4),
    P10(0.55f, 0.13f, 2, 4);

    /** Average bubbles a second at this pace. */
    val bubblesPerSecond: Float get() = (groupMin + groupMax) / 2f / spawnInterval
}
