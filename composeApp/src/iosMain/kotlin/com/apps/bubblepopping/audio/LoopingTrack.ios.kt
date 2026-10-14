package com.apps.bubblepopping.audio

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSData
import platform.Foundation.create

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun createLoopingTrack(audio: ByteArray): LoopingTrack {
    val data = audio.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = audio.size.toULong())
    }
    val player = AVAudioPlayer(data = data, error = null).apply {
        numberOfLoops = -1 // forever
        prepareToPlay()
    }
    return object : LoopingTrack {
        override fun play() {
            player.play()
        }
        override fun pause() = player.pause()
        override fun setVolume(volume: Float) {
            player.volume = volume
        }
        override fun release() = player.stop()
    }
}
