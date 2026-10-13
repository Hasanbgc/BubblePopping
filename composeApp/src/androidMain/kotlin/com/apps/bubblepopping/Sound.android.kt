package com.apps.bubblepopping

import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.util.concurrent.ConcurrentHashMap

actual fun createSoundManager(samples: List<ByteArray>): SoundManager {
    val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    val soundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(attributes)
        .build()

    // SoundPool loads from a file, so each sample is written to a temp file
    // that is deleted once loading finishes.
    val loaded = ConcurrentHashMap.newKeySet<Int>()
    val pendingFiles = ConcurrentHashMap<Int, File>()
    soundPool.setOnLoadCompleteListener { _, soundId, status ->
        if (status == 0) loaded += soundId
        pendingFiles.remove(soundId)?.delete()
    }

    val soundIds = samples.mapIndexed { index, bytes ->
        val file = File.createTempFile("sound_$index", ".mp3").apply { writeBytes(bytes) }
        soundPool.load(file.absolutePath, 1).also { pendingFiles[it] = file }
    }

    return SoundManager(
        sampleCount = samples.size,
        player = { sample, rate ->
            val soundId = soundIds[sample]
            if (soundId in loaded) soundPool.play(soundId, 1f, 1f, 1, 0, rate)
        },
    ).also { it.onDispose = { soundPool.release() } }
}
