package com.apps.bubblepopping.ui.components

import androidx.compose.runtime.Composable

/**
 * While in the composition, asks the display to refresh at most [maxFps] times a
 * second (where the platform allows it), and restores the previous rate on
 * leaving. The game is tuned for 60 fps; on 90 and 120 Hz screens this halves
 * the drawing work and the heat that comes with it.
 */
@Composable
expect fun LimitFrameRate(maxFps: Float)
