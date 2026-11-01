package com.apps.bubblepopping.view.play.component

import androidx.compose.animation.AnimatedVisibility
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apps.bubblepopping.stage.Objective
import com.apps.bubblepopping.stage.ObjectiveProgress
import com.apps.bubblepopping.stage.StarMeterState
import com.apps.bubblepopping.stage.World
import com.apps.bubblepopping.stage.glyphs
import com.apps.bubblepopping.stage.hudReadout
import com.apps.bubblepopping.stage.starMeterStates
import com.apps.bubblepopping.ui.components.ButtonColor
import com.apps.bubblepopping.ui.components.OutlinedText
import com.apps.bubblepopping.ui.components.StatusBadge
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.BubbleTheme
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing
import com.apps.bubblepopping.view.stage.GlyphPicture
import com.apps.bubblepopping.view.stage.GoalCard

/*
 * The stage parts of the HUD ([GameHud]): the star meters on the left, the timer
 * over the score; plus the goal banner shown as the round starts.
 */

/**
 * The HUD's left side in a stage: one star per objective, all one size, side by
 * side — they run at the same time, in no order. Each star carries its objective's
 * picture as a badge on its corner, with the live number under it ([hudReadout]).
 * Each gets an equal share of the width, so they stay evenly spaced whatever their
 * numbers. On a narrow phone, where a share is under [ROOMY_SHARE], the stars
 * shrink to fit (down to [MIN_STAR]) and the numbers go short ("75s", "240").
 * See [StarMeter] for how each star fills.
 */
@Composable
fun StarMeters(objectives: List<ObjectiveProgress>, world: World, modifier: Modifier = Modifier) {
    val states = starMeterStates(objectives)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = objectives.joinToString(". ") { "${it.objective.description}: ${it.hudReadout()}" }
            },
    ) {
        val share = maxWidth / objectives.size.coerceAtLeast(1)
        val roomy = share >= ROOMY_SHARE
        val star = if (roomy) STAR else (share - Spacing.s).coerceIn(MIN_STAR, STAR)
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            objectives.forEachIndexed { i, p ->
                val state = states[i]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    modifier = Modifier.weight(1f),
                ) {
                    Box {
                        StarMeter(state, star)
                        GlyphPicture(
                            p.objective.glyphs().first(),
                            world,
                            star * BADGE_FRACTION,
                            modifier = Modifier.align(Alignment.BottomEnd).offset(x = Spacing.xs, y = Spacing.xs / 2),
                        )
                    }
                    OutlinedText(
                        p.hudReadout(short = !roomy),
                        if (roomy) READOUT_STYLE() else HUD_LABEL_STYLE(),
                        color = if (state.broken) GameColors.RedTop else GameColors.TextPrimary,
                        outlineWidth = if (roomy) 1.5.dp else 1.dp,
                        maxLines = 1,
                        overflow = TextOverflow.Visible,
                        // Its natural width, centred under the star, even if a little wider than its share.
                        modifier = Modifier.wrapContentWidth(unbounded = true),
                    )
                }
            }
        }
    }
}

/** The number under each star: the caption size, set tight under the star. */
@Composable
private fun READOUT_STYLE() = GameTheme.type.caption.copy(lineHeight = 14.sp)

/**
 * One objective's star: gold fills it from the bottom as the objective gets on
 * (a constraint is full while it holds). Until earned the gold is a pale cream,
 * with a band of light sweeping across it now and then. Once [StarMeterState.lit]
 * it turns full gold with a pop and a burst of sparkles, then glows softly. A
 * broken constraint's star empties to grey with a shake and a red ✕. Any progress
 * shows at least a [MIN_FILL] of the star, so a slow start is still visible.
 *
 * The glow, shimmer and sparkles are read only while drawing, so they never
 * recompose the HUD.
 */
