package com.apps.bubblepopping.navigation

import androidx.navigation3.runtime.NavKey
import com.apps.bubblepopping.view.home.Difficulty
import kotlinx.serialization.Serializable

@Serializable
sealed class Routes: NavKey{
    @Serializable
    object Home: Routes()

    @Serializable
    data class Play(val difficulty: Difficulty): Routes()

    @Serializable
    object Ranking: Routes()

    @Serializable
    object Settings: Routes()
}