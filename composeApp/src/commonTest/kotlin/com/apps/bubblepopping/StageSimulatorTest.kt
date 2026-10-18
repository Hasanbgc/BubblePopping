package com.apps.bubblepopping

import com.apps.bubblepopping.game.GameOverReason
import com.apps.bubblepopping.game.Pace
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
import kotlin.test.assertTrue

/**
 * Plays every catalog stage with simulated players (`stage-plan.md` §2.3) to catch
 * stages that can't be won, bonus stars nobody can get, and — for stages designed
 * to a length — stages that are over too fast or too easy. [report] prints how
 * every stage went (see the test's standard output).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StageSimulatorTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun everyStage_canBeWonByAGoodPlayer() {
        val failures = runs().filter { (_, bySkill) -> bySkill.getValue(PlayerSkill.GOOD).none { it.won } }
            .map { (stage, bySkill) -> "#${stage.id} ${stage.goal.description} (best ${best(bySkill.getValue(PlayerSkill.GOOD), 0)})" }
        assertTrue(failures.isEmpty(), "A good player never won these in $SEEDS tries: $failures")
    }

    @Test
    fun everyBonusStar_canBeEarnedByAGoodPlayer() {
        val failures = runs().flatMap { (stage, bySkill) ->
            val good = bySkill.getValue(PlayerSkill.GOOD)
            stage.bonusGoals.indices.map { it + 1 }
                .filter { index -> good.none { it.met(index) } }
                .map { index -> "#${stage.id} ${stage.allObjectives[index].description} (best ${best(good, index)})" }
        }
        assertTrue(failures.isEmpty(), "A good player never earned these stars in $SEEDS tries: $failures")
    }

    @Test
    fun designedStages_lastTheirLength_andArentTooEasy() {
        val failures = runs().filterKeys { it.designSec != null }.flatMap { (stage, bySkill) ->
            val design = requireNotNull(stage.designSec)
            val good = bySkill.getValue(PlayerSkill.GOOD)
            // The yardstick for "too easy" and "never won": a casual player early on, an improving one later.
            val yardstick = yardstickFor(stage)
            val benchmark = bySkill.getValue(yardstick)
            buildList {
                val fastest = good.filter { it.won }.minOfOrNull { it.seconds }
                if (fastest != null && fastest < design * MIN_LENGTH_SHARE) {
                    add("#${stage.id} won in ${fastest.roundToInt()} s, designed for $design s")
                }
                // Stages 1–10 teach the game, so they may be easy.
                if (stage.boss == null && stage.id > LEARNING_STAGES) {
                    if (benchmark.count { it.stars == stage.maxStars } > MAX_CASUAL_FULL_STARS) {
                        add("#${stage.id} a ${yardstick.name} player gets every star")
                    }
                    if (benchmark.none { it.won }) add("#${stage.id} a ${yardstick.name} player never wins")
                }
            }
        }
        assertTrue(failures.isEmpty(), "Stages off their design: $failures")
    }

    @Test
    fun report() {
        println(buildString {
            appendLine("STAGE SIMULATION ($SEEDS runs per player)")
            appendLine("won = wins · ★ = average stars · t = average winning time · met = runs meeting each objective")
            appendLine("lost = why lost rounds ended: P skull, L out of lives, T time up")
            for ((stage, bySkill) in runs()) {
                append("#${stage.id.toString().padStart(3)} ${stage.goal.description.take(34).padEnd(34)}")
                stage.designSec?.let { append(" [${it}s]") }
                for ((skill, results) in bySkill) {
                    val wins = results.filter { it.won }
                    val winTime = wins.map { it.seconds }.average().takeIf { wins.isNotEmpty() }?.roundToInt()
                    val met = stage.allObjectives.indices.joinToString("") { i -> results.count { it.met(i) }.toString() }
                    val losses = results.filter { !it.won }.joinToString("") {
                        when (it.reason) {
                            GameOverReason.POISON -> "P"
                            GameOverReason.OUT_OF_LIVES -> "L"
                            GameOverReason.TIME_UP -> "T"
                            else -> "?"
                        }
                    }
                    append(" | ${skill.name} won ${wins.size}/$SEEDS ★${"%.1f".format(results.map { it.stars }.average())}")
                    val score = results.map { it.score }.average().roundToInt()
                    append(" t=${winTime ?: "-"}s pts=$score met=$met lost=${losses.ifEmpty { "-" }}")
                }
                appendLine()
            }
            // The designed stages as `stage-plan.md` lists them, to keep the plan in step with the game.
            appendLine()
            appendLine("| # | Goal ★ | ★★ | ★★★ | Time | Lives | Pace |")
            appendLine("|---|---|---|---|---|---|---|")
            for (stage in runs().keys.filter { it.designSec != null }) {
                val stars = stage.bonusGoals.map { it.description }
                val time = stage.durationSec?.let { "$it s" } ?: "~${stage.designSec} s"
                val pace = paceOf(stage.spawn.spawnInterval) + paceOf(stage.spawn.minSpawnInterval)
                    .takeIf { it != paceOf(stage.spawn.spawnInterval) }?.let { "→$it" }.orEmpty()
                appendLine("| ${stage.id} | ${stage.goal.description} | ${stars.getOrElse(0) { "" }} | ${stars.getOrElse(1) { "" }} | $time | ${stage.lives} | $pace |")
            }
        })
    }

    private fun paceOf(spawnInterval: Float): String =
        Pace.entries.firstOrNull { it.spawnInterval == spawnInterval }?.name ?: "-"

    /** The furthest any run got on objective [index], as "current/target". */
    private fun best(results: List<SimResult>, index: Int): String {
        val top = results.map { it.objectives[index] }.maxBy { it.current }
        return "${top.current}/${top.target}"
    }

    private companion object {
        const val SEEDS = 4
        /** A good player must not win in under this share of the design length. */
        const val MIN_LENGTH_SHARE = 0.8f
        /** Out of [SEEDS], how many runs a casual player may get every star in. */
        const val MAX_CASUAL_FULL_STARS = 1
        /** The first stages teach the game and are allowed to be easy. */
        const val LEARNING_STAGES = 10
        val SKILLS = listOf(PlayerSkill.GOOD, PlayerSkill.IMPROVING, PlayerSkill.CASUAL)
        /** Players get better as they go: from this stage on, the improving player is the yardstick. */
        const val IMPROVING_FROM_STAGE = 41

        fun yardstickFor(stage: StageConfig): PlayerSkill =
            if (stage.id >= IMPROVING_FROM_STAGE) PlayerSkill.IMPROVING else PlayerSkill.CASUAL

        /** Every stage, played [SEEDS] times by each skill — simulated once, shared by all the tests. */
        private var cache: Map<StageConfig, Map<PlayerSkill, List<SimResult>>>? = null

        fun runs(): Map<StageConfig, Map<PlayerSkill, List<SimResult>>> = cache ?: StageCatalog.stages
            .associateWith { stage ->
                SKILLS.associateWith { skill -> (1..SEEDS).map { StageBot(skill).play(stage, seed = it.toLong()) } }
            }
            .also { cache = it }
    }
}
