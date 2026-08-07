package com.apps.bubblepopping.view.play.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun BubbleSurface(
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    color: Color = Color.Transparent,
    borderColor: Color = Color.White.copy(alpha = 0.25f),
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.45f),
                        color.copy(alpha = 0.45f),
                        color.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(0.25f, 0.18f),
                    radius = 900f
                )
            )
            .border(
                width = 0.1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f),
                        borderColor.copy(alpha = 0.4f),
                        Color.White.copy(alpha = 0.15f)
                    )
                ),
                shape = shape
            )
            .drawWithCache {
                onDrawWithContent {
                    drawContent()

                    // Big soft shine
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.55f),
                                Color.White.copy(alpha = 0.18f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.28f, size.height * 0.22f),
                            radius = size.minDimension * 0.38f
                        ),
                        radius = size.minDimension * 0.38f,
                        center = Offset(size.width * 0.28f, size.height * 0.22f)
                    )

                    // Small highlight
                    drawCircle(
                        color = Color.White.copy(alpha = 0.8f),
                        radius = size.minDimension * 0.07f,
                        center = Offset(size.width * 0.28f, size.height * 0.18f)
                    )

                    // Bottom depth shadow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.12f)
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.9f),
                            radius = size.minDimension * 0.7f
                        ),
                        radius = size.minDimension * 0.7f,
                        center = Offset(size.width * 0.5f, size.height * 0.9f)
                    )
                }
            },
        contentAlignment = Alignment.Center,
        content = content
    )
}


@Composable
@Preview
fun BubbleSurfacePreview(){
    BubbleSurface(modifier = Modifier.size(30.dp).background(
        color = Color.Transparent,
        shape = CircleShape
    )) {
        /*Image(
            painter = painterResource(Res.drawable.ic_close),
            contentDescription = "",
            modifier = Modifier.size(30.dp)
        )*/
    }
}