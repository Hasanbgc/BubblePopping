package com.apps.bubblepopping.game

import com.apps.bubblepopping.stage.Objective
import com.apps.bubblepopping.stage.ObjectiveProgress
import com.apps.bubblepopping.stage.StageConfig
import com.apps.bubblepopping.view.play.component.BubbleType

/**
 * Tracks stage objectives from the [GameEvent] stream. Feed every event to
 * [onEvent]; query with [progress], [isSatisfied] and [stars].
 */
class ObjectiveTracker {
    private var score = 0
    private var pops = 0
    private var elapsedSec = 0
    private var taps = 0
    private var missedTaps = 0
    private var livesLost = 0
    private var bestCombo = 0
    /** Peaks of the combos that have ended, and of the one running now. */
    private val finishedComboPeaks = mutableListOf<Int>()
    private var currentComboPeak = 0
    private var bestBombChain = 0
    private var windSaves = 0
    private var lightningZaps = 0
    private var armoredByBlast = 0
    private var chainPairs = 0
    private var bossDamage = 0
    private var bossDefeated = false
    private val popsByType = mutableMapOf<BubbleType, Int>()
    private val popsBySize = mutableMapOf<BubbleSize, Int>()

    fun onEvent(event: GameEvent) {
        when (event) {
            is GameEvent.Popped -> {
                score += event.points
                if (event.bubble.type != BubbleType.POISON) {
                    pops++
                    popsByType[event.bubble.type] = (popsByType[event.bubble.type] ?: 0) + 1
                    popsBySize[event.bubble.size] = (popsBySize[event.bubble.size] ?: 0) + 1
                    if (event.blast && event.bubble.type == BubbleType.ARMORED) armoredByBlast++
                }
            }
            is GameEvent.Missed -> if (event.lifeLost) livesLost++
            is GameEvent.Tapped -> {
                taps++
                if (!event.hit) missedTaps++
            }
            is GameEvent.ComboChanged -> {
                bestCombo = maxOf(bestCombo, event.combo)
                // A combo that ends (0) or restarts (not higher than before) closes the previous one.
                if (event.combo <= event.previous) {
                    if (currentComboPeak > 0) finishedComboPeaks += currentComboPeak
                    currentComboPeak = event.combo
                } else {
                    currentComboPeak = maxOf(currentComboPeak, event.combo)
                }
            }
            is GameEvent.BombExploded -> bestBombChain = maxOf(bestBombChain, event.chainSize)
            is GameEvent.WindSave -> windSaves++
            is GameEvent.LightningZapped -> lightningZaps += event.count
            is GameEvent.ChainPaired -> {
                chainPairs++
                score += event.points
            }
            is GameEvent.BossHit -> {
                bossDamage += event.damage
                score += event.points
            }
            is GameEvent.BossDefeated -> {
                bossDefeated = true
                score += event.points
            }
            is GameEvent.Tick -> elapsedSec = event.elapsedSec
            GameEvent.ShieldUsed, GameEvent.ArmorCracked, is GameEvent.GameOver,
            is GameEvent.WaveCleared, is GameEvent.WaveStarted,
            is GameEvent.ReviveOffered, is GameEvent.Revived -> Unit
        }
    }

    fun progress(objective: Objective): ObjectiveProgress = when (objective) {
        is Objective.ReachScore -> goal(objective, score, objective.target)
        is Objective.PopCount -> goal(objective, pops, objective.count)
        is Objective.PopType -> goal(objective, popsByType[objective.type] ?: 0, objective.count)
        is Objective.PopSize -> goal(objective, popsBySize[objective.size] ?: 0, objective.count)
        is Objective.ReachCombo -> goal(objective, bestCombo, objective.combo)
        is Objective.ComboTimes -> goal(
            objective,
            finishedComboPeaks.count { it >= objective.combo } + if (currentComboPeak >= objective.combo) 1 else 0,
            objective.times,
        )
        is Objective.Survive -> goal(objective, elapsedSec, objective.seconds)
        is Objective.BombChain -> goal(objective, bestBombChain, objective.bubbles)
        is Objective.WindSaves -> goal(objective, windSaves, objective.count)
        is Objective.LightningZaps -> goal(objective, lightningZaps, objective.count)
        is Objective.CrackArmored -> goal(objective, popsByType[BubbleType.ARMORED] ?: 0, objective.count)
        is Objective.BreakArmoredWithBlast -> goal(objective, armoredByBlast, objective.count)
        is Objective.ChainPairs -> goal(objective, chainPairs, objective.count)
        is Objective.DefeatBoss ->
            ObjectiveProgress(objective, bossDamage, objective.boss.totalHp, satisfied = bossDefeated)
        Objective.NoMisses -> ObjectiveProgress(objective, missedTaps, 0, missedTaps == 0)
        Objective.NoLivesLost -> ObjectiveProgress(objective, livesLost, 0, livesLost == 0)
        is Objective.LivesLostAtMost ->
            ObjectiveProgress(objective, livesLost, objective.lives, livesLost <= objective.lives)
        is Objective.Accuracy -> {
            // No taps yet counts as perfect.
            val percent = if (taps == 0) 100 else (taps - missedTaps) * 100 / taps
            ObjectiveProgress(objective, percent, objective.percent, percent >= objective.percent)
        }
        is Objective.AllOf -> {
            val parts = objective.goals.map(::progress)
            ObjectiveProgress(
                objective,
                current = parts.sumOf { minOf(it.current, it.target) },
                target = parts.sumOf { it.target },
                satisfied = parts.all { it.satisfied },
            )
        }
        is Objective.FinishUnder ->
            ObjectiveProgress(objective, elapsedSec, objective.seconds, elapsedSec <= objective.seconds)
    }

    fun isSatisfied(objective: Objective): Boolean = progress(objective).satisfied

    /** 0 if the main goal is unmet, else 1 + met bonus objectives. */
    fun stars(stage: StageConfig): Int =
        if (!isSatisfied(stage.goal)) 0 else 1 + stage.bonusGoals.count(::isSatisfied)

    private fun goal(objective: Objective, current: Int, target: Int) =
        ObjectiveProgress(objective, current, target, current >= target)
}
