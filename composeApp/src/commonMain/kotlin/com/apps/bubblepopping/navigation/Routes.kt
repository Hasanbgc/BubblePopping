package com.apps.bubblepopping.navigation

import androidx.navigation3.runtime.NavKey
import com.apps.bubblepopping.game.GameMode
import kotlinx.serialization.Serializable

@Serializable
sealed class Routes: NavKey{
    @Serializable
    object Home: Routes()

    @Serializable
    data class Play(val mode: GameMode): Routes()

    @Serializable
    object Ranking: Routes()

    @Serializable
    object Profile: Routes()

    /** The stage map. */
    @Serializable
    object Stages: Routes()

    /** Endless mode: pick a difficulty card. */
    @Serializable
    object Endless: Routes()

    /** The card shown before a stage starts: goal, bonus objectives, Play. */
    @Serializable
    data class StageIntro(val stageId: Int): Routes()
}
