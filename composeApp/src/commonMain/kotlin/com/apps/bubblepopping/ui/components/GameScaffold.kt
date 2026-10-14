package com.apps.bubblepopping.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.Res
import com.apps.bubblepopping.bg
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.Corners
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing
import com.apps.bubblepopping.view.home.DriftingBubbles
import org.jetbrains.compose.resources.painterResource

/** Which sky a menu screen sits on. */
enum class BackgroundStyle {
    /** The sunset art — Home only, where it greets the player. */
    ART,
    /** The calm night sky — every other menu screen, so cards and text get the focus. */
    NIGHT_SKY,
}

/**
 * The sky behind menu screens, with bubbles drifting up. [BackgroundStyle.ART]
 * is the sunset picture with a soft indigo scrim for readability;
 * [BackgroundStyle.NIGHT_SKY] is the quieter night sky with dimmer bubbles,
 * shaded with black so cards and text stand out.
 */
@Composable
fun GameBackground(
    modifier: Modifier = Modifier,
    style: BackgroundStyle = BackgroundStyle.NIGHT_SKY,
    bubbles: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.fillMaxSize().background(GameColors.NightTop)) {
        when (style) {
            BackgroundStyle.ART -> {
                Image(
                    painter = painterResource(Res.drawable.bg),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (bubbles) DriftingBubbles(Modifier.fillMaxSize())
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to GameColors.Scrim.copy(alpha = 0.25f),
                                0.4f to GameColors.Scrim.copy(alpha = 0.05f),
                                1f to GameColors.Scrim.copy(alpha = 0.5f),
                            )
                        )
                )
            }
            BackgroundStyle.NIGHT_SKY -> {
                NightSky(Modifier.fillMaxSize())
                if (bubbles) DriftingBubbles(Modifier.fillMaxSize().alpha(NIGHT_BUBBLE_ALPHA))
                // Shade the whole sky down so the content on top gets the focus.
                Box(Modifier.fillMaxSize().background(GameColors.BackgroundShade))
            }
        }
        content()
    }
}

/** Bubbles stay in the background on the night sky. */
private const val NIGHT_BUBBLE_ALPHA = 0.35f

/**
 * Top bar used by every screen: round back button on the left, outlined
 * title in the middle, resource pills (or other [actions]) on the right.
 *
 * The title stays centred on the screen but never runs under the side
 * content: it gets the width left between the wider side, mirrored, and is
 * shortened with "…" if that isn't enough (narrow phones, long titles).
 */
