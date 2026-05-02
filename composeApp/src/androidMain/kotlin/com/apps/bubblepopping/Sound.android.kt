package com.apps.bubblepopping

import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.io.FileOutputStream

actual fun createSoundManager(audioData: ByteArray): SoundManager {
    val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    val soundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(attributes)
        .build()
    // SoundPool.load() needs a file path or FD, so write bytes to a temp file
    val tempFile = File.createTempFile("pop_sound", ".mp3")
    FileOutputStream(tempFile).use { it.write(audioData) }

    var soundId  = -1
    var isLoaded = false

    soundPool.setOnLoadCompleteListener { _, _, status ->
        if (status == 0) {
            isLoaded = true
        }
        try { tempFile.delete() } catch(_: Exception) {}
    }

    // Use FileDescriptor for reliable loading
    val fis = java.io.FileInputStream(tempFile)
    soundId = soundPool.load(fis.fd, 0, audioData.size.toLong(), 1)
    fis.close() 

    return SoundManager(
        trigger = {
            if (isLoaded) {
                soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
            }
        }
    ).also { it.onDispose = { soundPool.release() } }
}