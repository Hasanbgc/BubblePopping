package com.apps.bubblepopping.game

/**
 * "Continue?" after a lost round: pay [cost] coins within [timeLeft] seconds to
 * undo [reason] and play on. Offered once per round.
 */
data class ReviveOffer(
    val reason: GameOverReason,
    val cost: Int,
    val timeLeft: Float,
    val duration: Float = timeLeft,
) {
    /** Fraction of the offer's time left, 1 → 0. */
    val progress: Float get() = if (duration > 0f) (timeLeft / duration).coerceIn(0f, 1f) else 0f
}