@Composable
private fun StarMeter(state: StarMeterState, size: Dp) {
    val shown by animateFloatAsState(
        targetValue = if (state.fill <= 0f) 0f else MIN_FILL + (1f - MIN_FILL) * state.fill,
        animationSpec = tween(durationMillis = FILL_MS),
        label = "star_fill",
    )
    val pop = remember { Animatable(1f) }
    val sparkle = remember { Animatable(1f) }
    LaunchedEffect(state.lit) {
        if (state.lit) {
            sparkle.snapTo(0f)
            launch { sparkle.animateTo(1f, tween(durationMillis = SPARKLE_MS, easing = LinearOutSlowInEasing)) }
            pop.snapTo(DONE_POP_SCALE)
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(state.broken) {
        if (state.broken) {
            for (x in SHAKE_STEPS) shake.animateTo(x, tween(durationMillis = SHAKE_STEP_MS))
        }
    }
    val loop = rememberInfiniteTransition(label = "star_loop")
    // 0 → 1 → 0: how bright the lit star's glow is right now.
    val breath = loop.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = GLOW_BREATH_MS), RepeatMode.Reverse),
        label = "star_glow",
    )
    // 0 → 1 across a cycle; the shimmer band crosses the star during the first part.
    val sweep = loop.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = SHIMMER_CYCLE_MS, easing = LinearEasing)),
        label = "star_shimmer",
    )
    val shimmering = !state.lit && !state.broken && state.fill > 0f
    Box(
        Modifier
            .size(size)
            .drawBehind {
                if (state.lit) {
                    val alpha = GLOW_MIN_ALPHA + (GLOW_MAX_ALPHA - GLOW_MIN_ALPHA) * breath.value
                    drawCircle(
                        Brush.radialGradient(
                            0f to GameColors.GoldTop.copy(alpha = alpha),
                            1f to Color.Transparent,
                            center = center,
                            radius = this.size.minDimension * GLOW_REACH,
                        ),
                        radius = this.size.minDimension * GLOW_REACH,
                    )
                }
            }
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
                translationX = shake.value * density
            }
            .drawWithContent {
                drawContent()
                if (sparkle.value < 1f) drawSparkles(sparkle.value)
            },
    ) {
        // The empty star underneath; the gold one on top, shown up to the fill line.
        Image(GameIcons.Star, contentDescription = null, colorFilter = GREY, modifier = Modifier.size(size).alpha(EMPTY_ALPHA))
        Image(
            GameIcons.Star,
            contentDescription = null,
            colorFilter = if (state.lit) null else CREAM,
            modifier = Modifier
                .size(size)
                // Its own layer, so the shimmer lands only on the star's pixels.
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    clipRect(top = this.size.height * (1f - shown)) {
                        this@drawWithContent.drawContent()
                        if (shimmering) drawShimmer(sweep.value)
                    }
                },
        )
        if (state.broken) {
            StatusBadge(
                GameIcons.Close,
                ButtonColor.RED,
                size = BROKEN_MARK,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = Spacing.xs, y = -Spacing.xs / 2),
            )
        }
    }
}

/**
 * A diagonal band of light crossing the star while [cycle] is in its first
 * [SHIMMER_SHARE]; drawn only over the star's own pixels (SrcAtop).
 */
private fun DrawScope.drawShimmer(cycle: Float) {
    if (cycle >= SHIMMER_SHARE) return
    val t = cycle / SHIMMER_SHARE
    val band = size.width * 0.5f
    val x = -band + (size.width + band * 2f) * t
    drawRect(
        Brush.linearGradient(
            0f to Color.Transparent,
            0.5f to Color.White.copy(alpha = SHIMMER_ALPHA),
            1f to Color.Transparent,
            start = Offset(x - band, 0f),
            end = Offset(x, size.height),
        ),
        blendMode = BlendMode.SrcAtop,
    )
}

/**
 * The burst as a star is earned: [SPARKLE_COUNT] small four-pointed sparkles,
 * white and gold, flying out from the centre and shrinking away as [t] goes 0 → 1.
 */
private fun DrawScope.drawSparkles(t: Float) {
    val reach = size.minDimension * (0.45f + 0.55f * t)
    val arm = size.minDimension * 0.14f * (1f - t)
    for (i in 0 until SPARKLE_COUNT) {
        val angle = (i * 360f / SPARKLE_COUNT + 15f) * (PI.toFloat() / 180f)
        val c = center + Offset(cos(angle), sin(angle)) * reach
        val color = (if (i % 2 == 0) Color.White else GameColors.GoldTop).copy(alpha = 1f - t)
        val path = Path().apply {
            moveTo(c.x, c.y - arm)
            lineTo(c.x + arm * 0.3f, c.y - arm * 0.3f)
            lineTo(c.x + arm, c.y)
            lineTo(c.x + arm * 0.3f, c.y + arm * 0.3f)
            lineTo(c.x, c.y + arm)
            lineTo(c.x - arm * 0.3f, c.y + arm * 0.3f)
            lineTo(c.x - arm, c.y)
            lineTo(c.x - arm * 0.3f, c.y - arm * 0.3f)
            close()
        }
        drawPath(path, color)
    }
}

