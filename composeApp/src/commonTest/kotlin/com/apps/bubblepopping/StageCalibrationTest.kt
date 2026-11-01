package com.apps.bubblepopping

import com.apps.bubblepopping.stage.BossConfig
import com.apps.bubblepopping.stage.Objective
import com.apps.bubblepopping.stage.StageCatalog
import com.apps.bubblepopping.stage.StageConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.math.roundToInt
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * A tuning tool, off by default ([ENABLED]): plays every stage for its full length with
 * nothing able to end it early, measures what each simulated player reaches on every
 * goal, and prints targets sized from that — the yardstick player (casual, or improving
 * from stage 41) for ★, between it and a good player for ★★, near a good player for ★★★.
 * Bosses get an HP that a good player gets through in their design length.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StageCalibrationTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun calibrate() {
        if (!ENABLED) return
        println(buildString {
            appendLine("CALIBRATION")
            for (stage in StageCatalog.stages) appendLine(calibrate(stage))
        })
    }

    private fun calibrate(stage: StageConfig): String {
        val length = stage.durationSec ?: requireNotNull(stage.designSec)
        val boss = stage.boss
        if (boss != null) {
            // Endless HP, so the whole length is measured; the HP is what a good player deals in it.
            val endless = BossConfig(boss.id, boss.name, listOf(ENDLESS_HP), boss.phases, boss.enrageAfterSec, boss.radiusDp, boss.blastDamage, boss.lightningDamage)
            val probe = stage.copy(goal = Objective.DefeatBoss(endless), bonusGoals = emptyList(), durationSec = stage.designSec, designSec = null)
            val dealt = median(runs(probe, PlayerSkill.GOOD).map { it.objectives[0].current })
            return "#${stage.id}|BOSS|${boss.name}|hp=${(dealt * BOSS_HP_SHARE).roundToInt()}|dealt=$dealt"
        }
        // Survive the full length, with the stage's own goals riding along to be measured,
        // and an unreachable score so meeting them all can't end the round early.
        val measured = stage.allObjectives.filter { !it.isConstraint && it !is Objective.Survive }
        val probe = stage.copy(
            goal = Objective.Survive(length),
            bonusGoals = measured + Objective.ReachScore(UNREACHABLE),
            durationSec = length,
            designSec = null,
        )
        val bySkill = SKILLS.associateWith { skill -> runs(probe, skill) }
        val yardstick = if (stage.id >= 41) PlayerSkill.IMPROVING else PlayerSkill.CASUAL
        val learning = stage.id <= 10
        val untimed = stage.durationSec == null

        fun reached(skill: PlayerSkill, objective: Objective): Int =
            median(bySkill.getValue(skill).map { run -> run.objectives[1 + measured.indexOf(objective)].current })

        var previous = 0
        val suggested = stage.allObjectives.mapIndexed { slot, objective ->
            if (objective !in measured) return@mapIndexed objective
            val y = reached(yardstick, objective)
            val g = reached(PlayerSkill.GOOD, objective)
            var target = when {
                // An untimed stage ends at its goal: sized so a good player takes about its length.
                untimed && slot == 0 -> g * 0.9f
                slot == 0 -> y * if (learning) 0.6f else 0.75f
                slot == 1 -> (y + g) / 2f * 0.85f
                else -> g * 0.85f
            }.roundToInt().coerceAtLeast(1)
            // Each star asks for more than the one before when they measure the same thing.
            if (slot > 0 && sameKind(objective, stage.allObjectives[slot - 1])) target = maxOf(target, previous + 1)
            previous = target
            withTarget(objective, rounded(objective, target))
        }
        val stats = SKILLS.joinToString(" ") { skill ->
            "${skill.name}=" + measured.joinToString(",") { reached(skill, it).toString() }
        }
        return "#${stage.id}|${suggested.joinToString("|") { kotlin(it) }}|$stats"
    }

    private fun runs(stage: StageConfig, skill: PlayerSkill) = (1..SEEDS).map { StageBot(skill).play(stage, seed = it.toLong()) }

    private fun median(values: List<Int>): Int = values.sorted()[values.size / 2]

    private fun sameKind(a: Objective, b: Objective) = a::class == b::class && when (a) {
        is Objective.PopType -> a.type == (b as Objective.PopType).type
        is Objective.ComboTimes -> a.combo == (b as Objective.ComboTimes).combo
        else -> true
    }

    /** Scores to a round number; everything else as is. */
    private fun rounded(objective: Objective, value: Int): Int = when (objective) {
        is Objective.ReachScore -> if (value >= 1_000) value / 50 * 50 else if (value >= 100) value / 10 * 10 else value
        else -> value
    }

    private fun withTarget(objective: Objective, value: Int): Objective = when (objective) {
        is Objective.ReachScore -> objective.copy(target = value)
        is Objective.PopCount -> objective.copy(count = value)
        is Objective.PopType -> objective.copy(count = value)
        is Objective.PopSize -> objective.copy(count = value)
        is Objective.ReachCombo -> objective.copy(combo = maxOf(value, 2))
        is Objective.ComboTimes -> objective.copy(times = value)
        is Objective.BombChain -> objective.copy(bubbles = maxOf(value, 2))
        is Objective.WindSaves -> objective.copy(count = value)
        is Objective.LightningZaps -> objective.copy(count = value)
        is Objective.CrackArmored -> objective.copy(count = value)
        is Objective.BreakArmoredWithBlast -> objective.copy(count = value)
        is Objective.ChainPairs -> objective.copy(count = value)
        // A combined goal is scaled as a whole: each part by the same share.
        is Objective.AllOf -> objective
        else -> objective
    }

    /** The objective as it's written in `StageCatalog.kt`. */
    private fun kotlin(objective: Objective): String = when (objective) {
        is Objective.ReachScore -> "Objective.ReachScore(${number(objective.target)})"
        is Objective.PopCount -> "Objective.PopCount(${objective.count})"
        is Objective.PopType -> "Objective.PopType(BubbleType.${objective.type}, ${objective.count})"
        is Objective.PopSize -> "Objective.PopSize(BubbleSize.${objective.size}, ${objective.count})"
        is Objective.ReachCombo -> "Objective.ReachCombo(${objective.combo})"
        is Objective.ComboTimes -> "Objective.ComboTimes(${objective.combo}, ${objective.times})"
        is Objective.Survive -> "Objective.Survive(${objective.seconds})"
        is Objective.BombChain -> "Objective.BombChain(${objective.bubbles})"
        is Objective.WindSaves -> "Objective.WindSaves(${objective.count})"
        is Objective.LightningZaps -> "Objective.LightningZaps(${objective.count})"
        is Objective.CrackArmored -> "Objective.CrackArmored(${objective.count})"
        is Objective.BreakArmoredWithBlast -> "Objective.BreakArmoredWithBlast(${objective.count})"
        is Objective.ChainPairs -> "Objective.ChainPairs(${objective.count})"
        is Objective.AllOf -> "Objective.AllOf(listOf(${objective.goals.joinToString(", ") { kotlin(it) }}))"
        is Objective.DefeatBoss -> "Objective.DefeatBoss(…)"
        Objective.NoMisses -> "Objective.NoMisses"
        Objective.NoLivesLost -> "Objective.NoLivesLost"
        is Objective.LivesLostAtMost -> "Objective.LivesLostAtMost(${objective.lives})"
        is Objective.Accuracy -> "Objective.Accuracy(${objective.percent})"
        is Objective.FinishUnder -> "Objective.FinishUnder(${objective.seconds})"
    }

    private fun number(value: Int) = if (value >= 1_000) "${value / 1_000}_${(value % 1_000).toString().padStart(3, '0')}" else "$value"

    private companion object {
        /** Flip on to retune, then copy the printed targets into the catalog. */
        const val ENABLED = false
        const val SEEDS = 3
        const val UNREACHABLE = 100_000_000
        const val ENDLESS_HP = 100_000
        /** A boss's HP is this share of what a good player deals in its design length. */
        const val BOSS_HP_SHARE = 0.8f
        val SKILLS = listOf(PlayerSkill.GOOD, PlayerSkill.IMPROVING, PlayerSkill.CASUAL)
    }
}
