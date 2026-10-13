package com.apps.bubblepopping

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSData
import platform.Foundation.create

/**
 * One AVAudioPlayer plays one sound at a time, so rapid pops on a single player
 * cut each other off. Each sample gets a small pool used in rotation instead.
 */
private const val PLAYERS_PER_SAMPLE = 4

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun createSoundManager(samples: List<ByteArray>): SoundManager {
    val pools = samples.map { bytes ->
        val nsData = bytes.usePinned { pinned ->
            NSData.create(
                bytes  = pinned.addressOf(0),
                length = bytes.size.toULong(),
            )
        }
        List(PLAYERS_PER_SAMPLE) {
            AVAudioPlayer(data = nsData, error = null).also {
                it.enableRate    = true   // must be set before prepareToPlay
                it.numberOfLoops = 0
                it.volume        = 1f
                it.prepareToPlay()
            }
        }
    }
    val nextPlayer = IntArray(pools.size)

    return SoundManager(
        sampleCount = samples.size,
        player = { sample, rate ->
            val pool = pools[sample]
            val player = pool[nextPlayer[sample]]
            nextPlayer[sample] = (nextPlayer[sample] + 1) % pool.size
            player.rate = rate
            player.currentTime = 0.0
            player.play()
        },
    ).also {
        it.onDispose = {
            pools.flatten().forEach { player -> player.stop() }
        }
    }
}
