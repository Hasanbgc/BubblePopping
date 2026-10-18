package com.apps.bubblepopping.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/** An offscreen layer here would be redrawn into a fresh buffer every frame, so a plain layer is cheaper. */
actual fun Modifier.cachedLayer(): Modifier = graphicsLayer()
