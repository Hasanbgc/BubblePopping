package com.apps.bubblepopping.audio

import com.apps.bubblepopping.stage.World
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Placeholder background music, synthesised in code because the project has no
 * music files yet. Each world gets a looping track in two layers that play in sync:
 *
 * - [Layer.BASE]: soft chord pad, bass and a gentle arpeggio — always playing.
 * - [Layer.INTENSE]: kick, hi-hat and a fast high arpeggio — faded in as the
 *   action heats up (combos, later waves, bosses).
 *
 * Output is a mono 16-bit PCM WAV file. Same world + layer → same bytes.
 * Swap for real recordings later by loading files instead (see [GameMusic]).
 */
object MusicSynth {

    enum class Layer { BASE, INTENSE }

    const val SAMPLE_RATE = 22_050
    private const val BEATS_PER_BAR = 4

    /** One world's musical identity. Chords are semitone offsets from A4 (440 Hz). */
    private class Style(
        val bpm: Int,
        val chords: List<List<Int>>,
        /** 0 = pure sine (soft) … 1 = bright triangle-ish tone. */
        val brightness: Float,
        /** Arpeggio notes per beat in the base layer. */
        val arpPerBeat: Int,
    )

    private fun style(world: World): Style = when (world) {
        // C – G – Am – F
        World.SKY -> Style(100, listOf(listOf(-9, -5, -2), listOf(-14, -10, -7), listOf(-12, -9, -5), listOf(-16, -12, -9)), 0.6f, 2)
        // Am – F – C – G, slower and rounder
        World.OCEAN -> Style(84, listOf(listOf(-12, -9, -5), listOf(-16, -12, -9), listOf(-9, -5, -2), listOf(-14, -10, -7)), 0.2f, 2)
        // Dm – G – Dm – Am, spacey
        World.SPACE -> Style(90, listOf(listOf(-7, -4, 0), listOf(-14, -10, -7), listOf(-7, -4, 0), listOf(-12, -9, -5)), 0.1f, 1)
        // F – Dm – Gm – C, bouncy
        World.CANDY -> Style(120, listOf(listOf(-16, -12, -9), listOf(-19, -16, -12), listOf(-14, -11, -7), listOf(-9, -5, -2)), 0.9f, 4)
    }

    /** Length of one loop: two passes through the four chords. */
    fun loopSamples(world: World): Int {
        val style = style(world)
        val bars = style.chords.size * 2
        val seconds = bars * BEATS_PER_BAR * 60.0 / style.bpm
        return (seconds * SAMPLE_RATE).roundToInt()
    }

    /** The [layer] of [world]'s loop as WAV bytes. */
    fun track(world: World, layer: Layer): ByteArray = wav(render(world, layer))

