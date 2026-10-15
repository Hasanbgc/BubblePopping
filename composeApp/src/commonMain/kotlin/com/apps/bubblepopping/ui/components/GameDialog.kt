package com.apps.bubblepopping.ui.components

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.Corners
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing

/**
 * The game's dialog: a violet card with a gold rim and engraved inner frame, a ribbon title, an optional
 * red close button, and a bouncy pop-in. Replaces every system AlertDialog.
 *
 * [content] goes in the body; put the buttons there too (stacked, full width).
 */
@Composable
fun GameDialog(
    title: String,
    onDismiss: () -> Unit,
    showClose: Boolean = true,
    dismissOnOutsideTap: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = dismissOnOutsideTap),
    ) {
        GameDialogCard(title = title, onClose = onDismiss.takeIf { showClose }, content = content)
    }
}

/** The dialog card on its own, for overlays drawn inside a screen (pause, results). */
@Composable
fun GameDialogCard(
    title: String,
    onClose: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scale = remember { Animatable(POP_IN_FROM) }
    val density = LocalDensity.current
    // Measured, so the content never slides under a taller ribbon.
    var ribbonHeight by remember { mutableStateOf(RIBBON_ESTIMATE) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xl)
            .widthIn(max = 420.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
            modifier = Modifier
                .padding(top = RIBBON_OVERLAP)
                .fillMaxWidth()
                // Soft drop shadow and dark outline, like the buttons and panels.
                .drawBehind {
                    val outline = CARD_OUTLINE.toPx()
                    val radius = CARD_CORNER.toPx()
                    drawRoundRect(
                        color = GameColors.ButtonShadow,
                        topLeft = Offset(0f, outline * 3f),
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
                .background(Brush.verticalGradient(listOf(GameColors.DialogTop, GameColors.DialogBottom)))
                .drawWithContent {
                    // Light from above: a soft glow behind the ribbon, a darker vignette at the bottom.
                    drawRect(
                        Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                            center = Offset(size.width / 2f, 0f),
                            radius = size.width * 0.7f,
                        )
                    )
                    drawRect(Brush.verticalGradient(0.6f to Color.Transparent, 1f to GameColors.Ink.copy(alpha = 0.35f)))
                    // Engraved inner frame, inset from the gold rim.
                    val inset = INNER_FRAME_INSET.toPx()
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(GameColors.GoldTop.copy(alpha = 0.45f), GameColors.GoldLip.copy(alpha = 0.25f))),
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - inset * 2f, size.height - inset * 2f),
                        cornerRadius = CornerRadius(CARD_CORNER.toPx() - inset),
                        style = Stroke(INNER_FRAME.toPx()),
                    )
                    drawContent()
                }
                // Gold rim: bright gold → amber → bronze.
                .border(
                    CARD_RIM,
                    Brush.verticalGradient(
                        0f to GameColors.GoldTop,
                        0.5f to GameColors.GoldBottom,
                        1f to GameColors.GoldLip,
                    ),
                    Corners.large,
                )
                // Content starts below the ribbon, whatever its real height (font, text size).
                .padding(start = Spacing.xl, end = Spacing.xl, top = (ribbonHeight - RIBBON_OVERLAP).coerceAtLeast(0.dp) + Spacing.l, bottom = Spacing.xl),
            content = content,
        )
        Ribbon(
            title,
            Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged { ribbonHeight = with(density) { it.height.toDp() } },
        )
        if (onClose != null) {
            GameIconButton(
                icon = GameIcons.Close,
                contentDescription = "Close",
                onClick = onClose,
                color = ButtonColor.RED,
                size = 42.dp,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = Spacing.s, y = RIBBON_OVERLAP - Spacing.m),
            )
        }
    }
}

/** The button stack at the bottom of a dialog: full width, main action first. */
@Composable
fun DialogButtons(content: @Composable ColumnScope.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
        content = content,
    )
}

/**
 * A violet ribbon banner, drawn in code: folded tails with shaded folds, a
 * candy band (three-tone face, bottom shade, glassy shine and glint), gold trim,
 * and the dark outline used across the UI.
 */
