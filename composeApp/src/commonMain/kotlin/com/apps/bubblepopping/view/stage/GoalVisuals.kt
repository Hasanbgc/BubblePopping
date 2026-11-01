package com.apps.bubblepopping.view.stage

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.game.BubbleSize
import com.apps.bubblepopping.stage.GlyphArt
import com.apps.bubblepopping.stage.GlyphBadge
import com.apps.bubblepopping.stage.GlyphIcon
import com.apps.bubblepopping.stage.Objective
import com.apps.bubblepopping.stage.ObjectiveGlyph
import com.apps.bubblepopping.stage.ObjectiveProgress
import com.apps.bubblepopping.stage.World
import com.apps.bubblepopping.stage.countText
import com.apps.bubblepopping.stage.glyphProgress
import com.apps.bubblepopping.stage.glyphs
import com.apps.bubblepopping.ui.components.ButtonColor
import com.apps.bubblepopping.ui.components.GameProgressBar
import com.apps.bubblepopping.ui.components.OutlinedText
import com.apps.bubblepopping.ui.components.PanelDivider
import com.apps.bubblepopping.ui.components.StatusBadge
import com.apps.bubblepopping.ui.components.goldInset
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.BubbleTheme
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing
import com.apps.bubblepopping.view.play.component.BubbleGlyph
import com.apps.bubblepopping.view.play.component.BubbleType

/*
 * A stage's targets as rows of picture + short title + number ("heart bubble ·
 * Hearts · ×8"): the picture and number are taken in at a glance, the title
 * settles what they mean without reading a sentence. The main goal is the big
 * row; bonus goals are smaller rows below it, each with a star at its end (one
 * star each). The same rows show on the stage card, the round's opening banner
 * and the pause menu, so a target is learnt once and recognised after.
 */

/** A small gold heading over a section of a card: "TARGETS", "POWER-UPS". */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    OutlinedText(text, GameTheme.type.label, color = GameColors.Gold, maxLines = 1, modifier = modifier.padding(start = Spacing.xs))
}

/**
 * Every target of a stage in one gold-framed well under a "TARGETS" heading: the
 * main goal big, then the bonus goals. With [progress] (pause menu: the stage's
 * objectives, main goal first) the numbers become "7/15", a bar fills under the
 * main goal, and each row's end mark shows where it stands. [popIn] springs the
 * main goal in after the card opens, so the eye lands there first.
 */
@Composable
fun TargetsSection(
    goal: Objective,
    bonusGoals: List<Objective>,
    world: World,
    modifier: Modifier = Modifier,
    progress: List<ObjectiveProgress>? = null,
    popIn: Boolean = false,
    /** Smaller main goal and no divider, for the pause menu where space is short. */
    compact: Boolean = false,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        SectionTitle("TARGETS")
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
            modifier = Modifier
                .fillMaxWidth()
                .goldInset()
                .padding(horizontal = Spacing.m, vertical = Spacing.m),
        ) {
            MainGoalRows(goal, world, progress?.firstOrNull(), popIn, if (compact) TargetSize.MAIN_COMPACT else TargetSize.MAIN)
            if (bonusGoals.isNotEmpty()) {
                if (!compact) PanelDivider()
                bonusGoals.forEachIndexed { index, objective ->
                    BonusGoalRows(objective, world, progress?.getOrNull(index + 1))
                }
            }
        }
    }
}

/** The main goal on its own in a gold-framed well, for the round's opening banner. */
@Composable
fun GoalCard(goal: Objective, world: World, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
        modifier = modifier
            .goldInset()
            .padding(horizontal = Spacing.l, vertical = Spacing.m),
    ) {
        MainGoalRows(goal, world, progress = null, popIn = false, size = TargetSize.MAIN)
    }
}

