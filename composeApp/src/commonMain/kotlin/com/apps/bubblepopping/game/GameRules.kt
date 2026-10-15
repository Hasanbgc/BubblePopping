package com.apps.bubblepopping.game

import com.apps.bubblepopping.view.play.component.Bubble
import com.apps.bubblepopping.view.play.component.BubbleType

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
    /** "Larger tap area" setting: touch radii grow by this factor. */
    const val LARGE_HIT_AREA_SCALE = 1.35f

    const val MIN_AMPLITUDE_DP = 7f
    const val MAX_AMPLITUDE_DP = 22f
    /** Sideways wobble frequency, in radians per dp risen. */
    const val WOBBLE_FREQUENCY = 0.1375f

    /**
     * Full-strength breeze speed, in canvas widths per second. Strong enough
     * that a gust can blow a skull off the screen; other bubbles stop at the edges.
     */
    const val BREEZE_SPEED = 0.45f
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

    // ── Special bubbles ─────────────────────────────────────────────────────

    const val GOLDEN_POINTS = 25
    /** Freeze, splitter and shield bubbles. */
    const val POWER_UP_POINTS = 3
    const val BOMB_POINTS = 5

    /** Golden bubbles rise this much faster than normal ones. */
    const val GOLDEN_SPEED_MULTIPLIER = 1.5f

    /** Bubbles move at this fraction of their speed while frozen. */
    const val FREEZE_SLOWDOWN = 0.4f
    const val FREEZE_SECONDS = 3f

    /** A bomb pops every bubble whose edge is within this distance of its centre. */
    const val BOMB_RADIUS_DP = 95f
    const val SHAKE_BOMB_DP = 5f
    const val SHAKE_BOMB_SECONDS = 0.3f

    /** A splitter bursts into this many small bubbles, spread sideways by [SPLIT_SPREAD_DP]. */
    const val SPLIT_COUNT = 3
    const val SPLIT_SPREAD_DP = 30f

    /** Breeze strength above which skulls can be blown off the side of the screen. */
    const val WIND_SAVE_MIN_BREEZE = 0.05f
    /** A horizontal swipe across this fraction of the screen gives full-strength wind. */
    const val FULL_WIND_SWIPE_FRACTION = 0.3f

    // ── Bosses ──────────────────────────────────────────────────────────────

    const val BOSS_RADIUS_DP = 70f
    /** A boss stage opens with normal bubbles; the boss arrives this long in. */
    const val BOSS_ENTRY_DELAY_SECONDS = 5.5f
    /** A boss shrinks to this fraction of its size as its HP runs out. */
    const val BOSS_MIN_SCALE = 0.65f
    /** Each split copy is this fraction of its parent's size. */
    const val BOSS_SPLIT_SCALE = 0.55f
    const val BOSS_SPLIT_COUNT = 3

    /** Points per hit (× combo multiplier), and for the final blow. */
    const val BOSS_HIT_POINTS = 2
    const val BOSS_DEFEAT_POINTS = 100
    /** Damage a bomb blast deals to each boss it reaches. */
    const val BOMB_BOSS_DAMAGE = 2

    // A boss fight builds up over three phases: above ⅔ of its HP, above ⅓, below ⅓.
    // Each list below has one value per phase.

    /** Movement speed. */
    val BOSS_PHASE_SPEED = listOf(1.4f, 2f, 2.8f)
    /** Boss stages: the spawn interval is multiplied by this, so bubbles come faster. */
    val BOSS_SPAWN_PACE = listOf(1f, 0.8f, 0.62f)
    /** Boss centre sits this far down the play area, bobbing by [BOSS_BOB_DP]. */
    const val BOSS_HEIGHT_FRACTION = 0.32f
    const val BOSS_BOB_DP = 24f

    /** Cloud King: hits between thrown skulls. */
    val SKULL_EVERY_HITS = listOf(4, 3, 2)
    /** Jelly Queen: hits between stings, and how long each locks taps. */
    val STING_EVERY_HITS = listOf(6, 5, 4)
    val STING_SECONDS = listOf(1f, 1.25f, 1.5f)
    const val NEBULA_BOMB_COUNT = 3

    const val BOSS_HIT_FLASH_SECONDS = 0.15f
    const val SHAKE_BOSS_HIT_DP = 1.5f
    const val SHAKE_BOSS_HIT_SECONDS = 0.08f
    const val SHAKE_BOSS_DEFEAT_DP = 7f
    const val SHAKE_BOSS_DEFEAT_SECONDS = 0.5f

    /** After the final blow the game runs in slow motion this long before the stage ends. */
    const val BOSS_CELEBRATION_SECONDS = 1.5f
    const val CELEBRATION_SLOWMO = 0.25f
    /** When a round ends, bubbles drift on at this speed for [GAME_OVER_SLOWMO_SECONDS]. */
    const val GAME_OVER_SLOWMO = 0.2f
    const val GAME_OVER_SLOWMO_SECONDS = 0.8f
    const val CONFETTI_COUNT = 90

    // ── Music intensity (Phase 9) ─────────────────────────────────────────────
    /** A combo this long plays the drums at full volume. */
    const val MUSIC_FULL_COMBO = 15
    /** Endless: the drums are at full volume from this many waves in. */
    const val MUSIC_FULL_WAVE = 4
    const val MUSIC_BOSS_INTENSITY = 0.8f
    /** Stages: the last this many seconds play at full intensity. */
    const val MUSIC_URGENT_SECONDS = 10

    fun sizeForRadius(radiusDp: Float): BubbleSize = when {
        radiusDp < SMALL_RADIUS_DP -> BubbleSize.SMALL
        radiusDp < MEDIUM_RADIUS_DP -> BubbleSize.MEDIUM
        else -> BubbleSize.LARGE
    }

    /** Points for popping [bubble] before the combo multiplier. */
    fun basePoints(bubble: Bubble): Int = when (bubble.type) {
        BubbleType.NORMAL, BubbleType.HEART -> bubble.size.points
        BubbleType.GOLDEN -> GOLDEN_POINTS
        BubbleType.FREEZE, BubbleType.SPLITTER, BubbleType.SHIELD -> POWER_UP_POINTS
        BubbleType.BOMB -> BOMB_POINTS
        BubbleType.POISON -> 0
    }
}
