package com.apps.bubblepopping

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController {
    App(HapticFeedback())
}