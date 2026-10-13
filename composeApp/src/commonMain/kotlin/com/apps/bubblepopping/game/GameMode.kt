package com.apps.bubblepopping.game

import com.apps.bubblepopping.view.home.Difficulty
import kotlinx.serialization.Serializable

/** What kind of round is being played. Part of the Play navigation route. */
@Serializable
sealed interface GameMode {
    @Serializable
    data class Endless(val difficulty: Difficulty) : GameMode

    @Serializable
    data class Stage(val stageId: Int) : GameMode
}
