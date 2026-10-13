package com.apps.bubblepopping.stage

import com.apps.bubblepopping.game.GameRules

/** Each boss's special trick, on top of speeding up as it loses HP. */
enum class BossGimmick {
    /** Every few hits it throws a skull bubble out of its top. */
    THROWS_SKULLS,
    /** Every few hits it stings: the player's taps do nothing for a moment. */
    STINGS,
    /** At low HP it releases bombs — whose blasts also hurt it. */
    DROPS_BOMBS,
    /** When beaten it splits into smaller copies, which split again. */
    SPLITS,
}

/**
 * A boss: a giant bubble that takes many taps to defeat.
 *
 * [hpPerGeneration] is the HP of the boss, then of each copy it splits into,
 * and so on — a single entry means it doesn't split. Each split makes
 * [GameRules.BOSS_SPLIT_COUNT] copies.
 */
data class BossConfig(
    val id: String,
    val name: String,
    val gimmick: BossGimmick,
    val hpPerGeneration: List<Int>,
    val radiusDp: Float = GameRules.BOSS_RADIUS_DP,
) {
    init {
        require(hpPerGeneration.isNotEmpty() && hpPerGeneration.all { it > 0 })
    }

    /** Taps (or bomb damage) needed to beat every copy of the boss. */
    val totalHp: Int
        get() = hpPerGeneration.withIndex().sumOf { (generation, hp) ->
            hp * pow(GameRules.BOSS_SPLIT_COUNT, generation)
        }

    private fun pow(base: Int, exponent: Int): Int {
        var result = 1
        repeat(exponent) { result *= base }
        return result
    }
}

object Bosses {
    val CLOUD_KING = BossConfig(
        id = "cloud_king",
        name = "Cloud King",
        gimmick = BossGimmick.THROWS_SKULLS,
        hpPerGeneration = listOf(20),
    )
    val JELLY_QUEEN = BossConfig(
        id = "jelly_queen",
        name = "Jelly Queen",
        gimmick = BossGimmick.STINGS,
        hpPerGeneration = listOf(26),
    )
    val NEBULA_CORE = BossConfig(
        id = "nebula_core",
        name = "Nebula Core",
        gimmick = BossGimmick.DROPS_BOMBS,
        hpPerGeneration = listOf(32),
    )
    /** 12 HP, then 3 copies × 4 HP, then 9 copies × 1 HP = 33 in total. */
    val GUMBALL_GIANT = BossConfig(
        id = "gumball_giant",
        name = "Gumball Giant",
        gimmick = BossGimmick.SPLITS,
        hpPerGeneration = listOf(12, 4, 1),
        radiusDp = GameRules.BOSS_RADIUS_DP + 6f,
    )
}
