package com.apps.bubblepopping.view.play.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.ui.components.ButtonColor
import com.apps.bubblepopping.ui.components.candyFace
import com.apps.bubblepopping.ui.theme.GameColors

/**
 * A round candy disc with a dark outline ring, at the left of the in-game info
 * pills (the wave number, the stage goal's icon).
 */
@Composable
internal fun Medallion(
    color: ButtonColor,
    modifier: Modifier = Modifier,
    size: Dp = MEDALLION_SIZE,
    content: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .drawBehind {
                val outline = MEDALLION_OUTLINE.toPx()
                drawCircle(GameColors.ButtonOutline, radius = this.size.minDimension / 2f + outline / 2f, style = Stroke(outline))
            }
            .candyFace(color, CircleShape),
    ) {
        content()
    }
}

internal val MEDALLION_SIZE = 38.dp
private val MEDALLION_OUTLINE = 2.dp
