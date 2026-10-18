package com.apps.bubblepopping.game

/**
 * A stage's speed level, P1 (calm) → P10 (frantic): how often bubbles spawn and
 * how fast the fastest rise. See `stage-plan.md` §2.2.
 */
enum class Pace(
    /** Seconds between spawns. */
    val spawnInterval: Float,
    /** Fastest rise speed, in screen heights per second. */
    val maxSpeed: Float,
) {
    P1(1.25f, 0.045f),
    P2(1.05f, 0.055f),
    P3(0.90f, 0.065f),
    P4(0.80f, 0.075f),
    P5(0.70f, 0.085f),
    P6(0.62f, 0.095f),
    P7(0.55f, 0.105f),
    P8(0.50f, 0.11f),
    P9(0.45f, 0.115f),
    P10(0.40f, 0.12f),
}
