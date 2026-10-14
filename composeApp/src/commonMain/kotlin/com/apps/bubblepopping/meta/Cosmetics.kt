package com.apps.bubblepopping.meta

import com.apps.bubblepopping.data.StageProgress
import com.apps.bubblepopping.stage.StageCatalog

enum class CosmeticKind(val title: String) {
    /** The look of normal bubbles (special bubbles keep theirs, so they stay readable). */
    SKIN("Bubble skins"),
    /** The burst drawn when a bubble pops. */
    POP_EFFECT("Pop effects"),
    /** The play background in Endless and the Daily Challenge (stages keep their world). */
    BACKGROUND("Backgrounds"),
}

/**
 * One cosmetic. It is owned when it's free, bought ([price] coins), won from a
 * star chest, or — for boss rewards — once the boss stage [bossStageId] is cleared.
 */
data class Cosmetic(
    val id: String,
    val name: String,
    val kind: CosmeticKind,
    val price: Int? = null,
    val bossStageId: Int? = null,
) {
    val isFree: Boolean get() = price == null && bossStageId == null
    /** Bought items can also come out of star chests; boss rewards can't. */
    val inChests: Boolean get() = price != null
}

object Cosmetics {

    val CLASSIC = Cosmetic("skin_classic", "Classic", CosmeticKind.SKIN)
    val RAINBOW = Cosmetic("pop_rainbow", "Rainbow ring", CosmeticKind.POP_EFFECT)
    val DEFAULT_BACKGROUND = Cosmetic("bg_default", "Daylight", CosmeticKind.BACKGROUND)

    val all: List<Cosmetic> = listOf(
        // Skins — one per boss, plus shop skins.
        CLASSIC,
        Cosmetic("skin_cloud", "Cloud", CosmeticKind.SKIN, bossStageId = 10),
        Cosmetic("skin_jelly", "Jelly", CosmeticKind.SKIN, bossStageId = 20),
        Cosmetic("skin_galaxy", "Galaxy", CosmeticKind.SKIN, bossStageId = 30),
        Cosmetic("skin_candy", "Candy", CosmeticKind.SKIN, bossStageId = 40),
        Cosmetic("skin_neon", "Neon", CosmeticKind.SKIN, price = 300),
        Cosmetic("skin_gold", "Gold", CosmeticKind.SKIN, price = 800),
        // Pop effects
        RAINBOW,
        Cosmetic("pop_confetti", "Confetti", CosmeticKind.POP_EFFECT, price = 150),
        Cosmetic("pop_stars", "Stars", CosmeticKind.POP_EFFECT, price = 200),
        Cosmetic("pop_hearts", "Hearts", CosmeticKind.POP_EFFECT, price = 250),
        // Backgrounds
        DEFAULT_BACKGROUND,
        Cosmetic("bg_sunset", "Sunset", CosmeticKind.BACKGROUND, price = 200),
        Cosmetic("bg_midnight", "Midnight", CosmeticKind.BACKGROUND, price = 250),
        Cosmetic("bg_aurora", "Aurora", CosmeticKind.BACKGROUND, price = 400),
    )

    fun byId(id: String): Cosmetic? = all.firstOrNull { it.id == id }

    fun of(kind: CosmeticKind): List<Cosmetic> = all.filter { it.kind == kind }

    fun default(kind: CosmeticKind): Cosmetic = when (kind) {
        CosmeticKind.SKIN -> CLASSIC
        CosmeticKind.POP_EFFECT -> RAINBOW
        CosmeticKind.BACKGROUND -> DEFAULT_BACKGROUND
    }

    /** Everything the player can equip: free items, [purchased] ones, and cleared boss rewards. */
    fun owned(purchased: Set<String>, progress: Map<Int, StageProgress>): Set<String> =
        all.filterTo(mutableSetOf()) { cosmetic ->
            cosmetic.isFree || cosmetic.id in purchased ||
                cosmetic.bossStageId?.let { (progress[it]?.stars ?: 0) > 0 } == true
        }.mapTo(mutableSetOf()) { it.id }

    /** "Beat the Cloud King" for a boss reward. */
    fun unlockHint(cosmetic: Cosmetic): String? = cosmetic.bossStageId?.let { stageId ->
        StageCatalog.stage(stageId).boss?.let { "Beat the ${it.name}" } ?: "Clear stage $stageId"
    }
}
