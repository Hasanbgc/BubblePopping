package com.apps.bubblepopping.view.home

import androidx.compose.ui.unit.Dp
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.ui.components.ButtonColor
import com.apps.bubblepopping.ui.components.LocalUiFeedback
import com.apps.bubblepopping.ui.components.OutlinedText
import com.apps.bubblepopping.ui.components.candyFace
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.Corners
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing

/**
 * A game mode on Home: a tall framed card in the mode's [color] — its title,
 * its [art] bobbing gently on a soft glow, its live progress ([info]), and a
 * PLAY tag. The whole card is the button: pressing squashes it like the
 * game's buttons, with the click sound and buzz.
 */
@Composable
fun ModeCard(
    title: String,
    art: Painter,
    color: ButtonColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    info: @Composable () -> Unit,
) = ModeCard(
    title = title,
    color = color,
    onClick = onClick,
    modifier = modifier,
    art = { artModifier -> Image(art, contentDescription = null, modifier = artModifier) },
    info = info,
)

/**
 * [ModeCard] with drawn [art] (given the modifier that sizes and bobs it), an
 * optional [subtitle] under the title, and a [locked] state: dimmed art, a slate
 * LOCKED tag instead of PLAY, and no press.
 */
@Composable
fun ModeCard(
    title: String,
    color: ButtonColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    locked: Boolean = false,
    artSize: Dp = ART_SIZE,
    art: @Composable (Modifier) -> Unit,
    info: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "mode_card_press",
    )
    // Read only while drawing, so the bob never recomposes the card.
    val bob = rememberInfiniteTransition(label = "mode_card").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(BOB_MS, easing = LinearEasing), RepeatMode.Reverse),
        label = "bob",
    )
    val feedback = LocalUiFeedback.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer {
                // Squash: a little wider and shorter, and sunk slightly.
                scaleX = 1f + SQUASH_X * press
                scaleY = 1f - SQUASH_Y * press
                translationY = PRESS_SINK.toPx() * press
            }
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = if (locked) "$title, locked" else "Play $title"
            }
            .clickable(interactionSource = interaction, indication = null, enabled = !locked) {
                feedback.click()
                onClick()
            }
            // Soft drop shadow and dark outline, like the other cards.
            .drawBehind {
                val outline = CARD_OUTLINE.toPx()
                val radius = CARD_CORNER.toPx()
                drawRoundRect(
                    GameColors.ButtonShadow,
                    topLeft = Offset(0f, outline * 3f),
                    size = Size(size.width, size.height + outline),
                    cornerRadius = CornerRadius(radius),
                )
                drawRoundRect(
                    GameColors.ButtonOutline,
                    topLeft = Offset(-outline / 2f, -outline / 2f),
                    size = Size(size.width + outline, size.height + outline),
                    cornerRadius = CornerRadius(radius + outline / 2f),
                    style = Stroke(outline),
                )
            }
            .clip(Corners.large)
            // Body in the mode's colour, a glow behind the art, and a shine across the top.
            .background(Brush.verticalGradient(listOf(color.top, color.bottom, lerp(color.bottom, color.lip, 0.6f))))
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color.White.copy(alpha = GLOW_ALPHA), Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 0.42f),
                        radius = size.width * 0.55f,
                    ),
                    radius = size.width * 0.55f,
                    center = Offset(size.width / 2f, size.height * 0.42f),
                )
                drawRoundRect(
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent), endY = size.height * 0.2f),
                    topLeft = Offset(size.width * 0.1f, size.height * 0.02f),
                    size = Size(size.width * 0.8f, size.height * 0.18f),
                    cornerRadius = CornerRadius(CARD_CORNER.toPx() * 0.6f),
                )
            }
            // Gold rim: bright gold → amber → bronze.
            .border(CARD_RIM, Brush.verticalGradient(listOf(GameColors.GoldTop, GameColors.GoldBottom, GameColors.GoldLip)), Corners.large)
            .padding(horizontal = Spacing.s, vertical = Spacing.m),
    ) {
        OutlinedText(title, GameTheme.type.headline, outlineColor = color.lip, outlineWidth = 2.5.dp, maxLines = 1)
        subtitle?.let { Text(it, style = GameTheme.type.caption, color = GameColors.TextSecondary, maxLines = 1) }
        // The art fills the room between title and info, up to [artSize]; smaller on short cards.
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            art(
                Modifier
                    .sizeIn(maxWidth = artSize, maxHeight = artSize)
                    .aspectRatio(1f)
                    .graphicsLayer {
                        translationY = -BOB_HEIGHT.toPx() * bob.value
                        alpha = if (locked) LOCKED_ART_ALPHA else 1f
                    },
            )
        }
        info()
        Spacer(Modifier.height(Spacing.s))
        // The PLAY tag (or LOCKED): looks like a button; the whole card is what's pressed.
        val tag = if (locked) ButtonColor.SLATE else ButtonColor.GREEN
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.CenterHorizontally),
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val outline = 2.dp.toPx()
                    drawRoundRect(
                        GameColors.ButtonOutline,
                        topLeft = Offset(-outline / 2f, -outline / 2f),
                        size = Size(size.width + outline, size.height + outline),
                        cornerRadius = CornerRadius(size.height / 2f + outline / 2f),
                        style = Stroke(outline),
                    )
                }
                .candyFace(tag, Corners.pill)
                .padding(vertical = Spacing.xs),
        ) {
            Image(if (locked) GameIcons.Lock else GameIcons.Play, contentDescription = null, modifier = Modifier.size(PLAY_ICON))
            OutlinedText(if (locked) "LOCKED" else "PLAY", GameTheme.type.label, outlineColor = tag.lip, maxLines = 1)
        }
    }
}

private val CARD_CORNER = 28.dp
private val CARD_OUTLINE = 3.dp
private val CARD_RIM = 3.dp
private val ART_SIZE = 96.dp
private val BOB_HEIGHT = 6.dp
private val PLAY_ICON = 20.dp
private val PRESS_SINK = 4.dp
private const val SQUASH_X = 0.03f
private const val SQUASH_Y = 0.05f
private const val GLOW_ALPHA = 0.35f
private const val BOB_MS = 1_600
private const val LOCKED_ART_ALPHA = 0.45f
