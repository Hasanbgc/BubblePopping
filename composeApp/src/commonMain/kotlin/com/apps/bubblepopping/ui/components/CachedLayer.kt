package com.apps.bubblepopping.ui.components

import androidx.compose.ui.Modifier

/**
 * Its own graphics layer, kept as a ready-drawn image where the platform does that
 * (on Android, a hardware layer), so a large still drawing — a stage map slice — is
 * only re-rendered when it changes, not on every frame something else animates.
 * Use it on content that rarely changes: every change re-renders the whole layer.
 */
expect fun Modifier.cachedLayer(): Modifier
