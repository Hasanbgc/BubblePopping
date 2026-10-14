package com.apps.bubblepopping.audio

import com.apps.bubblepopping.stage.World
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One looping audio track. Platform code provides it; see [createLoopingTrack]. */
interface LoopingTrack {
    fun play()
    fun pause()
    /** 0 = silent … 1 = full. */
    fun setVolume(volume: Float)
    fun release()
}

/** Loads [audio] (an encoded file, e.g. WAV) as a track that loops forever. */
expect fun createLoopingTrack(audio: ByteArray): LoopingTrack

/**
 * Background music for a round: a [base] layer that always plays and an
 * [intense] layer on top whose volume follows the action ([setIntensity]).
 * Both loops have the same length and start together, so they stay in time.
 */
class GameMusic(private val base: LoopingTrack, private val intense: LoopingTrack) {

    private var playing = false

    init {
        base.setVolume(BASE_VOLUME)
        intense.setVolume(0f)
    }

    fun play() {
        if (playing) return
        playing = true
        base.play()
        intense.play()
    }

    fun pause() {
        if (!playing) return
        playing = false
        base.pause()
        intense.pause()
    }

    /** [intensity] 0 → calm, 1 → full drums. */
    fun setIntensity(intensity: Float) {
        intense.setVolume(intensity.coerceIn(0f, 1f) * INTENSE_VOLUME)
    }

    fun release() {
        base.release()
        intense.release()
    }

    companion object {
        const val BASE_VOLUME = 0.45f
        const val INTENSE_VOLUME = 0.5f

        private val cacheLock = Mutex()
        /** The last world's layers (~1.5 MB), so retries and next stages start instantly. */
        private var cached: Pair<World, Pair<ByteArray, ByteArray>>? = null

        /** [world]'s two layers. The first call per world is CPU work — call off the main thread. */
        suspend fun synthesize(world: World): Pair<ByteArray, ByteArray> = cacheLock.withLock {
            cached?.takeIf { it.first == world }?.second
                ?: (MusicSynth.track(world, MusicSynth.Layer.BASE) to MusicSynth.track(world, MusicSynth.Layer.INTENSE))
                    .also { cached = world to it }
        }

        fun create(layers: Pair<ByteArray, ByteArray>): GameMusic =
            GameMusic(createLoopingTrack(layers.first), createLoopingTrack(layers.second))
    }
}
