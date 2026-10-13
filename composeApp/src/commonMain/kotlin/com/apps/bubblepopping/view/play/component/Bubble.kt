package com.apps.bubblepopping.view.play.component


import androidx.compose.ui.graphics.Color
import com.apps.bubblepopping.game.BubbleSize

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
 *   x = baseX + sin(yDp * frequency + phase) * amplitude
 *
 * Positions, [radius], [speed] and [amplitude] are in pixels; the ViewModel
 * converts from dp / screen-fractions when the bubble is created.
 */
data class Bubble(
    val id: String,
    var x: Float,
    var y: Float,
    val radius: Float,
    val size: BubbleSize,     // size class — decides points
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
    /** 1 = normal burst; grows with the combo for bigger, brighter bursts. */
    val intensity: Float = 1f,
    var progress: Float = 0f,
)

/**
 * Text that rises and fades from a pop: the points scored ("+15 ×3") or a
 * combo milestone ("GREAT!"). [progress] goes from 0f → 1f, then it is removed.
 */
data class FloatingText(
    val id: String,
    val text: String,
    val x: Float,
    val y: Float,
    val color: Color,
    val isMilestone: Boolean = false,
    var progress: Float = 0f,
)

