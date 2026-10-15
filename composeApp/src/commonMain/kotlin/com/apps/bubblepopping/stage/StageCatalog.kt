package com.apps.bubblepopping.stage

import com.apps.bubblepopping.data.StageProgress
import com.apps.bubblepopping.game.BubbleSize
import com.apps.bubblepopping.game.SpawnConfig
import com.apps.bubblepopping.view.play.component.BubbleType

/** Every stage in the game, in play order. Stage ids are 1, 2, 3… with no gaps. */
object StageCatalog {

    // ── Spawn mixes: each world adds its new bubbles to the previous mix ─────

    private val NORMAL_ONLY = mapOf(BubbleType.NORMAL to 1)
    private val WITH_HEARTS = mapOf(BubbleType.NORMAL to 75, BubbleType.HEART to 25)
    private val WITH_POISON = SpawnConfig.DEFAULT_TYPE_WEIGHTS
    private val SKULL_HEAVY = mapOf(BubbleType.NORMAL to 65, BubbleType.POISON to 28, BubbleType.HEART to 7)

    private val OCEAN_GOLDEN = mapOf(
        BubbleType.NORMAL to 68, BubbleType.POISON to 12, BubbleType.HEART to 8, BubbleType.GOLDEN to 12,
    )
    private val OCEAN = mapOf(
        BubbleType.NORMAL to 60, BubbleType.POISON to 13, BubbleType.HEART to 7,
        BubbleType.GOLDEN to 9, BubbleType.FREEZE to 11,
    )
    private val SPACE = mapOf(
        BubbleType.NORMAL to 58, BubbleType.POISON to 15, BubbleType.HEART to 7,
        BubbleType.GOLDEN to 6, BubbleType.FREEZE to 4, BubbleType.BOMB to 10,
    )
    private val SPACE_SKULLS = SPACE + (BubbleType.POISON to 28)
    private val CANDY = mapOf(
        BubbleType.NORMAL to 52, BubbleType.POISON to 15, BubbleType.HEART to 6, BubbleType.GOLDEN to 6,
        BubbleType.FREEZE to 4, BubbleType.BOMB to 6, BubbleType.SPLITTER to 7, BubbleType.SHIELD to 4,
    )
    private val CANDY_SKULLS = CANDY + (BubbleType.POISON to 28) + (BubbleType.SHIELD to 8)

    /** Boss stages: bubbles start calm so the boss is the focus, then speed up with each phase ([GameRules.BOSS_SPAWN_PACE]). */
    private val BOSS_SPAWN = SpawnConfig.MEDIUM.copy(spawnInterval = 1.25f)

    /** Faster than HARD, for the late stages of later worlds. */
    private val VERY_HARD = SpawnConfig.HARD.copy(spawnInterval = 0.4f, maxSpeed = 0.115f, maxSpeedCap = 0.22f)

