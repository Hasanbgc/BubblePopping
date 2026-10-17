package com.apps.bubblepopping.data

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.apps.bubblepopping.data.db.AchievementEntity
import com.apps.bubblepopping.data.db.AppDatabase
import com.apps.bubblepopping.data.db.MissionEntity
import com.apps.bubblepopping.data.db.OwnedCosmeticEntity
import com.apps.bubblepopping.data.db.PlayerEntity
import com.apps.bubblepopping.meta.Achievement
import com.apps.bubblepopping.meta.Cosmetic
import com.apps.bubblepopping.meta.MetaRules
import com.apps.bubblepopping.meta.MetaState
import com.apps.bubblepopping.meta.Mission
import com.apps.bubblepopping.meta.MissionKind
import com.apps.bubblepopping.meta.PlayerState
import com.apps.bubblepopping.meta.ProgressSnapshot
import com.apps.bubblepopping.meta.RunRewards
import com.apps.bubblepopping.meta.RunSummary
import com.apps.bubblepopping.view.home.Difficulty
import com.apps.bubblepopping.view.play.component.BubbleType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Coins, missions, daily rewards, chests, cosmetics and achievements (Phase 8).
 * Rules live in [MetaRules]; this only loads and saves around them.
 */
interface MetaRepository {
    fun player(): Flow<PlayerState>
    fun missions(): Flow<List<Mission>>
    /** Cosmetics bought or won from chests. */
    fun purchasedCosmetics(): Flow<Set<String>>
    fun achievements(): Flow<Set<Achievement>>

    /** Makes sure three missions are active. */
    suspend fun ensureMissions()

    /** Claims today's login reward; returns the coins, or null if already claimed. */
    suspend fun claimDailyLogin(today: Long): Int?

    /** Buys [cosmetic]; false if not for sale, already owned or too expensive. */
    suspend fun buy(cosmetic: Cosmetic): Boolean

    /** Takes [amount] coins; false (and nothing taken) if the player has fewer. */
    suspend fun spendCoins(amount: Int): Boolean

    /** Pays out a finished round. Call after the round itself was saved to [ProgressRepository]. */
    suspend fun onRunFinished(run: RunSummary): RunRewards

    /** Deletes coins, missions, cosmetics, achievements and daily state. */
    suspend fun resetProgress()
}

class RoomMetaRepository(
    private val database: AppDatabase,
    private val progressRepository: ProgressRepository,
    private val random: Random = Random.Default,
) : MetaRepository {

    private val dao = database.metaDao()

    override fun player(): Flow<PlayerState> = dao.observePlayer().map { it?.toState() ?: PlayerState() }

    override fun missions(): Flow<List<Mission>> = dao.observeMissions().map { rows -> rows.mapNotNull { it.toMission() } }

    override fun purchasedCosmetics(): Flow<Set<String>> = dao.observeOwnedCosmetics().map { it.toSet() }

    override fun achievements(): Flow<Set<Achievement>> = dao.observeAchievements().map { it.toAchievements() }

    override suspend fun ensureMissions() {
        val snapshot = snapshot()
        update { state -> MetaRules.withMissions(state, snapshot, random) to Unit }
    }

    override suspend fun claimDailyLogin(today: Long): Int? =
        update { state -> MetaRules.claimLogin(state, today) ?: (state to null) }

    override suspend fun buy(cosmetic: Cosmetic): Boolean {
        val progress = progressRepository.stageProgress().first()
        return update { state -> MetaRules.buy(state, cosmetic, progress)?.let { it to true } ?: (state to false) }
    }

    override suspend fun spendCoins(amount: Int): Boolean =
        update { state -> MetaRules.spendCoins(state, amount)?.let { it to true } ?: (state to false) }

    override suspend fun onRunFinished(run: RunSummary): RunRewards {
        val snapshot = snapshot()
        return update { state -> MetaRules.applyRun(state, run, snapshot, random) }
    }

    override suspend fun resetProgress() {
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                dao.deletePlayer()
                dao.deleteMissions()
                dao.deleteOwnedCosmetics()
                dao.deleteAchievements()
            }
        }
    }

    /** Game progress as it is now (read outside the write transaction). */
    private suspend fun snapshot() = ProgressSnapshot(
        stageProgress = progressRepository.stageProgress().first(),
        stats = progressRepository.stats().first(),
        endlessBests = Difficulty.entries.associateWith { progressRepository.bestEndlessScore(it).first() },
    )

    /** Loads the state, applies [change] and saves the result, all in one transaction. */
    private suspend fun <T> update(change: (MetaState) -> Pair<MetaState, T>): T =
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                val before = MetaState(
                    player = dao.getPlayer()?.toState() ?: PlayerState(),
                    missions = dao.getMissions().mapNotNull { it.toMission() },
                    purchased = dao.getOwnedCosmetics().toSet(),
                    achievements = dao.getAchievements().toAchievements(),
                )
                val (after, result) = change(before)
                if (after != before) save(before, after)
                result
            }
        }

    private suspend fun save(before: MetaState, after: MetaState) {
        dao.upsertPlayer(after.player.toEntity())
        if (after.missions != before.missions) {
            dao.upsertMissions(after.missions.mapIndexed { slot, mission -> mission.toEntity(slot) })
        }
        val now = nowMillis()
        (after.purchased - before.purchased).forEach { dao.insertOwnedCosmetic(OwnedCosmeticEntity(it, now)) }
        (after.achievements - before.achievements).forEach { dao.insertAchievement(AchievementEntity(it.name, now)) }
    }

    @OptIn(ExperimentalTime::class)
    private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
}

// ── Mapping ─────────────────────────────────────────────────────────────────

private fun PlayerEntity.toState() = PlayerState(
    coins = coins,
    loginDaysClaimed = loginDaysClaimed,
    lastLoginDay = lastLoginDay,
    chestsOpened = chestsOpened,
    missionsCompleted = missionsCompleted,
    dailyChallengeDay = dailyChallengeDay,
    dailyChallengeBest = dailyChallengeBest,
    dailyStageClearedDay = dailyStageClearedDay,
)

private fun PlayerState.toEntity() = PlayerEntity(
    coins = coins,
    loginDaysClaimed = loginDaysClaimed,
    lastLoginDay = lastLoginDay,
    chestsOpened = chestsOpened,
    missionsCompleted = missionsCompleted,
    dailyChallengeDay = dailyChallengeDay,
    dailyChallengeBest = dailyChallengeBest,
    dailyStageClearedDay = dailyStageClearedDay,
)

/** Null for a row whose kind no longer exists (it gets replaced on the next fill). */
private fun MissionEntity.toMission(): Mission? {
    val kind = MissionKind.entries.firstOrNull { it.name == kind } ?: return null
    return Mission(
        kind = kind,
        target = target,
        reward = reward,
        progress = progress,
        bubbleType = bubbleType?.let { name -> BubbleType.entries.firstOrNull { it.name == name } },
        difficulty = difficulty?.let { name -> Difficulty.entries.firstOrNull { it.name == name } },
    )
}

private fun Mission.toEntity(slot: Int) = MissionEntity(
    slot = slot,
    kind = kind.name,
    target = target,
    reward = reward,
    progress = progress,
    bubbleType = bubbleType?.name,
    difficulty = difficulty?.name,
)

private fun List<String>.toAchievements(): Set<Achievement> =
    mapNotNullTo(mutableSetOf()) { id -> Achievement.entries.firstOrNull { it.name == id } }
