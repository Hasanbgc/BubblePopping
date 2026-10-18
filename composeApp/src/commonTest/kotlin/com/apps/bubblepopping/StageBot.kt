package com.apps.bubblepopping

import androidx.compose.ui.geometry.Offset
import com.apps.bubblepopping.game.GameMode
import com.apps.bubblepopping.game.GameOverReason
import com.apps.bubblepopping.game.GameRules
import com.apps.bubblepopping.stage.Objective
import com.apps.bubblepopping.stage.ObjectiveProgress
import com.apps.bubblepopping.stage.StageConfig
import com.apps.bubblepopping.view.BubbleGameViewModel
import com.apps.bubblepopping.view.play.component.Bubble
import com.apps.bubblepopping.view.play.component.BubbleType
import kotlin.math.hypot
import kotlin.random.Random

/**
 * How well a simulated player plays: how fast it taps, how often a tap lands, and
 * how often it picks the bubble that matters most rather than just one it sees.
 */
data class PlayerSkill(
    val name: String,
    /** Seconds between taps: seeing a bubble, moving to it, tapping. */
    val tapInterval: Float,
    /** Chance a tap hits the bubble it aims at; the rest miss. */
    val aim: Float,
    /** Chance it goes for the most pressing bubble; otherwise any one in reach. */
    val focus: Float,
) {
    companion object {
        val GOOD = PlayerSkill("good", tapInterval = 0.32f, aim = 0.93f, focus = 1f)
        val CASUAL = PlayerSkill("casual", tapInterval = 0.7f, aim = 0.75f, focus = 0.4f)
        /** A player who has got better by the later worlds: the yardstick from stage 41 on. */
        val IMPROVING = PlayerSkill("improving", tapInterval = 0.5f, aim = 0.85f, focus = 0.7f)
    }
}

/** One simulated attempt at a stage. [objectives] is how far each objective got (main goal first). */
data class SimResult(
    val stars: Int,
    val seconds: Float,
    val score: Int,
    val objectives: List<ObjectiveProgress>,
    val reason: GameOverReason?,
) {
    val won: Boolean get() = stars > 0

    fun met(index: Int): Boolean = objectives[index].satisfied
}

/**
 * A simulated player for the stage simulator: plays the real game on a phone-sized
 * canvas at a human pace. It works towards the stage's objectives, saves the
 * bubbles about to escape, keeps skulls out of bomb blasts and never taps a skull
 * on purpose — the mistakes it makes are slow taps and missed taps.
 */
class StageBot(private val skill: PlayerSkill) {

    /** Taps since the last one on a boss. */
    private var tapsSinceBoss = 0

    fun play(stage: StageConfig, seed: Long): SimResult {
        val random = Random(seed * 31 + skill.name.hashCode())
        val game = BubbleGameViewModel(
            mode = GameMode.Stage(stage.id),
            progressRepository = FakeProgressRepository(),
            settingsRepository = FakeSettingsRepository(),
            metaRepository = FakeMetaRepository(),
            stage = stage,
            seed = seed,
        ).apply { setCanvasBounds(WIDTH, HEIGHT, topOffset = TOP, density = DENSITY) }
        val wants = Wants(stage)

        var time = 0f
        var nextTap = skill.tapInterval
        var nextSwipe = 0f
        while (!game.isGameOver && time < MAX_SECONDS) {
            game.update(FRAME)
            time += FRAME
            if (game.isGameOver) break
            if (wants.wind && time >= nextSwipe && blowSkull(game)) nextSwipe = time + SWIPE_COOLDOWN
            if (time >= nextTap) {
                tap(game, wants, random)
                nextTap = time + skill.tapInterval
            }
        }
        // A boss win ends after its slow-motion celebration.
        while (!game.isGameOver && time < MAX_SECONDS + CELEBRATION_ALLOWANCE) {
            game.update(FRAME)
            time += FRAME
        }
        val result = game.stageResult
        return SimResult(
            stars = result?.stars ?: 0,
            seconds = time,
            score = game.score,
            // A round still going at the cut-off counts as lost, with the progress it had.
            objectives = result?.objectives ?: game.objectives,
            reason = game.gameOverReason,
        )
    }

