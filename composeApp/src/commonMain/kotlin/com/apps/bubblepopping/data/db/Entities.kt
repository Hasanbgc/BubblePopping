package com.apps.bubblepopping.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Best result for one stage. Only ever improves. */
@Entity(tableName = "stage_progress")
data class StageProgressEntity(
    @PrimaryKey val stageId: Int,
    val stars: Int,
    val bestScore: Int,
    val completedAt: Long,
)

/** One finished Endless round. Feeds best scores and the Local leaderboard. */
@Entity(tableName = "endless_scores")
data class EndlessScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** [com.apps.bubblepopping.view.home.Difficulty] name. */
    val difficulty: String,
    val score: Int,
    val bubblesPopped: Int,
    val playedAt: Long,
)

/** Lifetime stats — a single row with [id] = [StatsEntity.ROW_ID]. */
@Entity(tableName = "stats")
data class StatsEntity(
    @PrimaryKey val id: Int = ROW_ID,
    val gamesPlayed: Int = 0,
    val bubblesPopped: Long = 0,
    val bestCombo: Int = 0,
) {
    companion object {
        const val ROW_ID = 0
    }
}
