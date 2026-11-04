# Bubble Popping — Stages 1–100 & Economy Plan

Phase 1 of the 500-stage roadmap (100 → 300 → 500). Design only; nothing here is built yet.
All numbers are first-pass targets: the stage simulator test (§2.3) and phone playtests tune them.

---

## 1. Goals

- **Stages 1–10** teach the game and calibrate the player: short and forgiving.
- **Stages 11–40** take 60–120 s. **Stages 41–100** take 90 s – 2:30. **Bosses** take 1:30 – 3:30 and are
  the hardest stage of their block.
- **Every stage is completable** (checked by a test) and **different from its neighbours**.
- **Coins are earned slowly enough** that the shop is a long-term goal, not a one-week purchase.
- **Ad slots exist from day one but cost coins for now**; each becomes "watch an ad" later.

---

## 2. Stage system

### 2.1 Length is the clock

A **timed stage always plays out its clock**, however early its stars are earned: the clock is
the stage, and the score keeps counting. An **untimed stage** ("pop 90 bubbles") ends at its goal,
which is sized so a good player takes about the stage's design length.

Targets come from **calibration** (`StageCalibrationTest`, switched on when retuning): every
stage is played for its full length by simulated players with nothing able to end it, and what
they reach sets the stars — ★ about ¾ of the yardstick player (casual, or improving from stage 41),
★★ between it and a good player, ★★★ near a good player. Bosses get the HP a good player deals
in the design length.

### 2.2 Pace levels

Bubbles come in **groups** of 1–4 that rise together in a loose cluster, so the screen is never
empty and pops chain into combos (the combo window is **1 s**). The slowest bubbles still cross
the screen in about 20 s. Stages also ramp by **time, in waves**: each wave lasts 25 s, then
**2.5 s of calm** lets the player breathe, and each new wave spawns 8 % faster and moves 8 %
faster, up to the stage's top pace.

| Pace | Group every | Bubbles per group | Bubbles / s | Top rise speed (screen heights / s) |
|---|---|---|---|---|
| P1 | 0.95 s | 1–2 | 1.6 | 0.06 |
| P2 | 0.85 s | 1–2 | 1.8 | 0.065 |
| P3 | 0.80 s | 1–3 | 2.5 | 0.07 |
| P4 | 0.75 s | 1–3 | 2.7 | 0.08 |
| P5 | 0.70 s | 2–3 | 3.6 | 0.09 |
| P6 | 0.66 s | 2–3 | 3.8 | 0.10 |
| P7 | 0.62 s | 2–3 | 4.0 | 0.11 |
| P8 | 0.60 s | 2–3 | 4.2 | 0.115 |
| P9 | 0.58 s | 2–4 | 5.2 | 0.12 |
| P10 | 0.55 s | 2–4 | 5.5 | 0.13 |

"P4→P7" in the tables means the stage starts at P4 and its waves climb to P7. Survive stages run
a step or two calmer than the rest: more bubbles mean more chances to lose lives.

### 2.3 The stage simulator test

`StageSimulatorTest` plays every stage, 4 times each, with three simulated players: **good**
(5 taps/s, 93 % aim, always the most pressing bubble), **improving** (3.3 taps/s, 85 %) and
**casual** (2.5 taps/s, 75 %, often just any bubble). None of them waits for crowds: they pop
what's there, as real players do. It **fails the build** if:

- a good player never wins a stage, or never earns one of its stars;
- an untimed stage can be won in under 80 % of its design length;
- the yardstick player (casual up to stage 40, improving from 41) never wins, or gets every
  star more than once in four tries — except on boss stages.

Findings along the way: one bomb catches only a handful of bubbles, so "N with one bomb" goals
stay small; with plenty of bubbles even slow players chain long combos, so combo stars don't
separate skill and third stars mostly ask for accuracy instead.

### 2.4 Stars

- ★ = the goal. ★★ and ★★★ = higher scores, extra goals or a rule ("lose no lives").
- The **"use at most N taps" rule is removed**: it can't be met because ignored bubbles cost lives.
  It's replaced by **accuracy** ("80 % of taps hit a bubble"), which is always achievable.
- 3 stars per stage → **300 ★** in stages 1–100.

### 2.5 The 10-stage shape

Every block of 10 follows the same shape, so neighbouring stages always ask for something different.
Difficulty rises through the block, peaks at the boss, and drops a little at the next x1.

