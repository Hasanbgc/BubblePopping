package com.apps.bubblepopping.data

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.apps.bubblepopping.data.db.AppDatabase
import com.apps.bubblepopping.data.db.EndlessScoreEntity
import com.apps.bubblepopping.data.db.StageProgressEntity
import com.apps.bubblepopping.data.db.StatsEntity
import com.apps.bubblepopping.view.home.Difficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class StageProgress(
    val stageId: Int,
    val stars: Int,
    val bestScore: Int,
)

data class PlayerStats(
    val gamesPlayed: Int = 0,
    val bubblesPopped: Long = 0,
    val bestCombo: Int = 0,
)

/**
 * Local player progress. UI and game code depend only on this interface, so a
 * cloud-synced implementation (Firebase / Supabase) can be added later.
 */
interface ProgressRepository {
    fun bestEndlessScore(difficulty: Difficulty): Flow<Int?>
    fun stageProgress(): Flow<Map<Int, StageProgress>>
    fun stats(): Flow<PlayerStats>

    /** Saves a finished Endless round and updates lifetime stats. */
    suspend fun recordEndlessGame(difficulty: Difficulty, score: Int, bubblesPopped: Int, bestCombo: Int)

    /** Saves a stage result, keeping the best stars and score seen so far. */
    suspend fun recordStageResult(stageId: Int, stars: Int, score: Int)
}

class RoomProgressRepository(
    private val database: AppDatabase,
) : ProgressRepository {

    private val stageDao = database.stageProgressDao()
    private val endlessDao = database.endlessScoreDao()
    private val statsDao = database.statsDao()

    override fun bestEndlessScore(difficulty: Difficulty): Flow<Int?> =
        endlessDao.observeBestScore(difficulty.name)

    override fun stageProgress(): Flow<Map<Int, StageProgress>> =
        stageDao.observeAll().map { rows ->
            rows.associate { it.stageId to StageProgress(it.stageId, it.stars, it.bestScore) }
        }

    override fun stats(): Flow<PlayerStats> =
        statsDao.observe().map { row ->
            row?.let { PlayerStats(it.gamesPlayed, it.bubblesPopped, it.bestCombo) } ?: PlayerStats()
        }

    override suspend fun recordEndlessGame(
        difficulty: Difficulty,
        score: Int,
        bubblesPopped: Int,
        bestCombo: Int,
    ) {
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                endlessDao.insert(
                    EndlessScoreEntity(
                        difficulty = difficulty.name,
                        score = score,
                        bubblesPopped = bubblesPopped,
                        playedAt = nowMillis(),
                    )
                )
                val stats = statsDao.get() ?: StatsEntity()
                statsDao.upsert(
                    stats.copy(
                        gamesPlayed = stats.gamesPlayed + 1,
                        bubblesPopped = stats.bubblesPopped + bubblesPopped,
                        bestCombo = maxOf(stats.bestCombo, bestCombo),
                    )
                )
            }
        }
    }

    override suspend fun recordStageResult(stageId: Int, stars: Int, score: Int) {
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                val previous = stageDao.get(stageId)
                stageDao.upsert(
                    StageProgressEntity(
                        stageId = stageId,
                        stars = maxOf(stars, previous?.stars ?: 0),
                        bestScore = maxOf(score, previous?.bestScore ?: 0),
                        completedAt = nowMillis(),
                    )
                )
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
}
