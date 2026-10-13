package com.apps.bubblepopping

import com.apps.bubblepopping.data.PlayerStats
import com.apps.bubblepopping.data.ProgressRepository
import com.apps.bubblepopping.data.StageProgress
import com.apps.bubblepopping.data.prefs.GameSettings
import com.apps.bubblepopping.data.prefs.SettingsRepository
import com.apps.bubblepopping.view.home.Difficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update

data class RecordedGame(
    val difficulty: Difficulty,
    val score: Int,
    val bubblesPopped: Int,
    val bestCombo: Int,
)

data class RecordedStage(val stageId: Int, val stars: Int, val score: Int)

class FakeProgressRepository(best: Int? = null) : ProgressRepository {
    val bestScore = MutableStateFlow(best)
    val stages = MutableStateFlow<Map<Int, StageProgress>>(emptyMap())
    val recordedGames = mutableListOf<RecordedGame>()
    val recordedStages = mutableListOf<RecordedStage>()

    override fun bestEndlessScore(difficulty: Difficulty): Flow<Int?> = bestScore
    override fun stageProgress(): Flow<Map<Int, StageProgress>> = stages
    override fun stats(): Flow<PlayerStats> = flowOf(PlayerStats())

    override suspend fun recordEndlessGame(
        difficulty: Difficulty,
        score: Int,
        bubblesPopped: Int,
        bestCombo: Int,
    ) {
        recordedGames += RecordedGame(difficulty, score, bubblesPopped, bestCombo)
    }

    override suspend fun recordStageGame(
        stageId: Int,
        stars: Int,
        score: Int,
        bubblesPopped: Int,
        bestCombo: Int,
    ) {
        recordedStages += RecordedStage(stageId, stars, score)
        if (stars > 0) {
            val previous = stages.value[stageId]
            stages.value = stages.value + (stageId to StageProgress(
                stageId = stageId,
                stars = maxOf(stars, previous?.stars ?: 0),
                bestScore = maxOf(score, previous?.bestScore ?: 0),
            ))
        }
    }
}

class FakeSettingsRepository : SettingsRepository {
    override val settings = MutableStateFlow(GameSettings())

    override suspend fun setSoundEnabled(enabled: Boolean) =
        settings.update { it.copy(soundEnabled = enabled) }

    override suspend fun setMusicEnabled(enabled: Boolean) =
        settings.update { it.copy(musicEnabled = enabled) }

    override suspend fun setHapticsEnabled(enabled: Boolean) =
        settings.update { it.copy(hapticsEnabled = enabled) }

    override suspend fun setWindButtonsEnabled(enabled: Boolean) =
        settings.update { it.copy(windButtonsEnabled = enabled) }
}
