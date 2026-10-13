package com.apps.bubblepopping

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.apps.bubblepopping.data.iosAppContainer
import com.apps.bubblepopping.navigation.NavGraph

fun MainViewController() = ComposeUIViewController {
    NavGraph(
        container = iosAppContainer,
        hapticFeedback = remember { HapticFeedback() },
    )
}