@Composable
fun GameTopBar(
    title: String?,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    /** Shown on the left when there is no back button (Home's avatar). */
    leading: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Layout(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(TOP_BAR_HEIGHT)
            .padding(horizontal = Spacing.screen),
        content = {
            Box {
                if (onBack != null) {
                    GameIconButton(icon = GameIcons.Back, contentDescription = "Back", onClick = onBack)
                } else if (leading != null) {
                    leading()
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
            Box {
                if (title != null) {
                    OutlinedText(text = title, style = GameTheme.type.title, outlineWidth = 3.dp, maxLines = 1)
                }
            }
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val start = measurables[0].measure(loose)
        val end = measurables[1].measure(loose)
        val gap = Spacing.s.roundToPx()
        val side = maxOf(start.width, end.width) + gap
        val titleWidth = (constraints.maxWidth - side * 2).coerceAtLeast(0)
        val title = measurables[2].measure(loose.copy(maxWidth = titleWidth))
        val height = constraints.maxHeight
        layout(constraints.maxWidth, height) {
            start.place(0, (height - start.height) / 2)
            end.place(constraints.maxWidth - end.width, (height - end.height) / 2)
            title.place((constraints.maxWidth - title.width) / 2, (height - title.height) / 2)
        }
    }
}

/**
 * Standard menu screen: sky background, top bar, and a scrolling column of
 * content with the standard side padding and spacing. [bottomBar] sits below.
 */
@Composable
fun GameScreen(
    title: String?,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    scrollable: Boolean = true,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    GameBackground(modifier) {
        Column(Modifier.fillMaxSize().screenBottomPadding()) {
            GameTopBar(title = title, onBack = onBack, actions = actions)
            Column(
                horizontalAlignment = horizontalAlignment,
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(horizontal = Spacing.screen, vertical = Spacing.s),
                content = content,
            )
            bottomBar?.invoke()
        }
    }
}

/** One tab in the [BottomDock]. */
data class DockItem(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val showBadge: Boolean = false,
)

/**
 * The bottom tab bar on every menu screen: a dark glass bar of icon tabs. The
 * current tab ([selectedIndex]) sits on a glossy blue highlight that slides
 * between tabs; its icon grows and its label gets the outline.
 */
@Composable
fun BottomDock(items: List<DockItem>, selectedIndex: Int, modifier: Modifier = Modifier) {
    val feedback = LocalUiFeedback.current
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screen, vertical = Spacing.s)
            .clip(Corners.large)
            .background(GameColors.Ink.copy(alpha = 0.7f))
            .border(2.dp, GameColors.GlassRim, Corners.large)
            .padding(DOCK_INSET),
    ) {
        val slot = maxWidth / items.size
        val highlightX by animateDpAsState(
            targetValue = slot * selectedIndex.coerceIn(0, items.lastIndex),
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
            label = "dock_highlight",
        )
        // The sliding highlight, drawn under the tabs.
        if (selectedIndex in items.indices) {
            Box(
                Modifier
                    .offset(x = highlightX)
                    .size(width = slot, height = DOCK_HEIGHT)
                    .clip(Corners.medium)
                    .background(Brush.verticalGradient(listOf(GameColors.BlueTop, GameColors.BlueBottom)))
                    .drawBehind {
                        // Gloss across the top.
                        drawRoundRect(
                            brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.05f))),
                            topLeft = Offset(size.width * 0.12f, size.height * 0.06f),
                            size = Size(size.width * 0.76f, size.height * 0.36f),
                            cornerRadius = CornerRadius(size.height * 0.2f),
                        )
                    }
                    .border(2.dp, Color.White.copy(alpha = 0.6f), Corners.medium)
            )
        }
        Row(Modifier.fillMaxWidth().height(DOCK_HEIGHT)) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                val iconScale by animateFloatAsState(
                    targetValue = if (selected) SELECTED_ICON_SCALE else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "dock_icon_${item.label}",
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics {
                            role = Role.Tab
                            this.selected = selected
                        }
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            feedback.click()
                            item.onClick()
                        },
                ) {
                    Box {
                        Image(
                            item.icon,
                            contentDescription = null,
                            modifier = Modifier.size(DOCK_ICON).graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            },
                        )
                        if (item.showBadge) BadgeDot(Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-2).dp))
                    }
                    if (selected) {
                        OutlinedText(item.label, GameTheme.type.caption, outlineColor = GameColors.BlueLip, maxLines = 1)
                    } else {
                        Text(item.label, style = GameTheme.type.caption, color = GameColors.TextSecondary, maxLines = 1)
                    }
                }
            }
        }
    }
}

val TOP_BAR_HEIGHT = 64.dp
private val DOCK_ICON = 30.dp
private val DOCK_HEIGHT = 58.dp
private val DOCK_INSET = 4.dp
private const val SELECTED_ICON_SCALE = 1.15f

/**
 * How much of the bottom of the screen the bottom dock covers, including the
 * system navigation bar under it; 0 when no dock is showing (e.g. in a round).
 */
val LocalBottomDockHeight = compositionLocalOf { 0.dp }

/**
 * Bottom padding for a screen's content: clears the bottom dock when it is
 * showing, otherwise just the system navigation bar.
 */
@Composable
fun Modifier.screenBottomPadding(): Modifier {
    val dock = LocalBottomDockHeight.current
    return if (dock > 0.dp) padding(bottom = dock) else navigationBarsPadding()
}
