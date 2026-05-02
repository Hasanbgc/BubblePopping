package com.apps.bubblepopping

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle

actual class HapticFeedback {
    // UIImpactFeedbackStyleLight = short, crisp — perfect for a bubble pop
    private val generator = UIImpactFeedbackGenerator(
        style = UIImpactFeedbackStyle.UIImpactFeedbackStyleLight
    )

    actual fun popVibration() {
        generator.prepare()          // pre-warms the taptic engine, reduces latency
        generator.impactOccurred()
    }
}