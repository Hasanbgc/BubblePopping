package com.apps.bubblepopping

import com.apps.bubblepopping.meta.CosmeticKind
import com.apps.bubblepopping.meta.Cosmetics
import com.apps.bubblepopping.meta.DailyStageGenerator
import com.apps.bubblepopping.stage.Objective
import com.apps.bubblepopping.stage.StageCatalog
import com.apps.bubblepopping.stage.World
import com.apps.bubblepopping.view.play.component.skinScheme
import com.apps.bubblepopping.view.playBackgroundColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DailyAndCosmeticsTest {

    // ── Daily Stage ─────────────────────────────────────────────────────────

    @Test
    fun dailyStage_isTheSameAllDay() {
        assertEquals(
            DailyStageGenerator.generate(20_000, World.OCEAN),
            DailyStageGenerator.generate(20_000, World.OCEAN),
        )
    }

    @Test
    fun dailyStage_changesFromDayToDay() {
        val goals = (20_000L until 20_030L).map { DailyStageGenerator.generate(it, World.SKY).goal }.toSet()
        assertTrue(goals.size > 5, "30 days gave only ${goals.size} different goals")
    }

    @Test
    fun everyDailyStage_isPlayable() {
        for (world in StageCatalog.worlds) {
            for (day in 20_000L until 20_100L) {
                val stage = DailyStageGenerator.generate(day, world)
                assertEquals(world, stage.world)
                assertNull(stage.boss)
                val duration = assertNotNull(stage.durationSec)
                assertTrue(duration > 0)
                for (objective in stage.allObjectives) {
                    when (objective) {
                        is Objective.PopType -> assertTrue(
                            (stage.spawn.typeWeights[objective.type] ?: 0) > 0,
                            "$world day $day asks for ${objective.type} that never spawns",
                        )
                        is Objective.Survive -> assertTrue(objective.seconds <= duration)
                        else -> Unit
                    }
                }
            }
        }
    }

    // ── Cosmetics ───────────────────────────────────────────────────────────

    @Test
    fun cosmeticIds_areUnique_andEachKindHasAFreeDefault() {
        assertEquals(Cosmetics.all.size, Cosmetics.all.map { it.id }.toSet().size)
        for (kind in CosmeticKind.entries) {
            val default = Cosmetics.default(kind)
            assertEquals(kind, default.kind)
            assertTrue(default.isFree)
        }
    }

    @Test
    fun everyBoss_hasASkin() {
        val bossStages = StageCatalog.stages.filter { it.boss != null }.map { it.id }.toSet()
        assertEquals(bossStages, Cosmetics.all.mapNotNull { it.bossStageId }.toSet())
    }

    @Test
    fun everyCosmetic_hasALook() {
        for (cosmetic in Cosmetics.all) {
            when (cosmetic.kind) {
                CosmeticKind.SKIN ->
                    if (cosmetic != Cosmetics.CLASSIC) assertNotNull(skinScheme(cosmetic.id), cosmetic.id)
                CosmeticKind.BACKGROUND ->
                    if (cosmetic != Cosmetics.DEFAULT_BACKGROUND) assertNotNull(playBackgroundColors(cosmetic.id), cosmetic.id)
                CosmeticKind.POP_EFFECT -> Unit // unknown ids fall back to the rainbow ring
            }
        }
    }
}
