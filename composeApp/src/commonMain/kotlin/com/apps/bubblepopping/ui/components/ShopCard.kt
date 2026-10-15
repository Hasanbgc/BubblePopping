package com.apps.bubblepopping.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.BubbleTheme
import com.apps.bubblepopping.ui.theme.Corners
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing
import kotlin.math.PI
import kotlin.math.sin

/** Rarity of a shop card: the colour of its tag and of the glow behind its preview. */
enum class CardTier(val label: String, val tag: ButtonColor, val glow: Color) {
    COMMON("Common", ButtonColor.GLASS, GameColors.TierCommon),
    RARE("Rare", ButtonColor.BLUE, GameColors.BlueTop),
    EPIC("Epic", ButtonColor.VIOLET, GameColors.VioletTop),
    LEGENDARY("Legendary", ButtonColor.GOLD, GameColors.Gold),
}

/**
 * A collectible card for the shop: dark enamel body with a gold rim and corner
 * glint, its [tier] tag on top, the item's [preview] in a gold-framed window lit by a tier
 * glow, its name, and an [action] (buy / equip / unlock hint) underneath.
 *
 * - [equipped]: a slowly pulsing green glow (show [EquippedLabel] as the action).
 * - [locked]: the window is dimmed under a big lock.
 * - [owned] turning true (a purchase) sweeps a shimmer across the window.
 * - Legendary cards get a brighter rim and twinkling sparkles.
 *
 * [preview] draws the item and gets the glow colour to light it from behind.
 */
@Composable
fun ShopCard(
    name: String,
    tier: CardTier,
    preview: @Composable (glow: Color, modifier: Modifier) -> Unit,
    action: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    owned: Boolean = false,
    equipped: Boolean = false,
    locked: Boolean = false,
) {
    val legendary = tier == CardTier.LEGENDARY
    val transition = rememberInfiniteTransition(label = "shop_card")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(PULSE_MS, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val twinkle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(TWINKLE_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "twinkle",
    )
    // Shimmer once when the item becomes owned (not on first show). 1 = finished.
    val shimmer = remember { Animatable(1f) }
    var wasOwned by remember { mutableStateOf(owned) }
    LaunchedEffect(owned) {
        if (owned && !wasOwned) {
            shimmer.snapTo(0f)
            shimmer.animateTo(1f, tween(SHIMMER_MS, easing = FastOutSlowInEasing))
        }
        wasOwned = owned
    }

    val glow = when {
        locked -> Color.Transparent
        equipped -> GameColors.Success.copy(alpha = 0.4f + 0.35f * pulse)
        else -> tier.glow.copy(alpha = GLOW_ALPHA)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
        modifier = modifier
            // Soft drop shadow and dark outline, like the buttons and pills.
            .drawBehind {
                val outline = CARD_OUTLINE.toPx()
                val radius = CARD_CORNER.toPx()
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
            .background(Brush.verticalGradient(listOf(GameColors.InkSoft, GameColors.Ink)))
            // Glint on the gold rim at the top-left corner, drawn over the rim.
            .drawWithContent {
                drawContent()
                val inset = CARD_CORNER.toPx() * 0.3f
                sparkle(Offset(inset, inset), CORNER_GLINT.toPx(), GameColors.CardGlint)
            }
            .border(if (legendary) RIM_LEGENDARY else RIM, goldRim(legendary), Corners.large)
            .padding(Spacing.m),
    ) {
        // Tier tag on top of the card, above the window.
        TierTag(tier)
        // The preview window.
        Box(Modifier.fillMaxWidth().aspectRatio(WINDOW_ASPECT)) {
            Box(Modifier.matchParentSize().clip(Corners.medium)) {
                preview(glow, Modifier.matchParentSize())
                if (locked) {
                    Box(Modifier.matchParentSize().background(GameColors.Ink.copy(alpha = LOCKED_SHADE)), contentAlignment = Alignment.Center) {
                        Image(GameIcons.Lock, contentDescription = "Locked", modifier = Modifier.size(LOCK_SIZE))
                    }
                }
                // Glass: inner shadow, legendary sparkles, purchase shimmer.
                Canvas(Modifier.matchParentSize()) {
                    drawRect(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.35f), 0.2f to Color.Transparent))
                    drawRect(Brush.verticalGradient(0.8f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.3f)))
                    if (legendary && !locked) {
                        SPARKLES.forEachIndexed { i, (fx, fy) ->
                            val alpha = 0.5f + 0.5f * sin(twinkle + i * 1.7f)
                            sparkle(Offset(fx * size.width, fy * size.height), SPARKLE.toPx() * (0.6f + 0.4f * alpha), Color.White.copy(alpha = alpha))
                        }
                    }
                    if (shimmer.value < 1f) {
                        val x = (-0.4f + 1.8f * shimmer.value) * size.width
                        val band = size.width * 0.25f
                        drawRect(
                            Brush.linearGradient(
                                0f to Color.Transparent,
                                0.5f to Color.White.copy(alpha = 0.6f),
                                1f to Color.Transparent,
                                start = Offset(x - band, 0f),
                                end = Offset(x + band, size.height * 0.4f),
                            )
                        )
                    }
                }
            }
            // Gold picture frame around the window.
            Box(Modifier.matchParentSize().border(FRAME, Brush.verticalGradient(listOf(GameColors.GoldTop, GameColors.GoldLip)), Corners.medium))
        }
        OutlinedText(name, GameTheme.type.label, maxLines = 1)
        Box(Modifier.fillMaxWidth().heightIn(min = ACTION_HEIGHT), contentAlignment = Alignment.Center) {
            action()
        }
    }
}