| Slot | Kind | What it asks |
|---|---|---|
| x1 | **Learn** | Meet the block's new bubble, gently |
| x2 | **Score** | Hit a score in time |
| x3 | **Twist** | A goal for one or two bubble types |
| x4 | **Pressure** | Survive with fewer lives and more skulls |
| x5 | **Combo** | Reach a combo, several times |
| x6 | **Volume** | Pop a lot of bubbles |
| x7 | **Precision** | Small / golden / ghost bubbles, accuracy stars |
| x8 | **Guard** | Only 2 lives: let almost nothing escape |
| x9 | **Endurance** | The longest, fastest stage in the block |
| x0 | 👑 **Boss** | The hardest stage in the block |

### 2.6 New goal types

| Goal | Text | Notes |
|---|---|---|
| `ComboTimes(combo, times)` | "Reach a 10 combo 3 times" | |
| `Accuracy(percent)` | "Hit 80 % of your taps" | Constraint; replaces `MaxTaps` |
| `LivesLostAtMost(n)` | "Lose at most 1 life" | Constraint |
| `AllOf(goals…)` | "Pop 6 bombs and 6 golden" | Two goals in one |
| `LightningZaps(n)` | "Zap 15 bubbles with lightning" | |
| `CrackArmored(n)` / `BreakArmoredWithBlast(n)` | "Crack 8 armored bubbles" | |
| `ChainPairs(n)` | "Pop 6 chain pairs" | |
| `PopType` for the new types | "Pop 10 ghost bubbles" | Existing goal, new types |

### 2.7 Worlds and star gates

Five worlds of 20 stages, with a mini-boss at x10 and a world boss at x20.

| World | Stages | New things | Stars to unlock |
|---|---|---|---|
| ☁️ Sky | 1–20 | Basics, hearts, skulls, wind, **Clock** (11) | 0 |
| 🌊 Ocean | 21–40 | Golden (21), ice (23), **×2** (31) | 35 ★ of 60 |
| 🌌 Space | 41–60 | Bombs (41), **Lightning** (51) | 75 ★ of 120 |
| 🍬 Candy | 61–80 | Splitter (61), shield (63), **Armored** (71) | 115 ★ of 180 |
| ❄️ Frost *(new)* | 81–100 | **Ghost** (81), **Chain** (91) | 160 ★ of 240 |

The **Mystery bubble** is held back as the headline new feature of Phase 2 (stage 101+).

---

## 3. Bubbles

### 3.1 New special bubbles (helpful)

| Bubble | Effect | First stage |
|---|---|---|
| ⏱ **Clock** | +10 s on a stage with a time limit. Only spawns in stages where more time helps (not in "survive" stages). | 11 |
| ✖2 **×2** | All points doubled for 8 s; another ×2 adds 8 s more. A HUD badge counts down. | 31 |
| ⚡ **Lightning** | Zaps the 6 nearest bubbles (never skulls or bombs, so a zap can never set off a hidden skull). Each zap counts as a pop and keeps the combo going. Hits a boss for 3 damage and breaks armor in one hit. | 51 |

The existing helpers stay: ❤️ heart, 🛡 shield, ❄️ ice. In stages, every helper spawns rarely (2–4 %),
so boosters (§6) matter.

### 3.2 New challenge bubbles

| Bubble | Behaviour | Points | First stage |
|---|---|---|---|
| 🔩 **Armored** | Takes 2 taps: the first cracks it. A bomb or lightning breaks it in one. Escaping costs a life. | 5 | 71 |
| 👻 **Ghost** | Fades in and out every ~1.2 s; can only be popped while visible (a tap while faded is a miss). | 4 | 81 |
| 🔗 **Chain** | Two bubbles tied by a string. Pop both within 1.5 s for +10 bonus; otherwise the popped one re-forms. | 4 + 10 | 91 |
| ❓ **Mystery** | A random effect, good or bad | — | Phase 2 |

---

## 4. Stages 1–100

Columns: **Goal** = ★. **Time** = time limit (or about how long an untimed stage takes).
Bubble mixes add each world's new bubbles to the previous world's.

**All 100 stages are built and tuned (steps 2, 4 and 5).** Their tables are printed by the stage simulator
(`StageSimulatorTest.report`) from `StageCatalog.kt`, so they match the game.
From stage 41 the yardstick is an "improving" player (a casual player who
has got better), so third stars there ask for **90 % accuracy**.

