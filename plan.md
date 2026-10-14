# BubblePopping — Game Roadmap

Goal: turn BubblePopping from a single endless round into a game players **want to come back to** —
great-feeling pops, hand-designed stages with stars to chase, and long-term progression.

Two modes after this roadmap:

- **Stages** — hand-designed levels with objectives and 1–3 stars, grouped into themed worlds.
- **Endless** — today's game (Easy / Medium / Hard), chasing a personal best.

---

## Table of contents

1. [Design principles](#1-design-principles)
2. [Architecture](#2-architecture)
3. [Phase 0 — Cleanup & known issues](#phase-0--cleanup--known-issues)
4. [Phase 1 — Foundation](#phase-1--foundation)
5. [Phase 2 — Game feel ("juice")](#phase-2--game-feel-juice)
6. [Phase 3 — Stage system core](#phase-3--stage-system-core)
7. [Phase 4 — Stage map & World 1](#phase-4--stage-map--world-1)
8. [Phase 5 — New bubble types & worlds 2–4](#phase-5--new-bubble-types--worlds-24)
9. [Phase 6 — Boss stages](#phase-6--boss-stages)
10. [Phase 7 — Endless mode upgrades](#phase-7--endless-mode-upgrades)
11. [Phase 8 — Meta progression & retention](#phase-8--meta-progression--retention)
12. [Phase 9 — Polish](#phase-9--polish)
13. [Game UI — "Bubble Glass"](#game-ui--bubble-glass)
14. [Phase 10 — Monetization (optional)](#phase-10--monetization-optional)
15. [Testing strategy](#testing-strategy)
16. [Content reference](#content-reference)
17. [Open questions](#open-questions)

---

## 1. Design principles

- **Feel first.** A great 30-second loop beats any number of features. Every pop must feel rewarding.
- **One new idea at a time.** Each world introduces exactly one new mechanic.
- **Fail fast, retry faster.** "Play Again" is one tap and instant. Show "12 points from your best!".
- **Stages are data, not code.** Adding a stage = adding a `StageConfig`, never new game logic.
- **One event stream.** Combos, objectives, missions and stats all consume the same `GameEvent`s.
- **Fair, not predatory.** No energy timers, no pay-to-win, no forced ads between rounds.

---

## 2. Architecture

### 2.1 Target package layout

```
com.apps.bubblepopping
├── data/
│   ├── AppContainer.kt           // app-wide dependencies (+ android/ios factories)
│   ├── ProgressRepository.kt     // stars, bests, coins, unlocks (persisted)
│   ├── db/                       // Room: AppDatabase, entities, DAOs
│   └── prefs/                    // DataStore: SettingsRepository
├── game/
│   ├── GameEvent.kt              // Popped, Missed, ComboChanged, Tick, GameOver…
│   ├── SpawnConfig.kt            // all tuning numbers (was hardcoded in ViewModel)
│   ├── ComboTracker.kt
│   ├── ObjectiveTracker.kt
│   ├── WaveDirector.kt
│   └── GameRules.kt              // scoring, lives, bubble-type behaviour
├── stage/
│   ├── StageConfig.kt
│   ├── Objective.kt
│   ├── World.kt
│   └── StageCatalog.kt           // all stage definitions
├── navigation/
├── view/
│   ├── home/  play/  leaderboard/
│   ├── map/                      // stage map + stage card
│   ├── result/                   // result screen with stars
│   └── meta/                     // Today + Shop screens (Phase 8)
├── meta/                         // coins, missions, chests, achievements, cosmetics, daily stage (Phase 8)
└── platform/                     // HapticFeedback, Sound (expect/actual)
```

### 2.2 Core data model

```kotlin
data class SpawnConfig(
    val spawnInterval: Float,          // seconds
    val minSpawnInterval: Float,
    val spawnRampPerPoint: Float,
    val minSpeed: Float,               // fraction of screen height / second
    val maxSpeed: Float,
    val maxSpeedCap: Float,
    val typeWeights: Map<BubbleType, Int>,
    val windEnabled: Boolean = true,
)

data class StageConfig(
    val id: Int,
    val world: World,
    val durationSec: Int?,             // null = no time limit
    val lives: Int,
    val spawn: SpawnConfig,
    val goal: Objective,               // required for 1★
    val bonusGoals: List<Objective>,   // 2★ and 3★
    val seed: Long? = null,            // fixed seed → deterministic stage
    val introText: String,
)

sealed interface Objective {
    data class ReachScore(val target: Int) : Objective
    data class PopType(val type: BubbleType, val count: Int) : Objective
    data class PopCount(val count: Int) : Objective
    data class ReachCombo(val combo: Int) : Objective
    data class Survive(val seconds: Int) : Objective
    data object NoMisses : Objective
    data object NoLivesLost : Objective
    data class MaxTaps(val taps: Int) : Objective
    data class FinishUnder(val seconds: Int) : Objective
    data class PopSize(val size: BubbleSize, val count: Int) : Objective
    data class BombChain(val bubbles: Int) : Objective
    data class WindSaves(val count: Int) : Objective
    data class DefeatBoss(val bossId: String) : Objective
}

sealed interface GameEvent {
    // Implemented (game/GameEvent.kt)
    data class Popped(val bubble: Bubble, val points: Int, val combo: Int) : GameEvent
    data class Missed(val bubble: Bubble, val lifeLost: Boolean) : GameEvent
    data class Tapped(val hit: Boolean) : GameEvent
    data class ComboChanged(val combo: Int, val previous: Int, val broken: Boolean) : GameEvent
    data class Tick(val elapsedSec: Int) : GameEvent
    data class GameOver(val reason: GameOverReason, val score: Int, val isNewBest: Boolean) : GameEvent

    // Added by later phases
    data class BombExploded(val chainSize: Int) : GameEvent    // Phase 5
    data class WindSave(val bubble: Bubble) : GameEvent        // Phase 5
    data class WaveCleared(val wave: Int) : GameEvent          // Phase 7
    data class WaveStarted(val wave: Int) : GameEvent          // Phase 7
}
```

### 2.3 Event flow

```
BubbleGameViewModel (frame loop, physics)
        │  owns ComboTracker — the multiplier must apply at the moment of the pop,
        │  so combos are computed in the ViewModel, not by an event listener
        │ emits GameEvent
        ├──► Play screen       → sounds and haptics (Phase 2)
        ├──► ObjectiveTracker  → goal progress, stars, win/lose (Phase 3)
        ├──► MissionTracker    → daily/active missions (Phase 8)
        └──► StatsRecorder     → lifetime stats (Phase 8)
```

Endless mode is just a `StageConfig` with no goal and no time limit.

---

## Phase 0 — Cleanup & known issues

Small fixes found while reviewing the current code.

**Status: done** (Android build and unit tests pass; iOS code not yet compiled on a Mac).

- [x] **Unit scaling** — bubble radius (30–68 px), hit radius (68 px) and speeds are raw pixels.
      Convert to dp (radius/hit area) and fraction-of-screen-height (speed) so gameplay is
      identical on every density. *(Done in Phase 1.)*
- [x] `HomeContent` ignored its `modifier`. It now uses it; the unused `Scaffold` was removed so the
      background stays edge-to-edge (insets were already handled inside `HomeContent`).
- [x] Home top bar:
  - Profile picture opens a new **Profile** screen (`view/profile/ProfileScreen.kt`, `Routes.Profile`)
    showing games played, bubbles popped and best score per difficulty.
  - Hardcoded `i.pravatar.cc` URL replaced by a default avatar (`drawable/ic_avatar.xml`), also used
    when an image fails to load. Google/Facebook photo comes with sign-in (§8.7).
  - Close button is now optional (`onClose`) and hidden on Home. (iOS apps can't close themselves.)
- [x] System back on the play screen: playing → pause; paused → leave; game over → leave.
      The pause screen also has a **Quit** button, because iOS has no back button.
- [x] iOS sound: a pool of `AVAudioPlayer`s used in rotation, so rapid pops overlap
      (4 per sample since Phase 2).
- [x] `MainActivity`: `HapticFeedback(this)` is re-created on every recomposition → `remember` it.
- [x] Rotating the phone restarted the round (`LaunchedEffect` re-ran `applyDifficulty`). Fixed in
      Phase 1: the difficulty is now a ViewModel constructor argument.
- [x] Remove unused imports in `PlayScreenRoot.kt` (only `bg` was unused; both `Canvas` imports are used).
- [x] Old `plan.md` content is obsolete (replaced by this file).

---

## Phase 1 — Foundation

Everything else depends on this.

**Status: done** (Android build and unit tests pass; not yet played on a device or built for iOS).

### 1.1 Density-independent gameplay
- [x] `game/SpawnConfig.kt` holds spawn/speed tuning; `game/GameRules.kt` holds fixed rules
      (lives, sizes in dp, wobble, breeze).
- [x] Radii, hit area and wobble in dp → px using the density passed to `setCanvasBounds`.
- [x] Speeds are fractions of canvas height per second; breeze is a fraction of canvas width.
- [x] `Difficulty.spawn` points to the EASY/MEDIUM/HARD `SpawnConfig` presets (multipliers removed).
- [x] Bubbles carry a `BubbleSize` (SMALL/MEDIUM/LARGE = 1/3/5 pts), ready for `PopSize` goals.
- [x] Values were converted from the old pixel numbers for a ~1080×2340 screen at density 2.75,
      so the game should look the same as before on a typical phone.
- **Done when:** a round feels the same on a 1080p and a 1440p device. *(Covered by a unit test;
  still to confirm by playing.)*

### 1.2 Persistence (local only — online backend comes later)
Use both KMP-ready Jetpack libraries, each for what it does best:

- [x] **Room 2.8.5 (KMP)** with `sqlite-bundled` + KSP; `AppDatabase` in `data/db/`,
      schema exported to `composeApp/schemas/`.
  - [x] `StageProgressEntity` — stageId, stars, bestScore, completedAt
  - [x] `EndlessScoreEntity` — difficulty, score, bubblesPopped, playedAt
  - [x] `StatsEntity` — gamesPlayed, bubblesPopped, bestCombo (single row)
  - [ ] `MissionEntity` — added in Phase 8 (with a Room migration)
  - [ ] `CosmeticEntity` — added in Phase 8 (with a Room migration)
- [x] **DataStore Preferences 1.2.1 (KMP)** — `data/prefs/SettingsRepository.kt`:
  - [x] settings: sound, music, haptics (sound and haptics already respected in play)
  - [ ] wind controls setting — Phase 5
  - [ ] coins — Phase 8
  - [ ] tutorial-seen flag — Phase 9; last daily-claim date, login streak — Phase 8
- [x] Two repositories instead of one: `ProgressRepository` (Room) and `SettingsRepository`
      (DataStore), both interfaces, held by a process-wide `AppContainer`
      (`appContainer(context)` on Android, `iosAppContainer` on iOS).
- [x] ViewModels get them through `viewModel { … }` factories in `NavGraph`.
- [x] Endless rounds are saved at game over; the game-over screen shows **BEST** or **NEW BEST!**.

### 1.3 Game events
- [x] `game/GameEvent.kt` and `BubbleGameViewModel.events: SharedFlow<GameEvent>`.
- [x] Emits `Tapped`, `Popped`, `Missed(lifeLost)`, `Tick` (each second) and `GameOver(reason, score)`.

### 1.4 Deterministic randomness
- [x] Optional `seed` ViewModel argument: same seed → same round, and Play Again replays it.

### 1.5 Tests
- [x] `commonTest/BubbleGameViewModelTest` (13 tests): seeding, density independence, scoring,
      events, lives, poison, best score, ticks, pause. Run with `./gradlew :composeApp:testAndroidHostTest`.
- [ ] `RoomProgressRepository` test — needs an in-memory Room database on a device or iOS
      simulator (JVM host tests have no Android `Context`).

---

## Phase 2 — Game feel ("juice")

The cheapest, highest-impact work in this plan.

**Status: done** (Android build and 27 unit tests pass; not yet played on a device, iOS code
not yet compiled on a Mac). All tuning numbers live in `game/GameRules.kt`, colours in `view/Color.kt`.

### 2.1 Combo system
- [x] `game/ComboTracker.kt`: each pop within **0.8 s** (game time) of the previous one extends the combo.
- [x] Multiplier tiers: ×1 (1–2), ×2 (3–5), ×3 (6–9), ×5 (10+); applied to the points of each pop.
- [x] Milestone text at the pop: "NICE!" (5), "GREAT!" (10), "PERFECT!" (15), "INSANE!" (25).
- [x] HUD combo meter: gold ring around the score drains over the window; "×N" shown beside it.
- [x] An escaped normal bubble or a poison pop breaks the combo (`ComboChanged(broken = true)`).
- [x] Best combo saved in `StatsEntity.bestCombo` and shown on the Profile screen.

### 2.2 Audio
- [x] **Rising pitch:** `1.0 + min(combo, 12) × 0.05` (Android `SoundPool` rate, iOS `AVAudioPlayer.rate`).
- [x] The 3 pop samples (`pop_1..3.mp3`) are all loaded and picked at random.
- [x] Heart, poison, combo-break (combos of 3+) and new-best sounds — **placeholders made by
      pitch-shifting the pop sample** (`GameSounds` in `Sound.kt`).
- [ ] Record or source real heart-chime, poison-thud, combo-break and fanfare sounds, then swap
      them into `GameSounds`.

### 2.3 Visual feedback
- [x] **Floating score text** ("+5", "+15 ×3") rises and fades from each pop (Baloo font).
- [x] **Screen shake** (play field only, not the HUD): large pop 2 dp, milestone 3 dp, poison 4 dp.
- [x] **Hit-stop:** 50 ms freeze on combo milestones.
- [ ] Hit-stop on poison pop — the round ends instantly on poison, so this moves to the Phase 9
      game-over sequence (slow-mo).
- [x] **Near-miss warning:** normal bubbles in the top 15% of the play area pulse red.
- [x] Bursts grow wider and thicker with the combo (up to combo 10).
- [x] Bursts, floating text and shake keep animating after game over instead of freezing.

### 2.4 Haptics
- [x] `HapticFeedback`: `light()` small pop, `medium()` medium/large pop and heart, `heavy()` poison
      and game over, `success()` new best. Android uses the system's predefined effects on 10+.
- [x] Sound and haptics settings are respected.
- [x] Sound and haptics are driven by `GameEvent`s on the play screen, not by the tap handler.

**Done when:** popping a fast chain of bubbles feels noticeably satisfying with sound off *and* on.
*(Needs a playtest on a real device.)*

---

## Phase 3 — Stage system core

**Status: done** (Android build and 47 unit tests pass; not yet played on a device, iOS not yet
compiled on a Mac).

### 3.1 Model
- [x] `stage/StageConfig.kt`, `stage/Objective.kt`, `stage/World.kt`, `stage/StageResult.kt`.
- [x] Objectives implemented: `ReachScore`, `PopCount`, `PopType`, `PopSize`, `ReachCombo`, `Survive`,
      and constraints `NoMisses` (every tap hits), `NoLivesLost`, `MaxTaps`, `FinishUnder`.
      `BombChain`, `WindSaves` (Phase 5) and `DefeatBoss` (Phase 6) were added with their mechanics.
- [x] `stage/StageCatalog.kt` — Kotlin list of all stages; World 1's 10 stages are in (see §4.3).
- [x] Unlocking: stage 1 always open; every other stage needs ≥1★ on the previous one.
- [x] `StageCatalogTest` guards the data: sequential ids, reachable goals, timers that fit.

### 3.2 ObjectiveTracker
- [x] `game/ObjectiveTracker.kt` consumes `GameEvent`s; `progress()` gives `current / target` per objective.
- [x] How a stage ends:
  - **Untimed:** as soon as the main goal is met (so bonus objectives there must be constraints).
  - **Timed:** when the timer runs out, or earlier once *every* objective is met.
  - Running out of lives or popping poison ends it immediately.
- [x] Stars: 0 if the main goal is unmet (= failed), else 1 + met bonus objectives.
      Stars are judged at the moment the stage ends, whatever ended it.

### 3.3 Gameplay integration
- [x] `BubbleGameViewModel(mode: GameMode, …)` runs Endless or a stage (no separate `start()` —
      the mode is a constructor argument, which keeps rotation safe).
- [x] Countdown timer when `durationSec != null`; `GameOverReason.TIME_UP` / `GOAL_COMPLETE`.
- [x] Lives come from `StageConfig.lives` (default 5).
- [x] Results saved with `recordStageGame()`: cleared stages keep best stars/score; every attempt
      counts towards lifetime stats.

### 3.4 UI
- [x] **Stage intro card** (`view/stage/StageIntroScreen.kt`): stage number, earned stars, tip text,
      goal, timer, bonus objectives with ☆, best score, PLAY.
- [x] **Goal bar under the HUD** (`StageGoalBar`): timer pill (red under 10 s) and goal pill
      "Pop 10 bubbles 7 / 10" with progress bar; turns green when met.
- [x] **Result overlay** (`StageResultOverlay`): STAGE CLEAR / FAILED, stars spring in one by one
      with a rising pop sound + haptic each, score counts up, ✓/✗ per objective, NEW BEST / BEST.
      Buttons: Next (cleared + next exists) / Retry / Stages.
- [x] Failed screen shows "12 / 15 — so close!" when the goal was ≥60% done; Retry is primary.
- [ ] Coins earned on the result — Phase 8.
- [x] Total stars on the Profile screen.

### 3.5 Navigation
- [x] Routes: `Stages` (the map since Phase 4), `StageIntro(stageId)`,
      `Play(mode: GameMode)`. The result is an overlay on Play, not a separate route.
- [x] Flow: Home → Stages → StageIntro → Play (replaces the card) → Next → StageIntro of the next stage.
- [x] `GameMode`: `Stage(stageId)` | `Endless(difficulty)`. `Daily` is added in Phase 8.
- [x] ~~Temporary STAGES button on Home~~ — replaced by the Phase 4 home redesign.

---

## Phase 4 — Stage map & World 1

**Status: done** (Android build and 51 unit tests pass; the map has not yet been seen on a
device — check layout and scrolling on a phone; iOS not yet compiled on a Mac).

### 4.1 Home changes
- [x] Home: logo, ribbon and two big buttons — **STAGES** (blue) and **ENDLESS** (orange), in the Baloo font.
- [x] **ENDLESS** opens `view/endless/EndlessScreen.kt` (`Routes.Endless`) with the unchanged
      three-card difficulty pager, now with a back button.
- [ ] The Leaderboard screen (`Routes.Ranking`) has no entry point (it had none before either).
      Add one when the Local tab uses real data (§8.6).

### 4.2 Map screen
- [x] `view/stage/StageMapScreen.kt` replaces the temporary grid. Vertical scroll, stage 1 at the
      bottom, nodes swinging left/right on a sine wave, joined by a curved road drawn on Canvas.
- [x] Node states: locked 🔒 (grey, not tappable), current (blue ring, pulsing), completed (gold ring) with 1–3 ★.
- [x] The road is dashed in gold up to the last unlocked stage.
- [x] Opens scrolled so the current stage is mid-screen (waits for saved progress, so it doesn't jump).
- [x] Star counter at the top; a banner below each world's first stage ("☁️ Sky World",
      or "🔒 ★ 20 needed" while locked).
- [x] Teaser at the top for the next world without stages: "🌊 Ocean World — Coming soon · ★ 20 needed".
- [x] Themed background per world (gradient + soft cloud puffs); map colours in `view/Color.kt`.
- [x] World gating: a stage also needs its world unlocked (`World.starsToUnlock` total stars).
- [x] `StageCatalog.currentStageId()` — first open stage without stars, else the last open one.

### 4.3 World 1 — Sky ☁️ (tutorial world)

| # | Main goal | Bonus goals (2★ / 3★) | Introduces |
|---|---|---|---|
| 1 | Pop 10 bubbles | No misses · Under 20 s | Tapping (tutorial) |
| 2 | Score 30 in 45 s | Score 45 · No lives lost | Size → points |
| 3 | Pop 3 hearts | Score 40 · No misses | Hearts heal |
| 4 | Survive 30 s | No lives lost · Score 50 | Poison |
| 5 | Reach a 4 combo (60 s) | 6 combo · Score 60 | Combos |
| 6 | Blow 3 skulls away with the wind (60 s) | No lives lost · Score 50 | Wind |
| 7 | Score 80 in 60 s | 6 combo · No lives lost | Mixed |
| 8 | Pop 25 small bubbles | Max 35 taps · No misses | Precision |
| 9 | Survive 60 s (fast spawns) | No lives lost · Score 120 | Pressure |
| 10 | 👑 **Boss: Cloud King** | Under 60 s · No lives lost | Bosses (Phase 6) |

- [x] All 10 stages are in `StageCatalog.kt` (added in Phase 3 so stage mode is playable).
- Changes from the original table: stage 4's "avoid all poison" became "no lives lost" (popping
  poison already ends the round); stage 6 used a pop goal until wind saves arrived in Phase 5.
- Stage 10 is the Cloud King boss (Phase 6).
- [ ] Playtest and tune targets/timers so each stage is clearable and 3★ is a stretch.

**Done when:** a new player can go from Home → Map → finish World 1 with stars saved across app restarts.

---

## Phase 5 — New bubble types & worlds 2–4

Each world introduces one new bubble and is gated by stars.

### 5.1 New bubble types

| Bubble | Look | Behaviour | Points |
|---|---|---|---|
| ⭐ **Golden** | Gold scheme, sparkle trail | Small, fast, rare | 25 |
| ❄️ **Freeze** | Ice-blue, frost rim | Slows everything to 40% for 3 s, blue screen tint | 3 |
| 💣 **Bomb** | Dark red, fuse spark | Pops all bubbles in radius (incl. poison — careful!) | 5 + chain |
| 🔗 **Splitter** | Segmented rim | Splits into 3 small bubbles | 3 |
| 🛡️ **Shield** | Silver ring | Next poison pop is absorbed | 3 |

**Status: done** (Android build and 65 unit tests pass; not yet played on a device, iOS not yet
compiled on a Mac). Tuning numbers in `game/GameRules.kt`.

- [x] All five added to `BubbleType`, each with a `BubbleScheme` in `BubbleCanvas.kt` and behaviour
      in the ViewModel; points in `GameRules.basePoints()`.
- [x] Icons are **drawn in code** (star, snowflake, bomb, three dots, shield) — no image assets needed.
      Swap for artwork later if wanted.
- [x] Golden: always small, rises 1.5× faster, 25 pts × combo multiplier.
- [x] Freeze: bubbles and spawning run at 40% for 3 s (the stage clock doesn't slow); light-blue screen tint.
- [x] Bomb: pops every bubble whose edge is within 95 dp. Caught bubbles score with the bomb's multiplier
      (they don't extend the combo) and trigger their own effects; caught bombs explode too (chain).
      "CHAIN ×N +pts" text, big shake, `BombExploded(chainSize)`. A skull in the blast ends the
      round unless a shield is up.
- [x] Splitter: replaced by 3 small normal bubbles spread sideways at the same height.
- [x] Shield: absorbs the next skull — tapped or caught in a blast ("SHIELD!", `ShieldUsed` event).
- [x] HUD badges next to the lives: 🛡 while a shield is ready, "❄ 3" while frozen.
- [x] Placeholder sounds (pitch-shifted pops) for golden, ice, bomb, shield and wind saves.

### 5.2 Worlds

| World | Theme | New mechanic | Unlock |
|---|---|---|---|
| 1 ☁️ Sky | Day sky | Basics, combos, wind | — |
| 2 🌊 Ocean | Underwater, slower sway | Golden + Freeze | 20 ★ |
| 3 🌌 Space | Stars, low "gravity" drift | Bomb + chain reactions | 45 ★ |
| 4 🍬 Candy | Pastel, chaotic | Splitter + Shield | 75 ★ |

- [x] 10 stages per world (stages 11–40 in `StageCatalog.kt`): introduce → practice → combine → finale.
      Each world's spawn mix adds its new bubbles to the previous one; late stages use a
      `VERY_HARD` speed preset.
- [x] Stages 20 / 30 / 40 are the Jelly Queen, Nebula Core and Gumball Giant bosses (Phase 6).
- [x] Bubble palette per world (normal bubbles' rainbow rims), and the world's map colours
      washed over the play background.
- [x] Map shows all four worlds with their banners and star gates (no "coming soon" teaser any more).
- [x] New objectives: `BombChain(n)` (one explosion pops n), `WindSaves(n)`.
- [x] `StageCatalogTest` now also checks that every objective's bubble type actually spawns in
      that stage, and that each world's stars can unlock the next.
- [x] **Music tracks per world** — synthesised placeholders (Phase 9); swap for recordings later.
- [ ] Playtest and tune stages 11–40.

### 5.3 Wind rework
- [x] **Swipe gesture:** a press that misses every bubble and moves sideways > 24 dp becomes wind;
      swipe distance sets strength (30% of the screen width = full). A press released without
      swiping is a missed tap (so swipes don't break "Don't miss a tap").
- [x] Wind buttons kept, behind a new `windButtonsEnabled` setting (default on; toggle comes with
      the Phase 9 settings screen).
- [x] **Wind save** (redefined): wind can't push bubbles back down, so a wind save is blowing a
      **skull off the side of the screen** — it's gone for good (`WindSave` event). All other bubbles
      now stop at the screen edges (before, wind could push them out of reach to escape and cost a life).
- [x] Wind is ~3× stronger (`BREEZE_SPEED` 0.148 → 0.45 widths/s) so a gust can actually clear a skull.
- [x] Sky stage 6 now teaches it: "Blow 3 skulls away with the wind".

---

## Phase 6 — Boss stages

Every 10th stage.

**Status: done** (Android build and 75 unit tests pass; not yet played on a device, iOS not yet
compiled on a Mac). Data in `stage/Boss.kt`, tuning in `game/GameRules.kt`.

- [x] Boss = a giant bubble (70 dp radius) drifting side to side and bobbing in the upper play area,
      with an **HP ring** (green → red), a crown, and the world's colours.
- [x] Each tap deals 1 damage (+2 pts × combo, extends the combo); boss shrinks to 65% as HP drops,
      flashes white and wobbles on hit; cracks appear at ⅔ and ⅓ HP.
- [x] Phases: moves 1.4× faster below ⅔ HP and 1.8× below ⅓. Normal bubbles keep spawning (slower than
      usual) so lives still matter.
- [x] Bomb blasts deal 3 damage to any boss they reach (any world).
- [x] Defeat: +100 pts, big burst, heavy shake, confetti, "CLOUD KING DEFEATED!", then 1.5 s of slow
      motion before the result ("BOSS DEFEATED!").
- [x] Taps hit bubbles before the boss (a skull in front of a boss is still a skull).
- [x] Boss stages show 👑 on the map; the goal bar shows damage dealt / total HP.
- [x] Boss stages are untimed with bonuses "finish within N s" and "lose no lives".

| World | Boss | Gimmick (as built) | HP |
|---|---|---|---|
| Sky | Cloud King | Every 3rd hit throws a skull out of its top | 20 |
| Ocean | Jelly Queen | Every 5th hit stings: taps do nothing for 1 s (and don't count as misses) | 26 |
| Space | Nebula Core | Below ⅓ HP drops 4 bombs — whose blasts hurt it | 32 |
| Candy | Gumball Giant | Splits into 3, then each into 3 more (9) | 12 + 3×4 + 9×1 = 33 |

- [x] Beating a boss unlocks that world's **cosmetic skin** (Cloud, Jelly, Galaxy, Candy — Phase 8).
- [ ] Playtest boss HP, speeds and gimmick frequencies.

---

## Phase 7 — Endless mode upgrades

Endless keeps the **three difficulty cards** (Easy / Medium / Hard) for now. Whether it later
becomes a single Endless mode is still open — build waves so they work either way.

**Status: done** (Android build and 98 unit tests pass; not yet played on a device, iOS not yet
compiled on a Mac). Waves in `game/WaveDirector.kt`, unlock rules in `game/EndlessUnlocks.kt`.

- [x] **Waves:** 20 s of rising pressure → 3 s calm (nothing spawns) with a "WAVE N CLEAR / WAVE N+1"
      banner → harder wave. `WaveDirector` replaces the score ramp in Endless (stages keep it):
      pressure is a level that rises smoothly through a wave (+1 per wave) and the next wave starts
      where the last one peaked. Spawn interval ×0.85 per level, top speed +15% of base per level,
      both clamped by the difficulty's `minSpawnInterval` / `maxSpeedCap` (reached around wave 6–8).
      Works the same for all three cards, or for a single Endless mode later.
- [x] HUD pill under the score: "WAVE N" with a progress bar ("GET READY" during the calm).
      `WaveCleared(wave)` / `WaveStarted(wave)` events (sound + haptics now; missions later).
- [x] **New bubble types join Endless once unlocked in Stages:** a special type is unlocked when the
      first stage that spawns it has ≥1★. Endless weights next to 75 / 15 / 10: golden 6, ice 4,
      bomb 5, splitter 6, shield 3.
- [x] **Persistent best per difficulty** on the Endless cards ("★ BEST 123" / "No best yet").
- [x] **Unlock difficulties:** Hard is locked (dimmed card, 🔒 and how to unlock it) until 150 on
      Medium or beating the Cloud King (stage 10). The round that unlocks it shows "🔓 HARD UNLOCKED!".
- [x] **Endless game-over sequence:** bubbles drift on in slow motion for 0.8 s → card fades in →
      score counts up → "NEW BEST!" (with confetti) or "X from your best!" / "You tied your best!" →
      big **Play Again** button. Also shows the wave reached and why the round ended.
- [ ] Playtest wave length, calm length and per-wave ramp on each difficulty.

---

## Phase 8 — Meta progression & retention

**Status: done except §8.7** (Android build and 130 unit tests pass; not yet played on a device,
iOS not yet compiled on a Mac). Rules are pure Kotlin in `meta/` (`MetaRules`, `Missions`,
`Rewards`, `Cosmetics`, `DailyStageGenerator`); `data/MetaRepository.kt` only loads and saves
around them. Room is now **version 2** with an auto-migration that only adds the new tables
(`player`, `missions`, `owned_cosmetics`, `achievements`), so existing progress is kept.
Player name and equipped cosmetics are in DataStore. "Today" uses the device's local date
(`kotlinx-datetime`), so daily content resets at local midnight.

Home now has a coin counter and **Today** 📅 (red dot while the login reward is unclaimed),
**Shop** 🛍️ and **Ranks** 🏆 buttons. Result screens list what the round earned.

### 8.1 Currency
- [x] **Coins** from: each stage star the first time it's earned (10), Endless and Daily Challenge
      score (score / 10), missions (25–80), daily login, star and Daily Stage chests (50), achievements.

### 8.2 Missions
- [x] 3 active missions, each of a different kind; completed ones pay out at the end of the round
      and are replaced. 12 kinds — e.g. "Pop 3 golden bubbles in one round", "Reach a 10 combo",
      "Pop 300 bubbles", "Earn 3★ on any stage", "Survive 90 s in Endless Hard", "Reach wave 4".
- [x] Missions only ask for what the player has: bubble-type missions need the type unlocked,
      Hard missions need Hard unlocked.
- [x] A `RunTracker` consumes the same `GameEvent` stream in every mode and builds the round's
      `RunStats`; missions and achievements read those (no separate `MissionTracker`).

### 8.3 Daily content
- [x] **Daily Challenge:** one Endless round per day seeded with the date — Medium pace, waves,
      classic bubble mix — so every player gets the same round. Today's best is shown on Today and
      on its game-over screen. Becomes the **Global** leaderboard tab once a backend exists.
- [x] **Daily Stage:** generated from the date and the player's newest open world: a 60 s stage
      using one of that world's stage bubble mixes, with a random goal (score, pops, combo, survive,
      or popping a special bubble that mix spawns) plus "score N" and "lose no lives" bonus stars.
      The first clear each day wins a chest.
- [x] **Daily login reward:** 10 / 15 / 20 / 25 / 30 / 40 / **100** coins over a 7-day week; a popup on
      Home and a calendar on Today. Missed days are never lost — the week just continues.

### 8.4 Rewards
- [x] **Star chests:** every 10 ★ in total opens a chest — 30% a shop cosmetic the player doesn't own,
      otherwise 50 coins. (Players who already have stars get their chests after their next round.)
- [x] **Achievements** (9, each pays once): Pop 1,000 / 10,000 bubbles, Play 50 rounds, 25 combo,
      100 stars, every Sky star, beat all four bosses, 300 in Endless Hard, 25 missions. Shown with
      progress on the Profile screen.

### 8.5 Cosmetics shop
- [x] **Bubble skins** (normal bubbles only — special bubbles keep their look so they stay readable):
      Classic (free, follows the world palette), boss rewards **Cloud / Jelly / Galaxy / Candy**
      (beat that world's boss), shop **Neon** (300) and **Gold** (800).
- [x] **Pop effects:** Rainbow ring (free), Confetti (150), Stars (200), Hearts (250).
- [x] **Backgrounds:** Daylight (free), Sunset (200), Midnight (250), Aurora (400). Shown in Endless
      and the Daily Challenge; stages keep their world's look.
- [x] Shop screen: tabs per kind, live animated previews, buy (equips straight away) and equip.

### 8.6 Profile & leaderboard
- [x] Profile button on Home → Profile screen (done in Phase 0: games played, bubbles popped, best scores).
- [x] Best combo on the Profile screen (Phase 2).
- [x] Total stars on the Profile screen (Phase 3), plus coins, missions completed and achievements.
- [x] Until sign-in exists: default avatar + editable local player name (tap the name; DataStore, max 16).
- [x] Leaderboard **Local** tab: the 20 best Endless rounds on this device from Room (mock data removed).
      Reached from the 🏆 button on Home.
- [x] **Global** tab: kept, shows "Coming soon" until the backend phase.
- [ ] Playtest coin amounts and prices.

### 8.7 Sign-in & online (later — needs Firebase or Supabase)
- [ ] **Google and Facebook sign-in** (Android: Credential Manager / Facebook SDK; iOS: Google Sign-In / Facebook SDK),
      authenticated through Firebase Auth or Supabase Auth.
- [ ] Profile picture and display name come from the signed-in Google/Facebook account
      (loaded with Coil, as `ProfileButton` already does).
- [ ] Global leaderboard + Daily Challenge rankings stored in the backend.
- [ ] Optional: sync Room/DataStore progress to the cloud so it survives reinstall / new device.

---

## Phase 9 — Polish

**Status: done** (Android build and 141 unit tests pass; not yet played on a device, iOS not yet
compiled on a Mac — nothing here has been seen or heard on real hardware yet).

- [x] **Animated home screen:** 14 soap bubbles drift up and sway behind the logo and buttons
      (`view/home/DriftingBubbles.kt`, reusing the bubble renderer).
- [x] **Background music** per world, with an intensity layer. There are no music files, so the
      tracks are **synthesised in code** (`audio/MusicSynth.kt`): each world has its own chords, tempo
      and tone (Sky bright C major 100 bpm, Ocean soft A minor 84 bpm, Space airy D minor 90 bpm,
      Candy bouncy F major 120 bpm). Two looping layers of equal length play together: a calm base
      (pad, bass, arpeggio) and a drum layer (kick, hi-hat, fast arpeggio) whose volume follows
      `musicIntensity` — combo (full at 15), Endless wave (full from wave 5), boss fights, and the
      last 10 s of a stage timer. Plays only while a round is live, the app is in the foreground and
      Music is on. `GameMusic` / `LoopingTrack` (Android `MediaPlayer`, iOS `AVAudioPlayer`) can play
      real recordings later — just pass file bytes instead of synthesised ones.
- [x] **Game-over sequence:** Endless done in Phase 7. The stage result card now also waits for the
      0.8 s slow-motion drift and fades in.
- [x] **First-run tutorial** before World 1 Stage 1: pop a ghost bubble (finger hint), "Avoid this!"
      skull, pop a heart. Skippable; shown once (DataStore flag); the round waits behind it.
      "Replay tutorial" in Settings.
- [x] **Pause menu:** Resume, Restart, Settings (in-game toggles), Quit to map / Quit to Today / Quit.
- [x] **Settings screen** (⚙️ on Home): sound, music, vibration, wind buttons (swiping always works),
      accessibility, replay tutorial, **reset progress** (confirmed; deletes stars, scores, coins,
      missions, cosmetics, achievements — keeps settings and the name).
- [x] Screen transitions: new screens slide in a quarter-width and fade; back reverses it
      (also for predictive back).
- [x] Accessibility: **skull warning ring** (black-and-yellow striped ring, readable without colour)
      and **larger tap area** (touch radii ×1.35). Both off by default, in Settings and the pause menu.
- [x] Performance: each bubble's rainbow rim was 52 `drawArc` calls (26 colour segments × 2); it is
      now 2 arcs with a sweep gradient cached per colour scheme. The pop ring went from 36 arcs to
      1 circle. Skin and world schemes are built once. *Not profiled on a device yet.*
- [ ] Listen to and tune the music; replace with real tracks if wanted.
- [ ] Profile on a low-end Android phone with 40+ bubbles.

---

## Game UI — "Bubble Glass"

**Status: done** (Android build and 142 unit tests pass; not yet seen on a device or in Android
Studio previews, iOS not yet compiled on a Mac). Replaced the stock Material look with a game UI.

**Style:** glossy, rounded, casual-game. Frosted glass panels over the sunset-sky art, chunky
candy buttons that sink onto a darker lip when pressed, Baloo 2 everywhere with an indigo outline
on titles (like the logo), and icons drawn in code. Colours come from the existing art: indigo
(logo outline), violet and gold (ribbon), pale glass blue (cards).

**Where it lives** (`ui/`, built on top of `MaterialTheme` so leftover Material parts match):
- `ui/theme/` — `BubbleTheme` (wraps the whole app in `NavGraph`), `GameColors`, `Spacing`
  (4 · 8 · 12 · 16 · 24 · 32; 16 dp screen padding), `Corners` (12 / 20 / 28 / pill), and six text
  styles via `GameTheme.type` (display, title, headline, body, label, caption).
- `ui/icons/GameIcons` — 30 full-colour vector icons drawn in code (coin, star, heart, gear, trophy,
  calendar, shop, lock, home, person, chest, target, crown, music, sound, vibrate, wind, bubble,
  play, pause, restart, check, back, close, pencil, skull, clock, shield, snowflake, tap hand).
  No emojis in the UI any more.
- `ui/components/`:
  - `GameButton` — six colours, each with one job: blue main, violet featured, green confirm, gold
    coins, red destructive, glass secondary; three sizes. `GameIconButton` — round bubble buttons.
    Every press plays a soft click and a light buzz (`UiFeedbackProvider`, follows Sound / Vibration).
  - `GlassPanel`, `GameDialog` / `GameDialogCard` (violet card, drawn ribbon title, red close,
    bouncy pop-in), `OverlayScrim` (in-game cards; scrolls on short screens), `DialogButtons`.
  - `GameTopBar` (back or avatar, outlined title that never overlaps the right side, counters),
    `CoinCounter` / `StarCounter` (one app-wide source, `LocalPlayerResources`), `AvatarButton`
    (ready for the sign-in photo), `BottomDock` (Home), `GameScreen` (standard scrolling screen),
    `GameBackground` (sky art + drifting bubbles + scrim).
  - `GameSwitch`, `ToggleRow`, `GameTabs`, `GameProgressBar`, `GameTextField`, `ResourcePill`,
    `CoinAmount`, `BadgeDot`, `OutlinedText`, `Ribbon`.
- `ui/UiKitScreen.kt` — every component on one screen; view it as `UiKitScreenPreview` in
  Android Studio. Use it as the reference when adding UI.

**Rules for new UI:** take colours, spacing, corners and text styles from the tokens only; use the
components above instead of Material ones; no emojis — add a `GameIcons` icon instead.

**Screens converted:** every dialog (daily reward, reset progress, name, in-game settings), every
menu screen (Home with bottom dock, Endless, Stage map, Stage card, Today, Shop, Profile, Settings,
Leaderboard), and every in-game layer (HUD, timer / goal / wave pills, wave banner, pause menu,
game-over and stage-result cards, rewards panel, wind buttons, tutorial).

- [ ] Look at every screen on a real phone, including a small one (~320 dp wide). Checked in code
      only: the top bar title and the Profile counters were fixed to fit; the HUD can get crowded
      on very narrow screens while a shield, a freeze and a combo are all showing at once.
- [ ] Optional: check font scaling (large system text) on the dialogs and HUD.

---

## Phase 10 — Monetization (optional)

Only after the game is fun and retaining players.

- ✅ **Cosmetic purchases** (skin packs, "remove ads").
- ✅ **Optional rewarded ad:** "Watch an ad to continue once" on game over; "double coins" on result.
- ❌ No energy / lives timers.
- ❌ No interstitial ads between rounds.
- ❌ No pay-to-win power-ups.

---

## Testing strategy

- [ ] Replace placeholder `ComposeAppCommonTest` with real `commonTest` tests:
  - `ComboTracker` — window timing, multiplier tiers, reset on miss/poison.
  - `ObjectiveTracker` — every `Objective` type, star calculation, win/lose conditions.
  - `GameRules` — scoring per size/type, lives, heart healing, shield, bomb chains.
  - `WaveDirector` — wave progression. *(Done: `WaveDirectorTest`, `EndlessUnlocksTest`, `EndlessModeTest`.)*
  - `StageCatalog` — every stage is valid (positive targets, reachable goals, unique ids).
  - `ProgressRepository` — save/load round-trip (in-memory Room database + test DataStore file).
- [ ] Seeded `Random` makes ViewModel simulations deterministic: run 60 s of fake frames and assert state.
- [ ] Manual playtest checklist per world: difficulty curve, 3★ achievable, no unfair poison spawns.

---

## Content reference

### Scoring

| Source | Points |
|---|---|
| Small / medium / large bubble | 1 / 3 / 5 |
| Golden | 25 |
| Heart | size points + 1 life |
| Bomb | 5 + points of everything it pops |
| Combo multiplier | ×1 / ×2 / ×3 / ×5 |

### Combo text

| Combo | Text |
|---|---|
| 5 | NICE! |
| 10 | GREAT! |
| 15 | PERFECT! |
| 25 | INSANE! |

### Star gates

| World | Stars needed |
|---|---|
| Ocean | 20 ★ |
| Space | 45 ★ |
| Candy | 75 ★ |

---

## Recommended build order

| Order | Phase | Why |
|---|---|---|
| 1 | Phase 0 + Phase 1 | Fixes + foundation everything depends on |
| 2 | Phase 2 | Biggest feel improvement; combos needed by stages |
| 3 | Phase 3 + Phase 4 | First shippable Stage mode (World 1) |
| 4 | Phase 9 (game-over, tutorial) | Retention for new players |
| 5 | Phase 5 + Phase 6 | Content: new bubbles, worlds, bosses |
| 6 | Phase 7 | Endless waves and unlocks |
| 7 | Phase 8 | Long-term retention loop |
| 8 | §8.7 Sign-in & online | After choosing Firebase or Supabase |
| 9 | Phase 10 | Only if desired |

**Milestone 1 (shippable):** Phases 0–4 + game-over sequence + tutorial.

---

## Decisions

| Topic | Decision |
|---|---|
| Stage definitions | Kotlin, in `stage/StageCatalog.kt`. No JSON for now. |
| Local storage | **Room** (KMP) for structured data + **DataStore** (KMP) for settings and simple values. See §1.2. |
| Online backend | **Later.** Firebase or Supabase, for the Global leaderboard and cloud sync. Everything is local-only until then. |
| Endless mode | **Keep the three difficulty cards** (Easy / Medium / Hard) for now. Single Endless vs three cards will be decided later. |
| Profile picture | Comes from the player's **Google or Facebook** account after sign-in. Until then, show a default avatar. |

## Open questions

- Firebase or Supabase? (Decide before starting the Global leaderboard / sign-in work.)
- Single Endless mode with waves, or keep three cards permanently?
- Does signing in sync progress (stars, coins, cosmetics) to the cloud, or only provide name + avatar?
