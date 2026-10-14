package com.apps.bubblepopping.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import java.io.File

/** MediaPlayer plays from a file, so the audio is written to a temp file deleted on release. */
actual fun createLoopingTrack(audio: ByteArray): LoopingTrack {
    val file = File.createTempFile("music", ".wav").apply { writeBytes(audio) }
    val player = MediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        setDataSource(file.absolutePath)
        isLooping = true
        prepare()
    }
    return object : LoopingTrack {
        override fun play() = player.start()
        override fun pause() = player.pause()
        override fun setVolume(volume: Float) = player.setVolume(volume, volume)
        override fun release() {
            player.release()
            file.delete()
        }
    }
}
