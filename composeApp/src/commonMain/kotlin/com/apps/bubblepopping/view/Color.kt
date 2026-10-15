package com.apps.bubblepopping.view

import androidx.compose.ui.graphics.Color
import com.apps.bubblepopping.stage.World

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
/** "SHIELD!" text and the HUD shield badge. */
val ShieldColor = Color(0xFFCFD8DC)
/** Screen tint and HUD badge while bubbles are frozen. */
val FreezeColor = Color(0xFF80D8FF)

// ── Bosses ──────────────────────────────────────────────────────────────────

/** "STUNG!" text and the screen tint while taps are locked. */
val StingColor = Color(0xFFE040FB)
/** Boss HP ring: full → empty. */
val BossHpFullColor = Color(0xFF66BB6A)
val BossHpLowColor = Color(0xFFEF5350)
val BossCrownColor = Color(0xFFFFC107)
val ConfettiColors = listOf(
    Color(0xFFFFD54F), Color(0xFFFF80AB), Color(0xFF80D8FF), Color(0xFF69F0AE),
    Color(0xFFEA80FC), Color(0xFFFFAB40), Color(0xFFFFFFFF),
)

// ── Stage map ───────────────────────────────────────────────────────────────

/** Map background per world, top → bottom of that world's section. */
fun worldMapColors(world: World): List<Color> = when (world) {
    World.SKY -> listOf(Color(0xFF0D47A1), Color(0xFF1976D2), Color(0xFF4FC3F7))
    World.OCEAN -> listOf(Color(0xFF002F4B), Color(0xFF00597A), Color(0xFF0097A7))
    World.SPACE -> listOf(Color(0xFF0B0221), Color(0xFF26114D), Color(0xFF4A2380))
    World.CANDY -> listOf(Color(0xFF880E4F), Color(0xFFC2185B), Color(0xFFF48FB1))
}
/** The road between stage nodes. */
val MapRoadColor = Color.White.copy(alpha = 0.16f)
/** Dashes on the road; gold once the player has passed that stretch. */
val MapRoadDashColor = Color.White.copy(alpha = 0.45f)
val MapRoadDoneColor = Color(0xFFFFD54F)

// ── Cosmetics ───────────────────────────────────────────────────────────────

/**
 * Play background for an equipped background cosmetic, top → bottom (its
 * scenery is in PlayBackdrop). Null keeps the world's own colours.
 */
fun playBackgroundColors(backgroundId: String): List<Color>? = when (backgroundId) {
    "bg_sunset" -> listOf(Color(0xFF4A148C), Color(0xFFD84315), Color(0xFFFFB74D))
    "bg_midnight" -> listOf(Color(0xFF000814), Color(0xFF001D3D), Color(0xFF003566))
    "bg_aurora" -> listOf(Color(0xFF0B132B), Color(0xFF1B998B), Color(0xFF7B2CBF))
    else -> null
}
