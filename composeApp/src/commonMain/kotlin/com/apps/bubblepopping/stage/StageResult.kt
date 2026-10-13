package com.apps.bubblepopping.stage

/** How a finished stage went. [stars] is 0 when [won] is false. */
data class StageResult(
    val won: Boolean,
    val stars: Int,
    /** Final state of every objective, main goal first. */
    val objectives: List<ObjectiveProgress>,
)
