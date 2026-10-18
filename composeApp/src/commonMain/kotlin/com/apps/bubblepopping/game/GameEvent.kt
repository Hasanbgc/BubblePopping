package com.apps.bubblepopping.game

import com.apps.bubblepopping.view.play.component.Bubble

/**
 * Everything that happens during a round, emitted by the game ViewModel.
 *
 * Sound, haptics, stage objectives, missions and stats all consume this one
 * stream instead of hooking into the game loop separately.
 */
sealed interface GameEvent {
    /**
     * A bubble was popped by the player. [points] already include the combo
     * multiplier; [combo] is the combo count after this pop (0 for poison). [blast] is true
     * when a bomb or lightning popped it rather than a tap.
     */
    data class Popped(val bubble: Bubble, val points: Int, val combo: Int, val blast: Boolean = false) : GameEvent

    /** A bubble escaped off the top. [lifeLost] is true when it cost a life. */
    data class Missed(val bubble: Bubble, val lifeLost: Boolean) : GameEvent

    /** The player tapped the play area. */
    data class Tapped(val hit: Boolean) : GameEvent

    /**
     * The combo count changed from [previous] to [combo] (0 when it ended).
     * [broken] is true when a miss or poison ended it, false when it extended
     * or simply ran out of time.
     */
    data class ComboChanged(val combo: Int, val previous: Int, val broken: Boolean = false) : GameEvent

    /** A bomb went off; [chainSize] bubbles were popped by it and any bombs it set off. */
    data class BombExploded(val chainSize: Int) : GameEvent

    /** The wind blew a skull bubble off the side of the screen. */
    data class WindSave(val bubble: Bubble) : GameEvent

    /** A lightning bubble zapped [count] bubbles. */
    data class LightningZapped(val count: Int) : GameEvent

    /** Both bubbles of a chain pair were popped in time, scoring a [points] bonus. */
    data class ChainPaired(val points: Int) : GameEvent

    /** A tap cracked an armored bubble's armor (the next tap pops it). */
    data object ArmorCracked : GameEvent

    /** A shield absorbed a skull pop. */
    data object ShieldUsed : GameEvent

    /** A boss took [damage] (from a tap or a bomb); [points] were scored for it. */
    data class BossHit(val damage: Int, val points: Int) : GameEvent

    /** The last copy of the boss was beaten; [points] is the defeat bonus. */
    data class BossDefeated(val points: Int) : GameEvent

    /** Endless: [wave] was survived and the calm before the next one began. */
    data class WaveCleared(val wave: Int) : GameEvent

    /** Endless: [wave] (2 or later) began after a calm. */
    data class WaveStarted(val wave: Int) : GameEvent

    /** Emitted once per whole second of (unpaused) play time. */
    data class Tick(val elapsedSec: Int) : GameEvent

    /** The round was lost and a coin revive is on offer; [GameOver] follows only if it's declined. */
    data class ReviveOffered(val reason: GameOverReason) : GameEvent

    /** The player paid to continue after losing to [reason]. */
    data class Revived(val reason: GameOverReason) : GameEvent

    data class GameOver(
        val reason: GameOverReason,
        val score: Int,
        val isNewBest: Boolean,
    ) : GameEvent
}

enum class GameOverReason {
    POISON,
    OUT_OF_LIVES,
    /** Stage only: the timer ran out. */
    TIME_UP,
    /** Stage only: ended early because its objectives were met. */
    GOAL_COMPLETE,
}
