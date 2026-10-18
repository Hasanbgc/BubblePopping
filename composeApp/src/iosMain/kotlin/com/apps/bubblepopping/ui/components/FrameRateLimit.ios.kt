package com.apps.bubblepopping.ui.components

import androidx.compose.runtime.Composable

/** iOS: the refresh rate is set app-wide (CADisableMinimumFrameDurationOnPhone in Info.plist), so nothing to do here. */
@Composable
actual fun LimitFrameRate(maxFps: Float) = Unit