/** Small candy tag naming the tier, e.g. "RARE". */
@Composable
private fun TierTag(tier: CardTier, modifier: Modifier = Modifier) {
    Box(
        modifier
            .candyFace(tier.tag, Corners.pill)
            .padding(horizontal = Spacing.s, vertical = 1.dp),
    ) {
        OutlinedText(
            text = tier.label.uppercase(),
            style = GameTheme.type.caption,
            outlineColor = if (tier.tag == ButtonColor.GLASS) GameColors.Ink else tier.tag.lip,
            outlineWidth = 1.5.dp,
            maxLines = 1,
        )
    }
}

/** "✓ Equipped" for a card's action row: a round green candy check and the word. */
@Composable
fun EquippedLabel(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(CHECK_BADGE)
                .candyFace(ButtonColor.GREEN, CircleShape),
        ) {
            Image(GameIcons.Check, contentDescription = null, modifier = Modifier.size(CHECK_BADGE * 0.6f))
        }
        OutlinedText("Equipped", GameTheme.type.label, maxLines = 1)
    }
}

/** Gold rim: bright gold → amber → bronze; brighter and whiter for legendary cards. */
private fun goldRim(legendary: Boolean): Brush = if (legendary) {
    Brush.verticalGradient(
        0f to GameColors.CardGlint,
        0.35f to GameColors.GoldTop,
        0.7f to GameColors.Gold,
        1f to GameColors.GoldBottom,
    )
} else {
    Brush.verticalGradient(
        0f to GameColors.GoldTop,
        0.5f to GameColors.GoldBottom,
        1f to GameColors.GoldLip,
    )
}

/** A four-point sparkle: two crossed tapered strokes with a bright core. */
private fun DrawScope.sparkle(center: Offset, radius: Float, color: Color) {
    val stroke = radius * 0.28f
    drawLine(color, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), stroke, StrokeCap.Round)
    drawLine(color, Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), stroke, StrokeCap.Round)
    drawCircle(color, radius = stroke * 1.1f, center = center)
}

/** Corner of [Corners.large], needed in px for the outline and glint. */
private val CARD_CORNER = 28.dp
private val CARD_OUTLINE = 2.5.dp
private val RIM = 2.5.dp
private val RIM_LEGENDARY = 3.dp
private val FRAME = 1.5.dp
private val CORNER_GLINT = 6.dp
private val SPARKLE = 5.dp
private val LOCK_SIZE = 40.dp
private val CHECK_BADGE = 26.dp
/** Every card keeps the same height whatever its action. */
private val ACTION_HEIGHT = 42.dp
private const val WINDOW_ASPECT = 1.2f
private const val GLOW_ALPHA = 0.55f
private const val LOCKED_SHADE = 0.6f
private const val PULSE_MS = 1_400
private const val TWINKLE_MS = 2_400
private const val SHIMMER_MS = 900
/** Where the legendary sparkles sit, as fractions of the window. */
private val SPARKLES = listOf(0.2f to 0.7f, 0.82f to 0.3f, 0.75f to 0.8f, 0.35f to 0.25f)

@Preview
@Composable
fun ShopCardPreview() {
    BubbleTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.m), modifier = Modifier.background(GameColors.NightMiddle).padding(Spacing.l)) {
            CardTier.entries.forEachIndexed { index, tier ->
                ShopCard(
                    name = tier.label,
                    tier = tier,
                    equipped = index == 1,
                    locked = index == 3,
                    preview = { glow, modifier ->
                        Canvas(modifier.background(GameColors.NightBottom)) {
                            drawCircle(Brush.radialGradient(listOf(glow, Color.Transparent)), radius = size.minDimension * 0.6f)
                        }
                    },
                    action = { GameButton("250", onClick = {}, color = ButtonColor.GOLD, size = ButtonSize.SMALL, icon = GameIcons.Coin) },
                    modifier = Modifier.size(width = 170.dp, height = 240.dp),
                )
            }
        }
    }
}
