package com.apps.bubblepopping.view

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.apps.bubblepopping.HapticFeedback
import com.apps.bubblepopping.view.home.Difficulty
import com.apps.bubblepopping.view.home.DifficultyScreen
import com.apps.bubblepopping.view.play.BubblePoppingScreen

@Composable
fun App(hapticFeedback: HapticFeedback) {
    MaterialTheme {
        var selectedDifficulty by remember { mutableStateOf<Difficulty?>(null) }

        AnimatedContent(
            targetState  = selectedDifficulty,
            transitionSpec = {
                if (targetState != null) {
                    // DifficultyScreen → GameScreen: slide up
                    (slideInVertically(tween(380)) { it } + fadeIn(tween(300))) togetherWith
                    (slideOutVertically(tween(280)) { -it } + fadeOut(tween(200)))
                } else {
                    // GameScreen → DifficultyScreen (future back nav)
                    (slideInVertically(tween(380)) { -it } + fadeIn(tween(300))) togetherWith
                    (slideOutVertically(tween(280)) { it } + fadeOut(tween(200)))
                }
            },
            label = "screenTransition",
        ) { difficulty ->
            if (difficulty == null) {
                DifficultyScreen(onDifficultySelected = { selectedDifficulty = it })
            } else {
                BubblePoppingScreen(
                    difficulty = difficulty,
                    hapticFeedback = hapticFeedback,
                    onBack = { selectedDifficulty = null },
                )
            }
        }
    }
}
