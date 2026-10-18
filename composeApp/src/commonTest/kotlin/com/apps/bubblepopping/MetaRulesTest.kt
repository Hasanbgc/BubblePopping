package com.apps.bubblepopping

import com.apps.bubblepopping.data.PlayerStats
import com.apps.bubblepopping.data.StageProgress
import com.apps.bubblepopping.game.GameMode
import com.apps.bubblepopping.game.RunStats
import com.apps.bubblepopping.meta.Achievement
import com.apps.bubblepopping.meta.ChestReward
import com.apps.bubblepopping.meta.Cosmetics
import com.apps.bubblepopping.meta.Economy
import com.apps.bubblepopping.meta.MetaRules
import com.apps.bubblepopping.meta.MetaState
import com.apps.bubblepopping.meta.Mission
import com.apps.bubblepopping.meta.MissionKind
import com.apps.bubblepopping.meta.PlayerState
import com.apps.bubblepopping.meta.ProgressSnapshot
import com.apps.bubblepopping.meta.RunSummary
import com.apps.bubblepopping.stage.StageCatalog
import com.apps.bubblepopping.stage.World
import com.apps.bubblepopping.view.home.Difficulty
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MetaRulesTest {

    private val random = Random(7)
    private val endless = GameMode.Endless(Difficulty.MEDIUM)

    /** Missions that a typical test run can't complete, so they don't add coins. */
    private val idleMissions = listOf(
        Mission(MissionKind.POP_BUBBLES, target = 10_000, reward = 30),
        Mission(MissionKind.CLEAR_STAGES, target = 100, reward = 40),
        Mission(MissionKind.WIND_SAVES, target = 100, reward = 40),
    )
    private val start = MetaState(missions = idleMissions)

    private fun stars(vararg idToStars: Pair<Int, Int>) =
        idToStars.associate { (id, stars) -> id to StageProgress(id, stars, bestScore = 10) }

    private fun apply(
        run: RunSummary,
        state: MetaState = start,
        snapshot: ProgressSnapshot = ProgressSnapshot(),
    ) = MetaRules.applyRun(state, run, snapshot, random)

    // ── Coins ───────────────────────────────────────────────────────────────

    @Test
    fun endlessScore_paysOneCoinPerFiftyPoints_upToACap() {
        val (state, rewards) = apply(RunSummary(endless, score = 157))
        assertEquals(3, rewards.coins)
        assertEquals(3, state.player.coins)

        val huge = apply(RunSummary(endless, score = 1_000_000)).second
        assertEquals(Economy.MAX_SCORE_COINS, huge.coins)
    }

    @Test
    fun stageStars_payOnlyTheFirstTime_andReplaysPayALittle() {
        val firstClear = apply(RunSummary(GameMode.Stage(1), score = 50, stars = 2, previousStars = 0)).second
        assertEquals(Economy.STAR_COINS[0] + Economy.STAR_COINS[1], firstClear.coins)

        val oneMore = apply(RunSummary(GameMode.Stage(1), score = 50, stars = 3, previousStars = 2)).second
        assertEquals(Economy.STAR_COINS[2], oneMore.coins)

        val replay = apply(RunSummary(GameMode.Stage(1), score = 50, stars = 3, previousStars = 3)).second
        assertEquals(Economy.REPLAY_COINS, replay.coins)

        val lost = apply(RunSummary(GameMode.Stage(1), score = 5, stars = 0, previousStars = 3)).second
        assertEquals(0, lost.coins)
    }

    @Test
    fun bossStages_payABigFirstWin_andMoreForTheThirdStar() {
        val bossId = StageCatalog.stages.first { it.boss != null }.id
        val win = apply(RunSummary(GameMode.Stage(bossId), score = 500, stars = 1, previousStars = 0)).second
        assertEquals(Economy.BOSS_STAR_COINS[0], win.coins)

        val perfect = apply(RunSummary(GameMode.Stage(bossId), score = 500, stars = 3, previousStars = 1)).second
        assertEquals(Economy.BOSS_STAR_COINS[1] + Economy.BOSS_STAR_COINS[2], perfect.coins)
    }

    // ── Missions ────────────────────────────────────────────────────────────

    @Test
    fun completedMission_isPaid_andReplacedByAnotherKind() {
        val combo = Mission(MissionKind.REACH_COMBO, target = 8, reward = 30)
        val state = start.copy(missions = listOf(combo) + idleMissions.drop(1))

        val (after, rewards) = apply(RunSummary(endless, score = 0, stats = RunStats(bestCombo = 9)), state)

        assertEquals(listOf(combo.copy(progress = 8)), rewards.completedMissions)
        assertEquals(30, rewards.coins)
        assertEquals(1, after.player.missionsCompleted)
        assertEquals(3, after.missions.size)
        assertEquals(3, after.missions.map { it.kind }.toSet().size)
        assertNotEquals(MissionKind.REACH_COMBO, after.missions.first().kind)
    }

    @Test
    fun missions_areFilledToThree() {
        val (after, _) = apply(RunSummary(endless, score = 0), MetaState())
        assertEquals(3, after.missions.size)
    }

    // ── Chests ──────────────────────────────────────────────────────────────

    @Test
    fun everyFifteenStars_opensOneChest() {
        val snapshot = ProgressSnapshot(stageProgress = stars(1 to 3, 2 to 3, 3 to 3, 4 to 3, 5 to 3))
        val (after, rewards) = apply(RunSummary(GameMode.Stage(5), score = 0, stars = 3, previousStars = 3), snapshot = snapshot)

        assertEquals(1, rewards.chests.size)
        assertEquals(1, after.player.chestsOpened)

        // Same stars again: no second chest.
        val (_, again) = apply(RunSummary(endless, score = 0), after, snapshot)
        assertTrue(again.chests.isEmpty())
    }

    @Test
    fun chests_neverGiveBossRewards_orThingsAlreadyOwned() {
        val allBought = Cosmetics.all.filter { it.inChests }.map { it.id }.toSet()
        repeat(100) { seed ->
            val reward = Economy.openChest(Random(seed), owned = allBought)
            assertEquals(ChestReward.Coins(Economy.CHEST_COINS), reward)
        }
        repeat(100) { seed ->
            val reward = Economy.openChest(Random(seed), owned = emptySet())
            if (reward is ChestReward.Item) {
                assertTrue(reward.cosmetic.price != null && reward.cosmetic.bossStageId == null)
            }
        }
    }

    @Test
    fun chestItem_isAddedToOwnedCosmetics() {
        // nextFloat() = 0 → always below the item chance; nextInt() = 0 → the first prize.
        val alwaysZero = object : Random() {
            override fun nextBits(bitCount: Int) = 0
        }
        val snapshot = ProgressSnapshot(stageProgress = stars(1 to 3, 2 to 3, 3 to 3, 4 to 3, 5 to 3))
        val (after, rewards) = MetaRules.applyRun(MetaState(missions = idleMissions), RunSummary(endless, 0), snapshot, alwaysZero)

        val item = rewards.chests.filterIsInstance<ChestReward.Item>().single()
        assertTrue(item.cosmetic.id in after.purchased)
        assertTrue(item.cosmetic.id in Cosmetics.owned(after.purchased, snapshot.stageProgress))
        assertEquals(0, rewards.coins, "an item chest pays no coins")
    }

    // ── Daily modes ─────────────────────────────────────────────────────────

    @Test
    fun dailyChallenge_keepsTheBestOfTheDay_andResetsTheNextDay() {
        var state = start
        state = apply(RunSummary(GameMode.DailyChallenge(100), score = 80), state).first
        state = apply(RunSummary(GameMode.DailyChallenge(100), score = 50), state).first
        assertEquals(80, state.player.dailyChallengeBest(100))

        state = apply(RunSummary(GameMode.DailyChallenge(101), score = 30), state).first
        assertEquals(30, state.player.dailyChallengeBest(101))
        assertNull(state.player.dailyChallengeBest(102))
    }

    @Test
    fun dailyStage_givesOneChestPerDay() {
        val clear = RunSummary(GameMode.DailyStage(100, World.SKY), score = 120, stars = 1)
        val (afterFirst, first) = apply(clear)
        assertEquals(1, first.chests.size)
        assertTrue(afterFirst.player.dailyStageCleared(100))

        val (_, second) = apply(clear, afterFirst)
        assertTrue(second.chests.isEmpty())

        val failed = apply(RunSummary(GameMode.DailyStage(101, World.SKY), score = 10, stars = 0), afterFirst).second
        assertTrue(failed.chests.isEmpty())
    }

    // ── Achievements ────────────────────────────────────────────────────────

    @Test
    fun achievements_payOnce() {
        val snapshot = ProgressSnapshot(stats = PlayerStats(gamesPlayed = 1, bubblesPopped = 1_200))
        val (after, rewards) = apply(RunSummary(endless, score = 0), snapshot = snapshot)
        assertEquals(listOf(Achievement.POP_1000), rewards.achievements)
        assertEquals(Achievement.POP_1000.reward, rewards.coins)

        val (_, again) = apply(RunSummary(endless, score = 0), after, snapshot)
        assertTrue(again.achievements.isEmpty())
    }

    @Test
    fun achievementTargets_matchTheCatalog() {
        val skyStars = StageCatalog.stages.filter { it.world == World.SKY }.sumOf { it.maxStars }
        assertEquals(skyStars, Achievement.SKY_PERFECT.target)
        assertEquals(StageCatalog.stages.count { it.boss != null }, Achievement.ALL_BOSSES.target)
    }

    // ── Login rewards ───────────────────────────────────────────────────────

    @Test
    fun loginReward_onceADay_andDaySevenPaysMost() {
        var state = MetaState()
        val paid = mutableListOf<Int>()
        // Days with gaps: missed days don't reset the week.
        for (day in listOf(10L, 11L, 15L, 16L, 30L, 31L, 32L, 40L)) {
            val (next, coins) = MetaRules.claimLogin(state, day)!!
            assertNull(MetaRules.claimLogin(next, day), "only once per day")
            paid += coins
            state = next
        }
        assertEquals(Economy.LOGIN_REWARDS + Economy.LOGIN_REWARDS.first(), paid)
        assertEquals(Economy.LOGIN_REWARDS.max(), paid[6])
    }

    // ── Shop ────────────────────────────────────────────────────────────────

    @Test
    fun buying_needsCoins_andOnlyWorksOnce() {
        val neon = Cosmetics.byId("skin_neon")!!
        assertNull(MetaRules.buy(MetaState(player = PlayerState(coins = 10)), neon, emptyMap()))

        val bought = MetaRules.buy(MetaState(player = PlayerState(coins = 1_000)), neon, emptyMap())!!
        assertEquals(1_000 - neon.price!!, bought.player.coins)
        assertTrue(neon.id in bought.purchased)
        assertNull(MetaRules.buy(bought, neon, emptyMap()), "already owned")
    }

    @Test
    fun bossRewards_cantBeBought_butUnlockByBeatingTheBoss() {
        val cloud = Cosmetics.byId("skin_cloud")!!
        assertNull(MetaRules.buy(MetaState(player = PlayerState(coins = 10_000)), cloud, emptyMap()))

        assertFalse(cloud.id in Cosmetics.owned(emptySet(), emptyMap()))
        assertTrue(cloud.id in Cosmetics.owned(emptySet(), stars(cloud.bossStageId!! to 1)))
    }
}