    /** What the stage's objectives ask the bot to go for. */
    private class Wants(stage: StageConfig) {
        private val goals = stage.allObjectives.flatMap { if (it is Objective.AllOf) it.goals else listOf(it) }
        val types = goals.filterIsInstance<Objective.PopType>().map { it.type }.toSet() +
            (if (goals.any { it is Objective.CrackArmored }) setOf(BubbleType.ARMORED) else emptySet()) +
            (if (goals.any { it is Objective.LightningZaps }) setOf(BubbleType.LIGHTNING) else emptySet())
        /** Leave armored bubbles for bombs and lightning. */
        val blastArmor = goals.any { it is Objective.BreakArmoredWithBlast }
        val sizes = goals.filterIsInstance<Objective.PopSize>().map { it.size }.toSet()
        val wind = goals.any { it is Objective.WindSaves }
        /** Bubbles a bomb should catch before it's worth setting off. */
        val bombCrowd = goals.filterIsInstance<Objective.BombChain>().maxOfOrNull { it.bubbles }
            // "Pop N bombs" wants them popped whenever it's safe.
            ?: if (BubbleType.BOMB in types) 0 else DEFAULT_BOMB_CROWD
        /** Let bubbles gather around bombs instead of popping them one by one. */
        val chains = goals.any { it is Objective.BombChain }
        /** The longest combo asked for, if any: worth letting a crowd gather first. */
        val combo = goals.mapNotNull {
            when (it) {
                is Objective.ReachCombo -> it.combo
                is Objective.ComboTimes -> it.combo
                else -> null
            }
        }.maxOrNull()
    }

    private fun tap(game: BubbleGameViewModel, wants: Wants, random: Random) {
        if (game.tapLockLeft > 0f) return
        val point = choosePoint(game, wants, random) ?: return
        if (random.nextFloat() < skill.aim) game.tryPop(point) else game.onMissedTap()
    }

