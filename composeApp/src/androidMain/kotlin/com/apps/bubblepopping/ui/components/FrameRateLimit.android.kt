package com.apps.bubblepopping.ui.components

import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

/**
 * Picks the fastest display mode at the current resolution that stays within
 * [maxFps] and also passes [maxFps] as the window's preferred refresh rate,
 * which covers phones whose high rates are a single variable-rate mode.
 */
@Composable
actual fun LimitFrameRate(maxFps: Float) {
    val activity = LocalActivity.current ?: return
    DisposableEffect(activity, maxFps) {
        val window = activity.window
        val previous = window.attributes
        val previousModeId = previous.preferredDisplayModeId
        val previousRate = previous.preferredRefreshRate

        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.display
        } else {
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay
        }
        val current = display?.mode
        val mode = display?.supportedModes
            ?.filter {
                it.physicalWidth == current?.physicalWidth &&
                    it.physicalHeight == current.physicalHeight &&
                    it.refreshRate <= maxFps + REFRESH_TOLERANCE
            }
            ?.maxByOrNull { it.refreshRate }

        window.attributes = window.attributes.apply {
            if (mode != null) preferredDisplayModeId = mode.modeId
            preferredRefreshRate = mode?.refreshRate ?: maxFps
        }
        onDispose {
            window.attributes = window.attributes.apply {
                preferredDisplayModeId = previousModeId
                preferredRefreshRate = previousRate
            }
        }
    }
}

/** Display modes report rates like 60.000004 Hz. */
private const val REFRESH_TOLERANCE = 0.5f