What tuning showed: score stars don't separate good and casual players (slower players let bubbles
pile up into bigger combos), so most third stars ask for **85 % accuracy** or **no lives lost**.
Clocks spawn at 2 %: at 4 % they stretched stages by half.

### ☁️ World 1 — Sky (1–20)

Mix: normal, hearts, skulls (12 % → 25 %). Clocks (2 %) from 11 in timed stages. Bosses: Cloud King, Storm Cloud.

| # | Goal ★ | ★★ | ★★★ | Time | Lives | Pace |
|---|---|---|---|---|---|---|
| 1 | Pop 22 bubbles | Hit 90% of your taps | Finish within 25 seconds | ~18 s | 5 | P1 |
| 2 | Score 220 points | Score 340 points | Score 370 points | 45 s | 5 | P1 |
| 3 | Pop 8 hearts | Score 440 points | Hit 90% of your taps | 60 s | 5 | P1 |
| 4 | Survive 45 seconds | Lose no lives | Score 390 points | 45 s | 5 | P2 |
| 5 | Reach a 9 combo | Reach a 14 combo | Score 480 points | 60 s | 5 | P2 |
| 6 | Blow 10 skulls away with the wind | Blow 14 skulls away with the wind | Lose no lives | 60 s | 5 | P2 |
| 7 | Score 680 points | Score 1050 points | Score 1150 points | 60 s | 5 | P3 |
| 8 | Pop 23 small bubbles | Hit 80% of your taps | Pop 37 small bubbles | 60 s | 5 | P3 |
| 9 | Survive 60 seconds | Lose at most 1 life | Score 1350 points | 60 s | 5 | P3→P5 |
| 10 | Defeat the Cloud King | Finish within 85 seconds | Lose no lives | ~75 s | 5 | P2→P4 |
| 11 | Score 1600 points | Pop 3 clock bubbles | Hit 85% of your taps | 60 s | 5 | P3 |
| 12 | Pop 135 bubbles | Lose no lives | Finish within 68 seconds | ~70 s | 5 | P4 |
| 13 | Pop 8 hearts | Score 2000 points | Hit 85% of your taps | 75 s | 3 | P5 |
| 14 | Survive 75 seconds | Blow 33 skulls away with the wind | Hit 85% of your taps | 75 s | 4 | P4→P6 |
| 15 | Reach an 8 combo 5 times | Reach an 8 combo 7 times | Reach a 42 combo | 75 s | 5 | P4 |
| 16 | Score 1600 points | Score 2200 points | Hit 85% of your taps | 90 s | 5 | P4 |
| 17 | Pop 52 small bubbles | Hit 80% of your taps | Hit 90% of your taps | ~75 s | 5 | P4 |
| 18 | Survive 75 seconds | Lose no lives | Hit 85% of your taps | 75 s | 2 | P4 |
| 19 | Survive 120 seconds | Lose at most 2 lives | Score 3100 points | 120 s | 5 | P3→P4 |
| 20 | Defeat the Storm Cloud | Finish within 125 seconds | Lose at most 2 lives | ~110 s | 4 | P3→P5 |

### 🌊 World 2 — Ocean (21–40)

Adds golden (8 %), ice from 23 (4 %), ×2 from 31 (4 %). Bosses: Jelly Queen, Tide Kraken.

