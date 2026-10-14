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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
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

/**
 * The sky behind every menu: the sunset art, bubbles drifting up, and a soft
 * indigo scrim towards the bottom so text and panels stay readable.
 */
@Composable
fun GameBackground(modifier: Modifier = Modifier, bubbles: Boolean = true, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize().background(GameColors.Scrim)) {
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
        content()
    }
}

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
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
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

/** One slot in the [BottomDock]. */
data class DockItem(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val showBadge: Boolean = false,
    /** The raised middle slot (the screen you're on). */
    val isCenter: Boolean = false,
)

/** Home's bottom dock: glass bar of icon buttons, the middle one raised. */
@Composable
fun BottomDock(items: List<DockItem>, modifier: Modifier = Modifier) {
    val feedback = LocalUiFeedback.current
    Box(modifier.fillMaxWidth().padding(horizontal = Spacing.screen, vertical = Spacing.s)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = DOCK_RAISE)
                .clip(Corners.large)
                .background(GameColors.Ink.copy(alpha = 0.55f))
                .border(2.dp, GameColors.GlassRim, Corners.large)
                .padding(vertical = Spacing.s),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            items.forEach { item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { role = Role.Button }
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            feedback.click()
                            item.onClick()
                        },
                ) {
                    if (item.isCenter) {
                        // Leave room: the raised bubble is drawn on top.
                        Spacer(Modifier.height(DOCK_ICON))
                    } else {
                        Box {
                            Image(item.icon, contentDescription = null, modifier = Modifier.size(DOCK_ICON))
                            if (item.showBadge) BadgeDot(Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-2).dp))
                        }
                    }
                    Text(item.label, style = GameTheme.type.caption, color = GameColors.TextPrimary)
                }
            }
        }
        // Raised centre bubble.
        val centerIndex = items.indexOfFirst { it.isCenter }
        if (centerIndex >= 0) {
            val center = items[centerIndex]
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(DOCK_CENTER)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(GameColors.BlueTop, GameColors.BlueBottom)))
                    .border(3.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                    .clickable { feedback.click(); center.onClick() },
            ) {
                Image(center.icon, contentDescription = center.label, modifier = Modifier.size(DOCK_ICON + 6.dp))
            }
        }
    }
}

val TOP_BAR_HEIGHT = 64.dp
private val DOCK_ICON = 34.dp
private val DOCK_CENTER = 64.dp
private val DOCK_RAISE = 18.dp
