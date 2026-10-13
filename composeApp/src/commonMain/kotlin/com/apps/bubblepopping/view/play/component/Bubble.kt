package com.apps.bubblepopping.view.play.component


import androidx.compose.ui.graphics.Color
import com.apps.bubblepopping.game.BubbleSize
import com.apps.bubblepopping.game.GameRules
import com.apps.bubblepopping.stage.BossConfig

enum class BubbleType {
    NORMAL,
    /** Popping it ends the round (unless a shield is up). */
    POISON,
    /** Gives back a lost life. */
    HEART,
    /** Small, fast and rare; worth 25 points. */
    GOLDEN,
    /** Slows every bubble for a few seconds. */
    FREEZE,
    /** Pops everything around it — skulls too. Bombs caught in the blast explode as well. */
    BOMB,
    /** Bursts into three small bubbles. */
    SPLITTER,
    /** Absorbs the next skull pop. */
    SHIELD,
}

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

/**
 * One copy of a boss on screen (a splitting boss has several). Positions are
 * mutated every frame by the ViewModel, like [Bubble]'s.
 *
 * It shrinks from [fullRadius] towards [GameRules.BOSS_MIN_SCALE] of it as [hp] drops.
 */
class BossBubble(
    val id: String,
    val config: BossConfig,
    /** 0 for the original boss, 1 for its first split copies, and so on. */
    val generation: Int,
    val maxHp: Int,
    val fullRadius: Float,
    /** Offsets this copy's movement so split copies don't overlap. */
    val motionPhase: Float,
    var x: Float,
    var y: Float,
) {
    var hp: Int = maxHp
    /** Seconds left of the white hit flash and wobble. */
    var hitFlash: Float = 0f

    val hpFraction: Float get() = hp.toFloat() / maxHp
    val radius: Float get() = fullRadius * (GameRules.BOSS_MIN_SCALE + (1f - GameRules.BOSS_MIN_SCALE) * hpFraction)
}

/** A confetti scrap from a boss defeat; falls and spins until [life] runs out. */
class Confetti(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val size: Float,
    var rotation: Float,
    val spin: Float,
    var life: Float,
)