    val stages: List<StageConfig> = listOf(
        // ── World 1 — Sky: the basics ────────────────────────────────────────
        StageConfig(
            id = 1,
            world = World.SKY,
            introText = "Tap the bubbles to pop them!",
            goal = Objective.PopCount(10),
            bonusGoals = listOf(Objective.NoMisses, Objective.FinishUnder(20)),
            spawn = SpawnConfig.EASY.copy(typeWeights = NORMAL_ONLY),
        ),
        StageConfig(
            id = 2,
            world = World.SKY,
            introText = "Bigger bubbles are worth more: 1, 3 or 5 points.",
            goal = Objective.ReachScore(30),
            bonusGoals = listOf(Objective.ReachScore(45), Objective.NoLivesLost),
            spawn = SpawnConfig.EASY.copy(typeWeights = NORMAL_ONLY),
            durationSec = 45,
        ),
        StageConfig(
            id = 3,
            world = World.SKY,
            introText = "Pink heart bubbles give you back a lost life.",
            goal = Objective.PopType(BubbleType.HEART, 3),
            bonusGoals = listOf(Objective.ReachScore(40), Objective.NoMisses),
            spawn = SpawnConfig.EASY.copy(typeWeights = WITH_HEARTS),
            durationSec = 60,
        ),
        StageConfig(
            id = 4,
            world = World.SKY,
            introText = "Never pop a green skull bubble — it ends the round!",
            goal = Objective.Survive(30),
            bonusGoals = listOf(Objective.NoLivesLost, Objective.ReachScore(50)),
            spawn = SpawnConfig.MEDIUM.copy(typeWeights = WITH_POISON),
            durationSec = 30,
        ),
        StageConfig(
            id = 5,
            world = World.SKY,
            introText = "Pop bubbles quickly in a row to build a combo and multiply your points.",
            goal = Objective.ReachCombo(4),
            bonusGoals = listOf(Objective.ReachCombo(6), Objective.ReachScore(60)),
            spawn = SpawnConfig.MEDIUM.copy(typeWeights = WITH_HEARTS),
            durationSec = 60,
        ),
        StageConfig(
            id = 6,
            world = World.SKY,
            introText = "Swipe sideways to make wind — blow skull bubbles off the screen!",
            goal = Objective.WindSaves(3),
            bonusGoals = listOf(Objective.NoLivesLost, Objective.ReachScore(50)),
            spawn = SpawnConfig.MEDIUM.copy(typeWeights = SKULL_HEAVY),
            durationSec = 60,
        ),
        StageConfig(
            id = 7,
            world = World.SKY,
            introText = "Put it all together!",
            goal = Objective.ReachScore(80),
            bonusGoals = listOf(Objective.ReachCombo(6), Objective.NoLivesLost),
            spawn = SpawnConfig.MEDIUM.copy(typeWeights = WITH_POISON),
            durationSec = 60,
        ),
        StageConfig(
            id = 8,
            world = World.SKY,
            introText = "Small bubbles only count here. Aim carefully!",
            goal = Objective.PopSize(BubbleSize.SMALL, 25),
            bonusGoals = listOf(Objective.MaxTaps(35), Objective.NoMisses),
            spawn = SpawnConfig.MEDIUM.copy(typeWeights = WITH_POISON),
        ),
        StageConfig(
            id = 9,
            world = World.SKY,
            introText = "The bubbles are speeding up. Hold on!",
            goal = Objective.Survive(60),
            bonusGoals = listOf(Objective.NoLivesLost, Objective.ReachScore(120)),
            spawn = SpawnConfig.HARD.copy(typeWeights = WITH_POISON),
            durationSec = 60,
        ),
        StageConfig(
            id = 10,
            world = World.SKY,
            introText = "👑 The Cloud King! Every few hits he sends a skull up from below — don't tap it!",
            goal = Objective.DefeatBoss(Bosses.CLOUD_KING),
            bonusGoals = listOf(Objective.FinishUnder(55), Objective.NoLivesLost),
            spawn = BOSS_SPAWN.copy(typeWeights = WITH_HEARTS),
        ),

        // ── World 2 — Ocean: golden + ice bubbles ────────────────────────────
        StageConfig(
            id = 11,
            world = World.OCEAN,
            introText = "Golden bubbles are small and fast — but worth 25 points!",
            goal = Objective.PopType(BubbleType.GOLDEN, 3),
            bonusGoals = listOf(Objective.ReachScore(100), Objective.NoLivesLost),
            spawn = SpawnConfig.MEDIUM.copy(typeWeights = OCEAN_GOLDEN),
            durationSec = 60,
        ),
        StageConfig(
            id = 12,
            world = World.OCEAN,
            introText = "Chase the gold!",
            goal = Objective.ReachScore(150),
            bonusGoals = listOf(Objective.PopType(BubbleType.GOLDEN, 5), Objective.NoLivesLost),
            spawn = SpawnConfig.MEDIUM.copy(typeWeights = OCEAN_GOLDEN),
            durationSec = 60,
        ),
        StageConfig(
            id = 13,
            world = World.OCEAN,
            introText = "Ice bubbles slow every bubble down for 3 seconds.",
            goal = Objective.PopType(BubbleType.FREEZE, 2),
            bonusGoals = listOf(Objective.ReachScore(80), Objective.NoLivesLost),
            spawn = SpawnConfig.MEDIUM.copy(typeWeights = OCEAN),
            durationSec = 60,
        ),
        StageConfig(
            id = 14,
            world = World.OCEAN,
            introText = "Fast currents! Use ice to catch your breath.",
            goal = Objective.Survive(45),
            bonusGoals = listOf(Objective.NoLivesLost, Objective.PopType(BubbleType.FREEZE, 3)),
            spawn = SpawnConfig.HARD.copy(typeWeights = OCEAN),
            durationSec = 45,
        ),
        StageConfig(
            id = 15,
            world = World.OCEAN,
            introText = "Freeze the bubbles, then pop them in a row.",
            goal = Objective.ReachCombo(8),
            bonusGoals = listOf(Objective.ReachCombo(10), Objective.ReachScore(150)),
            spawn = SpawnConfig.MEDIUM.copy(typeWeights = OCEAN),
            durationSec = 60,
        ),
        StageConfig(
            id = 16,
            world = World.OCEAN,
            introText = "A school of bubbles!",
            goal = Objective.PopCount(40),
            bonusGoals = listOf(Objective.PopType(BubbleType.GOLDEN, 4), Objective.NoLivesLost),
            spawn = SpawnConfig.HARD.copy(typeWeights = OCEAN),
            durationSec = 60,
        ),
        StageConfig(
            id = 17,
            world = World.OCEAN,
            introText = "Only small bubbles count. Steady aim!",
            goal = Objective.PopSize(BubbleSize.SMALL, 20),
            bonusGoals = listOf(Objective.MaxTaps(30), Objective.NoMisses),
            spawn = SpawnConfig.MEDIUM.copy(typeWeights = OCEAN),
        ),
        StageConfig(
            id = 18,
            world = World.OCEAN,
            introText = "Deep water, big scores.",
            goal = Objective.ReachScore(200),
            bonusGoals = listOf(Objective.ReachCombo(8), Objective.NoLivesLost),
            spawn = SpawnConfig.HARD.copy(typeWeights = OCEAN),
            durationSec = 60,
        ),
        StageConfig(
            id = 19,
            world = World.OCEAN,
            introText = "The tide is rising. Hold on!",
            goal = Objective.Survive(75),
            bonusGoals = listOf(Objective.NoLivesLost, Objective.ReachScore(250)),
            spawn = VERY_HARD.copy(typeWeights = OCEAN),
            durationSec = 75,
        ),
        StageConfig(
            id = 20,
            world = World.OCEAN,
            introText = "👑 The Jelly Queen! Every few hits she stings — your taps stop working for a moment.",
            goal = Objective.DefeatBoss(Bosses.JELLY_QUEEN),
            bonusGoals = listOf(Objective.FinishUnder(70), Objective.NoLivesLost),
            spawn = BOSS_SPAWN.copy(typeWeights = OCEAN),
        ),

        // ── World 3 — Space: bombs + chain reactions ─────────────────────────
        StageConfig(
            id = 21,
            world = World.SPACE,
            introText = "Bombs pop every bubble around them — skulls too, so watch out!",
            goal = Objective.PopType(BubbleType.BOMB, 2),
            bonusGoals = listOf(Objective.ReachScore(100), Objective.NoLivesLost),
            spawn = SpawnConfig.MEDIUM.copy(typeWeights = SPACE),
            durationSec = 60,
        ),
        StageConfig(
            id = 22,
            world = World.SPACE,
            introText = "Wait for a crowd, then set off a bomb in the middle.",
            goal = Objective.BombChain(5),
            bonusGoals = listOf(Objective.ReachScore(150), Objective.NoLivesLost),
            spawn = SpawnConfig.MEDIUM.copy(typeWeights = SPACE),
            durationSec = 60,
        ),
        StageConfig(
            id = 23,
            world = World.SPACE,
            introText = "Bombs caught in a blast explode too. Chain them!",
            goal = Objective.ReachScore(200),
            bonusGoals = listOf(Objective.BombChain(8), Objective.NoLivesLost),
            spawn = SpawnConfig.HARD.copy(typeWeights = SPACE),
            durationSec = 60,
        ),
        StageConfig(
            id = 24,
            world = World.SPACE,
            introText = "A skull storm! Blow them away with the wind.",
            goal = Objective.Survive(60),
            bonusGoals = listOf(Objective.WindSaves(5), Objective.NoLivesLost),
            spawn = SpawnConfig.MEDIUM.copy(typeWeights = SPACE_SKULLS),
            durationSec = 60,
        ),
        StageConfig(
            id = 25,
            world = World.SPACE,
            introText = "Keep the rhythm going.",
            goal = Objective.ReachCombo(10),
            bonusGoals = listOf(Objective.ReachCombo(12), Objective.ReachScore(250)),
            spawn = SpawnConfig.HARD.copy(typeWeights = SPACE),
            durationSec = 60,
        ),
        StageConfig(
            id = 26,
            world = World.SPACE,
            introText = "Meteor shower!",
            goal = Objective.PopCount(50),
            bonusGoals = listOf(Objective.BombChain(10), Objective.NoLivesLost),
            spawn = SpawnConfig.HARD.copy(typeWeights = SPACE),
            durationSec = 60,
        ),
        StageConfig(
            id = 27,
            world = World.SPACE,
            introText = "Tiny stars only. Aim true!",
            goal = Objective.PopSize(BubbleSize.SMALL, 30),
            bonusGoals = listOf(Objective.MaxTaps(40), Objective.NoMisses),
            spawn = SpawnConfig.HARD.copy(typeWeights = SPACE),
        ),
        StageConfig(
            id = 28,
            world = World.SPACE,
            introText = "Light up the galaxy.",
            goal = Objective.ReachScore(300),
            bonusGoals = listOf(Objective.ReachCombo(10), Objective.NoLivesLost),
            spawn = SpawnConfig.HARD.copy(typeWeights = SPACE),
            durationSec = 60,
        ),
        StageConfig(
            id = 29,
            world = World.SPACE,
            introText = "Gravity is failing. Survive!",
            goal = Objective.Survive(90),
            bonusGoals = listOf(Objective.NoLivesLost, Objective.ReachScore(300)),
            spawn = VERY_HARD.copy(typeWeights = SPACE),
            durationSec = 90,
        ),
        StageConfig(
            id = 30,
            world = World.SPACE,
            introText = "👑 The Nebula Core! Bomb blasts hurt it — and it drops bombs when it's weak.",
            goal = Objective.DefeatBoss(Bosses.NEBULA_CORE),
            bonusGoals = listOf(Objective.FinishUnder(85), Objective.NoLivesLost),
            spawn = BOSS_SPAWN.copy(typeWeights = SPACE),
        ),

        // ── World 4 — Candy: splitters + shields ─────────────────────────────
        StageConfig(
            id = 31,
            world = World.CANDY,
            introText = "Splitter bubbles burst into three little bubbles.",
            goal = Objective.PopType(BubbleType.SPLITTER, 3),
            bonusGoals = listOf(Objective.ReachScore(150), Objective.NoLivesLost),
            spawn = SpawnConfig.MEDIUM.copy(typeWeights = CANDY),
            durationSec = 60,
        ),
        StageConfig(
            id = 32,
            world = World.CANDY,
            introText = "Sweet overload!",
            goal = Objective.PopCount(60),
            bonusGoals = listOf(Objective.PopType(BubbleType.SPLITTER, 5), Objective.NoLivesLost),
            spawn = SpawnConfig.HARD.copy(typeWeights = CANDY),
            durationSec = 60,
        ),
        StageConfig(
            id = 33,
            world = World.CANDY,
            introText = "A shield bubble protects you from one skull pop.",
            goal = Objective.PopType(BubbleType.SHIELD, 2),
            bonusGoals = listOf(Objective.ReachScore(150), Objective.NoLivesLost),
            spawn = SpawnConfig.MEDIUM.copy(typeWeights = CANDY),
            durationSec = 75,
        ),
        StageConfig(
            id = 34,
            world = World.CANDY,
            introText = "Skulls everywhere — grab a shield!",
            goal = Objective.Survive(60),
            bonusGoals = listOf(Objective.NoLivesLost, Objective.PopType(BubbleType.SHIELD, 3)),
            spawn = SpawnConfig.MEDIUM.copy(typeWeights = CANDY_SKULLS),
            durationSec = 60,
        ),
        StageConfig(
            id = 35,
            world = World.CANDY,
            introText = "A sugar rush of combos.",
            goal = Objective.ReachCombo(12),
            bonusGoals = listOf(Objective.ReachCombo(15), Objective.ReachScore(350)),
            spawn = SpawnConfig.HARD.copy(typeWeights = CANDY),
            durationSec = 60,
        ),
        StageConfig(
            id = 36,
            world = World.CANDY,
            introText = "Candy bombs! Make them chain.",
            goal = Objective.BombChain(12),
            bonusGoals = listOf(Objective.ReachScore(300), Objective.NoLivesLost),
            spawn = SpawnConfig.HARD.copy(typeWeights = CANDY + (BubbleType.BOMB to 12)),
            durationSec = 60,
        ),
        StageConfig(
            id = 37,
            world = World.CANDY,
            introText = "Sprinkles only — the small ones!",
            goal = Objective.PopSize(BubbleSize.SMALL, 40),
            bonusGoals = listOf(Objective.MaxTaps(50), Objective.NoMisses),
            spawn = SpawnConfig.HARD.copy(typeWeights = CANDY),
        ),
        StageConfig(
            id = 38,
            world = World.CANDY,
            introText = "The biggest score yet.",
            goal = Objective.ReachScore(450),
            bonusGoals = listOf(Objective.ReachCombo(12), Objective.NoLivesLost),
            spawn = SpawnConfig.HARD.copy(typeWeights = CANDY),
            durationSec = 60,
        ),
        StageConfig(
            id = 39,
            world = World.CANDY,
            introText = "Total chaos. Survive!",
            goal = Objective.Survive(90),
            bonusGoals = listOf(Objective.NoLivesLost, Objective.ReachScore(400)),
            spawn = VERY_HARD.copy(typeWeights = CANDY),
            durationSec = 90,
        ),
        StageConfig(
            id = 40,
            world = World.CANDY,
            introText = "👑 The Gumball Giant! Beat it and it splits into three… then nine.",
            goal = Objective.DefeatBoss(Bosses.GUMBALL_GIANT),
            bonusGoals = listOf(Objective.FinishUnder(90), Objective.NoLivesLost),
            spawn = BOSS_SPAWN.copy(typeWeights = CANDY),
        ),
    )

