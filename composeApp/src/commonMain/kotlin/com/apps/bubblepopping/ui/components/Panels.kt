package com.apps.bubblepopping.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.ui.theme.Corners
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing

/**
 * Frosted glass card: translucent white, a bright rim, and a shine across
 * the top — a flattened bubble. The standard container on every menu screen.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    title: String? = null,
    /** Drawn before the [title]. */
    icon: ImageVector? = null,
    /** Right end of the title row: a counter, a badge, a world name… */
    trailing: (@Composable () -> Unit)? = null,
    strong: Boolean = false,
    contentPadding: Dp = Spacing.panel,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(Corners.large)
            .background(if (strong) GameColors.GlassFillStrong else GameColors.GlassFill)
            .drawBehind {
                // Shine across the top third.
                drawRect(
                    Brush.verticalGradient(
                        0f to GameColors.GlassShine,
                        0.35f to Color.Transparent,
                    )
                )
            }
            .border(2.dp, GameColors.GlassRim, Corners.large)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        if (title != null || trailing != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                if (icon != null) Image(icon, contentDescription = null, modifier = Modifier.size(PANEL_ICON))
                if (title != null) OutlinedText(title, GameTheme.type.headline, modifier = Modifier.weight(1f))
                trailing?.invoke()
            }
        }
        content()
    }
}

private val PANEL_ICON = 30.dp

/**
 * The premium container: dark enamel like the pills and dock — dark outline,
 * soft drop shadow, deep ink gradient, a rim that shines at the top and fades
 * below, and a soft shine. Split it into sections with [PanelSectionTitle] and
 * [PanelDivider] instead of stacking several panels. [EnamelTone.LIGHT] is a brighter
 * body for panels over busy art (the Endless cards, the stage intro).
 */
@Composable
fun EnamelPanel(
    modifier: Modifier = Modifier,
    tone: EnamelTone = EnamelTone.DARK,
    contentPadding: Dp = Spacing.panel,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
        modifier = modifier
            .drawBehind {
                val outline = ENAMEL_OUTLINE.toPx()
                val radius = ENAMEL_CORNER.toPx()
                drawRoundRect(
                    color = GameColors.ButtonShadow,
                    topLeft = Offset(0f, outline * 2f),
                    size = Size(size.width, size.height + outline),
                    cornerRadius = CornerRadius(radius),
                )
                drawRoundRect(
                    color = GameColors.ButtonOutline,
                    topLeft = Offset(-outline / 2f, -outline / 2f),
                    size = Size(size.width + outline, size.height + outline),
                    cornerRadius = CornerRadius(radius + outline / 2f),
                    style = Stroke(outline),
                )
            }
            .clip(Corners.large)
            .background(Brush.verticalGradient(listOf(tone.top, tone.bottom)))
            .drawBehind {
                // Soft shine along the top, inset from the corners.
                val inset = ENAMEL_CORNER.toPx() * 0.6f
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        0f to GameColors.GlassShine,
                        1f to Color.Transparent,
                        startY = 0f,
                        endY = ENAMEL_SHINE.toPx(),
                    ),
                    topLeft = Offset(inset, ENAMEL_RIM.toPx()),
                    size = Size(size.width - inset * 2f, ENAMEL_SHINE.toPx()),
                    cornerRadius = CornerRadius(inset),
                )
            }
            .border(
                ENAMEL_RIM,
                Brush.verticalGradient(
                    0f to GameColors.GlassRim,
                    0.3f to GameColors.GlassFill,
                    1f to GameColors.InkSoft,
                ),
                Corners.large,
            )
            .padding(contentPadding),
        content = content,
    )
}

/** Body colour of an [EnamelPanel]. */
enum class EnamelTone(val top: Color, val bottom: Color) {
    DARK(GameColors.InkSoft, GameColors.Ink),
    LIGHT(GameColors.EnamelLightTop, GameColors.EnamelLightBottom),
    /** Light oceanic cyan: the stage map's world name plates. */
    OCEAN(GameColors.EnamelOceanTop, GameColors.EnamelOceanBottom),
    /** The violet of the Endless button. */
    VIOLET(GameColors.VioletTop, GameColors.VioletBottom),
}

/** A section heading inside an [EnamelPanel]: outlined text, no icon; [trailing] sits at the right end (a count, a world name…). */
@Composable
fun PanelSectionTitle(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        OutlinedText(text, GameTheme.type.headline, modifier = Modifier.weight(1f), maxLines = 1)
        trailing?.invoke()
    }
}

/**
 * An engraved line between sections of an [EnamelPanel]: a dark groove with a
 * faint light edge under it, fading out at both ends.
 */
@Composable
fun PanelDivider(modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.s)
            .height(DIVIDER_HEIGHT),
    ) {
        val line = size.height / 2f
        fun fade(color: Color) = Brush.horizontalGradient(
            0f to Color.Transparent,
            0.15f to color,
            0.85f to color,
            1f to Color.Transparent,
        )
        drawRect(fade(GameColors.ButtonOutline.copy(alpha = 0.7f)), size = Size(size.width, line))
        drawRect(fade(GameColors.GlassFill), topLeft = Offset(0f, line), size = Size(size.width, line))
    }
}

