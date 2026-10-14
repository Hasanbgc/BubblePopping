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

/** Coins, daily state and reward counters — a single row with [id] = [PlayerEntity.ROW_ID]. */
@Entity(tableName = "player")
data class PlayerEntity(
    @PrimaryKey val id: Int = ROW_ID,
    val coins: Int = 0,
    /** Daily login rewards claimed so far (the 7-day cycle continues from here). */
    val loginDaysClaimed: Int = 0,
    /** Epoch day of the last claimed login reward; -1 = never. */
    val lastLoginDay: Long = -1,
    /** Star chests opened so far; one is due every 10 total stars. */
    val chestsOpened: Int = 0,
    val missionsCompleted: Int = 0,
    /** Epoch day [dailyChallengeBest] belongs to; -1 = never played. */
    val dailyChallengeDay: Long = -1,
    val dailyChallengeBest: Int = 0,
    /** Epoch day the Daily Stage chest was last won; -1 = never. */
    val dailyStageClearedDay: Long = -1,
) {
    companion object {
        const val ROW_ID = 0
    }
}

/** One of the three active missions. */
@Entity(tableName = "missions")
data class MissionEntity(
    @PrimaryKey val slot: Int,
    /** [com.apps.bubblepopping.meta.MissionKind] name. */
    val kind: String,
    val target: Int,
    val reward: Int,
    val progress: Int,
    /** [com.apps.bubblepopping.view.play.component.BubbleType] name, for POP_TYPE. */
    val bubbleType: String?,
    /** [com.apps.bubblepopping.view.home.Difficulty] name, for ENDLESS_SURVIVE. */
    val difficulty: String?,
)

/** A cosmetic bought in the shop or won from a chest. Free and boss items aren't stored. */
@Entity(tableName = "owned_cosmetics")
data class OwnedCosmeticEntity(
    @PrimaryKey val id: String,
    val acquiredAt: Long,
)

/** A reached achievement (its coins have been paid). */
@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val unlockedAt: Long,
)