| # | Goal ★ | ★★ | ★★★ | Time | Lives | Pace |
|---|---|---|---|---|---|---|
| 21 | Pop 14 golden bubbles | Score 3100 points | Hit 85% of your taps | 75 s | 5 | P4 |
| 22 | Score 1150 points | Score 6100 points | Hit 85% of your taps | 90 s | 5 | P5 |
| 23 | Pop 11 ice bubbles | Pop 12 ice bubbles | Hit 85% of your taps | 75 s | 5 | P6 |
| 24 | Survive 90 seconds | Lose at most 1 life | Score 2600 points | 90 s | 4 | P4→P5 |
| 25 | Reach a 10 combo 5 times | Reach a 10 combo 6 times | Hit 85% of your taps | 90 s | 5 | P4 |
| 26 | Pop 72 bubbles | Pop 20 golden bubbles | Hit 85% of your taps | 110 s | 5 | P5 |
| 27 | Pop 13 golden bubbles | Hit 85% of your taps | Finish within 95 seconds | ~85 s | 5 | P4 |
| 28 | Score 1400 points | Score 3250 points | Hit 85% of your taps | 100 s | 2 | P5 |
| 29 | Survive 120 seconds | Lose at most 2 lives | Score 3300 points | 120 s | 5 | P4→P6 |
| 30 | Defeat the Jelly Queen | Finish within 80 seconds | Hit 85% of your taps | ~70 s | 4 | P3→P5 |
| 31 | Score 5700 points | Score 6000 points | Hit 85% of your taps | 90 s | 5 | P4 |
| 32 | Pop 14 golden bubbles | Score 8250 points | Hit 85% of your taps | 110 s | 5 | P5 |
| 33 | Pop 5 ice bubbles and pop 5 ×2 bubbles | Lose no lives | Hit 85% of your taps | 100 s | 5 | P6 |
| 34 | Survive 100 seconds | Blow 42 skulls away with the wind | Hit 85% of your taps | 100 s | 4 | P4→P6 |
| 35 | Reach an 8 combo 3 times | Reach a 10 combo 7 times | Reach a 43 combo | 100 s | 5 | P5 |
| 36 | Pop 180 bubbles | Finish within 100 seconds | Lose no lives | ~85 s | 5 | P5 |
| 37 | Pop 87 small bubbles | Hit 85% of your taps | Hit 92% of your taps | ~95 s | 5 | P5 |
| 38 | Score 1600 points | Score 5700 points | Lose no lives | 110 s | 2 | P6 |
| 39 | Survive 120 seconds | Lose at most 1 life | Score 8500 points | 120 s | 5 | P4→P7 |
| 40 | Defeat the Tide Kraken | Finish within 170 seconds | Lose at most 1 life | ~140 s | 4 | P3→P5 |

### 🌌 World 3 — Space (41–60)

Adds bombs (6 %, up to 14 % in bomb stages), lightning from 51 (4 %); skulls 20–25 %. Bosses: Nebula Core, Black Hole.
One bomb catches about 3–4 bubbles at these paces, so chain goals stay at 3–4.

| # | Goal ★ | ★★ | ★★★ | Time | Lives | Pace |
|---|---|---|---|---|---|---|
| 41 | Pop 16 bombs | Score 3000 points | Hit 90% of your taps | 90 s | 5 | P4 |
| 42 | Pop 5 bubbles with one bomb | Pop 6 bubbles with one bomb | Hit 90% of your taps | 100 s | 5 | P5 |
| 43 | Survive 100 seconds | Lose at most 1 life | Hit 90% of your taps | 100 s | 3 | P5→P6 |
| 44 | Score 7550 points | Score 7550 points | Hit 90% of your taps | 110 s | 5 | P6 |
| 45 | Reach a 15 combo 5 times | Reach a 15 combo 6 times | Hit 90% of your taps | 110 s | 5 | P5 |
| 46 | Pop 360 bubbles | Lose at most 1 life | Hit 90% of your taps | 140 s | 5 | P6 |
| 47 | Pop 75 small bubbles | Hit 90% of your taps | Hit 92% of your taps | ~85 s | 5 | P5 |
| 48 | Survive 110 seconds | Score 6050 points | Hit 90% of your taps | 110 s | 2 | P6 |
| 49 | Survive 150 seconds | Lose at most 2 lives | Hit 90% of your taps | 150 s | 4 | P6→P9 |
| 50 | Defeat the Nebula Core | Finish within 180 seconds | Lose at most 1 life | ~125 s | 4 | P3→P5 |
| 51 | Zap 59 bubbles with lightning | Zap 62 bubbles with lightning | Hit 90% of your taps | 100 s | 5 | P5 |
| 52 | Score 16550 points | Score 18450 points | Hit 90% of your taps | 120 s | 5 | P6 |
| 53 | Pop 8 bombs and pop 8 golden bubbles | Lose at most 1 life | Hit 90% of your taps | 120 s | 5 | P6 |
| 54 | Survive 120 seconds | Lose no lives | Hit 90% of your taps | 120 s | 2 | P6 |
| 55 | Reach a 46 combo | Reach a 15 combo 14 times | Hit 90% of your taps | 120 s | 5 | P6 |
| 56 | Pop 640 bubbles | Lose at most 1 life | Hit 90% of your taps | 150 s | 5 | P7 |
| 57 | Pop 18 golden bubbles | Hit 90% of your taps | Finish within 120 seconds | ~95 s | 5 | P6 |
| 58 | Score 16550 points | Score 18450 points | Hit 90% of your taps | 120 s | 2 | P6 |
| 59 | Survive 150 seconds | Lose at most 2 lives | Hit 90% of your taps | 150 s | 4 | P7→P9 |
| 60 | Defeat the Black Hole | Finish within 195 seconds | Hit 90% of your taps | ~165 s | 3 | P3→P5 |

