package com.apps.bubblepopping

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType

actual class HapticFeedback {
    private val lightGenerator  = UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
    private val mediumGenerator = UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
    private val heavyGenerator  = UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)
    private val notificationGenerator = UINotificationFeedbackGenerator()

    actual fun light() = impact(lightGenerator)
    actual fun medium() = impact(mediumGenerator)
    actual fun heavy() = impact(heavyGenerator)

    actual fun success() {
        notificationGenerator.prepare()
        notificationGenerator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
    }

    private fun impact(generator: UIImpactFeedbackGenerator) {
        generator.prepare()          // pre-warms the taptic engine, reduces latency
        generator.impactOccurred()
    }
}
