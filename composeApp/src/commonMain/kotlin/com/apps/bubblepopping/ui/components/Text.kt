package com.apps.bubblepopping.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.ui.theme.GameColors

/**
 * What a tap on a game control does besides its action: a soft pop and a light
 * buzz (each only if enabled in Settings). Provided once at the app root.
 */
fun interface UiFeedback {
    fun click()
}

val LocalUiFeedback = staticCompositionLocalOf { UiFeedback { } }

/**
 * Game-style text: white fill with a dark outline and a soft drop shadow, like
 * the logo. Used for titles, buttons and numbers on top of the busy sky.
 */
@Composable
fun OutlinedText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = GameColors.TextPrimary,
    outlineColor: Color = GameColors.Ink,
    outlineWidth: Dp = 2.dp,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    // A stroke is centred on the glyph edge, so twice the width shows [outlineWidth] outside it.
    val strokePx = with(LocalDensity.current) { outlineWidth.toPx() } * 2f
    Box(modifier) {
        // Outline underneath (with the shadow), fill on top.
        Text(
            text = text,
            style = style.copy(
                color = outlineColor,
                drawStyle = Stroke(width = strokePx, join = StrokeJoin.Round),
                shadow = Shadow(color = outlineColor.copy(alpha = 0.6f), offset = Offset(0f, 4f), blurRadius = 4f),
            ),
            textAlign = textAlign,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = text,
            style = style.copy(color = color),
            textAlign = textAlign,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
