package com.apps.bubblepopping

import androidx.compose.ui.window.ComposeUIViewController
import com.apps.bubblepopping.navigation.NavGraph
import com.apps.bubblepopping.view.App

fun MainViewController() = ComposeUIViewController {
    //App(HapticFeedback())
    NavGraph(hapticFeedback = HapticFeedback())
}