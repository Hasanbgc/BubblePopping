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
     * multiplier; [combo] is the combo count after this pop (0 for poison).
     */
    data class Popped(val bubble: Bubble, val points: Int, val combo: Int) : GameEvent

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

    /** Emitted once per whole second of (unpaused) play time. */
    data class Tick(val elapsedSec: Int) : GameEvent

    data class GameOver(
        val reason: GameOverReason,
        val score: Int,
        val isNewBest: Boolean,
    ) : GameEvent
}

enum class GameOverReason {
    POISON,
    OUT_OF_LIVES,
}
