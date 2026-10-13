package com.apps.bubblepopping.game

/** Size class of a bubble — decides its points. */
enum class BubbleSize(val points: Int) {
    SMALL(1),
    MEDIUM(3),
    LARGE(5),
}

/**
 * Fixed gameplay rules shared by every mode.
 *
 * Lengths are in dp and converted with the screen density at spawn time, so
 * bubbles look and feel the same size on every device.
 */
object GameRules {
    const val MAX_LIVES = 5

    const val MIN_RADIUS_DP = 11f
    const val MAX_RADIUS_DP = 25f
    /** Radius below this is [BubbleSize.SMALL]. */
    const val SMALL_RADIUS_DP = 16f
    /** Radius below this (and not small) is [BubbleSize.MEDIUM]; above is [BubbleSize.LARGE]. */
    const val MEDIUM_RADIUS_DP = 20f
    /** Minimum touch radius, so small bubbles stay easy to hit. */
    const val MIN_HIT_RADIUS_DP = 25f

    const val MIN_AMPLITUDE_DP = 7f
    const val MAX_AMPLITUDE_DP = 22f
    /** Sideways wobble frequency, in radians per dp risen. */
    const val WOBBLE_FREQUENCY = 0.1375f

    /** Full-strength breeze speed, in canvas widths per second. */
    const val BREEZE_SPEED = 0.148f
    /** Fraction of breeze strength lost per second. */
    const val BREEZE_DECAY = 2.0f

    /** Longest frame step simulated at once (avoids tunnelling after a stall). */
    const val MAX_DELTA = 0.05f

    const val POP_ANIM_SPEED = 2.5f

    // ── Feedback effects ────────────────────────────────────────────────────

    /** Floating "+5" texts live 1 / speed seconds. */
    const val FLOATING_TEXT_SPEED = 1.2f
    const val MILESTONE_TEXT_SPEED = 0.7f
    /** Milestone text appears this far above the popped bubble. */
    const val MILESTONE_TEXT_OFFSET_DP = 34f

    /** Bursts grow by this much per combo step, up to [MAX_BURST_COMBO]. */
    const val BURST_GROWTH_PER_COMBO = 0.08f
    const val MAX_BURST_COMBO = 10

    const val SHAKE_LARGE_POP_DP = 2f
    const val SHAKE_LARGE_POP_SECONDS = 0.12f
    const val SHAKE_MILESTONE_DP = 3f
    const val SHAKE_MILESTONE_SECONDS = 0.15f
    const val SHAKE_POISON_DP = 4f
    const val SHAKE_POISON_SECONDS = 0.25f

    /** Brief freeze on combo milestones so they land with weight. */
    const val HIT_STOP_SECONDS = 0.05f

    /** Normal bubbles in this top fraction of the play area pulse red. */
    const val NEAR_MISS_ZONE = 0.15f

    fun sizeForRadius(radiusDp: Float): BubbleSize = when {
        radiusDp < SMALL_RADIUS_DP -> BubbleSize.SMALL
        radiusDp < MEDIUM_RADIUS_DP -> BubbleSize.MEDIUM
        else -> BubbleSize.LARGE
    }
}