    /** Where to tap: the most pressing bubble (or the boss), as long as no skull sits on top of it. */
    private fun choosePoint(game: BubbleGameViewModel, wants: Wants, random: Random): Offset? {
        val skulls = game.bubbles.filter { it.type == BubbleType.POISON }
        val bombs = game.bubbles.filter { it.type == BubbleType.BOMB && it.y < HEIGHT }
        val blast = GameRules.BOMB_RADIUS_DP * DENSITY
        val candidates = game.bubbles
            .filter { it.type != BubbleType.POISON && it.y < HEIGHT && it.y > TOP }
            .filter { it.type != BubbleType.BOMB || bombIsWorthIt(game, it, skulls, wants) }
            // Lightning zaps the nearest bubbles, so it's saved for a crowd (or a boss).
            .filter { bubble ->
                bubble.type != BubbleType.LIGHTNING || game.bosses.isNotEmpty() || urgency(bubble) > BOMB_LAST_CHANCE ||
                    game.bubbles.count { it.type != BubbleType.POISON && it.type != BubbleType.BOMB && it !== bubble } >= GameRules.LIGHTNING_ZAPS
            }
            // Chain goals: leave the crowd around a bomb alone unless a bubble is about to escape.
            .filter { bubble ->
                !wants.chains || bubble.type == BubbleType.BOMB || urgency(bubble) > CROWD_RELEASE_URGENCY ||
                    bombs.none { hypot(it.x - bubble.x, it.y - bubble.y) - bubble.radius <= blast }
            }
            // Blast goals: leave armored bubbles for bombs and lightning unless one is about to escape.
            .filter { bubble -> !wants.blastArmor || bubble.type != BubbleType.ARMORED || urgency(bubble) > CROWD_RELEASE_URGENCY }
            .sortedByDescending { priority(game, it, wants, skulls.isNotEmpty()) }

        // Bosses: keep hitting unless a normal bubble is about to escape — but never stop for
        // long (every few taps go to the boss regardless), and never into an ice shield.
        val boss = game.bosses.lastOrNull()?.takeIf { !it.shielded && !it.blastOnly }
        val rushed = candidates.any { it.type == BubbleType.NORMAL && urgency(it) > BOSS_IGNORE_URGENCY }
        tapsSinceBoss++
        if (boss != null && (!rushed || tapsSinceBoss >= BOSS_EVERY_TAPS)) {
            val ring = listOf(Offset.Zero, Offset(0.5f, 0f), Offset(-0.5f, 0f), Offset(0f, 0.5f), Offset(0f, -0.5f))
            ring.map { Offset(boss.x + it.x * boss.radius, boss.y + it.y * boss.radius) }
                .firstOrNull { firstBubbleAt(game, it) == null }
                ?.let {
                    tapsSinceBoss = 0
                    return it
                }
        }
        // Combo goals: start a burst only once a crowd has gathered (or something is about to escape).
        val comboTarget = wants.combo
        if (comboTarget != null && game.combo == 0) {
            val crowd = minOf((comboTarget * COMBO_CROWD_SHARE).toInt() + 1, MAX_COMBO_CROWD)
            val urgent = candidates.any { it.type == BubbleType.NORMAL && urgency(it) > COMBO_RELEASE_URGENCY }
            if (candidates.size < crowd && !urgent) return null
        }
        val reachable = candidates.filter { firstBubbleAt(game, Offset(it.x, it.y)) === it }
        if (reachable.isEmpty()) return null
        // A distracted player taps whichever bubble catches its eye.
        val pick = if (random.nextFloat() < skill.focus) reachable.first() else reachable[random.nextInt(reachable.size)]
        return Offset(pick.x, pick.y)
    }

    /** 0 just above the bottom, 1 at the top edge (about to escape). */
    private fun urgency(bubble: Bubble): Float = 1f - ((bubble.y - TOP) / (HEIGHT - TOP)).coerceIn(0f, 1f)

    private fun priority(game: BubbleGameViewModel, bubble: Bubble, wants: Wants, skullsAround: Boolean): Float {
        // Only normal bubbles cost a life when they escape.
        var value = urgency(bubble) * if (bubble.type == BubbleType.NORMAL || bubble.type == BubbleType.ARMORED) 1f else OTHER_URGENCY
        if (bubble.type in wants.types) value += WANTED
        if (bubble.size in wants.sizes && bubble.type == BubbleType.NORMAL) value += WANTED
        value += when (bubble.type) {
            BubbleType.GOLDEN -> GOLDEN
            BubbleType.HEART -> if (game.missedCount > 0) HEART_WHEN_HURT else 0f
            BubbleType.SHIELD -> if (skullsAround) HELPER else 0f
            // Ice breaks a boss's shield.
            BubbleType.FREEZE -> if (game.bosses.any { it.shielded }) WANTED else HELPER
            BubbleType.CLOCK, BubbleType.DOUBLE -> HELPER
            BubbleType.LIGHTNING -> if (game.bosses.isNotEmpty()) WANTED else HELPER
            else -> 0f
        }
        return value
    }

