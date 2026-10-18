package com.apps.bubblepopping.stage

import com.apps.bubblepopping.data.StageProgress
import com.apps.bubblepopping.game.BubbleSize
import com.apps.bubblepopping.game.GameRules
import com.apps.bubblepopping.game.Pace
import com.apps.bubblepopping.game.SpawnConfig
import com.apps.bubblepopping.view.play.component.BubbleType

/**
 * Every stage in the game, in play order. Stage ids are 1, 2, 3… with no gaps.
 * Stages 1–80 follow `stage-plan.md` §4; the stage simulator test checks each one.
 */
object StageCatalog {

    // ── Spawn mixes: each world adds its new bubbles to the previous mix ─────

    private val NORMAL_ONLY = mapOf(BubbleType.NORMAL to 1)
    private val SKY_HEARTS = mapOf(BubbleType.NORMAL to 80, BubbleType.HEART to 20)
    private val SKY = mapOf(BubbleType.NORMAL to 76, BubbleType.POISON to 12, BubbleType.HEART to 12)
    private val SKY_SKULLS = mapOf(BubbleType.NORMAL to 63, BubbleType.POISON to 25, BubbleType.HEART to 12)
    /** From stage 11, timed stages that aren't about surviving also drop clocks. */
    private val SKY_TIMED = SKY + (BubbleType.POISON to 15) + (BubbleType.CLOCK to 2)

    private val OCEAN = mapOf(
        BubbleType.NORMAL to 64, BubbleType.POISON to 15, BubbleType.HEART to 8, BubbleType.GOLDEN to 8,
    )
    private val OCEAN_TIMED = OCEAN + (BubbleType.CLOCK to 2)
    private val OCEAN_ICE = OCEAN + (BubbleType.FREEZE to 4)
    private val OCEAN_ICE_TIMED = OCEAN_ICE + (BubbleType.CLOCK to 2)
    private val OCEAN_DEEP = OCEAN_ICE + (BubbleType.DOUBLE to 4)
    private val OCEAN_DEEP_TIMED = OCEAN_DEEP + (BubbleType.CLOCK to 2)

    private val SPACE = OCEAN_DEEP + (BubbleType.POISON to 20) + (BubbleType.BOMB to 6)
    private val SPACE_TIMED = SPACE + (BubbleType.CLOCK to 2)
    /** Pure aim: no bombs to clear the crowd. */
    private val SPACE_NO_BOMBS = SPACE - BubbleType.BOMB
    private val SPACE_BOMBS = SPACE_TIMED + (BubbleType.BOMB to 10)
    /** From stage 51, lightning too. */
    private val SPACE_STORM = SPACE + (BubbleType.LIGHTNING to 4)
    private val SPACE_STORM_TIMED = SPACE_STORM + (BubbleType.CLOCK to 2)

    private val CANDY = SPACE_STORM + (BubbleType.SPLITTER to 6)
    private val CANDY_TIMED = CANDY + (BubbleType.CLOCK to 2)
    /** From stage 63, shields. */
    private val CANDY_SHIELDS = CANDY + (BubbleType.SHIELD to 3)
    private val CANDY_SHIELDS_TIMED = CANDY_SHIELDS + (BubbleType.CLOCK to 2)
    /** From stage 71, armored bubbles. */
    private val CANDY_ARMOR = CANDY_SHIELDS + (BubbleType.ARMORED to 10)
    private val CANDY_ARMOR_TIMED = CANDY_ARMOR + (BubbleType.CLOCK to 2)

    /** A stage paced in waves from [pace] up to [top], designed to take about [designSec] seconds. */
    private fun stage(
        id: Int,
        world: World,
        intro: String,
        goal: Objective,
        bonus: List<Objective>,
        pace: Pace,
        top: Pace = pace,
        mix: Map<BubbleType, Int>,
        designSec: Int,
        durationSec: Int? = null,
        lives: Int = GameRules.MAX_LIVES,
    ) = StageConfig(
        id = id,
        world = world,
        goal = goal,
        bonusGoals = bonus,
        spawn = SpawnConfig.paced(pace, top, mix),
        introText = intro,
        durationSec = durationSec,
        lives = lives,
        designSec = designSec,
    )

