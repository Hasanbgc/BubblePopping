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

/** One finished Endless round, for the Local leaderboard. */
data class EndlessRun(
    val difficulty: Difficulty,
    val score: Int,
    val playedAt: Long,
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

    /** The best Endless rounds on this device, highest score first. */
    fun topEndlessRuns(limit: Int): Flow<List<EndlessRun>>

    /** Saves a finished Endless round and updates lifetime stats. */
    suspend fun recordEndlessGame(difficulty: Difficulty, score: Int, bubblesPopped: Int, bestCombo: Int)

    /**
     * Saves a finished stage attempt and updates lifetime stats. A cleared
     * stage ([stars] > 0) keeps the best stars and score seen so far; a failed
     * one only counts towards stats.
     */
    suspend fun recordStageGame(stageId: Int, stars: Int, score: Int, bubblesPopped: Int, bestCombo: Int)

    /** Daily Challenge / Daily Stage rounds only count towards lifetime stats here. */
    suspend fun recordDailyGame(bubblesPopped: Int, bestCombo: Int)

    /** Deletes all stage progress, Endless scores and stats. */
    suspend fun resetProgress()
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

    override fun topEndlessRuns(limit: Int): Flow<List<EndlessRun>> =
        endlessDao.observeTopScores(limit).map { rows ->
            rows.mapNotNull { row ->
                Difficulty.entries.firstOrNull { it.name == row.difficulty }
                    ?.let { EndlessRun(it, row.score, row.playedAt) }
            }
        }

    override suspend fun recordDailyGame(bubblesPopped: Int, bestCombo: Int) {
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction { addToStats(bubblesPopped, bestCombo) }
        }
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
                addToStats(bubblesPopped, bestCombo)
            }
        }
    }

    override suspend fun recordStageGame(
        stageId: Int,
        stars: Int,
        score: Int,
        bubblesPopped: Int,
        bestCombo: Int,
    ) {
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                if (stars > 0) {
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
                addToStats(bubblesPopped, bestCombo)
            }
        }
    }

    override suspend fun resetProgress() {
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                stageDao.deleteAll()
                endlessDao.deleteAll()
                statsDao.deleteAll()
            }
        }
    }

    /** Must be called inside a write transaction. */
    private suspend fun addToStats(bubblesPopped: Int, bestCombo: Int) {
        val stats = statsDao.get() ?: StatsEntity()
        statsDao.upsert(
            stats.copy(
                gamesPlayed = stats.gamesPlayed + 1,
                bubblesPopped = stats.bubblesPopped + bubblesPopped,
                bestCombo = maxOf(stats.bestCombo, bestCombo),
            )
        )
    }

    @OptIn(ExperimentalTime::class)
    private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
}
