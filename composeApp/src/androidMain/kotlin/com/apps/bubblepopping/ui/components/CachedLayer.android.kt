package com.apps.bubblepopping.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

/** An offscreen layer is a hardware layer: rendered once into a texture and reused until invalidated. */
actual fun Modifier.cachedLayer(): Modifier = graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