/** Corner of [Corners.large], needed in px for the outline and shine. */
private val ENAMEL_CORNER = 28.dp
private val ENAMEL_OUTLINE = 2.5.dp
private val ENAMEL_RIM = 1.5.dp
private val ENAMEL_SHINE = 36.dp
private val DIVIDER_HEIGHT = 2.dp

/**
 * A well sunk into a dialog or panel, framed in gold: dark at the top (in
 * shadow), lighter below, with a thin gold gradient frame. For the parts of a
 * card that matter most — a final score, the rewards list.
 */
fun Modifier.goldInset(shape: Shape = Corners.medium): Modifier = this
    .clip(shape)
    .background(
        Brush.verticalGradient(
            listOf(GameColors.ButtonOutline.copy(alpha = 0.6f), GameColors.Ink.copy(alpha = 0.35f)),
        )
    )
    .border(INSET_FRAME, Brush.verticalGradient(listOf(GameColors.GoldTop, GameColors.GoldLip)), shape)

private val INSET_FRAME = 1.5.dp

/**
 * A plain well sunk into a panel: dark at the top (in shadow), lighter below,
 * with a faint edge — for tiles and rows inside a panel (login days, stats,
 * achievements). [tint] washes it in a colour, e.g. green once something is done.
 */
fun Modifier.sunkenWell(shape: Shape = Corners.medium, tint: Color? = null): Modifier = this
    .clip(shape)
    .background(
        Brush.verticalGradient(
            listOf(GameColors.ButtonOutline.copy(alpha = 0.6f), GameColors.Ink.copy(alpha = 0.35f)),
        )
    )
    .then(if (tint != null) Modifier.background(tint.copy(alpha = WELL_TINT_ALPHA)) else Modifier)
    .border(1.dp, Brush.verticalGradient(listOf(GameColors.ButtonOutline, GameColors.GlassFill)), shape)

private const val WELL_TINT_ALPHA = 0.2f

/** A coin reward or price inline in text: coin icon + "+40". */
@Composable
fun CoinAmount(text: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Image(com.apps.bubblepopping.ui.icons.GameIcons.Coin, contentDescription = null, modifier = Modifier.size(20.dp))
        OutlinedText(text, GameTheme.type.label, color = GameColors.Gold)
    }
}

/**
 * "1,240" with its icon, inside a dark enamel pill: dark outline, deep ink
 * gradient, a rim that shines at the top and fades out below, and a soft shine.
 * [tint] colours the fill and rim by what the pill counts (coins gold, stars yellow…).
 */
@Composable
fun ResourcePill(
    icon: ImageVector,
    value: String,
    modifier: Modifier = Modifier,
    tint: Color = GameColors.PillNeutral,
    /** Body of the pill; dark by default, or a coloured body (ocean, violet…). */
    tone: EnamelTone = EnamelTone.DARK,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = modifier
            .height(PILL_HEIGHT)
            .enamelPill(tint, tone)
            .padding(start = Spacing.xs, end = Spacing.m),
    ) {
        Image(imageVector = icon, contentDescription = null, modifier = Modifier.size(PILL_ICON))
        OutlinedText(value, GameTheme.type.label)
    }
}

private val PILL_HEIGHT = 34.dp
private val PILL_ICON = 26.dp
private val PILL_RIM = 1.5.dp
private val PILL_OUTLINE = 2.dp

/**
 * The enamel pill surface shared by [ResourcePill] and the in-game wave pill: dark
 * outline, ink gradient warmed by [tint], a soft shine, and a [tint] rim that
 * shines at the top and fades below. [EnamelTone.LIGHT] gives a brighter body.
 */
fun Modifier.enamelPill(tint: Color, tone: EnamelTone = EnamelTone.DARK): Modifier = this
    // Dark outline just outside the pill, like the buttons and icon art.
    .drawBehind {
        val outlinePx = PILL_OUTLINE.toPx()
        drawRoundRect(
            color = GameColors.ButtonOutline,
            topLeft = Offset(-outlinePx / 2f, -outlinePx / 2f),
            size = Size(size.width + outlinePx, size.height + outlinePx),
            cornerRadius = CornerRadius(size.height / 2f + outlinePx / 2f),
            style = Stroke(outlinePx),
        )
    }
    .clip(Corners.pill)
    .background(Brush.verticalGradient(listOf(lerp(tone.top, tint, 0.35f), lerp(tone.bottom, tint, 0.12f))))
    .drawBehind {
        // Soft glassy shine over the top half.
        val inset = size.height * 0.3f
        drawRoundRect(
            brush = Brush.verticalGradient(
                0f to GameColors.GlassShine,
                1f to Color.Transparent,
                startY = size.height * 0.1f,
                endY = size.height * 0.5f,
            ),
            topLeft = Offset(inset, size.height * 0.1f),
            size = Size(size.width - inset * 2f, size.height * 0.4f),
            cornerRadius = CornerRadius(size.height * 0.2f),
        )
    }
    .border(
        PILL_RIM,
        Brush.verticalGradient(
            0f to lerp(tint, Color.White, 0.45f),
            0.6f to tint.copy(alpha = 0.4f),
            1f to lerp(GameColors.Ink, tint, 0.3f),
        ),
        Corners.pill,
    )

