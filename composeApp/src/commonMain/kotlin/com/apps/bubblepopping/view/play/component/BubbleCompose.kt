package com.apps.bubblepopping.view.play.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

@Composable
fun SoapBubble(
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    tint: Color = Color.Transparent,
    tintAlpha: Float = 0.12f,
    borderAlpha: Float = 0.85f,
    reflectionAlpha: Float = 0.75f,
    content: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = this.size.width
            val h = this.size.height
            val radius = minOf(w, h) / 2f
            val center = Offset(w / 2f, h / 2f)

            val rect = Rect(
                center.x - radius,
                center.y - radius,
                center.x + radius,
                center.y + radius
            )

            // Very transparent soap skin
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.05f),
                        tint.copy(alpha = tintAlpha),
                        Color(0xFFBFEAFF).copy(alpha = 0.06f),
                        Color(0xFFFFC8F6).copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    center = Offset(center.x - radius * 0.25f, center.y - radius * 0.25f),
                    radius = radius * 1.05f
                ),
                center = center,
                radius = radius * 0.92f
            )

            // Iridescent thin outer border
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.White.copy(alpha = borderAlpha),
                        Color(0xFF9CEBFF).copy(alpha = borderAlpha),
                        Color(0xFFFFB7F6).copy(alpha = borderAlpha * 0.8f),
                        Color(0xFFFFF6B0).copy(alpha = borderAlpha * 0.55f),
                        Color(0xFFB7A5FF).copy(alpha = borderAlpha * 0.75f),
                        Color.Transparent.copy(alpha = borderAlpha)
                    ),
                    center = center
                ),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(rect.left, rect.top),
                size = Size(rect.width, rect.height),
                style = Stroke(
                    width = radius * 0.035f,
                    cap = StrokeCap.Round
                )
            )

            // Extra thin white skin line
            drawCircle(
                color = Color.White.copy(alpha = 0.32f),
                center = center,
                radius = radius * 0.965f,
                style = Stroke(width = radius * 0.01f)
            )

            // Large curved reflection, like real soap bubble
            drawArc(
                color = Color.White.copy(alpha = reflectionAlpha),
                startAngle = 215f,
                sweepAngle = 72f,
                useCenter = false,
                topLeft = Offset(
                    center.x - radius * 0.74f,
                    center.y - radius * 0.76f
                ),
                size = Size(radius * 1.05f, radius * 0.72f),
                style = Stroke(
                    width = radius * 0.065f,
                    cap = StrokeCap.Round
                )
            )

            // Smaller vertical reflection on left
            drawArc(
                color = Color.White.copy(alpha = reflectionAlpha * 0.48f),
                startAngle = 165f,
                sweepAngle = 45f,
                useCenter = false,
                topLeft = Offset(
                    center.x - radius * 0.79f,
                    center.y - radius * 0.34f
                ),
                size = Size(radius * 0.38f, radius * 0.55f),
                style = Stroke(
                    width = radius * 0.045f,
                    cap = StrokeCap.Round
                )
            )

            // Bottom-left curved reflection
            drawArc(
                color = Color.White.copy(alpha = 0.22f),
                startAngle = 105f,
                sweepAngle = 92f,
                useCenter = false,
                topLeft = Offset(
                    center.x - radius * 0.88f,
                    center.y - radius * 0.84f
                ),
                size = Size(radius * 1.7f, radius * 1.7f),
                style = Stroke(
                    width = radius * 0.035f,
                    cap = StrokeCap.Round
                )
            )

            // Right-side small shine
            drawArc(
                color = Color.White.copy(alpha = 0.24f),
                startAngle = -28f,
                sweepAngle = 45f,
                useCenter = false,
                topLeft = Offset(
                    center.x - radius * 0.83f,
                    center.y - radius * 0.83f
                ),
                size = Size(radius * 1.66f, radius * 1.66f),
                style = Stroke(
                    width = radius * 0.025f,
                    cap = StrokeCap.Round
                )
            )

            // Soft blue/pink edge reflection
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFFFA8F5).copy(alpha = 0.32f),
                        Color(0xFF93EDFF).copy(alpha = 0.28f),
                        Color.Transparent
                    ),
                    center = center
                ),
                startAngle = 45f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(
                    center.x - radius * 0.91f,
                    center.y - radius * 0.91f
                ),
                size = Size(radius * 1.82f, radius * 1.82f),
                style = Stroke(
                    width = radius * 0.022f,
                    cap = StrokeCap.Round
                )
            )

            // Tiny star-like sparkles
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                center = Offset(center.x - radius * 0.71f, center.y - radius * 0.46f),
                radius = radius * 0.018f
            )

            drawCircle(
                color = Color.White.copy(alpha = 0.45f),
                center = Offset(center.x + radius * 0.5f, center.y + radius * 0.36f),
                radius = radius * 0.012f
            )
        }

        content?.invoke()
    }
}

@Preview
@Composable
fun SoapBubblePreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            //SoapBubble()
            SoapBubble(
                size = 172.dp,
                tint = Color.Cyan,
                borderAlpha = 0.9f,
                reflectionAlpha = 0.8f
            )
        }
    }
}
