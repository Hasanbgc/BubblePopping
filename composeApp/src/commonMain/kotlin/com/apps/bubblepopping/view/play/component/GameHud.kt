package com.apps.bubblepopping.view.play.component

import com.apps.bubblepopping.ui.components.GameProgressBar
import com.apps.bubblepopping.ui.components.candyFace
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.layout.Layout
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apps.bubblepopping.stage.Objective
import com.apps.bubblepopping.stage.ObjectiveProgress
import com.apps.bubblepopping.stage.World
import com.apps.bubblepopping.ui.components.ButtonColor
import com.apps.bubblepopping.ui.components.GameIconButton
import com.apps.bubblepopping.ui.components.OutlinedText
import com.apps.bubblepopping.ui.components.enamelPill
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.BubbleTheme
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing

/** Height of the HUD (below the status bar), panel and margins. Bubbles escape at its bottom edge. */
internal val HUD_CONTENT_HEIGHT: Dp = 78.dp

/**
 * The in-game HUD: one jewel-teal panel with a gold rim across the top ([hudPanel]),
 * in three parts.
 *  - Left: what the round is aiming for — the [goals] slot: the stage's star
 *    meters ([StarMeters]) or, in Endless, the wave ([WaveMeter]).
 *  - Middle: the score in white-to-gold lettering, bumping as it rises; while a
 *    combo runs a "×3" badge with a draining ring sits beside it.
 *  - Right: what the player has right now — the [timer] (timed stages) over the
 *    chips: lives, then shield, freeze and ×2 while active, overlapping rather than
 *    overflowing — and the gear in the corner, which pauses.
 * The score stays in the middle; both sides share what is left, so the HUD fits
 * down to ~320 dp.
 */
@Composable
fun GameHud(
    score: Int,
    lives: Int,
    isPaused: Boolean,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier,
    comboTimeLeft: () -> Float = { 0f },
    comboMultiplier: Int = 1,
    shieldActive: Boolean = false,
    freezeSecondsLeft: Int = 0,
    doubleSecondsLeft: Int = 0,
    timer: (@Composable () -> Unit)? = null,
    goals: @Composable () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0.00f to GameColors.Scrim.copy(alpha = 0.85f),
                    0.60f to GameColors.Scrim.copy(alpha = 0.55f),
                    1.00f to Color.Transparent,
                )
            ),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = Spacing.s, vertical = Spacing.xs)
                .height(PANEL_HEIGHT)
                .hudPanel()
                .padding(Spacing.s),
        ) {
            HudSections(
                modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(start = Spacing.xs),
                start = goals,
                center = { ScoreCenter(score, comboTimeLeft, comboMultiplier) },
                end = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.CenterVertically),
                    ) {
                        timer?.invoke()
                        PowerUpChips(lives, shieldActive, freezeSecondsLeft, doubleSecondsLeft)
                    }
                },
                corner = {
                    // The gear, tucked into the top-right corner; it pauses and opens the pause menu.
                    GameIconButton(
                        icon = GameIcons.Gear,
                        contentDescription = if (isPaused) "Resume" else "Pause",
                        onClick = onPlayPauseClick,
                        color = ButtonColor.BLUE,
                        size = PAUSE_BUTTON_SIZE,
                    )
                },
            )
        }
    }
}

/**
 * The HUD's panel. A soft shadow onto the play field, a dark outline, a jewel-teal
 * face deepening downwards over a darker lip along the bottom (so it reads thick),
 * a glass shine over the top third, and a thin gold rim just inside the edge, like
 * the dialog cards.
 */
