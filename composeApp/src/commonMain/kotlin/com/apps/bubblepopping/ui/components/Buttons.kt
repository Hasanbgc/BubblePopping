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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
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
    /** Locked things that can't be used yet: locked stages on the map. */
    SLATE(GameColors.SlateTop, GameColors.SlateBottom, GameColors.SlateLip),
}

enum class ButtonSize(val height: Dp, val lip: Dp, val horizontalPadding: Dp, val iconSize: Dp) {
    LARGE(60.dp, 6.dp, 28.dp, 28.dp),
    MEDIUM(50.dp, 5.dp, 22.dp, 24.dp),
    SMALL(38.dp, 4.dp, 14.dp, 18.dp),
}

/**
 * The chunky game button: an outlined candy face sitting on a darker lip. Pressing
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
) = GameButton(text, onClick, modifier, color, size, icon?.let { rememberVectorPainter(it) }, enabled)

/** [GameButton] with an image icon, e.g. a `painterResource` from the art. */
@Composable
fun GameButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: ButtonColor = ButtonColor.BLUE,
    size: ButtonSize = ButtonSize.MEDIUM,
    icon: Painter?,
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
            if (icon != null) Image(painter = icon, contentDescription = null, modifier = Modifier.size(size.iconSize))
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
) = GameIconButton(rememberVectorPainter(icon), contentDescription, onClick, modifier, color, size, enabled)

/** [GameIconButton] with an image icon, e.g. the Play art. */
@Composable
fun GameIconButton(
    icon: Painter,
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
        Image(painter = icon, contentDescription = null, modifier = Modifier.size(size * 0.55f))
    }
}

/**
 * Shared look of every pressable game control: dark outline and drop shadow,
 * a three-tone face with a bevelled rim, a glassy shine and glint, and a [lip] underneath that the face sinks
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
            // Below the face: soft drop shadow, dark outline around face + lip, then the lip,
            // which stays put while the face sinks onto it.
            .drawBehind {
                val lipPx = lip.toPx() * (1f - press)
                val outlinePx = OUTLINE_WIDTH.toPx()
                val outline = shape.createOutline(size, layoutDirection, this)
                val radius = (outline as? androidx.compose.ui.graphics.Outline.Rounded)?.roundRect?.topLeftCornerRadius
                    ?: CornerRadius(size.height / 2f)
                val outerRadius = CornerRadius(radius.x + outlinePx, radius.y + outlinePx)
                /*drawRoundRect(
                    color = GameColors.ButtonShadow,
                    topLeft = Offset(-outlinePx, lipPx + outlinePx * 2.5f),
                    size = Size(size.width + outlinePx * 2, size.height + outlinePx * 2),
                    cornerRadius = outerRadius,
                )*/
                /*drawRoundRect(
                    color = GameColors.ButtonOutline,
                    topLeft = Offset(-outlinePx / 2f, -outlinePx / 2f),
                    size = Size(size.width + outlinePx, size.height + lipPx + outlinePx),
                    cornerRadius = CornerRadius(radius.x + outlinePx / 2f, radius.y + outlinePx / 2f),
                    style = Stroke(outlinePx),
                )*/
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(color.bottom, color.lip), startY = lipPx, endY = size.height + lipPx),
                    topLeft = Offset(0f, lipPx),
                    size = size,
                    cornerRadius = radius,
                )
            }
            .candyFace(color, shape),
    ) {
        content()
    }
}

/**
 * The candy face shared by buttons and the selected dock tab: light top → rich
 * middle → deep bottom, an inner shade along the bottom, a glassy shine with a
 * small glint, and a bevelled rim (bright top, dark bottom).
 */
internal fun Modifier.candyFace(color: ButtonColor, shape: Shape): Modifier = this
    .clip(shape)
    .drawWithCache {
        val h = size.height
        val w = size.width
        // Face: light top → rich middle → deep bottom.
        val face = Brush.verticalGradient(
            0f to color.top,
            0.55f to lerp(color.top, color.bottom, 0.7f),
            1f to color.bottom,
        )
        // Inner shade along the bottom edge, so the face looks rounded, not flat.
        val shade = Brush.verticalGradient(
            0.55f to Color.Transparent,
            1f to color.lip.copy(alpha = 0.45f),
            startY = 0f,
            endY = h,
        )
        // Shine: a crisp glassy band in the top half with a hard-ish lower edge.
        val shineInset = h * 0.14f
        val shineTop = h * 0.08f
        val shineSize = Size(w - shineInset * 2f, h * 0.36f)
        val shine = Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.55f),
            0.85f to Color.White.copy(alpha = 0.12f),
            1f to Color.White.copy(alpha = 0f),
            startY = shineTop,
            endY = shineTop + shineSize.height,
        )
        // Glint: a small bright streak and dot near the top-left.
        val glintSize = Size(h * 0.32f, h * 0.1f)
        val glintTopLeft = Offset(shineInset + h * 0.08f, shineTop + h * 0.06f)
        onDrawBehind {
            drawRect(face)
            drawRect(shade)
            drawRoundRect(shine, Offset(shineInset, shineTop), shineSize, CornerRadius(shineSize.height / 2f))
            drawRoundRect(Color.White.copy(alpha = 0.85f), glintTopLeft, glintSize, CornerRadius(glintSize.height / 2f))
            drawCircle(
                Color.White.copy(alpha = 0.85f),
                radius = glintSize.height * 0.5f,
                center = Offset(glintTopLeft.x + glintSize.width + glintSize.height * 1.1f, glintTopLeft.y + glintSize.height / 2f),
            )
        }
    }
    // Bevel: bright rim along the top fading to a dark rim along the bottom.
    .border(
        BEVEL_WIDTH,
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.8f),
            0.45f to Color.White.copy(alpha = 0.1f),
            1f to Color.Black.copy(alpha = 0.18f),
        ),
        shape,
    )

private const val DISABLED_ALPHA = 0.45f
/** Dark outline around the whole button, matching the outlines in the icon art. */
private val OUTLINE_WIDTH = 2.5.dp
private val BEVEL_WIDTH = 1.5.dp

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