    val stages: List<StageConfig> = listOf(
        // ── World 1 — Sky (1–20): the basics, hearts, skulls, wind; clocks from 11 ──
        stage(
            1, World.SKY, "Tap the bubbles to pop them!",
            goal = Objective.PopCount(12),
            bonus = listOf(Objective.NoMisses, Objective.FinishUnder(25)),
            pace = Pace.P1, mix = NORMAL_ONLY, designSec = 18,
        ),
        stage(
            2, World.SKY, "Bigger bubbles are worth more: 1, 3 or 5 points.",
            goal = Objective.ReachScore(50),
            bonus = listOf(Objective.ReachScore(75), Objective.ReachScore(95)),
            pace = Pace.P1, mix = NORMAL_ONLY, designSec = 45, durationSec = 45,
        ),
        stage(
            3, World.SKY, "Pink heart bubbles give you back a lost life.",
            goal = Objective.PopType(BubbleType.HEART, 3),
            bonus = listOf(Objective.ReachScore(90), Objective.Accuracy(90)),
            pace = Pace.P1, mix = SKY_HEARTS, designSec = 50, durationSec = 60,
        ),
        stage(
            4, World.SKY, "Never pop a green skull bubble — it ends the round!",
            goal = Objective.Survive(45),
            bonus = listOf(Objective.NoLivesLost, Objective.ReachScore(80)),
            pace = Pace.P2, mix = SKY, designSec = 45, durationSec = 45,
        ),
        stage(
            5, World.SKY, "Pop bubbles quickly in a row to build a combo and multiply your points.",
            goal = Objective.ReachCombo(5),
            bonus = listOf(Objective.ReachCombo(8), Objective.ReachScore(150)),
            pace = Pace.P2, mix = SKY, designSec = 45, durationSec = 60,
        ),
        stage(
            6, World.SKY, "Swipe sideways to make wind — blow skull bubbles off the screen!",
            goal = Objective.WindSaves(4),
            bonus = listOf(Objective.WindSaves(7), Objective.NoLivesLost),
            pace = Pace.P2, mix = SKY_SKULLS, designSec = 40, durationSec = 60,
        ),
        stage(
            7, World.SKY, "Put it all together!",
            goal = Objective.ReachScore(100),
            bonus = listOf(Objective.ReachScore(130), Objective.ReachScore(160)),
            pace = Pace.P3, mix = SKY, designSec = 60, durationSec = 60,
        ),
        stage(
            8, World.SKY, "Only small bubbles count here. Aim carefully!",
            goal = Objective.PopSize(BubbleSize.SMALL, 15),
            bonus = listOf(Objective.Accuracy(80), Objective.PopSize(BubbleSize.SMALL, 20)),
            pace = Pace.P3, mix = SKY, designSec = 60, durationSec = 60,
        ),
        stage(
            9, World.SKY, "The bubbles are speeding up. Hold on!",
            goal = Objective.Survive(60),
            bonus = listOf(Objective.LivesLostAtMost(1), Objective.ReachScore(200)),
            pace = Pace.P3, top = Pace.P5, mix = SKY, designSec = 60, durationSec = 60,
        ),
        stage(
            10, World.SKY, "👑 The Cloud King! Once he's hurt he throws skulls from below — don't tap them!",
            goal = Objective.DefeatBoss(Bosses.CLOUD_KING),
            bonus = listOf(Objective.FinishUnder(100), Objective.NoLivesLost),
            pace = Pace.P2, top = Pace.P4, mix = SKY_HEARTS, designSec = 90,
        ),
        stage(
            11, World.SKY, "Clock bubbles add 10 seconds to the timer!",
            goal = Objective.ReachScore(200),
            bonus = listOf(Objective.PopType(BubbleType.CLOCK, 3), Objective.Accuracy(85)),
            pace = Pace.P3, mix = SKY_TIMED, designSec = 60, durationSec = 60,
        ),
        stage(
            12, World.SKY, "A big crowd! Pop them all.",
            goal = Objective.PopCount(70),
            bonus = listOf(Objective.NoLivesLost, Objective.FinishUnder(68)),
            pace = Pace.P4, mix = SKY, designSec = 70,
        ),
        stage(
            13, World.SKY, "Only 3 lives here — grab the hearts!",
            goal = Objective.PopType(BubbleType.HEART, 5),
            bonus = listOf(Objective.ReachScore(380), Objective.Accuracy(85)),
            pace = Pace.P5, mix = SKY_TIMED, designSec = 75, durationSec = 75, lives = 3,
        ),
        stage(
            14, World.SKY, "A skull swarm! Blow them away with the wind.",
            goal = Objective.Survive(75),
            bonus = listOf(Objective.WindSaves(12), Objective.Accuracy(85)),
            pace = Pace.P5, top = Pace.P7, mix = SKY_SKULLS, designSec = 75, durationSec = 75, lives = 4,
        ),
        stage(
            15, World.SKY, "Build a combo — then do it again, and again.",
            goal = Objective.ComboTimes(8, 3),
            bonus = listOf(Objective.ComboTimes(8, 5), Objective.ReachCombo(18)),
            pace = Pace.P4, mix = SKY_TIMED, designSec = 70, durationSec = 75,
        ),
        stage(
            16, World.SKY, "Rack up the points.",
            goal = Objective.ReachScore(350),
            bonus = listOf(Objective.ReachScore(450), Objective.Accuracy(85)),
            pace = Pace.P4, mix = SKY_TIMED, designSec = 90, durationSec = 90,
        ),
        stage(
            17, World.SKY, "Small bubbles only. Steady aim!",
            goal = Objective.PopSize(BubbleSize.SMALL, 25),
            bonus = listOf(Objective.Accuracy(80), Objective.Accuracy(90)),
            pace = Pace.P4, mix = SKY, designSec = 75,
        ),
        stage(
            18, World.SKY, "Only 2 lives. Let nothing slip past!",
            goal = Objective.Survive(75),
            bonus = listOf(Objective.NoLivesLost, Objective.Accuracy(85)),
            pace = Pace.P5, mix = SKY, designSec = 75, durationSec = 75, lives = 2,
        ),
        stage(
            19, World.SKY, "The storm is coming. Survive it!",
            goal = Objective.Survive(120),
            bonus = listOf(Objective.LivesLostAtMost(2), Objective.ReachScore(600)),
            pace = Pace.P4, top = Pace.P7, mix = SKY, designSec = 120, durationSec = 120,
        ),
        stage(
            20, World.SKY, "👑 The Storm Cloud! It throws skulls, then gusts blow every bubble aside.",
            goal = Objective.DefeatBoss(Bosses.STORM_CLOUD),
            bonus = listOf(Objective.FinishUnder(160), Objective.LivesLostAtMost(1)),
            pace = Pace.P3, top = Pace.P5, mix = SKY_HEARTS, designSec = 140, lives = 4,
        ),

        // ── World 2 — Ocean (21–40): golden, ice from 23, ×2 from 31 ────────────
        stage(
            21, World.OCEAN, "Golden bubbles are small and fast — but worth 25 points!",
            goal = Objective.PopType(BubbleType.GOLDEN, 6),
            bonus = listOf(Objective.ReachScore(450), Objective.Accuracy(85)),
            pace = Pace.P4, mix = OCEAN_TIMED, designSec = 60, durationSec = 75,
        ),
        stage(
            22, World.OCEAN, "Chase the gold!",
            goal = Objective.ReachScore(450),
            bonus = listOf(Objective.ReachScore(750), Objective.Accuracy(85)),
            pace = Pace.P5, mix = OCEAN_TIMED, designSec = 80, durationSec = 90,
        ),
        stage(
            23, World.OCEAN, "Ice bubbles slow every bubble down for 3 seconds.",
            goal = Objective.PopType(BubbleType.FREEZE, 3),
            bonus = listOf(Objective.PopType(BubbleType.FREEZE, 5), Objective.Accuracy(85)),
            pace = Pace.P6, mix = OCEAN_ICE_TIMED, designSec = 75, durationSec = 75,
        ),
        stage(
            24, World.OCEAN, "Fast currents and only 3 lives. Use the ice!",
            goal = Objective.Survive(90),
            bonus = listOf(Objective.LivesLostAtMost(1), Objective.ReachScore(550)),
            pace = Pace.P6, top = Pace.P7, mix = OCEAN_ICE, designSec = 90, durationSec = 90, lives = 3,
        ),
        stage(
            25, World.OCEAN, "Freeze the bubbles, then pop them in a row.",
            goal = Objective.ComboTimes(10, 3),
            bonus = listOf(Objective.ComboTimes(10, 4), Objective.ReachCombo(18)),
            pace = Pace.P4, mix = OCEAN_ICE_TIMED, designSec = 90, durationSec = 90,
        ),
        stage(
            26, World.OCEAN, "A school of bubbles!",
            goal = Objective.PopCount(100),
            bonus = listOf(Objective.PopType(BubbleType.GOLDEN, 5), Objective.Accuracy(85)),
            pace = Pace.P5, mix = OCEAN_ICE_TIMED, designSec = 100, durationSec = 110,
        ),
        stage(
            27, World.OCEAN, "Golden hunt. Aim true!",
            goal = Objective.PopType(BubbleType.GOLDEN, 9),
            bonus = listOf(Objective.Accuracy(85), Objective.FinishUnder(95)),
            pace = Pace.P4, mix = OCEAN_ICE, designSec = 90,
        ),
        stage(
            28, World.OCEAN, "Only 2 lives — and a big score to reach.",
            goal = Objective.ReachScore(600),
            bonus = listOf(Objective.ReachScore(750), Objective.Accuracy(85)),
            pace = Pace.P5, mix = OCEAN_ICE_TIMED, designSec = 100, durationSec = 100, lives = 2,
        ),
        stage(
            29, World.OCEAN, "The tide is rising. Hold on!",
            goal = Objective.Survive(120),
            bonus = listOf(Objective.LivesLostAtMost(2), Objective.ReachScore(1_000)),
            pace = Pace.P6, top = Pace.P9, mix = OCEAN_ICE, designSec = 120, durationSec = 120,
        ),
        stage(
            30, World.OCEAN, "👑 The Jelly Queen! Every few hits she stings — your taps stop working for a moment.",
            goal = Objective.DefeatBoss(Bosses.JELLY_QUEEN),
            bonus = listOf(Objective.FinishUnder(150), Objective.LivesLostAtMost(1)),
            pace = Pace.P3, top = Pace.P5, mix = OCEAN_ICE, designSec = 130, lives = 4,
        ),
        stage(
            31, World.OCEAN, "×2 bubbles double every point for 8 seconds!",
            goal = Objective.ReachScore(600),
            bonus = listOf(Objective.ReachScore(800), Objective.Accuracy(85)),
            pace = Pace.P4, mix = OCEAN_DEEP_TIMED, designSec = 90, durationSec = 90,
        ),
        stage(
            32, World.OCEAN, "So much gold!",
            goal = Objective.PopType(BubbleType.GOLDEN, 12),
            bonus = listOf(Objective.ReachScore(1_300), Objective.Accuracy(85)),
            pace = Pace.P5, mix = OCEAN_DEEP_TIMED + (BubbleType.GOLDEN to 9), designSec = 100, durationSec = 110,
        ),
        stage(
            33, World.OCEAN, "Catch the ice and the ×2s.",
            goal = Objective.AllOf(listOf(Objective.PopType(BubbleType.FREEZE, 5), Objective.PopType(BubbleType.DOUBLE, 5))),
            bonus = listOf(Objective.NoLivesLost, Objective.Accuracy(85)),
            pace = Pace.P6, mix = OCEAN_DEEP_TIMED, designSec = 75, durationSec = 100,
        ),
        stage(
            34, World.OCEAN, "The skull reef! Only 3 lives.",
            goal = Objective.Survive(100),
            bonus = listOf(Objective.WindSaves(10), Objective.Accuracy(85)),
            pace = Pace.P6, top = Pace.P8, mix = OCEAN_DEEP + (BubbleType.POISON to 28), designSec = 100, durationSec = 100, lives = 3,
        ),
        stage(
            35, World.OCEAN, "Long combos — again and again.",
            goal = Objective.ComboTimes(8, 2),
            bonus = listOf(Objective.ComboTimes(10, 3), Objective.ReachCombo(20)),
            pace = Pace.P5, mix = OCEAN_DEEP_TIMED, designSec = 90, durationSec = 100,
        ),
        stage(
            36, World.OCEAN, "A huge shoal. Pop them all!",
            goal = Objective.PopCount(110),
            bonus = listOf(Objective.FinishUnder(100), Objective.NoLivesLost),
            pace = Pace.P6, mix = OCEAN_DEEP, designSec = 110,
        ),
        stage(
            37, World.OCEAN, "Small bubbles only — in a fast current.",
            goal = Objective.PopSize(BubbleSize.SMALL, 40),
            bonus = listOf(Objective.Accuracy(85), Objective.Accuracy(92)),
            pace = Pace.P5, mix = OCEAN_DEEP, designSec = 100,
        ),
        stage(
            38, World.OCEAN, "Only 2 lives, and the biggest score yet.",
            goal = Objective.ReachScore(1_000),
            bonus = listOf(Objective.ReachScore(1_250), Objective.NoLivesLost),
            pace = Pace.P6, mix = OCEAN_DEEP_TIMED, designSec = 110, durationSec = 110, lives = 2,
        ),
        stage(
            39, World.OCEAN, "The deep. Survive it!",
            goal = Objective.Survive(120),
            bonus = listOf(Objective.LivesLostAtMost(1), Objective.ReachScore(2_200)),
            pace = Pace.P6, top = Pace.P9, mix = OCEAN_DEEP, designSec = 120, durationSec = 120, lives = 4,
        ),
        stage(
            40, World.OCEAN, "👑 The Tide Kraken! Its shield blocks you — pop an ice bubble to break it.",
            goal = Objective.DefeatBoss(Bosses.TIDE_KRAKEN),
            bonus = listOf(Objective.FinishUnder(170), Objective.LivesLostAtMost(1)),
            pace = Pace.P3, top = Pace.P5, mix = OCEAN_ICE, designSec = 150, lives = 4,
        ),

        // ── World 3 — Space (41–60): bombs and chain reactions; lightning from 51 ──
        stage(
            41, World.SPACE, "Bombs pop every bubble around them — skulls too, so watch out!",
            goal = Objective.PopType(BubbleType.BOMB, 3),
            bonus = listOf(Objective.ReachScore(600), Objective.Accuracy(90)),
            pace = Pace.P4, mix = SPACE_BOMBS + (BubbleType.BOMB to 14), designSec = 90, durationSec = 90,
        ),
        stage(
            42, World.SPACE, "Wait for a crowd, then set off a bomb in the middle.",
            goal = Objective.BombChain(3),
            bonus = listOf(Objective.BombChain(4), Objective.Accuracy(90)),
            pace = Pace.P5, mix = SPACE_BOMBS, designSec = 80, durationSec = 100,
        ),
        stage(
            43, World.SPACE, "An asteroid field of skulls. Only 3 lives!",
            goal = Objective.Survive(100),
            bonus = listOf(Objective.LivesLostAtMost(1), Objective.Accuracy(90)),
            pace = Pace.P5, top = Pace.P6, mix = SPACE + (BubbleType.POISON to 25), designSec = 100, durationSec = 100, lives = 3,
        ),
        stage(
            44, World.SPACE, "Light up the galaxy.",
            goal = Objective.ReachScore(800),
            bonus = listOf(Objective.ReachScore(1_000), Objective.Accuracy(90)),
            pace = Pace.P6, mix = SPACE_TIMED, designSec = 110, durationSec = 110,
        ),
        stage(
            45, World.SPACE, "Keep the rhythm going.",
            goal = Objective.ComboTimes(15, 2),
            bonus = listOf(Objective.ComboTimes(15, 4), Objective.ReachCombo(20)),
            pace = Pace.P5, mix = SPACE_TIMED, designSec = 90, durationSec = 110,
        ),
        stage(
            46, World.SPACE, "Meteor shower!",
            goal = Objective.PopCount(130),
            bonus = listOf(Objective.LivesLostAtMost(1), Objective.Accuracy(90)),
            pace = Pace.P6, mix = SPACE_TIMED, designSec = 130, durationSec = 140,
        ),
        stage(
            47, World.SPACE, "Tiny stars only — and no bombs to help. Aim true!",
            goal = Objective.PopSize(BubbleSize.SMALL, 35),
            bonus = listOf(Objective.Accuracy(90), Objective.Accuracy(92)),
            pace = Pace.P5, mix = SPACE_NO_BOMBS, designSec = 95,
        ),
        stage(
            48, World.SPACE, "Only 2 lives in the cold of space.",
            goal = Objective.Survive(110),
            bonus = listOf(Objective.ReachScore(800), Objective.NoLivesLost),
            pace = Pace.P6, mix = SPACE, designSec = 110, durationSec = 110, lives = 2,
        ),
        stage(
            49, World.SPACE, "Gravity is failing. Survive the meteor storm!",
            goal = Objective.Survive(150),
            bonus = listOf(Objective.LivesLostAtMost(2), Objective.Accuracy(90)),
            pace = Pace.P6, top = Pace.P9, mix = SPACE, designSec = 150, durationSec = 150, lives = 4,
        ),
        stage(
            50, World.SPACE, "👑 The Nebula Core! When it armors up, only bomb blasts can hurt it.",
            goal = Objective.DefeatBoss(Bosses.NEBULA_CORE),
            bonus = listOf(Objective.FinishUnder(180), Objective.LivesLostAtMost(1)),
            pace = Pace.P3, top = Pace.P5, mix = SPACE, designSec = 125, lives = 4,
        ),
        stage(
            51, World.SPACE, "Lightning bubbles zap the 6 bubbles nearest them — never skulls!",
            goal = Objective.LightningZaps(8),
            bonus = listOf(Objective.LightningZaps(11), Objective.Accuracy(90)),
            pace = Pace.P5, mix = SPACE_STORM_TIMED + (BubbleType.LIGHTNING to 8), designSec = 100, durationSec = 100,
        ),
        stage(
            52, World.SPACE, "A storm of points.",
            goal = Objective.ReachScore(1_000),
            bonus = listOf(Objective.ReachScore(1_250), Objective.Accuracy(90)),
            pace = Pace.P6, mix = SPACE_STORM_TIMED, designSec = 120, durationSec = 120,
        ),
        stage(
            53, World.SPACE, "Bombs and gold — catch both.",
            goal = Objective.AllOf(listOf(Objective.PopType(BubbleType.BOMB, 8), Objective.PopType(BubbleType.GOLDEN, 8))),
            bonus = listOf(Objective.LivesLostAtMost(1), Objective.Accuracy(90)),
            pace = Pace.P6, mix = SPACE_STORM_TIMED + (BubbleType.BOMB to 9), designSec = 85, durationSec = 120,
        ),
        stage(
            54, World.SPACE, "A skull storm and only 2 lives!",
            goal = Objective.Survive(120),
            bonus = listOf(Objective.NoLivesLost, Objective.Accuracy(90)),
            pace = Pace.P6, mix = SPACE_STORM + (BubbleType.POISON to 25), designSec = 120, durationSec = 120, lives = 2,
        ),
        stage(
            55, World.SPACE, "A combo as long as the Milky Way.",
            goal = Objective.ReachCombo(15),
            bonus = listOf(Objective.ComboTimes(15, 3), Objective.ReachCombo(24)),
            pace = Pace.P6, mix = SPACE_STORM_TIMED, designSec = 95, durationSec = 120,
        ),
        stage(
            56, World.SPACE, "The big bang!",
            goal = Objective.PopCount(150),
            bonus = listOf(Objective.LivesLostAtMost(1), Objective.Accuracy(90)),
            pace = Pace.P7, mix = SPACE_STORM_TIMED, designSec = 140, durationSec = 150,
        ),
        stage(
            57, World.SPACE, "Golden comets. Catch them all!",
            goal = Objective.PopType(BubbleType.GOLDEN, 12),
            bonus = listOf(Objective.Accuracy(90), Objective.FinishUnder(120)),
            pace = Pace.P6, mix = SPACE_STORM, designSec = 120,
        ),
        stage(
            58, World.SPACE, "Only 2 lives, and a sky full of points.",
            goal = Objective.ReachScore(1_000),
            bonus = listOf(Objective.ReachScore(1_250), Objective.Accuracy(90)),
            pace = Pace.P6, mix = SPACE_STORM_TIMED, designSec = 120, durationSec = 120, lives = 2,
        ),
        stage(
            59, World.SPACE, "Supernova! Hold on!",
            goal = Objective.Survive(150),
            bonus = listOf(Objective.LivesLostAtMost(2), Objective.Accuracy(90)),
            pace = Pace.P7, top = Pace.P9, mix = SPACE_STORM, designSec = 150, durationSec = 150, lives = 4,
        ),
        stage(
            60, World.SPACE, "👑 The Black Hole! It drags every bubble towards itself — lightning hurts it double.",
            goal = Objective.DefeatBoss(Bosses.BLACK_HOLE),
            bonus = listOf(Objective.FinishUnder(195), Objective.Accuracy(90)),
            pace = Pace.P3, top = Pace.P5, mix = SPACE_STORM, designSec = 165, lives = 3,
        ),

        // ── World 4 — Candy (61–80): splitters, shields from 63, armored from 71 ──
        stage(
            61, World.CANDY, "Splitter bubbles burst into three little bubbles.",
            goal = Objective.PopType(BubbleType.SPLITTER, 5),
            bonus = listOf(Objective.ReachScore(1_100), Objective.Accuracy(90)),
            pace = Pace.P5, mix = CANDY_TIMED, designSec = 100, durationSec = 100,
        ),
        stage(
            62, World.CANDY, "Sweet overload!",
            goal = Objective.ReachScore(1_200),
            bonus = listOf(Objective.ReachScore(1_500), Objective.Accuracy(90)),
            pace = Pace.P6, mix = CANDY_TIMED, designSec = 120, durationSec = 120,
        ),
        stage(
            63, World.CANDY, "A shield bubble protects you from one skull pop.",
            goal = Objective.PopType(BubbleType.SHIELD, 3),
            bonus = listOf(Objective.LivesLostAtMost(1), Objective.Accuracy(90)),
            pace = Pace.P5, mix = CANDY_SHIELDS_TIMED + (BubbleType.POISON to 22), designSec = 110, durationSec = 110,
        ),
        stage(
            64, World.CANDY, "A sour storm, and only 2 lives. Grab a shield!",
            goal = Objective.Survive(120),
            bonus = listOf(Objective.NoLivesLost, Objective.Accuracy(90)),
            pace = Pace.P6, mix = CANDY_SHIELDS + (BubbleType.POISON to 25), designSec = 120, durationSec = 120, lives = 2,
        ),
        stage(
            65, World.CANDY, "A sugar rush of combos.",
            goal = Objective.ComboTimes(15, 3),
            bonus = listOf(Objective.ComboTimes(15, 5), Objective.ReachCombo(30)),
            pace = Pace.P6, mix = CANDY_SHIELDS_TIMED, designSec = 100, durationSec = 120,
        ),
        stage(
            66, World.CANDY, "Sprinkles everywhere!",
            goal = Objective.PopCount(170),
            bonus = listOf(Objective.LivesLostAtMost(1), Objective.Accuracy(90)),
            pace = Pace.P7, mix = CANDY_SHIELDS_TIMED, designSec = 130, durationSec = 150,
        ),
        stage(
            67, World.CANDY, "Only the small ones count — splitter bits too!",
            goal = Objective.PopSize(BubbleSize.SMALL, 75),
            bonus = listOf(Objective.Accuracy(90), Objective.Accuracy(92)),
            pace = Pace.P6, mix = CANDY_SHIELDS + (BubbleType.SPLITTER to 10), designSec = 110,
        ),
        stage(
            68, World.CANDY, "Only 2 lives — and a mountain of sweets.",
            goal = Objective.ReachScore(1_300),
            bonus = listOf(Objective.NoLivesLost, Objective.Accuracy(90)),
            pace = Pace.P6, mix = CANDY_SHIELDS_TIMED, designSec = 130, durationSec = 130, lives = 2,
        ),
        stage(
            69, World.CANDY, "The sugar rush. Survive it!",
            goal = Objective.Survive(150),
            bonus = listOf(Objective.LivesLostAtMost(1), Objective.Accuracy(90)),
            pace = Pace.P7, top = Pace.P9, mix = CANDY_SHIELDS, designSec = 150, durationSec = 150, lives = 3,
        ),
        stage(
            70, World.CANDY, "👑 The Gumball Giant! Beat it and it splits into three… then nine.",
            goal = Objective.DefeatBoss(Bosses.GUMBALL_GIANT),
            bonus = listOf(Objective.FinishUnder(195), Objective.Accuracy(90)),
            pace = Pace.P3, top = Pace.P5, mix = CANDY_SHIELDS, designSec = 150, lives = 3,
        ),
        stage(
            71, World.CANDY, "Armored bubbles take two taps — the first one cracks them.",
            goal = Objective.CrackArmored(8),
            bonus = listOf(Objective.CrackArmored(10), Objective.ReachScore(1_400)),
            pace = Pace.P5, mix = CANDY_ARMOR_TIMED, designSec = 110, durationSec = 110,
        ),
        stage(
            72, World.CANDY, "Hard candy, big points.",
            goal = Objective.ReachScore(1_400),
            bonus = listOf(Objective.ReachScore(1_700), Objective.Accuracy(90)),
            pace = Pace.P6, mix = CANDY_ARMOR_TIMED, designSec = 130, durationSec = 130,
        ),
        stage(
            73, World.CANDY, "Blasts break armor in one go. Use bombs and lightning!",
            goal = Objective.BreakArmoredWithBlast(6),
            bonus = listOf(Objective.BreakArmoredWithBlast(8), Objective.Accuracy(90)),
            pace = Pace.P6, mix = CANDY_ARMOR_TIMED + (BubbleType.BOMB to 8) + (BubbleType.LIGHTNING to 5), designSec = 110, durationSec = 130,
        ),
        stage(
            74, World.CANDY, "Skulls and armor, and only 2 lives!",
            goal = Objective.Survive(130),
            bonus = listOf(Objective.NoLivesLost, Objective.Accuracy(90)),
            pace = Pace.P7, mix = CANDY_ARMOR + (BubbleType.POISON to 25), designSec = 130, durationSec = 130, lives = 2,
        ),
        stage(
            75, World.CANDY, "The longest combo yet.",
            goal = Objective.ReachCombo(25),
            bonus = listOf(Objective.ComboTimes(25, 3), Objective.ReachCombo(35)),
            pace = Pace.P7, mix = CANDY_ARMOR_TIMED, designSec = 90, durationSec = 130,
        ),
        stage(
            76, World.CANDY, "A candy avalanche!",
            goal = Objective.PopCount(190),
            bonus = listOf(Objective.LivesLostAtMost(1), Objective.Accuracy(90)),
            pace = Pace.P8, mix = CANDY_ARMOR_TIMED, designSec = 140, durationSec = 150,
        ),
        stage(
            77, World.CANDY, "Small ones only, among the armor.",
            goal = Objective.PopSize(BubbleSize.SMALL, 70),
            bonus = listOf(Objective.Accuracy(90), Objective.Accuracy(92)),
            pace = Pace.P7, mix = CANDY_ARMOR, designSec = 95,
        ),
        stage(
            78, World.CANDY, "Only 2 lives. Shields are your friends.",
            goal = Objective.Survive(120),
            bonus = listOf(Objective.ReachScore(2_500), Objective.PopType(BubbleType.SHIELD, 3)),
            pace = Pace.P7, mix = CANDY_ARMOR, designSec = 120, durationSec = 120, lives = 2,
        ),
        stage(
            79, World.CANDY, "The candy quake. Survive it!",
            goal = Objective.Survive(150),
            bonus = listOf(Objective.LivesLostAtMost(1), Objective.Accuracy(90)),
            pace = Pace.P8, top = Pace.P10, mix = CANDY_ARMOR, designSec = 150, durationSec = 150, lives = 3,
        ),
        stage(
            80, World.CANDY, "👑 The Sugar Golem! Break its armor plates — and every bubble that escapes heals it.",
            goal = Objective.DefeatBoss(Bosses.SUGAR_GOLEM),
            bonus = listOf(Objective.FinishUnder(210), Objective.LivesLostAtMost(1)),
            pace = Pace.P3, top = Pace.P5, mix = CANDY_ARMOR, designSec = 180, lives = 3,
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