private val GREY = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/** The gold star washed most of the way to white: earned-if-you-finish, not yet earned. */
private val CREAM = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            0.45f, 0f, 0f, 0f, 140f,
            0f, 0.45f, 0f, 0f, 140f,
            0f, 0f, 0.45f, 0f, 140f,
            0f, 0f, 0f, 1f, 0f,
        )
    )
)

/**
 * The timer in a timed stage: clock and m:ss. For the last [URGENT_SECONDS] it is
 * red and pops on every tick, so it can't be missed at the side of the HUD.
 */
@Composable
fun TimerLabel(secondsLeft: Int) {
    val urgent = secondsLeft in 1..URGENT_SECONDS
    val color by animateColorAsState(
        targetValue = if (secondsLeft <= URGENT_SECONDS) GameColors.RedTop else GameColors.TextPrimary,
        animationSpec = tween(durationMillis = TINT_FADE_MS),
        label = "timer_tint",
    )
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(secondsLeft) {
        if (urgent) {
            pulse.snapTo(PULSE_SCALE)
            pulse.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = Modifier
            .graphicsLayer {
                scaleX = pulse.value
                scaleY = pulse.value
            }
            .semantics(mergeDescendants = true) { contentDescription = "$secondsLeft seconds left" },
    ) {
        Image(GameIcons.Clock, contentDescription = null, modifier = Modifier.size(TIMER_ICON))
        OutlinedText(
            text = "${secondsLeft / 60}:${(secondsLeft % 60).toString().padStart(2, '0')}",
            style = GameTheme.type.label,
            color = color,
            maxLines = 1,
        )
    }
}

/** The HUD's small text: the numbers under the stars, the chips' count tags, the combo badge. */
@Composable
internal fun HUD_LABEL_STYLE() = GameTheme.type.caption.copy(fontSize = 11.sp, lineHeight = 12.sp)

/**
 * The stage's goal, shown big in the middle when the round starts — the same
 * gold card as on the stage card — then shrinking away towards the stars on the
 * HUD's left.
 */
@Composable
fun GoalBanner(goal: Objective, world: World, visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.6f),
        exit = fadeOut() + scaleOut(targetScale = 0.4f, transformOrigin = TransformOrigin(0.15f, -1.5f)),
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier.width(IntrinsicSize.Max),
        ) {
            OutlinedText("GOAL", GameTheme.type.headline, color = GameColors.Gold)
            GoalCard(goal, world)
        }
    }
}

private val STAR = 34.dp
private val MIN_STAR = 24.dp
/** A star's share of the width below which the narrow layout is used. */
private val ROOMY_SHARE = 34.dp
/** The objective's picture on a star's corner, as a fraction of the star. */
private const val BADGE_FRACTION = 0.47f
private val BROKEN_MARK = 12.dp
private val TIMER_ICON = 16.dp
private const val MIN_FILL = 0.2f
private const val SPARKLE_MS = 700
private const val SPARKLE_COUNT = 8
private const val GLOW_BREATH_MS = 1400
private const val GLOW_MIN_ALPHA = 0.35f
private const val GLOW_MAX_ALPHA = 0.7f
/** How far the glow reaches past the star's centre, as a fraction of its size. */
private const val GLOW_REACH = 0.8f
private const val SHIMMER_CYCLE_MS = 3000
/** The part of each shimmer cycle the band spends crossing; it rests for the rest. */
private const val SHIMMER_SHARE = 0.35f
private const val SHIMMER_ALPHA = 0.7f
private const val EMPTY_ALPHA = 0.45f
private const val FILL_MS = 350
private const val DONE_POP_SCALE = 1.3f
/** Sideways steps of the broken-star shake, in dp. */
private val SHAKE_STEPS = listOf(6f, -6f, 4f, -4f, 0f)
private const val SHAKE_STEP_MS = 40
private const val URGENT_SECONDS = 10
/** How big the timer pops on each of its last ticks. */
private const val PULSE_SCALE = 1.25f
private const val TINT_FADE_MS = 300

@Preview
@Composable
fun StageHudPartsPreview() {
    BubbleTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.m), modifier = Modifier.padding(Spacing.l).width(160.dp)) {
            StarMeters(
                listOf(
                    ObjectiveProgress(Objective.Survive(120), current = 75, target = 120, satisfied = false),
                    ObjectiveProgress(Objective.LivesLostAtMost(2), current = 3, target = 2, satisfied = false),
                    ObjectiveProgress(Objective.ReachScore(3100), current = 240, target = 3100, satisfied = false),
                ),
                World.SKY,
            )
            TimerLabel(8)
        }
    }
}
