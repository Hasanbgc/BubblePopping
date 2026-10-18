package com.apps.bubblepopping.stage

import com.apps.bubblepopping.game.GameRules

/**
 * One thing a boss does to make the fight harder. Tricks that count hits fire
 * on every [everyHits]-th tap that lands on the boss during their phase.
 */
sealed interface BossTrick {
    /** [count] skull bubbles rise from below among the others. */
    data class ThrowSkulls(val everyHits: Int, val count: Int = 1) : BossTrick

    /** The player's taps do nothing for [seconds]. */
    data class Sting(val everyHits: Int, val seconds: Float) : BossTrick

    /** A gust blows every bubble towards one side; the player swipes back. */
    data class Gust(val everyHits: Int) : BossTrick

    /**
     * The boss raises a shield that blocks taps and blasts until an ice bubble is
     * popped. An ice bubble rises with it (and another every few seconds while it holds).
     */
    data class IceShield(val everyHits: Int) : BossTrick

    /** [count] ghost bubbles rise from below: decoys that fade in and out, and cost a life if they escape. */
    data class ThrowGhosts(val everyHits: Int, val count: Int = 1) : BossTrick

    /** Once, as the phase begins, [count] bombs drop out from under the boss. */
    data class DropBombs(val count: Int) : BossTrick

    // The tricks below last for their whole phase rather than firing on hits.

    /**
     * Armor that taps can't get through: only bomb blasts and lightning hurt the boss.
     * Every [everySec] seconds it drops [bombs] bombs, so there's always a way through.
     */
    data class BlastArmor(val everySec: Float, val bombs: Int) : BossTrick

    /** The boss pulls every bubble sideways towards itself, [strength] of the way per second. */
    data class Gravity(val strength: Float) : BossTrick

    /**
     * As the phase begins the boss puts on [count] armor plates; each takes [tapsEach]
     * taps (or one blast) to break, and its HP only drops once they're all gone.
     */
    data class Plates(val count: Int, val tapsEach: Int) : BossTrick

    /**
     * The boss fades in and out, visible [visibleShare] of the time; taps pass through it while
     * it's faded. A lightning strike shows it in plain sight for a few seconds.
     */
    data class Fade(val visibleShare: Float) : BossTrick

    /** Every bubble that escapes (and costs a life) heals the boss by [hp]. */
    data class HealOnEscape(val hp: Int) : BossTrick
}

/**
 * A boss: a giant bubble that takes many taps to defeat.
 *
 * [hpPerGeneration] is the HP of the boss, then of each copy it splits into,
 * and so on — a single entry means it doesn't split. Each split makes
 * [GameRules.BOSS_SPLIT_COUNT] copies.
 *
 * The fight has three phases — above ⅔ of the total HP, above ⅓, below ⅓ — and
 * [phases] lists the tricks of each. [enrageAfterSec] seconds after it arrives the
 * boss enrages: it moves faster, its tricks come twice as often and bubbles spawn
 * faster, so a fight can't be dragged out.
 */
data class BossConfig(
    val id: String,
    val name: String,
    val hpPerGeneration: List<Int>,
    val phases: List<List<BossTrick>>,
    val enrageAfterSec: Int? = null,
    val radiusDp: Float = GameRules.BOSS_RADIUS_DP,
    /** Damage a bomb blast deals it. */
    val blastDamage: Int = GameRules.BOMB_BOSS_DAMAGE,
    /** Damage a lightning bubble deals it. */
    val lightningDamage: Int = GameRules.LIGHTNING_BOSS_DAMAGE,
) {
    init {
        require(hpPerGeneration.isNotEmpty() && hpPerGeneration.all { it > 0 })
        require(phases.size == PHASES) { "A boss has $PHASES phases" }
    }

    /** Taps (or bomb damage) needed to beat every copy of the boss. */
    val totalHp: Int
        get() = hpPerGeneration.withIndex().sumOf { (generation, hp) ->
            hp * pow(GameRules.BOSS_SPLIT_COUNT, generation)
        }

    /** Every trick the boss uses at some point. */
    val allTricks: List<BossTrick> get() = phases.flatten()

    private fun pow(base: Int, exponent: Int): Int {
        var result = 1
        repeat(exponent) { result *= base }
        return result
    }

    companion object {
        const val PHASES = 3
    }
}

/** Every boss, by the stage it guards. See `stage-plan.md` §5. */
object Bosses {
    /** Stage 10: drifts, then throws skulls — faster as it weakens. */
    val CLOUD_KING = BossConfig(
        id = "cloud_king",
        name = "Cloud King",
        hpPerGeneration = listOf(150),
        phases = listOf(
            emptyList(),
            listOf(BossTrick.ThrowSkulls(everyHits = 4)),
            listOf(BossTrick.ThrowSkulls(everyHits = 2)),
        ),
        enrageAfterSec = 150,
    )

    /** Stage 20: skulls, then gusts that push every bubble aside. */
    val STORM_CLOUD = BossConfig(
        id = "storm_cloud",
        name = "Storm Cloud",
        hpPerGeneration = listOf(220),
        phases = listOf(
            listOf(BossTrick.ThrowSkulls(everyHits = 5)),
            listOf(BossTrick.Gust(everyHits = 6), BossTrick.ThrowSkulls(everyHits = 4)),
            listOf(BossTrick.Gust(everyHits = 4), BossTrick.ThrowSkulls(everyHits = 3)),
        ),
        enrageAfterSec = 180,
    )

