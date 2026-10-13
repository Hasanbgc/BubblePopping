package com.apps.bubblepopping

import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Plays short preloaded samples, several at once, at an adjustable pitch.
 * Platform code supplies [player]; see [createSoundManager].
 */
class SoundManager(
    val sampleCount: Int,
    private val player: (sample: Int, rate: Float) -> Unit,
) {
    var onDispose: (() -> Unit)? = null

    /** Plays [sample] at [rate]: 0.5 = an octave lower, 2 = an octave higher. */
    fun play(sample: Int, rate: Float = 1f) {
        if (sample in 0 until sampleCount) player(sample, rate.coerceIn(MIN_RATE, MAX_RATE))
    }

    fun dispose() = onDispose?.invoke()

    companion object {
        /** Playback-rate range both Android SoundPool and iOS AVAudioPlayer support. */
        const val MIN_RATE = 0.5f
        const val MAX_RATE = 2f
    }
}

/** Loads [samples] (encoded audio files) so they can be played instantly. */
expect fun createSoundManager(samples: List<ByteArray>): SoundManager

/**
 * Maps game moments to sounds.
 *
 * Only pop samples exist so far, so the heart, poison, combo-break and new-best
 * sounds are pitch-shifted pops. Swap in dedicated audio files when available.
 */
class GameSounds(private val sound: SoundManager) {

    /** Random pop sample; pitch rises with the combo, capped at combo 12. */
    fun pop(combo: Int) =
        sound.play(Random.nextInt(sound.sampleCount), 1f + minOf(combo, MAX_PITCH_COMBO) * PITCH_STEP)

    fun heart() = sound.play(sample = 0, rate = 1.6f)
    fun poison() = sound.play(sample = 0, rate = 0.55f)
    fun comboBreak() = sound.play(sample = 0, rate = 0.75f)
    fun golden() = sound.play(sample = 1, rate = 1.9f)
    fun freeze() = sound.play(sample = 2, rate = 0.7f)
    fun bomb() = sound.play(sample = 0, rate = 0.5f)
    fun shield() = sound.play(sample = 1, rate = 1.3f)
    fun windSave() = sound.play(sample = 2, rate = 1.5f)
    fun bossHit() = sound.play(sample = Random.nextInt(sound.sampleCount), rate = 0.8f)

    /** Short rising arpeggio. */
    suspend fun newBest() {
        for (rate in NEW_BEST_RATES) {
            sound.play(sample = 0, rate = rate)
            delay(NEW_BEST_NOTE_MS)
        }
    }

    fun dispose() = sound.dispose()

    private companion object {
        const val MAX_PITCH_COMBO = 12
        const val PITCH_STEP = 0.05f
        val NEW_BEST_RATES = listOf(1f, 1.26f, 1.5f, 2f)
        const val NEW_BEST_NOTE_MS = 90L
    }
}
