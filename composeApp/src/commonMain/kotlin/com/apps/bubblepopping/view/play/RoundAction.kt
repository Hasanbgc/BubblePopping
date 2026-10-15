package com.apps.bubblepopping.view.play

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.ui.components.ButtonColor
import com.apps.bubblepopping.ui.components.GameIconButton
import com.apps.bubblepopping.ui.components.OutlinedText
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing

/**
 * The row of secondary actions under a card's main button (pause, stage
 * results): round candy buttons with labels, each an equal share of the width
 * so they never crowd on small phones.
 */
@Composable
internal fun RoundActions(content: @Composable RowScope.() -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth(),
        content = content,
    )
}

/** One round candy button with its label underneath, inside [RoundActions]. */
@Composable
internal fun RowScope.RoundAction(icon: ImageVector, label: String, color: ButtonColor, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
        modifier = Modifier.weight(1f),
    ) {
        GameIconButton(icon = icon, contentDescription = label, onClick = onClick, color = color, size = ROUND_ACTION_SIZE)
        OutlinedText(label, GameTheme.type.label, outlineWidth = 1.5.dp, textAlign = TextAlign.Center, maxLines = 1)
    }
}

private val ROUND_ACTION_SIZE = 54.dp
