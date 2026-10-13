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

class FakeProgressRepository(best: Int? = null) : ProgressRepository {
    val bestScore = MutableStateFlow(best)
    val recordedGames = mutableListOf<RecordedGame>()

    override fun bestEndlessScore(difficulty: Difficulty): Flow<Int?> = bestScore
    override fun stageProgress(): Flow<Map<Int, StageProgress>> = flowOf(emptyMap())
    override fun stats(): Flow<PlayerStats> = flowOf(PlayerStats())

    override suspend fun recordEndlessGame(
        difficulty: Difficulty,
        score: Int,
        bubblesPopped: Int,
        bestCombo: Int,
    ) {
        recordedGames += RecordedGame(difficulty, score, bubblesPopped, bestCombo)
    }

    override suspend fun recordStageResult(stageId: Int, stars: Int, score: Int) = Unit
}

class FakeSettingsRepository : SettingsRepository {
    override val settings = MutableStateFlow(GameSettings())

    override suspend fun setSoundEnabled(enabled: Boolean) =
        settings.update { it.copy(soundEnabled = enabled) }

    override suspend fun setMusicEnabled(enabled: Boolean) =
        settings.update { it.copy(musicEnabled = enabled) }

    override suspend fun setHapticsEnabled(enabled: Boolean) =
        settings.update { it.copy(hapticsEnabled = enabled) }
}
