package com.apps.bubblepopping.view.home

import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.apps.bubblepopping.ui.icons.playIcon
import com.apps.bubblepopping.ui.components.ButtonSize
import com.apps.bubblepopping.ui.components.GameButton
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
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
import androidx.compose.foundation.layout.Column
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
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.Corners
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing

/**
 * A game mode on Home: a tall framed card in the mode's [color] — its title,
 * its [art] (still) on a soft glow, its live progress ([info]), and a breathing
 * PLAY button. The whole card plays too: pressing it squashes it like the game's
 * buttons, with the click sound and buzz.
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
 * [ModeCard] with drawn [art] (given the modifier that sizes it), an optional
 * [subtitle] under the title, and a [locked] state: dimmed art, a greyed-out slate
 * LOCKED button instead of PLAY (not breathing), and no press.
 *
 * The card is a raised candy tile: a darker lip of its own colour underneath and a
 * soft shadow, a gold rim with an engraved light line inside it, soft rays and a
 * glow behind the art, a glossy sheen across the top, shading at the bottom and two
 * small sparkles. All of that is drawn once; only the button moves.
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
    // The PLAY button breathes; locked cards stay still. Read only in layers, so it never
    // recomposes or redraws the card.
    val breath = if (!locked) {
        rememberInfiniteTransition(label = "mode_card").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(BREATH_MS, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "breath",
        )
    } else null
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
            // The tile's base: a soft shadow, the lip (a darker band of the card's own
            // colour showing under the face) and the dark outline round both.
            .drawWithCache {
                val outline = CARD_OUTLINE.toPx()
                val radius = CornerRadius(CARD_CORNER.toPx())
                val lip = CARD_LIP.toPx()
                val lipBrush = Brush.verticalGradient(listOf(color.lip, lerp(color.lip, Color.Black, 0.3f)))
                onDrawBehind {
                    drawRoundRect(GameColors.ButtonShadow, topLeft = Offset(0f, lip * 0.8f), size = size, cornerRadius = radius)
                    drawRoundRect(lipBrush, topLeft = Offset(0f, lip), size = Size(size.width, size.height - lip), cornerRadius = radius)
                    drawRoundRect(
                        GameColors.ButtonOutline,
                        topLeft = Offset(-outline / 2f, -outline / 2f),
                        size = Size(size.width + outline, size.height + outline),
                        cornerRadius = CornerRadius(radius.x + outline / 2f),
                        style = Stroke(outline),
                    )
                }
            }
            // The face sits on top of the lip.
            .padding(bottom = CARD_LIP)
            .clip(Corners.large)
            .background(Brush.verticalGradient(listOf(color.top, color.bottom, lerp(color.bottom, color.lip, 0.6f))))
            .drawWithCache {
                val w = size.width
                val h = size.height
                val artCenter = Offset(w / 2f, h * ART_CENTER_Y)
                val reach = w * 0.85f
                val rays = sunburst(artCenter, reach, RAY_COUNT)
                val rayBrush = Brush.radialGradient(listOf(Color.White.copy(alpha = RAY_ALPHA), Color.Transparent), center = artCenter, radius = reach)
                val glow = Brush.radialGradient(listOf(Color.White.copy(alpha = GLOW_ALPHA), Color.Transparent), center = artCenter, radius = w * 0.55f)
                val sheen = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.38f), Color.Transparent), startY = h * 0.015f, endY = h * 0.2f)
                val shade = Brush.verticalGradient(0.62f to Color.Transparent, 1f to Color.Black.copy(alpha = BOTTOM_SHADE_ALPHA))
                val inset = FRAME_INSET.toPx()
                val frameRadius = CornerRadius(CARD_CORNER.toPx() - inset)
                val frame = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.08f)))
                onDrawBehind {
                    drawPath(rays, rayBrush)
                    drawCircle(glow, radius = w * 0.55f, center = artCenter)
                    drawRect(shade)
                    drawRoundRect(
                        sheen,
                        topLeft = Offset(w * 0.08f, h * 0.015f),
                        size = Size(w * 0.84f, h * 0.17f),
                        cornerRadius = CornerRadius(CARD_CORNER.toPx() * 0.6f),
                    )
                    // Engraved light line just inside the gold rim.
                    drawRoundRect(
                        frame,
                        topLeft = Offset(inset, inset),
                        size = Size(w - inset * 2f, h - inset * 2f),
                        cornerRadius = frameRadius,
                        style = Stroke(FRAME_WIDTH.toPx()),
                    )
                    drawSparkle(Offset(w * 0.14f, h * 0.07f), SPARKLE_SIZE.toPx())
                    drawSparkle(Offset(w * 0.88f, h * 0.13f), SPARKLE_SIZE.toPx() * 0.7f)
                }
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
                    .graphicsLayer { alpha = if (locked) LOCKED_ART_ALPHA else 1f },
            )
        }
        info()
        Spacer(Modifier.height(Spacing.s))
        // The standard game button (or LOCKED, greyed out). Tapping it or anywhere on the card plays.
        // Inset, so even at the top of a breath the button stays inside the card's inner frame.
        Box(Modifier.fillMaxWidth().padding(horizontal = Spacing.xs), contentAlignment = Alignment.Center) {
            if (breath != null) {
                // A soft green glow behind the button, brightening and swelling as it breathes.
                Box(
                    Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            val b = breath.value
                            alpha = BREATH_GLOW_MIN + (1f - BREATH_GLOW_MIN) * b
                            scaleX = 1f + BREATH_GLOW_GROW * b
                            scaleY = 1f + BREATH_GLOW_GROW * 2f * b
                        }
                        .drawWithCache {
                            val glowBrush = Brush.radialGradient(
                                listOf(GameColors.GreenTop.copy(alpha = BREATH_GLOW_ALPHA), Color.Transparent),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.width * 0.6f,
                            )
                            // Squashed into an oval round the button, soft on every side.
                            val ratio = (size.height * 1.8f) / size.width
                            onDrawBehind {
                                scale(scaleX = 1f, scaleY = ratio, pivot = center) {
                                    drawCircle(glowBrush, radius = size.width * 0.6f, center = center)
                                }
                            }
                        },
                )
            }
            GameButton(
                text = if (locked) "LOCKED" else "PLAY",
                onClick = onClick,
                color = if (locked) ButtonColor.SLATE else ButtonColor.GREEN,
                size = ButtonSize.MEDIUM,
                icon = if (locked) rememberVectorPainter(GameIcons.Lock) else playIcon(),
                enabled = !locked,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val grow = 1f + BREATH_SCALE * (breath?.value ?: 0f)
                        scaleX = grow
                        scaleY = grow
                    },
            )
        }
    }
}

/** [count] soft light rays fanning out from [center], [reach] long. */
private fun sunburst(center: Offset, reach: Float, count: Int) = Path().apply {
    val step = 2f * PI.toFloat() / count
    val half = step * RAY_WIDTH / 2f
    repeat(count) { i ->
        val a = i * step
        moveTo(center.x, center.y)
        lineTo(center.x + reach * cos(a - half), center.y + reach * sin(a - half))
        lineTo(center.x + reach * cos(a + half), center.y + reach * sin(a + half))
        close()
    }
}

