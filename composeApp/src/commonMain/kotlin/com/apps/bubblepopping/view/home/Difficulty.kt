package com.apps.bubblepopping.view.home

import androidx.compose.ui.graphics.Color

enum class Difficulty(
    val label: String,
    val description: String,
    /** >1 = slower spawns (easier), <1 = faster spawns (harder). Baseline is MEDIUM = 1.0. */
    val spawnIntervalMultiplier: Float,
    val speedMultiplier: Float,
    val maxSpeedCapMultiplier: Float,
    val glowColor: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
) {
    EASY(
        label                   = "Easy",
        description             = "Relaxed speed",
        spawnIntervalMultiplier = 1.45f,
        speedMultiplier         = 0.65f,
        maxSpeedCapMultiplier   = 0.70f,
        glowColor               = Color(0xFF66BB6A),
        gradientStart           = Color(0xFF1B5E20),
        gradientEnd             = Color(0xFF2E7D32),
    ),
    MEDIUM(
        label                   = "Medium",
        description             = "Balanced challenge",
        spawnIntervalMultiplier = 1.00f,
        speedMultiplier         = 1.00f,
        maxSpeedCapMultiplier   = 1.00f,
        glowColor               = Color(0xFFFFA726),
        gradientStart           = Color(0xFFBF360C),
        gradientEnd             = Color(0xFFE64A19),
    ),
    HARD(
        label                   = "Hard",
        description             = "Fast & intense",
        spawnIntervalMultiplier = 0.55f,
        speedMultiplier         = 1.50f,
        maxSpeedCapMultiplier   = 1.40f,
        glowColor               = Color(0xFFEF5350),
        gradientStart           = Color(0xFF7F0000),
        gradientEnd             = Color(0xFFC62828),
    ),
}