    /** Stage 30: stings that lock the player's taps, longer each phase. */
    val JELLY_QUEEN = BossConfig(
        id = "jelly_queen",
        name = "Jelly Queen",
        hpPerGeneration = listOf(150),
        phases = listOf(
            listOf(BossTrick.Sting(everyHits = 6, seconds = 1f)),
            listOf(BossTrick.Sting(everyHits = 5, seconds = 1.25f)),
            listOf(BossTrick.Sting(everyHits = 4, seconds = 1.5f), BossTrick.ThrowSkulls(everyHits = 5)),
        ),
        enrageAfterSec = 180,
    )

    /** Stage 40: an ice-breakable tentacle shield, then skulls rising in pairs. */
    val TIDE_KRAKEN = BossConfig(
        id = "tide_kraken",
        name = "Tide Kraken",
        hpPerGeneration = listOf(260),
        phases = listOf(
            listOf(BossTrick.ThrowSkulls(everyHits = 6)),
            listOf(BossTrick.IceShield(everyHits = 15), BossTrick.ThrowSkulls(everyHits = 5)),
            listOf(BossTrick.IceShield(everyHits = 12), BossTrick.ThrowSkulls(everyHits = 4, count = 2)),
        ),
        enrageAfterSec = 210,
    )

    /** Stage 50: drops bombs, then armors up so only bomb blasts (and lightning) hurt it. */
    val NEBULA_CORE = BossConfig(
        id = "nebula_core",
        name = "Nebula Core",
        hpPerGeneration = listOf(300),
        phases = listOf(
            listOf(BossTrick.DropBombs(count = 3), BossTrick.ThrowSkulls(everyHits = 6)),
            listOf(BossTrick.BlastArmor(everySec = 5f, bombs = 2)),
            listOf(BossTrick.DropBombs(count = 3), BossTrick.ThrowSkulls(everyHits = 3)),
        ),
        enrageAfterSec = 210,
        blastDamage = 8,
    )

    /** Stage 60: pulls every bubble towards itself, harder each phase; lightning hurts it double. */
    val BLACK_HOLE = BossConfig(
        id = "black_hole",
        name = "Black Hole",
        hpPerGeneration = listOf(330),
        phases = listOf(
            listOf(BossTrick.Gravity(strength = 0.25f)),
            listOf(BossTrick.Gravity(strength = 0.4f), BossTrick.ThrowSkulls(everyHits = 4, count = 2)),
            listOf(BossTrick.Gravity(strength = 0.6f), BossTrick.ThrowSkulls(everyHits = 3, count = 2)),
        ),
        enrageAfterSec = 225,
        lightningDamage = GameRules.LIGHTNING_BOSS_DAMAGE * 2,
    )

    /** Stage 70: 110 HP, then 3 copies × 35 HP, then 9 copies × 12 HP; the smallest copies throw skulls. */
    val GUMBALL_GIANT = BossConfig(
        id = "gumball_giant",
        name = "Gumball Giant",
        hpPerGeneration = listOf(110, 35, 12),
        phases = listOf(
            emptyList(),
            listOf(BossTrick.ThrowSkulls(everyHits = 8)),
            listOf(BossTrick.ThrowSkulls(everyHits = 5)),
        ),
        enrageAfterSec = 225,
        radiusDp = GameRules.BOSS_RADIUS_DP + 6f,
    )

    /** Stage 80: armor plates, then it heals on every bubble that escapes, then both, with skulls. */
    val SUGAR_GOLEM = BossConfig(
        id = "sugar_golem",
        name = "Sugar Golem",
        hpPerGeneration = listOf(340),
        phases = listOf(
            listOf(BossTrick.Plates(count = 3, tapsEach = 3)),
            listOf(BossTrick.HealOnEscape(hp = 5), BossTrick.ThrowSkulls(everyHits = 5)),
            listOf(BossTrick.Plates(count = 3, tapsEach = 3), BossTrick.HealOnEscape(hp = 5), BossTrick.ThrowSkulls(everyHits = 3)),
        ),
        enrageAfterSec = 240,
    )

    /** Stage 90: fades in and out, sends ghost decoys, and is all but invisible at the end — lightning shows it. */
    val PHANTOM = BossConfig(
        id = "phantom",
        name = "Phantom",
        hpPerGeneration = listOf(320),
        phases = listOf(
            listOf(BossTrick.Fade(visibleShare = 0.65f)),
            listOf(BossTrick.Fade(visibleShare = 0.5f), BossTrick.ThrowGhosts(everyHits = 4, count = 2)),
            listOf(BossTrick.Fade(visibleShare = 0.3f), BossTrick.ThrowGhosts(everyHits = 3, count = 2), BossTrick.ThrowSkulls(everyHits = 4)),
        ),
        enrageAfterSec = 240,
    )

    /**
     * Stage 100, the grand boss: ice armor plates, then frozen taps and skulls, then it
     * shatters into three shards that keep throwing skulls.
     */
    val FROST_TITAN = BossConfig(
        id = "frost_titan",
        name = "Frost Titan",
        hpPerGeneration = listOf(330, 30),
        phases = listOf(
            listOf(BossTrick.Plates(count = 3, tapsEach = 4), BossTrick.ThrowSkulls(everyHits = 6)),
            listOf(BossTrick.Sting(everyHits = 5, seconds = 1.5f), BossTrick.ThrowSkulls(everyHits = 4)),
            listOf(BossTrick.Plates(count = 3, tapsEach = 4), BossTrick.Sting(everyHits = 6, seconds = 1.5f), BossTrick.ThrowSkulls(everyHits = 3)),
        ),
        enrageAfterSec = 270,
        radiusDp = GameRules.BOSS_RADIUS_DP + 10f,
    )
}
