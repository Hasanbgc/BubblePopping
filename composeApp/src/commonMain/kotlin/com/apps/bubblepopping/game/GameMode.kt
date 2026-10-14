package com.apps.bubblepopping.game

import com.apps.bubblepopping.stage.World
import com.apps.bubblepopping.view.home.Difficulty
import kotlinx.serialization.Serializable

/** What kind of round is being played. Part of the Play navigation route. */
@Serializable
sealed interface GameMode {
    @Serializable
    data class Endless(val difficulty: Difficulty) : GameMode

    @Serializable
    data class Stage(val stageId: Int) : GameMode

    /**
     * Today's seeded Endless run: the same bubbles for every player on [epochDay].
     * Played at Medium with the classic bubble mix, so everyone gets the same round.
     */
    @Serializable
    data class DailyChallenge(val epochDay: Long) : GameMode

    /** Today's generated stage, themed on [world] (the player's newest open world). */
    @Serializable
    data class DailyStage(val epochDay: Long, val world: World) : GameMode
}