private fun Modifier.hudPanel(): Modifier = this.drawBehind {
    val corner = PANEL_CORNER.toPx()
    val outline = 2.5.dp.toPx()
    val lip = PANEL_LIP.toPx()
    val rim = 1.5.dp.toPx()
    // Shadow under the panel, onto the play field.
    drawRoundRect(Color.Black.copy(alpha = 0.35f), topLeft = Offset(0f, lip * 1.5f), size = size, cornerRadius = CornerRadius(corner))
    // The lip: the panel's darker underside showing below the face.
    drawRoundRect(GameColors.HudTealLip, size = size, cornerRadius = CornerRadius(corner))
    // The face.
    val face = Size(size.width, size.height - lip)
    drawRoundRect(
        Brush.verticalGradient(
            0f to GameColors.HudTealTop,
            0.45f to GameColors.HudTealMid,
            1f to GameColors.HudTealBottom,
            endY = face.height,
        ),
        size = face,
        cornerRadius = CornerRadius(corner),
    )
    // Glass shine over the top third, inset from the corners.
    val inset = corner * 0.7f
    drawRoundRect(
        Brush.verticalGradient(0f to Color.White.copy(alpha = 0.5f), 1f to Color.Transparent, startY = rim, endY = face.height * 0.4f),
        topLeft = Offset(inset, rim * 2f),
        size = Size(size.width - inset * 2f, face.height * 0.38f),
        cornerRadius = CornerRadius(corner * 0.6f),
    )
    // Gold rim just inside the edge, bright at the top, deeper below.
    drawRoundRect(
        Brush.verticalGradient(0f to GameColors.GoldTop, 1f to GameColors.GoldLip),
        topLeft = Offset(outline, outline),
        size = Size(size.width - outline * 2f, size.height - outline * 2f),
        cornerRadius = CornerRadius(corner - outline),
        style = Stroke(rim),
    )
    // Dark outline around everything, like the buttons and icon art.
    drawRoundRect(GameColors.ButtonOutline, size = size, cornerRadius = CornerRadius(corner), style = Stroke(outline))
}

/**
 * The HUD's parts side by side: [center] in the middle of the panel, [corner] in
 * the top-right corner, [start] filling everything left of the middle and [end]
 * centred in what is left between the middle and the corner (both given exactly
 * that width at most). All but the corner are vertically centred.
 *
 * The middle is [CENTER_WIDTH] wide, but gives way so the right side always keeps
 * [END_WIDTH] for the timer and chips; on a narrow phone (~320 dp) the gaps also
 * tighten to [NARROW_GAP]. The middle is told its width, and fits itself to it.
 */
@Composable
private fun HudSections(
    start: @Composable () -> Unit,
    center: @Composable () -> Unit,
    end: @Composable () -> Unit,
    corner: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(contents = listOf(start, center, end, corner), modifier = modifier) { (starts, centers, ends, corners), constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val cornerP = corners.map { it.measure(loose) }
        val cornerW = cornerP.maxOfOrNull { it.width } ?: 0
        // What the right side keeps (the corner, the timer and chips, two gaps), on each side of the middle.
        fun sideNeed(gap: Int) = cornerW + END_WIDTH.roundToPx() + gap * 2
        val wide = width - sideNeed(Spacing.s.roundToPx()) * 2 >= CENTER_WIDTH.roundToPx()
        val gap = (if (wide) Spacing.s else NARROW_GAP).roundToPx()
        val centerW = (width - sideNeed(gap) * 2).coerceIn(0, CENTER_WIDTH.roundToPx())
        val centerP = centers.map { it.measure(loose.copy(minWidth = centerW, maxWidth = centerW)) }
        val centerX = (width - centerW) / 2
        val startW = (centerX - gap).coerceAtLeast(0)
        val startP = starts.map { it.measure(loose.copy(minWidth = startW, maxWidth = startW)) }
        val endFrom = centerX + centerW + gap
        val endW = (width - cornerW - gap - endFrom).coerceAtLeast(0)
        val endP = ends.map { it.measure(loose.copy(maxWidth = endW)) }
        layout(width, height) {
            startP.forEach { it.place(0, (height - it.height) / 2) }
            centerP.forEach { it.place(centerX, (height - it.height) / 2) }
            endP.forEach { it.place(endFrom + (endW - it.width) / 2, (height - it.height) / 2) }
            cornerP.forEach { it.place(width - it.width, 0) }
        }
    }
}

/**
 * Lives, then a shield while one is ready, a snowflake while frozen and "×2" while
 * points count double; timed ones carry their seconds on a corner tag.
 */
