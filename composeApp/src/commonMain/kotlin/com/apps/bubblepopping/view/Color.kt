package com.apps.bubblepopping.view

import androidx.compose.ui.graphics.Color

/** Tints a new bubble can be given; one is picked at random per bubble. */
val BubbleColors = listOf(
    Color(0xFF81D4FA),
    Color(0xFFB2EBF2),
    Color(0xFF80CBC4),
    Color(0xFFA5D6A7),
    Color(0xFFCE93D8),
    Color(0xFFF48FB1),
    Color(0xFFFFCC80),
    Color(0xFFE0F7FA),
)

// ── Gameplay feedback ───────────────────────────────────────────────────────

/** Floating "+5" text for normal bubbles. */
val ScorePopupColor = Color.White
/** Floating "+5" text for heart bubbles. */
val HeartPopupColor = Color(0xFFFF80AB)
/** Combo milestone text ("NICE!", "GREAT!"…) and the combo meter. */
val ComboColor = Color(0xFFFFD54F)
/** Pulsing ring on bubbles about to escape. */
val NearMissColor = Color(0xFFFF1744)