/** Small red dot marking something new. */
@Composable
fun BadgeDot(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(GameColors.Badge)
            .border(2.dp, Color.White, CircleShape)
    )
}

/**
 * Rounded progress bar: a track sunk into the surface (dark, shaded at the top,
 * lit along the bottom edge) holding a shaded, glossy [color] fill.
 */
@Composable
fun GameProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = GameColors.Gold,
    height: Dp = 12.dp,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(Corners.pill)
            .background(
                Brush.verticalGradient(
                    listOf(GameColors.ButtonOutline.copy(alpha = 0.85f), GameColors.Ink.copy(alpha = 0.6f)),
                )
            )
            // Engraved edge: dark along the top, faint light along the bottom.
            .border(
                1.dp,
                Brush.verticalGradient(listOf(GameColors.ButtonOutline, GameColors.GlassFill)),
                Corners.pill,
            )
            .padding(2.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(Corners.pill)
                .background(
                    Brush.verticalGradient(
                        0f to lerp(color, Color.White, 0.35f),
                        0.5f to color,
                        1f to lerp(color, Color.Black, 0.25f),
                    )
                )
                .drawBehind {
                    // Shine along the top of the fill.
                    drawRoundRect(
                        Color.White.copy(alpha = 0.55f),
                        topLeft = Offset(size.height * 0.4f, size.height * 0.14f),
                        size = Size((size.width - size.height * 0.8f).coerceAtLeast(0f), size.height * 0.26f),
                        cornerRadius = CornerRadius(size.height),
                    )
                }
        )
    }
}

/** A chunky on/off switch: a bubble knob sliding along a glass or green track. */
@Composable
fun GameSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val feedback = LocalUiFeedback.current
    val knobOffset by animateDpAsState(
        targetValue = if (checked) SWITCH_TRAVEL else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "switch_knob",
    )
    val track by animateColorAsState(if (checked) GameColors.GreenBottom else GameColors.Ink.copy(alpha = 0.45f), label = "switch_track")
    Box(
        modifier = modifier
            .size(width = 58.dp, height = 34.dp)
            .semantics {
                role = Role.Switch
                toggleableState = ToggleableState(checked)
            }
            .clip(Corners.pill)
            .background(track)
            .border(2.dp, GameColors.GlassRim, Corners.pill)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                feedback.click()
                onCheckedChange(!checked)
            }
            .padding(4.dp),
    ) {
        Box(
            Modifier
                .offset(x = knobOffset)
                .size(26.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFDCE3FF))))
                .border(1.5.dp, GameColors.Ink.copy(alpha = 0.4f), CircleShape)
        )
    }
}

private val SWITCH_TRAVEL = 24.dp

/**
 * Tabs on an enamel track (dark outline, ink body, shine, rim): the selected tab
 * is a blue candy segment that slides between options, like the bottom dock.
 */
@Composable
fun GameTabs(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val feedback = LocalUiFeedback.current
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .enamelPill(GameColors.PillNeutral)
            .padding(TAB_INSET),
    ) {
        val slot = maxWidth / options.size
        val highlightX by animateDpAsState(
            targetValue = slot * selected.coerceIn(0, options.lastIndex),
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
            label = "tab_highlight",
        )
        // The sliding candy segment, under the labels.
        if (selected in options.indices) {
            Box(
                Modifier
                    .offset(x = highlightX)
                    .size(width = slot, height = TAB_HEIGHT)
                    .candyFace(ButtonColor.BLUE, Corners.pill),
            )
        }
        Row(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selected
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(TAB_HEIGHT)
                        .clip(Corners.pill)
                        .semantics {
                            role = Role.Tab
                            this.selected = isSelected
                        }
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            if (!isSelected) {
                                feedback.click()
                                onSelect(index)
                            }
                        },
                ) {
                    OutlinedText(
                        text = option,
                        style = GameTheme.type.label,
                        color = if (isSelected) GameColors.TextPrimary else GameColors.TextSecondary,
                        outlineColor = if (isSelected) GameColors.BlueLip else Color.Transparent,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private val TAB_HEIGHT = 38.dp
private val TAB_INSET = 4.dp

/** A labelled row with a switch — the standard settings line. */
@Composable
fun ToggleRow(
    icon: ImageVector,
    label: String,
    hint: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { stateDescription = if (checked) "On" else "Off" },
    ) {
        Image(imageVector = icon, contentDescription = null, modifier = Modifier.size(32.dp))
        Column(Modifier.weight(1f)) {
            androidx.compose.material3.Text(label, style = GameTheme.type.label, color = GameColors.TextPrimary)
            if (hint != null) androidx.compose.material3.Text(hint, style = GameTheme.type.caption, color = GameColors.TextSecondary)
        }
        GameSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
