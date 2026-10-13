package com.apps.bubblepopping.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface StageProgressDao {
    @Query("SELECT * FROM stage_progress")
    fun observeAll(): Flow<List<StageProgressEntity>>

    @Query("SELECT * FROM stage_progress WHERE stageId = :stageId")
    suspend fun get(stageId: Int): StageProgressEntity?

    @Upsert
    suspend fun upsert(progress: StageProgressEntity)
}

@Dao
interface EndlessScoreDao {
    @Query("SELECT MAX(score) FROM endless_scores WHERE difficulty = :difficulty")
    fun observeBestScore(difficulty: String): Flow<Int?>

    @Insert
    suspend fun insert(score: EndlessScoreEntity)
}

@Dao
interface StatsDao {
    @Query("SELECT * FROM stats WHERE id = ${StatsEntity.ROW_ID}")
    fun observe(): Flow<StatsEntity?>

    @Query("SELECT * FROM stats WHERE id = ${StatsEntity.ROW_ID}")
    suspend fun get(): StatsEntity?

    @Upsert
    suspend fun upsert(stats: StatsEntity)
}
