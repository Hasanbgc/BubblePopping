package com.apps.bubblepopping

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

actual class HapticFeedback(context: Context) {

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    actual fun light() = vibrate(VibrationEffect.EFFECT_TICK, fallbackMs = 10L)
    actual fun medium() = vibrate(VibrationEffect.EFFECT_CLICK, fallbackMs = 18L)
    actual fun heavy() = vibrate(VibrationEffect.EFFECT_HEAVY_CLICK, fallbackMs = 40L)
    actual fun success() = vibrate(VibrationEffect.EFFECT_DOUBLE_CLICK, fallbackMs = 30L)

    /**
     * Uses the system's tuned [predefinedEffect] on Android 10+, and a plain
     * one-shot of [fallbackMs] on older versions.
     */
    private fun vibrate(predefinedEffect: Int, fallbackMs: Long) {
        val vibrator = vibrator?.takeIf { it.hasVibrator() } ?: return
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                vibrator.vibrate(VibrationEffect.createPredefined(predefinedEffect))
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                vibrator.vibrate(VibrationEffect.createOneShot(fallbackMs, VibrationEffect.DEFAULT_AMPLITUDE))
            else -> {
                @Suppress("DEPRECATION")
                vibrator.vibrate(fallbackMs)
            }
        }
    }
}
