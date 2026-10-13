package com.apps.bubblepopping

/** Vibration feedback in increasing strength. */
expect class HapticFeedback {
    /** Small bubble pop. */
    fun light()

    /** Medium/large bubble pop, heart, combo milestone. */
    fun medium()

    /** Poison pop, game over. */
    fun heavy()

    /** New best score. */
    fun success()
}
