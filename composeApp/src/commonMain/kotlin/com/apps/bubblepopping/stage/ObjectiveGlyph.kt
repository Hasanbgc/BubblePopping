package com.apps.bubblepopping.stage

import com.apps.bubblepopping.game.BubbleSize
import com.apps.bubblepopping.view.play.component.BubbleType

/**
 * An objective as a picture, a short title and a number ("golden bubble ·
 * Golden bubbles · ×15"), so the stage card, round banner, HUD and pause menu
 * can show it at a glance instead of as a sentence. [Objective.description]
 * stays the full text for screen readers.
 */
data class ObjectiveGlyph(
    val art: GlyphArt,
    /** One or two words naming what to do: "Golden bubbles", "Accuracy". */
    val title: String,
    /** "×15", "60s", "90%", a boss's name. */
    val value: String,
    /** A small mark on the picture's corner: the wind that blows skulls away… */
    val badge: GlyphBadge? = null,
)

/** The picture of an objective. */
sealed interface GlyphArt {
    /** A bubble drawn just as it looks in play; [size] shows small / medium / large goals. */
    data class OfBubble(val type: BubbleType, val size: BubbleSize = BubbleSize.LARGE) : GlyphArt

    data class OfIcon(val icon: GlyphIcon) : GlyphArt
}

/** Icons for objectives that aren't about one kind of bubble. */
enum class GlyphIcon { SCORE, COMBO, CLOCK, CROWN, WIND, ACCURACY, HEART }

sealed interface GlyphBadge {
    data class Art(val art: GlyphArt) : GlyphBadge
}

/** One glyph per part: two for "pop 6 bombs and 6 golden bubbles", one otherwise. */
fun Objective.glyphs(): List<ObjectiveGlyph> = when (this) {
    is Objective.AllOf -> goals.flatMap { it.glyphs() }
    else -> listOf(glyph())
}

private fun Objective.glyph(): ObjectiveGlyph = when (this) {
    is Objective.ReachScore -> ObjectiveGlyph(icon(GlyphIcon.SCORE), "Score", "$target")
    is Objective.PopCount -> ObjectiveGlyph(bubble(BubbleType.NORMAL), "Bubbles", times(count))
    is Objective.PopType -> ObjectiveGlyph(bubble(type), type.displayName(2).capitalised(), times(count))
    is Objective.PopSize ->
        ObjectiveGlyph(GlyphArt.OfBubble(BubbleType.NORMAL, size), "${size.name.lowercase().capitalised()} bubbles", times(count))
    is Objective.ReachCombo -> ObjectiveGlyph(icon(GlyphIcon.COMBO), "Combo", "$combo")
    is Objective.ComboTimes ->
        if (times == 1) ObjectiveGlyph(icon(GlyphIcon.COMBO), "Combo", "$combo")
        else ObjectiveGlyph(icon(GlyphIcon.COMBO), "Combos of $combo", times(times))
    is Objective.Survive -> ObjectiveGlyph(icon(GlyphIcon.CLOCK), "Survive", "${seconds}s")
    is Objective.BombChain -> ObjectiveGlyph(bubble(BubbleType.BOMB), "One bomb pops", "$bubbles")
    is Objective.LightningZaps -> ObjectiveGlyph(bubble(BubbleType.LIGHTNING), "Lightning zaps", times(count))
    is Objective.ChainPairs -> ObjectiveGlyph(bubble(BubbleType.CHAIN), "Chain pairs", times(count))
    is Objective.CrackArmored -> ObjectiveGlyph(bubble(BubbleType.ARMORED), "Armored bubbles", times(count))
    is Objective.BreakArmoredWithBlast ->
        ObjectiveGlyph(bubble(BubbleType.ARMORED), "Blast armored", times(count), GlyphBadge.Art(bubble(BubbleType.BOMB)))
    is Objective.WindSaves ->
        ObjectiveGlyph(bubble(BubbleType.POISON), "Blow skulls away", times(count), GlyphBadge.Art(icon(GlyphIcon.WIND)))
    is Objective.DefeatBoss -> ObjectiveGlyph(icon(GlyphIcon.CROWN), "Defeat", boss.name)
    Objective.NoMisses -> ObjectiveGlyph(icon(GlyphIcon.ACCURACY), "No misses", "100%")
    is Objective.Accuracy -> ObjectiveGlyph(icon(GlyphIcon.ACCURACY), "Accuracy", "$percent%")
    Objective.NoLivesLost -> ObjectiveGlyph(icon(GlyphIcon.HEART), "Lives lost", "0")
    is Objective.LivesLostAtMost -> ObjectiveGlyph(icon(GlyphIcon.HEART), "Lives lost", "max $lives")
    is Objective.FinishUnder -> ObjectiveGlyph(icon(GlyphIcon.CLOCK), "Finish within", "${seconds}s")
    is Objective.AllOf -> error("AllOf has one glyph per part")
}

/**
 * Each glyph with the progress of its own part (the parts of an [Objective.AllOf]
 * are tracked separately), for the HUD and pause menu.
 */
fun ObjectiveProgress.glyphProgress(): List<Pair<ObjectiveGlyph, ObjectiveProgress>> {
    val glyphs = objective.glyphs()
    return if (parts.size == glyphs.size) glyphs.zip(parts) else glyphs.map { it to this }
}

/**
 * The glyph's number while playing: "7/15", "12/60s", "45%" of a boss's health.
 * Constraints keep their limit ("90%", "0") — their state shows as a mark instead.
 */
fun ObjectiveProgress.countText(glyph: ObjectiveGlyph): String = when {
    objective.isConstraint -> glyph.value
    objective is Objective.DefeatBoss -> "${(fraction * 100).toInt()}%"
    objective is Objective.Survive -> "${minOf(current, target)}/${target}s"
    else -> "${minOf(current, target)}/$target"
}

private fun bubble(type: BubbleType) = GlyphArt.OfBubble(type)
private fun icon(icon: GlyphIcon) = GlyphArt.OfIcon(icon)
private fun times(count: Int) = "×$count"
private fun String.capitalised() = replaceFirstChar(Char::uppercase)