### 🍬 World 4 — Candy (61–80)

Adds splitters (6 %), shields from 63 (3 %), armored from 71 (10 %). Bosses: Gumball Giant, Sugar Golem.

| # | Goal ★ | ★★ | ★★★ | Time | Lives | Pace |
|---|---|---|---|---|---|---|
| 61 | Pop 18 splitters | Score 9300 points | Hit 90% of your taps | 100 s | 5 | P5 |
| 62 | Score 7400 points | Score 10750 points | Hit 90% of your taps | 120 s | 5 | P6 |
| 63 | Pop 8 shields | Lose at most 1 life | Hit 90% of your taps | 110 s | 5 | P5 |
| 64 | Survive 120 seconds | Lose no lives | Hit 90% of your taps | 120 s | 2 | P6 |
| 65 | Reach a 15 combo 11 times | Reach a 15 combo 12 times | Hit 90% of your taps | 120 s | 5 | P6 |
| 66 | Pop 584 bubbles | Lose at most 1 life | Hit 90% of your taps | 150 s | 5 | P7 |
| 67 | Pop 173 small bubbles | Hit 90% of your taps | Hit 92% of your taps | ~110 s | 5 | P6 |
| 68 | Score 9900 points | Lose no lives | Hit 90% of your taps | 130 s | 2 | P6 |
| 69 | Survive 150 seconds | Lose at most 1 life | Hit 90% of your taps | 150 s | 3 | P7→P9 |
| 70 | Defeat the Gumball Giant | Finish within 195 seconds | Hit 90% of your taps | ~150 s | 3 | P3→P5 |
| 71 | Crack 35 armored bubbles | Crack 36 armored bubbles | Hit 90% of your taps | 110 s | 5 | P5 |
| 72 | Score 14850 points | Score 15000 points | Hit 90% of your taps | 130 s | 5 | P6 |
| 73 | Break 19 armored bubbles with a blast | Break 26 armored bubbles with a blast | Hit 90% of your taps | 130 s | 5 | P6 |
| 74 | Survive 130 seconds | Lose no lives | Hit 90% of your taps | 130 s | 2 | P7 |
| 75 | Reach a 50 combo | Reach a 25 combo 9 times | Hit 90% of your taps | 130 s | 5 | P7 |
| 76 | Pop 574 bubbles | Lose at most 1 life | Hit 90% of your taps | 150 s | 5 | P8 |
| 77 | Pop 131 small bubbles | Hit 90% of your taps | Hit 92% of your taps | ~95 s | 5 | P7 |
| 78 | Survive 120 seconds | Score 8550 points | Hit 90% of your taps | 120 s | 2 | P7 |
| 79 | Survive 150 seconds | Lose at most 1 life | Hit 90% of your taps | 150 s | 3 | P6→P8 |
| 80 | Defeat the Sugar Golem | Finish within 210 seconds | Lose at most 1 life | ~180 s | 3 | P3→P5 |

### ❄️ World 5 — Frost (81–100) *(new world)*

Adds ghosts (8 %, more in ghost stages), chain pairs from 91 (6 %); skulls 25–30 %. Bosses: Phantom, Frost Titan.
Ghosts and chain pairs cost a life if they escape, like normal and armored bubbles.