@Composable
private fun PowerUpChips(lives: Int, shieldActive: Boolean, freezeSecondsLeft: Int, doubleSecondsLeft: Int) {
    OverlapRow(spacing = Spacing.xs) {
        HudChip(GameColors.PillHeart, tag = "$lives", modifier = Modifier.semantics { contentDescription = "$lives lives" }) {
            Image(GameIcons.Heart, contentDescription = null, modifier = Modifier.size(CHIP_ICON).alpha(if (lives > 0) 1f else 0.4f))
        }
        if (shieldActive) {
            HudChip(GameColors.PillBubble, modifier = Modifier.semantics { contentDescription = "Shield ready" }) {
                Image(GameIcons.Shield, contentDescription = null, modifier = Modifier.size(CHIP_ICON))
            }
        }
        if (freezeSecondsLeft > 0) {
            val seconds = freezeSecondsLeft
            HudChip(GameColors.PillFreeze, tag = "$seconds", modifier = Modifier.semantics { contentDescription = "Frozen, $seconds seconds left" }) {
                Image(GameIcons.Snowflake, contentDescription = null, modifier = Modifier.size(CHIP_ICON))
            }
        }
        if (doubleSecondsLeft > 0) {
            val seconds = doubleSecondsLeft
            HudChip(GameColors.PillHeart, tag = "$seconds", modifier = Modifier.semantics { contentDescription = "Double points, $seconds seconds left" }) {
                OutlinedText("×2", GameTheme.type.caption, maxLines = 1)
            }
        }
    }
}

/** A round enamel chip with an icon, and [tag] (a count) on a small dark tag at its bottom-right. */
@Composable
private fun HudChip(
    tint: Color,
    modifier: Modifier = Modifier,
    tag: String? = null,
    content: @Composable () -> Unit,
) {
    // requiredSize: always a true circle, even when the space left under the timer is a little short.
    Box(modifier.requiredSize(CHIP_SIZE)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(CHIP_SIZE).enamelPill(tint)) { content() }
        if (tag != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = TAG_NUDGE, y = TAG_NUDGE)
                    .height(TAG_HEIGHT)
                    .widthIn(min = TAG_HEIGHT)
                    .background(GameColors.ButtonOutline, RoundedCornerShape(50))
                    .padding(horizontal = 3.dp),
            ) {
                OutlinedText(tag, HUD_LABEL_STYLE(), outlineWidth = 1.dp, maxLines = 1)
            }
        }
    }
}

/**
 * A row spaced by [spacing] that never grows past its width: when its children
 * don't fit, they overlap evenly (later ones on top) instead.
 */
@Composable
private fun OverlapRow(spacing: Dp, content: @Composable () -> Unit) {
    Layout(content) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val gap = spacing.roundToPx()
        val natural = placeables.sumOf { it.width } + gap * (placeables.size - 1).coerceAtLeast(0)
        val width = natural.coerceAtMost(constraints.maxWidth)
        val height = placeables.maxOfOrNull { it.height } ?: 0
        // Step from one child's left edge to the next: their width plus the gap, or less to fit.
        val squeeze = if (natural > width && placeables.size > 1) (natural - width) / (placeables.size - 1) else 0
        layout(width, height) {
            var x = 0
            placeables.forEach {
                it.place(x, (height - it.height) / 2)
                x += it.width + gap - squeeze
            }
        }
    }
}

/**
 * The middle of the HUD, with no box around it: the score in white-to-gold
 * lettering ([SCORE_GOLD]), filling the width it is given. The number springs
 * towards [score] and bumps on each increase.
 * While a combo runs, a small gold "×3" badge sits just past the number's top-right
 * with a ring around it that drains as [comboTimeLeft] goes 1 → 0 (read only while
 * drawing, so the draining never recomposes the HUD). The number and the badge's
 * place beside it are centred together, and a long score shrinks to fit.
 */
@Composable
private fun ScoreCenter(
    score: Int,
    comboTimeLeft: () -> Float,
    comboMultiplier: Int,
) {
    val displayed by animateIntAsState(
        targetValue = score,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "score",
    )
    val currentComboTimeLeft by rememberUpdatedState(comboTimeLeft)
    val comboRunning by remember { derivedStateOf { currentComboTimeLeft() > 0f } }
    val showBadge = comboRunning && comboMultiplier > 1
    // The number bumps each time the score goes up.
    val bump = remember { Animatable(1f) }
    LaunchedEffect(score) {
        if (score > 0) {
            bump.snapTo(SCORE_BUMP)
            bump.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
    }
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = if (showBadge) "Score $score, combo times $comboMultiplier" else "Score $score"
            },
    ) {
        val badgeRoom = COMBO_BADGE + Spacing.xs
        val numberRoom = maxWidth - badgeRoom
        // The badge's place is kept even without a combo, so the number never jumps.
        Box(Modifier.padding(end = badgeRoom)) {
            val text = displayed.toString()
            OutlinedText(
                text,
                fitScoreStyle(text, numberRoom),
                outlineWidth = 3.dp,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                brush = SCORE_GOLD,
                modifier = Modifier.graphicsLayer {
                    scaleX = bump.value
                    scaleY = bump.value
                },
            )
            if (showBadge) {
                ComboBadge(
                    comboMultiplier,
                    comboTimeLeft,
                    // Just past the number's right edge, so it never covers a digit.
                    Modifier.align(Alignment.TopEnd).offset(x = badgeRoom, y = Spacing.xs),
                )
            }
        }
    }
}

