package com.apps.bubblepopping

class SoundManager(private val trigger: () -> Unit) {
    fun playPop() = trigger()
    var onDispose: (() -> Unit)? = null
    fun dispose() = onDispose?.invoke()
}

expect fun createSoundManager(audioData: ByteArray): SoundManager