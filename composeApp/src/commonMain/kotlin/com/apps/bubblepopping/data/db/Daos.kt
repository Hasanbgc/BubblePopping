package com.apps.bubblepopping.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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

    @Query("DELETE FROM stage_progress")
    suspend fun deleteAll()
}

@Dao
interface EndlessScoreDao {
    @Query("SELECT MAX(score) FROM endless_scores WHERE difficulty = :difficulty")
    fun observeBestScore(difficulty: String): Flow<Int?>

    @Query("SELECT * FROM endless_scores ORDER BY score DESC, playedAt ASC LIMIT :limit")
    fun observeTopScores(limit: Int): Flow<List<EndlessScoreEntity>>

    @Query("SELECT * FROM endless_scores WHERE difficulty = :difficulty ORDER BY score DESC, playedAt ASC LIMIT :limit")
    fun observeTopScoresFor(difficulty: String, limit: Int): Flow<List<EndlessScoreEntity>>

    @Insert
    suspend fun insert(score: EndlessScoreEntity)

    @Query("DELETE FROM endless_scores")
    suspend fun deleteAll()
}

@Dao
interface StatsDao {
    @Query("SELECT * FROM stats WHERE id = ${StatsEntity.ROW_ID}")
    fun observe(): Flow<StatsEntity?>

    @Query("SELECT * FROM stats WHERE id = ${StatsEntity.ROW_ID}")
    suspend fun get(): StatsEntity?

    @Upsert
    suspend fun upsert(stats: StatsEntity)

    @Query("DELETE FROM stats")
    suspend fun deleteAll()
}

@Dao
interface MetaDao {
    @Query("SELECT * FROM player WHERE id = ${PlayerEntity.ROW_ID}")
    fun observePlayer(): Flow<PlayerEntity?>

    @Query("SELECT * FROM player WHERE id = ${PlayerEntity.ROW_ID}")
    suspend fun getPlayer(): PlayerEntity?

    @Upsert
    suspend fun upsertPlayer(player: PlayerEntity)

    @Query("SELECT * FROM missions ORDER BY slot")
    fun observeMissions(): Flow<List<MissionEntity>>

    @Query("SELECT * FROM missions ORDER BY slot")
    suspend fun getMissions(): List<MissionEntity>

    @Upsert
    suspend fun upsertMissions(missions: List<MissionEntity>)

    @Query("SELECT id FROM owned_cosmetics")
    fun observeOwnedCosmetics(): Flow<List<String>>

    @Query("SELECT id FROM owned_cosmetics")
    suspend fun getOwnedCosmetics(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOwnedCosmetic(cosmetic: OwnedCosmeticEntity)

    @Query("SELECT id FROM achievements")
    fun observeAchievements(): Flow<List<String>>

    @Query("SELECT id FROM achievements")
    suspend fun getAchievements(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievement(achievement: AchievementEntity)

    @Query("DELETE FROM player")
    suspend fun deletePlayer()

    @Query("DELETE FROM missions")
    suspend fun deleteMissions()

    @Query("DELETE FROM owned_cosmetics")
    suspend fun deleteOwnedCosmetics()

    @Query("DELETE FROM achievements")
    suspend fun deleteAchievements()
}
