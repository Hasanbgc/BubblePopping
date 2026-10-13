package com.apps.bubblepopping.game

/**
 * Counts pops made in quick succession. Each pop within [window] seconds of the
 * previous one extends the combo; a longer gap, a missed bubble or a poison pop
 * ends it.
 *
 * Times are game seconds (paused time doesn't count), supplied by the caller.
 */
class ComboTracker(private val window: Float = WINDOW_SECONDS) {

    /** Current combo; 0 when no combo is running. */
    var combo = 0
        private set

    /** Highest combo reached since the last [reset]. */
    var best = 0
        private set

    private var lastPopTime = 0f

    /** Records a pop at [now] and returns the new combo. */
    fun onPop(now: Float): Int {
        combo = if (combo > 0 && now - lastPopTime <= window) combo + 1 else 1
        lastPopTime = now
        if (combo > best) best = combo
        return combo
    }

    /** Ends the running combo. Returns the combo that was broken (0 if none). */
    fun breakCombo(): Int {
        val broken = combo
        combo = 0
        return broken
    }

    /** Ends the combo if its window has run out. Returns true if it just expired. */
    fun expireIfStale(now: Float): Boolean {
        if (combo == 0 || now - lastPopTime <= window) return false
        combo = 0
        return true
    }

    /** Fraction of the combo window left at [now]: 1 right after a pop, 0 when expired. */
    fun timeLeftFraction(now: Float): Float =
        if (combo == 0) 0f else (1f - (now - lastPopTime) / window).coerceIn(0f, 1f)

    fun reset() {
        combo = 0
        best = 0
        lastPopTime = 0f
    }

    companion object {
        const val WINDOW_SECONDS = 0.8f

        /** Score multiplier for a combo: ×1 (1–2), ×2 (3–5), ×3 (6–9), ×5 (10+). */
        fun multiplierFor(combo: Int): Int = when {
            combo >= 10 -> 5
            combo >= 6 -> 3
            combo >= 3 -> 2
            else -> 1
        }

        /** Celebration text shown when a combo reaches exactly this count, else null. */
        fun milestoneText(combo: Int): String? = when (combo) {
            5 -> "NICE!"
            10 -> "GREAT!"
            15 -> "PERFECT!"
            25 -> "INSANE!"
            else -> null
        }
    }
}
