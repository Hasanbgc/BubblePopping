package com.apps.bubblepopping.game

import com.apps.bubblepopping.view.play.component.BubbleType

/**
 * Lifetime-style counters for one round, in every mode. Missions and
 * achievements read the [RunStats] it builds when the round ends.
 */
class RunTracker {
    private var pops = 0
    private val popsByType = mutableMapOf<BubbleType, Int>()
    private var bestCombo = 0
    private var bestBombChain = 0
    private var windSaves = 0
    private var bossDefeated = false
    private var elapsedSec = 0
    private var wavesCleared = 0

    fun onEvent(event: GameEvent) {
        when (event) {
            is GameEvent.Popped -> if (event.bubble.type != BubbleType.POISON) {
                pops++
                popsByType[event.bubble.type] = (popsByType[event.bubble.type] ?: 0) + 1
            }
            is GameEvent.ComboChanged -> bestCombo = maxOf(bestCombo, event.combo)
            is GameEvent.BombExploded -> bestBombChain = maxOf(bestBombChain, event.chainSize)
            is GameEvent.WindSave -> windSaves++
            is GameEvent.BossDefeated -> bossDefeated = true
            is GameEvent.Tick -> elapsedSec = event.elapsedSec
            is GameEvent.WaveCleared -> wavesCleared = maxOf(wavesCleared, event.wave)
            is GameEvent.Missed, is GameEvent.Tapped, is GameEvent.BossHit, GameEvent.ShieldUsed,
            is GameEvent.WaveStarted, is GameEvent.GameOver,
            is GameEvent.ReviveOffered, is GameEvent.Revived -> Unit
        }
    }

    fun stats(): RunStats = RunStats(
        pops = pops,
        popsByType = popsByType.toMap(),
        bestCombo = bestCombo,
        bestBombChain = bestBombChain,
        windSaves = windSaves,
        bossDefeated = bossDefeated,
        elapsedSec = elapsedSec,
        wavesCleared = wavesCleared,
    )
}

/** What happened in one round (bubbles popped exclude skulls). */
data class RunStats(
    val pops: Int = 0,
    val popsByType: Map<BubbleType, Int> = emptyMap(),
    val bestCombo: Int = 0,
    val bestBombChain: Int = 0,
    val windSaves: Int = 0,
    val bossDefeated: Boolean = false,
    val elapsedSec: Int = 0,
    /** Endless: the highest wave survived (0 if the round ended in wave 1). */
    val wavesCleared: Int = 0,
)
