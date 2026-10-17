package com.apps.bubblepopping

import com.apps.bubblepopping.data.EndlessRun
import com.apps.bubblepopping.data.MetaRepository
import com.apps.bubblepopping.data.PlayerStats
import com.apps.bubblepopping.data.ProgressRepository
import com.apps.bubblepopping.data.StageProgress
import com.apps.bubblepopping.data.prefs.GameSettings
import com.apps.bubblepopping.data.prefs.SettingsRepository
import com.apps.bubblepopping.data.prefs.cleanPlayerName
import com.apps.bubblepopping.meta.Achievement
import com.apps.bubblepopping.meta.Cosmetic
import com.apps.bubblepopping.meta.CosmeticKind
import com.apps.bubblepopping.meta.MetaRules
import com.apps.bubblepopping.meta.MetaState
import com.apps.bubblepopping.meta.Mission
import com.apps.bubblepopping.meta.PlayerState
import com.apps.bubblepopping.meta.ProgressSnapshot
import com.apps.bubblepopping.meta.RunRewards
import com.apps.bubblepopping.meta.RunSummary
import com.apps.bubblepopping.view.home.Difficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.random.Random

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
    var dailyGames = 0

    override fun bestEndlessScore(difficulty: Difficulty): Flow<Int?> = bestScore
    override fun stageProgress(): Flow<Map<Int, StageProgress>> = stages
    override fun stats(): Flow<PlayerStats> = flowOf(PlayerStats())
    override fun topEndlessRuns(limit: Int, difficulty: Difficulty?): Flow<List<EndlessRun>> = flowOf(emptyList())

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

    override suspend fun recordDailyGame(bubblesPopped: Int, bestCombo: Int) {
        dailyGames++
    }

    override suspend fun resetProgress() {
        bestScore.value = null
        stages.value = emptyMap()
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

    override suspend fun setPlayerName(name: String) =
        settings.update { it.copy(playerName = cleanPlayerName(name)) }

    override suspend fun setTutorialSeen(seen: Boolean) =
        settings.update { it.copy(tutorialSeen = seen) }

    override suspend fun setLargeHitArea(enabled: Boolean) =
        settings.update { it.copy(largeHitArea = enabled) }

    override suspend fun setHighContrastSkulls(enabled: Boolean) =
        settings.update { it.copy(highContrastSkulls = enabled) }

    override suspend fun resetProgressSettings() = settings.update {
        val defaults = GameSettings()
        it.copy(
            equippedSkin = defaults.equippedSkin,
            equippedPopEffect = defaults.equippedPopEffect,
            equippedBackground = defaults.equippedBackground,
            tutorialSeen = false,
        )
    }

    override suspend fun equip(cosmetic: Cosmetic) = settings.update {
        when (cosmetic.kind) {
            CosmeticKind.SKIN -> it.copy(equippedSkin = cosmetic.id)
            CosmeticKind.POP_EFFECT -> it.copy(equippedPopEffect = cosmetic.id)
            CosmeticKind.BACKGROUND -> it.copy(equippedBackground = cosmetic.id)
        }
    }
}

/** Runs the real [MetaRules] on in-memory state. */
class FakeMetaRepository(
    private val progress: FakeProgressRepository? = null,
    private val random: Random = Random(1),
) : MetaRepository {
    val state = MutableStateFlow(MetaState())
    val finishedRuns = mutableListOf<RunSummary>()

    private fun snapshot() = ProgressSnapshot(stageProgress = progress?.stages?.value.orEmpty())

    override fun player(): Flow<PlayerState> = state.map { it.player }
    override fun missions(): Flow<List<Mission>> = state.map { it.missions }
    override fun purchasedCosmetics(): Flow<Set<String>> = state.map { it.purchased }
    override fun achievements(): Flow<Set<Achievement>> = state.map { it.achievements }

    override suspend fun ensureMissions() {
        state.value = MetaRules.withMissions(state.value, snapshot(), random)
    }

    override suspend fun claimDailyLogin(today: Long): Int? {
        val (next, coins) = MetaRules.claimLogin(state.value, today) ?: return null
        state.value = next
        return coins
    }

    override suspend fun buy(cosmetic: Cosmetic): Boolean {
        val next = MetaRules.buy(state.value, cosmetic, snapshot().stageProgress) ?: return false
        state.value = next
        return true
    }

    override suspend fun spendCoins(amount: Int): Boolean {
        val next = MetaRules.spendCoins(state.value, amount) ?: return false
        state.value = next
        return true
    }

    override suspend fun resetProgress() {
        state.value = MetaState()
    }

    override suspend fun onRunFinished(run: RunSummary): RunRewards {
        finishedRuns += run
        val (next, rewards) = MetaRules.applyRun(state.value, run, snapshot(), random)
        state.value = next
        return rewards
    }
}
