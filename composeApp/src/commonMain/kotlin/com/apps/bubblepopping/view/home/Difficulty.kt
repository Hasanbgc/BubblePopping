package com.apps.bubblepopping.view.home

import androidx.compose.ui.graphics.Color
import com.apps.bubblepopping.game.SpawnConfig

enum class Difficulty(
    val label: String,
    val description: String,
    val spawn: SpawnConfig,
    val glowColor: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
) {
    EASY(
        label         = "Easy",
        description   = "Relaxed speed",
        spawn         = SpawnConfig.EASY,
        glowColor     = Color(0xFF66BB6A),
        gradientStart = Color(0xFF1B5E20),
        gradientEnd   = Color(0xFF2E7D32),
    ),
    MEDIUM(
        label         = "Medium",
        description   = "Balanced challenge",
        spawn         = SpawnConfig.MEDIUM,
        glowColor     = Color(0xFFFFA726),
        gradientStart = Color(0xFFBF360C),
        gradientEnd   = Color(0xFFE64A19),
    ),
    HARD(
        label         = "Hard",
        description   = "Fast & intense",
        spawn         = SpawnConfig.HARD,
        glowColor     = Color(0xFFEF5350),
        gradientStart = Color(0xFF7F0000),
        gradientEnd   = Color(0xFFC62828),
    ),
}