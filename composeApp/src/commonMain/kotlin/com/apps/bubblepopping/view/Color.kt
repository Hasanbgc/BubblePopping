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

// ── Stages ──────────────────────────────────────────────────────────────────

/** Dark-blue backdrop shared by the menu screens (stages, profile…). */
val ScreenBackgroundColors = listOf(Color(0xFF050E1A), Color(0xFF0A1E35), Color(0xFF144B6E))
/** Earned star. Unearned stars use it at low alpha. */
val StarColor = Color(0xFFFFD54F)
/** Stage node / card surface. */
val StageCardColor = Color(0xFF12365A)
/** Accent for buttons and unlocked stage outlines. */
val StageAccentColor = Color(0xFF29B6F6)
/** Objective met. */
val ObjectiveMetColor = Color(0xFF66BB6A)
/** Objective failed. */
val ObjectiveFailedColor = Color(0xFFEF5350)
/** Home screen Endless button. */
val EndlessButtonColor = Color(0xFFFF8F00)

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
/** Soft cloud puffs drawn over the map background. */
val MapCloudColor = Color.White.copy(alpha = 0.07f)
val LockedNodeColor = Color(0xFF546E7A)