/** The main goal: one big row per part, and a progress bar once playing. */
@Composable
private fun MainGoalRows(goal: Objective, world: World, progress: ObjectiveProgress?, popIn: Boolean, size: TargetSize) {
    val scale = remember { Animatable(if (popIn) POP_FROM else 1f) }
    if (popIn) {
        LaunchedEffect(Unit) {
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        }
    }
    val parts = progress?.glyphProgress() ?: goal.glyphs().map { it to null }
    val paired = parts.size > 1
    val done = progress?.satisfied == true
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
        modifier = Modifier
            .clearAndSetSemantics { contentDescription = "Goal: " + describe(goal, progress) }
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
    ) {
        parts.forEachIndexed { index, (glyph, partProgress) ->
            TargetRow(
                glyph = glyph,
                value = partProgress?.countText(glyph) ?: glyph.value,
                world = world,
                size = size,
                // A two-part goal ticks off each part; the whole goal's tick sits on the last row.
                pictureDone = paired && partProgress?.satisfied == true,
                trailing = if (done && index == parts.lastIndex) {
                    { StatusBadge(GameIcons.Tick, ButtonColor.GREEN, size = END_MARK) }
                } else null,
            )
        }
        if (progress != null) {
            GameProgressBar(
                fraction = if (done) 1f else progress.fraction,
                color = if (done) GameColors.Success else GameColors.Gold,
                height = PROGRESS_HEIGHT,
            )
        }
    }
}

/** Where a bonus objective stands; decides its end mark. */
private enum class BonusState { PLANNED, PENDING, HOLDING, DONE, BROKEN }

private fun bonusState(objective: Objective, progress: ObjectiveProgress?): BonusState = when {
    progress == null -> BonusState.PLANNED
    progress.satisfied -> if (objective.isConstraint) BonusState.HOLDING else BonusState.DONE
    objective.isConstraint -> BonusState.BROKEN
    else -> BonusState.PENDING
}

/**
 * A bonus goal: a smaller row with a star at its end. With [progress] (pause
 * menu): "3/6" while underway; a green tick once met; a green star while a
 * constraint still holds; a red cross, faded, once it's broken.
 */
@Composable
private fun BonusGoalRows(objective: Objective, world: World, progress: ObjectiveProgress?) {
    val state = bonusState(objective, progress)
    val parts = progress?.glyphProgress() ?: objective.glyphs().map { it to null }
    val (markIcon, markColor) = when (state) {
        BonusState.PLANNED, BonusState.PENDING -> GameIcons.Star to ButtonColor.VIOLET
        BonusState.HOLDING -> GameIcons.Star to ButtonColor.GREEN
        BonusState.DONE -> GameIcons.Tick to ButtonColor.GREEN
        BonusState.BROKEN -> GameIcons.Close to ButtonColor.RED
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = Modifier
            .alpha(if (state == BonusState.BROKEN) BROKEN_ALPHA else 1f)
            .clearAndSetSemantics { contentDescription = "Bonus star: " + describe(objective, progress) },
    ) {
        parts.forEachIndexed { index, (glyph, partProgress) ->
            TargetRow(
                glyph = glyph,
                value = if (state == BonusState.PENDING) partProgress?.countText(glyph) ?: glyph.value else glyph.value,
                world = world,
                size = TargetSize.BONUS,
                trailing = if (index == 0) {
                    { StatusBadge(markIcon, markColor, size = END_MARK) }
                } else null,
            )
        }
    }
}

private enum class TargetSize(val art: Dp) {
    MAIN(56.dp),
    MAIN_COMPACT(44.dp),
    BONUS(36.dp),
}

/** Picture, then the title over the number; an optional mark at the far end. */
@Composable
private fun TargetRow(
    glyph: ObjectiveGlyph,
    value: String,
    world: World,
    size: TargetSize,
    pictureDone: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        modifier = Modifier.fillMaxWidth(),
    ) {
        GlyphPicture(glyph, world, size.art, done = pictureDone)
        Column(Modifier.weight(1f)) {
            Text(
                text = glyph.title,
                style = if (size == TargetSize.BONUS) GameTheme.type.caption else GameTheme.type.label,
                color = GameColors.TextSecondary,
                maxLines = 1,
            )
            OutlinedText(
                text = value,
                style = when (size) {
                    TargetSize.MAIN -> GameTheme.type.title
                    TargetSize.MAIN_COMPACT -> GameTheme.type.headline
                    TargetSize.BONUS -> GameTheme.type.label
                },
                maxLines = 1,
            )
        }
        trailing?.invoke()
    }
}