@Composable
fun Ribbon(text: String, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .defaultMinSize(minWidth = 220.dp)
            .drawBehind {
                val w = size.width
                val h = size.height
                val tail = w * 0.12f
                val bandBottom = h * 0.86f
                val outline = RIBBON_OUTLINE.toPx()
                val bandLeft = tail * 0.8f
                // Tails, behind the band: a darker violet with an outline.
                fun tailPath(left: Boolean) = Path().apply {
                    val outer = if (left) 0f else w
                    val inner = if (left) tail * 1.4f else w - tail * 1.4f
                    val notch = if (left) tail * 0.45f else w - tail * 0.45f
                    moveTo(inner, h * 0.25f)
                    lineTo(outer, h * 0.25f)
                    lineTo(notch, h * 0.62f)
                    lineTo(outer, h)
                    lineTo(inner, h)
                    close()
                }
                // The fold where each tail tucks under the band: a dark triangle.
                fun foldPath(left: Boolean) = Path().apply {
                    val edge = if (left) bandLeft else w - bandLeft
                    val inner = if (left) tail * 1.4f else w - tail * 1.4f
                    moveTo(edge, bandBottom)
                    lineTo(inner, bandBottom)
                    lineTo(inner, h)
                    close()
                }
                val tailBrush = Brush.verticalGradient(listOf(GameColors.VioletBottom, GameColors.VioletLip), startY = h * 0.25f, endY = h)
                for (left in listOf(true, false)) {
                    val path = tailPath(left)
                    drawPath(path, tailBrush)
                    drawPath(path, GameColors.ButtonOutline, style = Stroke(outline, join = StrokeJoin.Round))
                    drawPath(foldPath(left), GameColors.ButtonOutline.copy(alpha = 0.85f))
                }
                // Band: drop shadow, three-tone face, bottom shade, outline.
                val bandSize = Size(w - bandLeft * 2, bandBottom)
                val radius = CornerRadius(h * 0.3f)
                drawRoundRect(GameColors.ButtonShadow, Offset(bandLeft, outline * 2f), bandSize, radius)
                drawRoundRect(
                    Brush.verticalGradient(
                        0f to GameColors.VioletTop,
                        0.55f to lerp(GameColors.VioletTop, GameColors.VioletBottom, 0.7f),
                        1f to GameColors.VioletBottom,
                        endY = bandBottom,
                    ),
                    Offset(bandLeft, 0f), bandSize, radius,
                )
                drawRoundRect(
                    Brush.verticalGradient(0.55f to Color.Transparent, 1f to GameColors.VioletLip.copy(alpha = 0.5f), endY = bandBottom),
                    Offset(bandLeft, 0f), bandSize, radius,
                )
                // Shine across the top half, and a small glint at its left end.
                val shineInset = h * 0.3f
                val shineTop = h * 0.1f
                val shineSize = Size(bandSize.width - shineInset * 2, bandBottom * 0.34f)
                drawRoundRect(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.5f),
                        1f to Color.White.copy(alpha = 0f),
                        startY = shineTop,
                        endY = shineTop + shineSize.height,
                    ),
                    Offset(bandLeft + shineInset, shineTop), shineSize, CornerRadius(shineSize.height / 2f),
                )
                val glint = Size(h * 0.28f, h * 0.09f)
                drawRoundRect(
                    Color.White.copy(alpha = 0.85f),
                    Offset(bandLeft + shineInset + h * 0.1f, shineTop + h * 0.05f), glint, CornerRadius(glint.height / 2f),
                )
                // Gold trim line inside the band.
                val inset = h * 0.12f
                drawRoundRect(
                    Brush.verticalGradient(listOf(GameColors.GoldTop, GameColors.GoldBottom), startY = inset, endY = bandBottom - inset),
                    Offset(bandLeft + inset, inset),
                    Size(bandSize.width - inset * 2, bandBottom - inset * 2),
                    CornerRadius(h * 0.2f),
                    style = Stroke(width = 2.dp.toPx()),
                )
                drawRoundRect(
                    GameColors.ButtonOutline,
                    Offset(bandLeft, 0f), bandSize, radius,
                    style = Stroke(outline),
                )
            }
            .padding(start = Spacing.xxl + Spacing.l, end = Spacing.xxl + Spacing.l, top = Spacing.s, bottom = Spacing.m + 2.dp),
    ) {
        OutlinedText(
            text = text,
            // Long titles ("Congratulations!") step down a size so they fit the ribbon.
            style = if (text.length > LONG_TITLE_CHARS) GameTheme.type.headline else GameTheme.type.title,
            outlineColor = GameColors.VioletLip,
            outlineWidth = 2.5.dp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

private const val POP_IN_FROM = 0.8f
/** How far the ribbon sticks out above the card. */
private val RIBBON_OVERLAP = 26.dp
/** Ribbon height before it is measured (title font at normal text size). */
private val RIBBON_ESTIMATE = 56.dp
/** Corner of [Corners.large], needed in px for the outline and inner frame. */
private val CARD_CORNER = 28.dp
private val CARD_OUTLINE = 3.dp
private val CARD_RIM = 3.dp
private val INNER_FRAME = 1.5.dp
private val INNER_FRAME_INSET = 8.dp
private val RIBBON_OUTLINE = 2.5.dp
/** Ribbon titles longer than this use the smaller headline style. */
private const val LONG_TITLE_CHARS = 12

/**
 * A full-screen dim layer holding a centred dialog card, for in-game moments
 * (pause, results). Scrolls when the card is taller than the screen. Taps on
 * the dim area call [onScrimClick] (e.g. resume), or are swallowed.
 */
@Composable
fun OverlayScrim(
    modifier: Modifier = Modifier,
    onScrimClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GameColors.Scrim.copy(alpha = SCRIM_ALPHA))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onScrimClick?.invoke() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = Spacing.xl),
            content = content,
        )
    }
}

private const val SCRIM_ALPHA = 0.7f
