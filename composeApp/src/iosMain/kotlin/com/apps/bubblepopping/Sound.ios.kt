package com.apps.bubblepopping

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import kotlinx.cinterop.addressOf
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.create

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun createSoundManager(audioData: ByteArray): SoundManager {
    val nsData = audioData.usePinned { pinned ->
        NSData.create(
            bytes  = pinned.addressOf(0),
            length = audioData.size.toULong(),
        )
    }

    val player = AVAudioPlayer(data = nsData, error = null).also {
        it.numberOfLoops = 0
        it.volume        = 1f
        it.prepareToPlay()
    }
    return SoundManager(
        trigger = {
            player.currentTime = 0.0
            player.play()
        }
    )
}