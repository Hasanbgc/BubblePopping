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

    /** The stage map; [introStageId] opens it with that stage's card showing. */
    @Serializable
    data class Stages(val introStageId: Int? = null): Routes()

    /** Endless mode: pick a difficulty card. */
    @Serializable
    object Endless: Routes()

    /** Daily reward, Daily Challenge, Daily Stage and missions. */
    @Serializable
    object Today: Routes()

    /** Sound, controls, accessibility, tutorial and progress reset. */
    @Serializable
    object Settings: Routes()

    /** Cosmetics shop. */
    @Serializable
    object Shop: Routes()
}