    /** Renders samples in -1..1. */
    internal fun render(world: World, layer: Layer): FloatArray {
        val style = style(world)
        val total = loopSamples(world)
        val out = FloatArray(total)
        val beatSamples = SAMPLE_RATE * 60.0 / style.bpm
        val barSamples = beatSamples * BEATS_PER_BAR
        var noise = NOISE_SEED

        for (i in 0 until total) {
            val t = i.toDouble() / SAMPLE_RATE
            val bar = (i / barSamples).toInt()
            val chord = style.chords[bar % style.chords.size]
            val inBar = i - bar * barSamples
            val beat = (inBar / beatSamples).toInt()
            val inBeat = inBar - beat * beatSamples
            var sample = 0.0

            when (layer) {
                Layer.BASE -> {
                    // Pad: chord tones with a slow swell inside each bar.
                    val swell = 0.6 + 0.4 * sin(PI * inBar / barSamples)
                    for (note in chord) sample += 0.10 * swell * tone(freq(note), t, style.brightness * 0.3f)
                    // Bass: chord root two octaves down, one note per beat.
                    sample += 0.22 * decay(inBeat / SAMPLE_RATE, 3.0) * tone(freq(chord[0] - 24), t, 0.2f)
                    // Arpeggio up the chord.
                    val stepSamples = beatSamples / style.arpPerBeat
                    val step = (inBar / stepSamples).toInt()
                    val inStep = inBar - step * stepSamples
                    val note = chord[step % chord.size] + 12
                    sample += 0.12 * decay(inStep / SAMPLE_RATE, 9.0) * tone(freq(note), t, style.brightness)
                }
                Layer.INTENSE -> {
                    // Kick on every beat: a fast downward pitch sweep.
                    val k = inBeat / SAMPLE_RATE
                    sample += 0.45 * decay(k, 18.0) * sin(2 * PI * (50.0 * k + 60.0 * (1 - exp(-k * 30)) / 30))
                    // Hi-hat on the off-beats: short noise bursts.
                    noise = noise * 1_103_515_245 + 12_345
                    val white = ((noise ushr 16) and 0x7FFF) / 16_384.0 - 1.0
                    val offBeat = inBeat - beatSamples / 2
                    if (offBeat >= 0) sample += 0.10 * decay(offBeat / SAMPLE_RATE, 40.0) * white
                    // Fast arpeggio an octave above the base one.
                    val stepSamples = beatSamples / 4
                    val step = (inBar / stepSamples).toInt()
                    val inStep = inBar - step * stepSamples
                    val note = chord[(step * 2) % chord.size] + 24
                    sample += 0.07 * decay(inStep / SAMPLE_RATE, 14.0) * tone(freq(note), t, 0.8f)
                }
            }
            // Gentle saturation keeps peaks below full scale.
            out[i] = tanh(sample * 1.2).toFloat() * MASTER_GAIN
        }
        // Fade the first and last few milliseconds so the loop point never clicks.
        val fade = (SAMPLE_RATE * EDGE_FADE_SECONDS).toInt()
        for (i in 0 until fade) {
            val g = i.toFloat() / fade
            out[i] *= g
            out[total - 1 - i] *= g
        }
        return out
    }

    private fun freq(semitonesFromA4: Int): Double = 440.0 * 2.0.pow(semitonesFromA4 / 12.0)

    private fun decay(seconds: Double, rate: Double): Double = exp(-seconds * rate)

    /** Sine blended towards a triangle wave by [brightness]. */
    private fun tone(freq: Double, t: Double, brightness: Float): Double {
        val phase = freq * t
        val sine = sin(2 * PI * phase)
        val triangle = 4 * abs(phase - kotlin.math.floor(phase + 0.5)) - 1
        return sine * (1 - brightness) + triangle * brightness
    }

    /** Mono 16-bit little-endian PCM WAV. */
    internal fun wav(samples: FloatArray): ByteArray {
        val dataBytes = samples.size * 2
        val out = ByteArray(WAV_HEADER_BYTES + dataBytes)
        var p = 0
        fun ascii(s: String) { for (c in s) out[p++] = c.code.toByte() }
        fun int32(v: Int) { for (b in 0 until 4) out[p++] = (v shr (8 * b)).toByte() }
        fun int16(v: Int) { for (b in 0 until 2) out[p++] = (v shr (8 * b)).toByte() }

        ascii("RIFF"); int32(36 + dataBytes); ascii("WAVE")
        ascii("fmt "); int32(16); int16(1); int16(1)                 // PCM, mono
        int32(SAMPLE_RATE); int32(SAMPLE_RATE * 2); int16(2); int16(16)
        ascii("data"); int32(dataBytes)
        for (s in samples) int16((s.coerceIn(-1f, 1f) * Short.MAX_VALUE).roundToInt())
        return out
    }

    const val WAV_HEADER_BYTES = 44
    private const val MASTER_GAIN = 0.8f
    private const val EDGE_FADE_SECONDS = 0.004
    private const val NOISE_SEED = 0x2545F491
}
