package com.apps.bubblepopping.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.bubblepopping.HapticFeedback
import com.apps.bubblepopping.Res
import com.apps.bubblepopping.SoundManager
import com.apps.bubblepopping.createSoundManager
import com.apps.bubblepopping.data.prefs.GameSettings
import com.apps.bubblepopping.data.prefs.SettingsRepository

/**
 * Gives every game control a soft click: a high, short pop and a light buzz,
 * each following the Sound / Vibration settings.
 */
@Composable
fun UiFeedbackProvider(
    settingsRepository: SettingsRepository,
    hapticFeedback: HapticFeedback,
    content: @Composable () -> Unit,
) {
    val settings by remember(settingsRepository) { settingsRepository.settings }
        .collectAsStateWithLifecycle(GameSettings())
    val sound by produceState<SoundManager?>(initialValue = null) {
        value = try {
            createSoundManager(listOf(Res.readBytes(CLICK_SAMPLE)))
        } catch (_: Exception) {
            null // no click sound rather than a crash
        }
    }
    DisposableEffect(sound) {
        val current = sound
        onDispose { current?.dispose() }
    }
    val feedback = UiFeedback {
        if (settings.soundEnabled) sound?.play(sample = 0, rate = CLICK_RATE)
        if (settings.hapticsEnabled) hapticFeedback.light()
    }
    CompositionLocalProvider(LocalUiFeedback provides feedback, content = content)
}

private const val CLICK_SAMPLE = "files/pop_2.mp3"
/** Pitched up: a quick, light "tick" rather than a full bubble pop. */
private const val CLICK_RATE = 1.7f