/** A gold candy "×3" with a ring that drains as [timeLeft] goes 1 → 0. */
@Composable
private fun ComboBadge(multiplier: Int, timeLeft: () -> Float, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(COMBO_BADGE)
            .drawBehind {
                val ring = COMBO_RING.toPx()
                val radius = size.minDimension / 2f + ring / 2f
                drawCircle(GameColors.ButtonOutline, radius = radius + ring / 2f, style = Stroke(ring * 2f))
                drawArc(
                    color = GameColors.Gold,
                    startAngle = -90f,
                    sweepAngle = 360f * timeLeft().coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = Offset(size.width / 2f - radius, size.height / 2f - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(ring, cap = StrokeCap.Round),
                )
            }
            .candyFace(ButtonColor.GOLD, CircleShape),
    ) {
        OutlinedText("×$multiplier", HUD_LABEL_STYLE(), outlineColor = GameColors.GoldLip, outlineWidth = 1.dp, maxLines = 1)
    }
}

/** The score is the biggest thing in the HUD: the title style. */
@Composable
private fun SCORE_STYLE() = GameTheme.type.title

/**
 * [SCORE_STYLE], shrunk just enough for [text] to fit in [room] (the middle's width
 * less the combo badge's place), so a long score never gets cut off.
 */
@Composable
private fun fitScoreStyle(text: String, room: Dp): TextStyle {
    val style = SCORE_STYLE()
    val measurer = rememberTextMeasurer()
    val roomPx = with(LocalDensity.current) { room.toPx() }
    val natural = remember(text, style) { measurer.measure(text, style, maxLines = 1).size.width }
    return if (natural <= roomPx) style else style.copy(fontSize = style.fontSize * (roomPx / natural))
}

private val PANEL_HEIGHT = 70.dp
private val PANEL_CORNER = 28.dp
private val PANEL_LIP = 4.dp
private const val SCORE_BUMP = 1.12f
/** The score's fill — white at the top warming to gold below, like the logo. */
private val SCORE_GOLD = Brush.verticalGradient(0f to Color.White, 0.45f to GameColors.GoldTop, 1f to GameColors.GoldBottom)
private val PAUSE_BUTTON_SIZE = 36.dp
private val CENTER_WIDTH = 120.dp
/** What the right side keeps for the timer and chips (the timer is ~54 dp wide). */
private val END_WIDTH = 56.dp
private val NARROW_GAP = 4.dp
private val COMBO_RING = 2.dp
private val COMBO_BADGE = 20.dp
private val CHIP_SIZE = 26.dp
private val CHIP_ICON = 16.dp
private val TAG_HEIGHT = 14.dp
private val TAG_NUDGE = 3.dp

@Preview(widthDp = 320)
@Composable
fun GameHudPreview() {
    BubbleTheme {
        Box(Modifier.background(Color(0xFF4A2380))) {
            GameHud(
                score = 12400,
                lives = 3,
                isPaused = false,
                onPlayPauseClick = {},
                comboTimeLeft = { 0.6f },
                comboMultiplier = 3,
                shieldActive = true,
                freezeSecondsLeft = 3,
                doubleSecondsLeft = 6,
                timer = { TimerLabel(103) },
                goals = {
                    StarMeters(
                        listOf(
                            ObjectiveProgress(Objective.PopCount(21), current = 18, target = 21, satisfied = false),
                            ObjectiveProgress(Objective.LivesLostAtMost(2), current = 1, target = 2, satisfied = true),
                            ObjectiveProgress(Objective.ReachScore(3100), current = 240, target = 3100, satisfied = false),
                        ),
                        World.SKY,
                    )
                },
            )
        }
    }
}

@Preview(widthDp = 320)
@Composable
fun GameHudEndlessPreview() {
    BubbleTheme {
        Box(Modifier.background(Color(0xFF4A2380))) {
            GameHud(
                score = 840,
                lives = 2,
                isPaused = false,
                onPlayPauseClick = {},
                goals = { WaveMeter(wave = 3, progress = { 0.4f }, isCalm = false) },
            )
        }
    }
}