/**
 * An objective's picture at [size], with its [ObjectiveGlyph.badge] on the
 * bottom corner and, when [done], a small green tick on the top corner.
 */
@Composable
fun GlyphPicture(glyph: ObjectiveGlyph, world: World, size: Dp, modifier: Modifier = Modifier, done: Boolean = false) {
    Box(modifier.size(size)) {
        GlyphArtImage(glyph.art, world, Modifier.fillMaxSize())
        val badgeSize = size * BADGE_FRACTION
        when (val badge = glyph.badge) {
            is GlyphBadge.Art -> GlyphArtImage(
                badge.art,
                world,
                Modifier.align(Alignment.BottomEnd).offset(x = badgeSize / 4).size(badgeSize),
            )
            null -> Unit
        }
        if (done) {
            StatusBadge(GameIcons.Tick, ButtonColor.GREEN, size = badgeSize, modifier = Modifier.align(Alignment.TopEnd))
        }
    }
}

/** A bubble drawn as in play, or a drawn icon. */
@Composable
fun GlyphArtImage(art: GlyphArt, world: World, modifier: Modifier = Modifier) {
    when (art) {
        is GlyphArt.OfBubble -> BubbleGlyph(art.type, world, modifier, art.size)
        is GlyphArt.OfIcon -> Image(art.icon.vector, contentDescription = null, modifier = modifier)
    }
}

private val GlyphIcon.vector: ImageVector
    get() = when (this) {
        GlyphIcon.SCORE -> GameIcons.Trophy
        GlyphIcon.COMBO -> GameIcons.Flame
        GlyphIcon.CLOCK -> GameIcons.Clock
        GlyphIcon.CROWN -> GameIcons.Crown
        GlyphIcon.WIND -> GameIcons.Wind
        GlyphIcon.ACCURACY -> GameIcons.Target
        GlyphIcon.HEART -> GameIcons.Heart
    }

/** What a screen reader says: the full sentence, plus where it stands. */
private fun describe(objective: Objective, progress: ObjectiveProgress?): String = when {
    progress == null -> objective.description
    progress.satisfied && !objective.isConstraint -> "${objective.description}, done"
    !progress.satisfied && objective.isConstraint -> "${objective.description}, missed"
    objective.isConstraint -> objective.description
    else -> "${objective.description}, ${progress.current} of ${progress.target}"
}

private const val POP_FROM = 0.4f
private const val BADGE_FRACTION = 0.45f
private const val BROKEN_ALPHA = 0.55f
private val PROGRESS_HEIGHT = 10.dp
private val END_MARK = 24.dp

@Preview
@Composable
fun GoalVisualsPreview() {
    BubbleTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.l), modifier = Modifier.padding(Spacing.l)) {
            TargetsSection(
                Objective.PopType(BubbleType.HEART, 8),
                listOf(Objective.ReachScore(440), Objective.Accuracy(90)),
                World.SKY,
            )
            TargetsSection(
                Objective.AllOf(listOf(Objective.PopType(BubbleType.BOMB, 6), Objective.PopSize(BubbleSize.SMALL, 10))),
                listOf(Objective.NoLivesLost, Objective.ComboTimes(8, 3)),
                World.SKY,
                progress = listOf(
                    ObjectiveProgress(Objective.AllOf(listOf(Objective.PopType(BubbleType.BOMB, 6), Objective.PopSize(BubbleSize.SMALL, 10))), 9, 16, false),
                    ObjectiveProgress(Objective.NoLivesLost, 1, 0, false),
                    ObjectiveProgress(Objective.ComboTimes(8, 3), 1, 3, false),
                ),
            )
        }
    }
}