    /** A bomb is set off once it would catch a crowd — and never with a skull in reach. */
    private fun bombIsWorthIt(game: BubbleGameViewModel, bomb: Bubble, skulls: List<Bubble>, wants: Wants): Boolean {
        val blast = GameRules.BOMB_RADIUS_DP * DENSITY
        fun inBlast(center: Bubble, other: Bubble, margin: Float = 0f) =
            hypot(other.x - center.x, other.y - center.y) - other.radius <= blast + margin

        // Bombs caught in the blast go off too, so the whole chain must be clear of skulls —
        // all but one with a shield ready, which absorbs a single skull.
        val chain = mutableListOf(bomb)
        var i = 0
        while (i < chain.size) {
            game.bubbles.filter { it.type == BubbleType.BOMB && chain.none { b -> b === it } && inBlast(chain[i], it) }
                .forEach(chain::add)
            i++
        }
        val skullsCaught = skulls.count { skull -> chain.any { inBlast(it, skull, SAFETY_MARGIN_PX) } }
        if (skullsCaught > if (game.shieldActive) 1 else 0) return false
        val caught = game.bubbles.count { bubble -> bubble !== bomb && chain.any { inBlast(it, bubble) } }
        // Waiting for a crowd, but not past the point the bomb would float away.
        val leaving = urgency(bomb) > BOMB_LAST_CHANCE && caught >= DEFAULT_BOMB_CROWD
        val armorInReach = wants.blastArmor &&
            game.bubbles.any { bubble -> bubble.type == BubbleType.ARMORED && chain.any { inBlast(it, bubble) } }
        return caught >= wants.bombCrowd || leaving || armorInReach ||
            game.bosses.any { hypot(it.x - bomb.x, it.y - bomb.y) - it.radius <= blast }
    }

    /** The bubble a tap at [point] would pop: the first one in the list whose touch area covers it. */
    private fun firstBubbleAt(game: BubbleGameViewModel, point: Offset): Bubble? {
        val minHit = GameRules.MIN_HIT_RADIUS_DP * DENSITY
        return game.bubbles.firstOrNull { hypot(point.x - it.x, point.y - it.y) <= maxOf(it.radius, minHit) }
    }

    /** Swipes a skull in the top half off the nearer side. Returns true if it swiped. */
    private fun blowSkull(game: BubbleGameViewModel): Boolean {
        val skull = game.bubbles
            .filter { it.type == BubbleType.POISON && it.y < TOP + (HEIGHT - TOP) * WIND_ZONE }
            .minByOrNull { it.y } ?: return false
        game.swipeWind(if (skull.x < WIDTH / 2f) -SWIPE else SWIPE)
        return true
    }

    private companion object {
        const val WIDTH = 1080f
        const val HEIGHT = 2340f
        const val TOP = 200f
        const val DENSITY = 2.75f
        const val FRAME = 1f / 30f
        /** Rounds longer than this are cut off (nothing is designed to run that long). */
        const val MAX_SECONDS = 600f
        const val CELEBRATION_ALLOWANCE = 5f

        const val OTHER_URGENCY = 0.6f
        const val WANTED = 1f
        const val GOLDEN = 0.5f
        const val HEART_WHEN_HURT = 0.8f
        const val HELPER = 0.3f
        const val DEFAULT_BOMB_CROWD = 3
        /** Above this, a bubble near a bomb is popped anyway rather than left to escape. */
        const val CROWD_RELEASE_URGENCY = 0.8f
        /** A bomb this high goes off with whatever crowd it has. */
        const val BOMB_LAST_CHANCE = 0.75f
        const val SAFETY_MARGIN_PX = 20f
        /** A normal bubble higher than this pulls the bot off the boss. */
        const val BOSS_IGNORE_URGENCY = 0.6f
        /** Even while saving bubbles, at least one tap in this many goes to the boss. */
        const val BOSS_EVERY_TAPS = 3
        /** Combo goals: wait for about this share of the combo on screen before starting… */
        const val COMBO_CROWD_SHARE = 0.55f
        const val MAX_COMBO_CROWD = 14
        /** …unless a bubble is this close to escaping. */
        const val COMBO_RELEASE_URGENCY = 0.75f

        const val WIND_ZONE = 0.5f
        /** A swipe across this fraction of the screen width. */
        const val SWIPE = 0.35f
        const val SWIPE_COOLDOWN = 1.2f
    }
}
