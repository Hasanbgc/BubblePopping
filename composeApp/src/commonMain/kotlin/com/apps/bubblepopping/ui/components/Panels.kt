package com.apps.bubblepopping.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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

/** A coin reward or price inline in text: coin icon + "+40". */
@Composable
fun CoinAmount(text: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Image(com.apps.bubblepopping.ui.icons.GameIcons.Coin, contentDescription = null, modifier = Modifier.size(20.dp))
        OutlinedText(text, GameTheme.type.label, color = GameColors.Gold)
    }
}

/** "🪙 1,240": an icon sitting on a glass pill with a number. */
@Composable
fun ResourcePill(icon: ImageVector, value: String, modifier: Modifier = Modifier) {
    Box(modifier.height(36.dp), contentAlignment = Alignment.CenterStart) {
        Box(
            modifier = Modifier
                .padding(start = 14.dp)
                .clip(Corners.pill)
                .background(GameColors.Ink.copy(alpha = 0.45f))
                .border(1.5.dp, GameColors.GlassRim, Corners.pill)
                .padding(start = 22.dp, end = Spacing.m, top = 2.dp, bottom = 2.dp),
        ) {
            OutlinedText(value, GameTheme.type.label)
        }
        Image(imageVector = icon, contentDescription = null, modifier = Modifier.size(32.dp))
    }
}

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

/** Rounded progress bar with a glossy fill. */
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
            .background(GameColors.Ink.copy(alpha = 0.45f))
            .border(1.5.dp, GameColors.GlassRim, Corners.pill)
            .padding(2.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(Corners.pill)
                .background(color)
                .drawBehind {
                    drawRoundRect(
                        Color.White.copy(alpha = 0.45f),
                        topLeft = Offset(size.height * 0.4f, size.height * 0.15f),
                        size = Size((size.width - size.height * 0.8f).coerceAtLeast(0f), size.height * 0.3f),
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

/** Pill tabs: the selected option is a blue candy segment. */
@Composable
fun GameTabs(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val feedback = LocalUiFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Corners.pill)
            .background(GameColors.Ink.copy(alpha = 0.4f))
            .border(2.dp, GameColors.GlassRim, Corners.pill)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = index == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(Corners.pill)
                    .semantics {
                        role = Role.Tab
                        this.selected = isSelected
                    }
                    .background(
                        if (isSelected) Brush.verticalGradient(listOf(GameColors.BlueTop, GameColors.BlueBottom))
                        else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .clickable {
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