/** A small four-point sparkle: a white star with a soft glow. */
private fun DrawScope.drawSparkle(center: Offset, size: Float) {
    drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.5f), Color.Transparent), center = center, radius = size), radius = size, center = center)
    val thin = size * 0.18f
    val star = Path().apply {
        moveTo(center.x, center.y - size)
        quadraticTo(center.x + thin, center.y - thin, center.x + size, center.y)
        quadraticTo(center.x + thin, center.y + thin, center.x, center.y + size)
        quadraticTo(center.x - thin, center.y + thin, center.x - size, center.y)
        quadraticTo(center.x - thin, center.y - thin, center.x, center.y - size)
        close()
    }
    drawPath(star, Color.White.copy(alpha = 0.9f))
}

private val CARD_CORNER = 28.dp
private val CARD_OUTLINE = 3.dp
private val CARD_RIM = 3.dp
/** How far the darker lip shows under the face: the tile's thickness. */
private val CARD_LIP = 6.dp
private val ART_SIZE = 96.dp
private val PRESS_SINK = 4.dp
private const val SQUASH_X = 0.03f
private const val SQUASH_Y = 0.05f
private const val GLOW_ALPHA = 0.35f
private const val LOCKED_ART_ALPHA = 0.45f
/** Where the art's glow and rays centre, as a fraction of the face's height. */
private const val ART_CENTER_Y = 0.4f
private const val RAY_COUNT = 14
/** Each ray's width as a fraction of the gap between rays. */
private const val RAY_WIDTH = 0.45f
private const val RAY_ALPHA = 0.16f
private const val BOTTOM_SHADE_ALPHA = 0.22f
private val FRAME_INSET = 7.dp
private val FRAME_WIDTH = 1.5.dp
private val SPARKLE_SIZE = 9.dp
/** One breath in (or out); a full breath is twice this. */
private const val BREATH_MS = 1_100
/** How much the button swells at the top of a breath. */
private const val BREATH_SCALE = 0.04f
private const val BREATH_GLOW_ALPHA = 0.75f
/** The glow never fully fades: at the bottom of a breath it's this bright. */
private const val BREATH_GLOW_MIN = 0.25f
private const val BREATH_GLOW_GROW = 0.06f
