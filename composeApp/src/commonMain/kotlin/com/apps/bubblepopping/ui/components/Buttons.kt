package com.apps.bubblepopping.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.BubbleTheme
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing

/** Colour roles for buttons. Each colour has one job everywhere in the game. */
enum class ButtonColor(val top: Color, val bottom: Color, val lip: Color) {
    /** Main action of a screen: Play, Next, Resume. */
    BLUE(GameColors.BlueTop, GameColors.BlueBottom, GameColors.BlueLip),
    /** Special / featured: Daily Challenge, modes. */
    VIOLET(GameColors.VioletTop, GameColors.VioletBottom, GameColors.VioletLip),
    /** Confirm, claim, equip. */
    GREEN(GameColors.GreenTop, GameColors.GreenBottom, GameColors.GreenLip),
    /** Spending or receiving coins. */
    GOLD(GameColors.GoldTop, GameColors.GoldBottom, GameColors.GoldLip),
    /** Destructive: reset, quit. */
    RED(GameColors.RedTop, GameColors.RedBottom, GameColors.RedLip),
    /** Secondary actions: Back, Later, Restart. */
    GLASS(GameColors.GlassButtonTop, GameColors.GlassButtonBottom, GameColors.GlassButtonLip),
}

enum class ButtonSize(val height: Dp, val lip: Dp, val horizontalPadding: Dp, val iconSize: Dp) {
    LARGE(60.dp, 6.dp, 28.dp, 28.dp),
    MEDIUM(50.dp, 5.dp, 22.dp, 24.dp),
    SMALL(38.dp, 4.dp, 14.dp, 18.dp),
}

/**
 * The chunky game button: a glossy candy face sitting on a darker lip. Pressing
 * sinks the face onto the lip (plus a pop sound and a light buzz).
 *
 * Width follows [modifier] (e.g. `fillMaxWidth()`); otherwise it wraps the text.
 */
@Composable
fun GameButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: ButtonColor = ButtonColor.BLUE,
    size: ButtonSize = ButtonSize.MEDIUM,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val textStyle = when (size) {
        ButtonSize.LARGE -> GameTheme.type.headline
        ButtonSize.MEDIUM, ButtonSize.SMALL -> GameTheme.type.label
    }
    PressableSurface(
        onClick = onClick,
        color = color,
        shape = RoundedCornerShape(size.height / 2.4f),
        lip = size.lip,
        enabled = enabled,
        role = Role.Button,
        modifier = modifier.heightIn(min = size.height + size.lip),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = size.horizontalPadding),
        ) {
            if (icon != null) Image(imageVector = icon, contentDescription = null, modifier = Modifier.size(size.iconSize))
            ButtonLabel(text, textStyle, color)
        }
    }
}

@Composable
private fun ButtonLabel(text: String, style: TextStyle, color: ButtonColor) {
    OutlinedText(
        text = text,
        style = style,
        outlineColor = if (color == ButtonColor.GLASS) GameColors.Ink else color.lip,
        maxLines = 1,
    )
}

/** A round bubble button holding one icon: back, close, pause, settings… */
@Composable
fun GameIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: ButtonColor = ButtonColor.GLASS,
    size: Dp = 48.dp,
    enabled: Boolean = true,
) {
    val lip = size / 12
    PressableSurface(
        onClick = onClick,
        color = color,
        shape = CircleShape,
        lip = lip,
        enabled = enabled,
        role = Role.Button,
        modifier = modifier
            .size(width = size, height = size + lip)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Image(imageVector = icon, contentDescription = null, modifier = Modifier.size(size * 0.55f))
    }
}

/**
 * Shared look of every pressable game control: gradient face, white rim, a
 * gloss band across the top half, and a [lip] underneath that the face sinks
 * onto while pressed.
 */
@Composable
internal fun PressableSurface(
    onClick: () -> Unit,
    color: ButtonColor,
    shape: Shape,
    lip: Dp,
    enabled: Boolean,
    role: Role,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed && enabled) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "press",
    )
    val feedback = LocalUiFeedback.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .semantics { this.role = role }
            .clickable(interactionSource = interaction, indication = null, enabled = enabled) {
                feedback.click()
                onClick()
            }
            // Reserve the lip below the face.
            .padding(bottom = lip)
            .graphicsLayer {
                translationY = lip.toPx() * press
                // A small squash makes the press feel springy.
                scaleX = 1f + 0.02f * press
                scaleY = 1f - 0.03f * press
            }
            // The lip: same shape, shifted down; it stays put while the face sinks.
            .drawBehind {
                val lipPx = lip.toPx() * (1f - press)
                val outline = shape.createOutline(size, layoutDirection, this)
                val radius = (outline as? androidx.compose.ui.graphics.Outline.Rounded)?.roundRect?.topLeftCornerRadius
                    ?: CornerRadius(size.height / 2f)
                drawRoundRect(color.lip, topLeft = Offset(0f, lipPx), size = size, cornerRadius = radius)
            }
            .clip(shape)
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(color.top, color.bottom)))
                // Gloss: a soft white band across the top.
                val inset = size.height * 0.12f
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.05f))),
                    topLeft = Offset(inset * 1.5f, inset * 0.6f),
                    size = Size(size.width - inset * 3f, size.height * 0.42f),
                    cornerRadius = CornerRadius(size.height * 0.25f),
                )
            }
            .border(2.dp, Color.White.copy(alpha = 0.45f), shape),
    ) {
        content()
    }
}

private const val DISABLED_ALPHA = 0.45f

@Preview
@Composable
fun GameButtonsPreview() {
    BubbleTheme {
        androidx.compose.foundation.layout.Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
            modifier = Modifier.padding(Spacing.l),
        ) {
            ButtonColor.entries.forEach { GameButton(text = it.name.lowercase().replaceFirstChar(Char::uppercase), onClick = {}, color = it) }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                GameIconButton(GameIcons.Back, "Back", onClick = {})
                GameIconButton(GameIcons.Pause, "Pause", onClick = {}, color = ButtonColor.BLUE)
                GameIconButton(GameIcons.Gear, "Settings", onClick = {})
            }
        }
    }
}
