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

