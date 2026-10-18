package com.apps.bubblepopping.view.play.component


import androidx.compose.ui.geometry.Offset
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
    /** Adds a few seconds to a stage's timer. Only spawns in stages with a time limit. */
    CLOCK,
    /** Doubles every point scored for a few seconds. */
    DOUBLE,
    /** Zaps the nearest bubbles (never skulls), popping them all at once. */
    LIGHTNING,
    /** Takes two taps: the first cracks it. A bomb or lightning breaks it at once. Escaping costs a life. */
    ARMORED,
    /** Fades in and out; it can only be popped while it can be seen. Escaping costs a life. */
    GHOST,
    /** Comes in tied pairs: pop both quickly for a bonus, or the popped one comes back. Escaping costs a life. */
    CHAIN,
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
    /** Armored bubbles: true once the first tap has cracked the armor. */
    var cracked: Boolean = false,
    /** Ghost bubbles: how visible it is right now, 0 → 1; it can only be popped while mostly visible. */
    var visibility: Float = 1f,
    /** Chain bubbles: the id the two bubbles of a pair share. */
    val linkId: String? = null,
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
    /** Behind an ice shield ([com.apps.bubblepopping.stage.BossTrick.IceShield]): taps and blasts do nothing. */
    var shielded: Boolean = false
    /** The fight ran too long: faster, nastier, and drawn glowing red. */
    var enraged: Boolean = false
    /** Bomb armor ([com.apps.bubblepopping.stage.BossTrick.BlastArmor]): only bombs and lightning hurt it. */
    var blastOnly: Boolean = false
    /** Armor plates left ([com.apps.bubblepopping.stage.BossTrick.Plates]); each must break before HP drops. */
    var plates: Int = 0
    /** Phantom ([com.apps.bubblepopping.stage.BossTrick.Fade]): how visible it is, 0 → 1; it can only be hit while mostly visible. */
    var visibility: Float = 1f
    /** Seconds a lightning strike keeps a fading boss in plain sight. */
    var revealLeft: Float = 0f
    /** Taps on the current plate so far. */
    var plateHits: Int = 0

    val hpFraction: Float get() = hp.toFloat() / maxHp
    val radius: Float get() = fullRadius * (GameRules.BOSS_MIN_SCALE + (1f - GameRules.BOSS_MIN_SCALE) * hpFraction)
}

/** A lightning bolt from a lightning bubble to a zapped bubble; fades as [life] runs out. */
class LightningBolt(val id: Long, val from: Offset, val to: Offset, var life: Float)

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

