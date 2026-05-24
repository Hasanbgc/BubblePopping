package com.apps.bubblepopping.view.play.component


import androidx.compose.ui.graphics.Color

enum class BubbleType { NORMAL, POISON, HEART }

/**
 * Represents a single bubble in the game.
 *
 * [x] and [y] are var because they are mutated directly every frame
 * by the ViewModel instead of creating a new copy — keeps the game loop
 * allocation-free and GC-pressure low.
 *
 * [baseX] is the "center column" the bubble oscillates around.
 * Breeze shifts [baseX] over time; the actual [x] is then:
 *   x = baseX + sin(y * frequency + phase) * amplitude
 */
data class Bubble(
    val id: String,
    var x: Float,
    var y: Float,
    val radius: Float,
    val speed: Float,         // pixels per second (upward)
    val amplitude: Float,     // sine-wave swing half-width in pixels
    var baseX: Float,         // center column; drifts with breeze
    val phase: Float,         // sine phase offset so bubbles don't sync
    val color: Color,
    val shimmerAngle: Float,  // degrees — position of the shine highlight
    val type: BubbleType = BubbleType.NORMAL,
)

/**
 * A short-lived burst animation that plays at the position where a
 * bubble was popped.  [progress] goes from 0f → 1f; once it hits 1f
 * the ViewModel removes it from the list.
 */
data class PopAnimation(
    val id: String,
    val x: Float,
    val y: Float,
    val radius: Float,
    val color: Color,
    var progress: Float = 0f,
)