| # | Goal ★ | ★★ | ★★★ | Time | Lives | Pace |
|---|---|---|---|---|---|---|
| 81 | Pop 32 ghost bubbles | Pop 33 ghost bubbles | Hit 90% of your taps | 110 s | 5 | P5 |
| 82 | Score 12750 points | Score 16100 points | Hit 90% of your taps | 140 s | 5 | P7 |
| 83 | Pop 12 ghost bubbles and crack 10 armored bubbles | Lose at most 1 life | Hit 90% of your taps | 140 s | 5 | P7 |
| 84 | Survive 140 seconds | Lose no lives | Hit 90% of your taps | 140 s | 2 | P7 |
| 85 | Reach a 15 combo 8 times | Reach a 15 combo 14 times | Reach a 54 combo | 140 s | 5 | P7 |
| 86 | Pop 566 bubbles | Lose at most 1 life | Hit 90% of your taps | 150 s | 5 | P8 |
| 87 | Pop 37 ghost bubbles | Hit 90% of your taps | Hit 92% of your taps | ~130 s | 5 | P7 |
| 88 | Score 7000 points | Score 12850 points | Hit 90% of your taps | 140 s | 2 | P7 |
| 89 | Survive 150 seconds | Lose at most 1 life | Hit 90% of your taps | 150 s | 3 | P6→P8 |
| 90 | Defeat the Phantom | Finish within 210 seconds | Hit 90% of your taps | ~180 s | 3 | P3→P5 |
| 91 | Pop 18 chain pairs | Pop 23 chain pairs | Hit 90% of your taps | 120 s | 5 | P6 |
| 92 | Score 7600 points | Score 14800 points | Hit 90% of your taps | 150 s | 5 | P8 |
| 93 | Pop 8 chain pairs and pop 10 ghost bubbles | Lose at most 1 life | Hit 90% of your taps | 150 s | 5 | P8 |
| 94 | Survive 150 seconds | Blow 65 skulls away with the wind | Hit 90% of your taps | 150 s | 2 | P8 |
| 95 | Reach a 112 combo | Reach a 30 combo 6 times | Reach a 95 combo | 150 s | 5 | P8 |
| 96 | Pop 56 bubbles | Lose at most 1 life | Hit 90% of your taps | 150 s | 5 | P9 |
| 97 | Pop 116 small bubbles | Hit 90% of your taps | Hit 92% of your taps | ~85 s | 5 | P8 |
| 98 | Score 2750 points | Score 12050 points | Hit 90% of your taps | 150 s | 2 | P8 |
| 99 | Survive 150 seconds | Lose at most 1 life | Hit 90% of your taps | 150 s | 3 | P7→P8 |
| 100 | Defeat the Frost Titan | Finish within 240 seconds | Hit 90% of your taps | ~200 s | 3 | P3→P5 |

---

## 5. Bosses

Each fight has **3 phases** (above ⅔ HP, above ⅓, below ⅓). Each phase adds a trick, speeds the boss
up and spawns bubbles faster. Escaping bubbles still cost lives, so the player can't only tap the
boss. **Enrage:** if a fight runs 60 s past its design length, the boss gets faster and its trick
fires twice as often, so a fight can't be stalled. HP is set by calibration: about what a good player deals in the fight's design length.

Target: **30–50 % of players beat a boss on their first try** (a normal stage: 70–90 %).

| # | Boss | HP | Lives | Phase 1 | Phase 2 | Phase 3 | Length |
|---|---|---|---|---|---|---|---|
| 10 | Cloud King *(existing)* | 132 | 5 | Drifts slowly | Throws a skull every 4 hits | Skull every 2 hits, faster | ~1:15 |
| 20 | Storm Cloud *(new)* | 180 | 4 | Throws skulls | Gusts push every bubble sideways (swipe back) | Gusts + skulls, fast | ~1:50 |
| 30 | Jelly Queen *(existing)* | 80 | 4 | Stings every 6 hits | Stings every 5, longer lock | Stings every 4 + skulls | ~1:10 |
| 40 | Tide Kraken *(new)* | 320 | 4 | Moves in waves | Tentacle shield: immune until you pop an ice bubble | Shield + skulls rising from below | ~2:30 |
| 50 | Nebula Core *(rebuilt)* | 270 (blasts deal 8) | 4 | Drops bombs + skulls | **Armor: only bomb blasts hurt it** | Bombs + skulls, fast | ~2:30 |
| 60 | Black Hole *(new)* | 460 (lightning deals ×2) | 3 | Pulls bubbles toward itself | Stronger pull + skulls in pairs | Strongest pull, skulls faster | ~2:45 |
| 70 | Gumball Giant *(rebuilt)* | 140 → 3 × 45 → 9 × 16 (419) | 3 | Bounces | Splits into 3 | Splits into 9, each throws a skull | ~2:45 |
| 80 | Sugar Golem *(new)* | 460 | 3 | 3 armor plates (3 taps each) | **Heals 5 HP per escaped bubble** | Plates regrow + skulls | ~3:00 |
| 90 | Phantom *(new)* | 430 | 3 | Fades in and out (taps pass through while faded) | Fades more + ghost decoys | Mostly invisible, decoys + skulls; lightning shows it for 3 s | ~3:00 |
| 100 | Frost Titan *(new, grand)* | 210 + 3 shards × 18 | 3 | Ice armor plates | Freezes your taps (1.5 s) + skulls | Shatters into 3 shards that throw skulls | ~3:30 |

