package com.apps.bubblepopping

import com.apps.bubblepopping.game.BubbleSize
import com.apps.bubblepopping.stage.GlyphArt
import com.apps.bubblepopping.stage.GlyphIcon
import com.apps.bubblepopping.stage.Objective
import com.apps.bubblepopping.stage.ObjectiveProgress
import com.apps.bubblepopping.stage.StageCatalog
import com.apps.bubblepopping.stage.countText
import com.apps.bubblepopping.stage.glyphProgress
import com.apps.bubblepopping.stage.glyphs
import com.apps.bubblepopping.view.play.component.BubbleType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The stage goals' pictures and numbers (stage card, banner, HUD, pause menu). */
class ObjectiveGlyphTest {

    @Test
    fun everyStageObjective_hasAGlyph() {
        StageCatalog.stages.flatMap { it.allObjectives }.forEach { objective ->
            val glyphs = objective.glyphs()
            val parts = (objective as? Objective.AllOf)?.goals?.size ?: 1
            assertEquals(parts, glyphs.size, objective.description)
        }
    }

    @Test
    fun bubbleGoals_showTheBubbleAndCount() {
        val glyph = Objective.PopType(BubbleType.GOLDEN, 15).glyphs().single()
        assertEquals(GlyphArt.OfBubble(BubbleType.GOLDEN), glyph.art)
        assertEquals("×15", glyph.value)
        assertEquals("Golden bubbles", glyph.title)

        val small = Objective.PopSize(BubbleSize.SMALL, 10).glyphs().single()
        assertEquals(GlyphArt.OfBubble(BubbleType.NORMAL, BubbleSize.SMALL), small.art)
    }

    @Test
    fun comboTimes_namesTheComboSizeInItsTitle() {
        val glyph = Objective.ComboTimes(8, 3).glyphs().single()
        assertEquals(GlyphArt.OfIcon(GlyphIcon.COMBO), glyph.art)
        assertEquals("Combos of 8", glyph.title)
        assertEquals("×3", glyph.value)
        // Once is just "reach an 8 combo".
        assertEquals("8", Objective.ComboTimes(8, 1).glyphs().single().value)
    }

    @Test
    fun allOf_tracksEachPartSeparately() {
        val bombs = Objective.PopType(BubbleType.BOMB, 6)
        val gold = Objective.PopType(BubbleType.GOLDEN, 6)
        val progress = ObjectiveProgress(
            Objective.AllOf(listOf(bombs, gold)),
            current = 8,
            target = 12,
            satisfied = false,
            parts = listOf(ObjectiveProgress(bombs, 6, 6, true), ObjectiveProgress(gold, 2, 6, false)),
        )
        val shown = progress.glyphProgress().map { (glyph, part) -> part.countText(glyph) to part.satisfied }
        assertEquals(listOf("6/6" to true, "2/6" to false), shown)
    }

    @Test
    fun countText_capsAtTheTargetAndKeepsConstraintLimits() {
        val pop = Objective.PopCount(15)
        assertEquals("15/15", ObjectiveProgress(pop, 17, 15, true).glyphProgress().single().let { (g, p) -> p.countText(g) })
        val survive = Objective.Survive(60)
        assertEquals("12/60s", ObjectiveProgress(survive, 12, 60, false).glyphProgress().single().let { (g, p) -> p.countText(g) })
        val accuracy = Objective.Accuracy(90)
        assertEquals("90%", ObjectiveProgress(accuracy, 72, 90, false).glyphProgress().single().let { (g, p) -> p.countText(g) })
    }

    @Test
    fun constraints_showTheirLimit() {
        assertEquals("Lives lost" to "0", Objective.NoLivesLost.glyphs().single().let { it.title to it.value })
        assertEquals("Finish within" to "60s", Objective.FinishUnder(60).glyphs().single().let { it.title to it.value })
        assertTrue(Objective.NoMisses.glyphs().single().art == GlyphArt.OfIcon(GlyphIcon.ACCURACY))
    }
}