    fun stage(id: Int): StageConfig =
        requireNotNull(stages.firstOrNull { it.id == id }) { "No stage with id $id" }

    fun next(id: Int): StageConfig? = stages.firstOrNull { it.id == id + 1 }

    /**
     * A stage is open when its world is unlocked and the stage before it has at
     * least one star. The first stage is always open.
     */
    fun isUnlocked(id: Int, progress: Map<Int, StageProgress>): Boolean {
        if (id == stages.first().id) return true
        if (!isWorldUnlocked(stage(id).world, progress)) return false
        return (progress[id - 1]?.stars ?: 0) > 0
    }

    fun isWorldUnlocked(world: World, progress: Map<Int, StageProgress>): Boolean =
        totalStars(progress) >= world.starsToUnlock

    fun totalStars(progress: Map<Int, StageProgress>): Int = progress.values.sumOf { it.stars }

    /**
     * The stage the player should play next: the first open stage without a
     * star, or the last open stage once everything open is cleared.
     */
    fun currentStageId(progress: Map<Int, StageProgress>): Int {
        val open = stages.filter { isUnlocked(it.id, progress) }
        return (open.firstOrNull { (progress[it.id]?.stars ?: 0) == 0 } ?: open.last()).id
    }

    /** Worlds that have stages, in order. */
    val worlds: List<World> get() = stages.map { it.world }.distinct()

    /** The world after the last one with stages — shown on the map as a "coming next" teaser. */
    val upcomingWorld: World? get() = World.entries.getOrNull(stages.last().world.ordinal + 1)

    val maxStars: Int get() = stages.sumOf { it.maxStars }
}