**Boss stars:** ★ win, ★★ within the time shown in the stage tables, ★★★ lose at most 1 life.

---

## 6. Boosters and ad slots (coins now, ads later)

Every slot below costs coins today. Each later becomes **"Watch an ad"**, with coins kept as the
alternative. Ads are rewarded only; no interstitials yet.

| Slot | What you get | Price now | Later |
|---|---|---|---|
| Pre-stage booster: **Shield** | Start the stage with a shield | 60 | Ad |
| Pre-stage booster: **Ice** | An ice bubble in the first 5 s | 40 | Ad |
| Pre-stage booster: **+1 life** | One extra life | 80 | Ad |
| Pre-stage booster: **Lightning** | A lightning bubble ready at the start (from stage 51) | 80 | Ad |
| Pre-stage booster: **Clock** | +15 s on a timed stage (from stage 11) | 40 | Ad |
| **Revive** | Continue once (as today) | 100 (was 50) | Ad or coins |
| **Double coins** on the result screen | ×2 that round's coins | — | Ad only |
| **Free daily chest** | One chest per day | Free | Ad for a 2nd |

Up to one of each booster per stage. Boosters unlock as their bubble is introduced.

---

## 7. Coins

**Target:** about 150–250 coins per day of normal play (15–20 min), a new cosmetic every 3–7 days,
and boosters as a steady coin sink.

| Source | Now | New |
|---|---|---|
| Stage, first clear | 10 per new ★ | 1★ 10 · 2★ +10 · 3★ +15 (max 35) |
| Stage replay (win) | 0 | 2 |
| Boss, first clear | Same as any stage | 100, +50 for 3★ |
| Endless & Daily Challenge | 1 per 10 points | 1 per 50 points, max 100 per round |
| Daily login (week) | 10 → 100 (240) | 5, 10, 15, 20, 25, 30, 75 (180) |
| Missions | 25–80 | 10–30 |
| Achievements | 100–500 | Halved (50–250) |
| Star chest | Every 10 ★: 50 coins, 30 % item | Every 15 ★: 25 coins, 20 % item |
| Daily Stage chest | 1 per day | Unchanged |

**First clears of stages 1–100 pay ~4,700 coins in all** (90 stages × up to 35, 10 bosses × up to 150).

**Shop:** prices go from 150–800 to **600–5,000**, and ~15 items are added (skins, pop effects,
backgrounds, a boss-themed set per world), so there's always something to save for.

| Rarity | Price |
|---|---|
| Common | 600–900 |
| Rare | 1,200–1,800 |
| Epic | 2,500–3,500 |
| Legendary | 4,500–5,000 |

**Existing saves:** progress and coins are reset when this ships, since old stars and coins don't
match the new stages and prices.

---

## 8. Build order

| Step | Work | Needs |
|---|---|---|
| 1 ✅ | Stage engine: pace levels and waves, new goal types, accuracy instead of tap limits, simulator test | Code only |
| 2 ✅ | Stages 1–40 rewritten; Clock and ×2 bubbles; Storm Cloud and Tide Kraken bosses; boss phases and enrage | Drawn in code; bosses share one look |
| 3 ✅ | Economy: new coin rates, shop prices, boosters bought with coins, revive 100 | Drawn in code (skins, pop effects, backgrounds) |
| 4 ✅ | Stages 41–80: Lightning and Armored; Nebula Core and Gumball Giant rebuilt; Black Hole and Sugar Golem; Lightning booster | Drawn in code; bosses share one look |
| 5 ✅ | Frost world, stages 81–100: Ghost and Chain; Phantom and Frost Titan | Drawn in code (scenery, colours, music) |
| 6 | Playtest and tune on a phone; then ads (AdMob) replace the coin prices | AdMob account |

Phase 2 (101–300) and Phase 3 (301–500) reuse this engine: returning worlds as harder variants,
the Mystery bubble, new bosses, and the same 10-stage shape.
