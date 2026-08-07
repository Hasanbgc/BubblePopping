package com.apps.bubblepopping

import androidx.compose.ui.window.ComposeUIViewController
import com.apps.bubblepopping.navigation.NavGraph

fun MainViewController() = ComposeUIViewController {
    NavGraph(hapticFeedback = HapticFeedback())
}