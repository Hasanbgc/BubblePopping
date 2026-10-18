package com.apps.bubblepopping.ui.components

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import kotlin.math.floor

/**
 * The clock behind the menus' decorative motion — twinkling stars, drifting
 * bubbles, breathing buttons, pulsing nodes — in seconds since the screen
 * appeared, ticking once per frame.
 *
 * Read it only while drawing or in a graphics layer, so it never recomposes.
 */
val LocalAmbientTime = staticCompositionLocalOf<State<Float>?> { null }

/**
 * Gives [content] one shared [LocalAmbientTime], so every decorative animation on
 * the screen runs off a single frame loop instead of one each.
 */
@Composable
fun ProvideAmbientClock(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAmbientTime provides rememberAmbientClock(), content = content)
}

/** The screen's ambient clock, or one of its own outside [ProvideAmbientClock] (e.g. in previews). */
@Composable
fun ambientTime(): State<Float> = LocalAmbientTime.current ?: rememberAmbientClock()

@Composable
private fun rememberAmbientClock(): State<Float> {
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var start = -1L
        while (true) {
            withFrameNanos { now ->
                if (start < 0L) start = now
                time.floatValue = (now - start) / 1_000_000_000f
            }
        }
    }
    return time
}

/** 0 → 1 over every [periodMs] of [time] seconds, then starts again. */
fun ambientLoop(time: Float, periodMs: Int): Float {
    val cycles = time * 1000f / periodMs
    return cycles - floor(cycles)
}

/** 0 → 1 → 0 over every 2 × [periodMs], eased each way: a breathing or pulsing value. */
fun ambientPingPong(time: Float, periodMs: Int, easing: Easing = FastOutSlowInEasing): Float {
    val cycles = time * 1000f / periodMs
    val leg = floor(cycles)
    val fraction = easing.transform(cycles - leg)
    return if (leg.toLong() % 2L == 0L) fraction else 1f - fraction
}
