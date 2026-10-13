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
13. [Phase 10 — Monetization (optional)](#phase-10--monetization-optional)
14. [Testing strategy](#testing-strategy)
15. [Content reference](#content-reference)
16. [Open questions](#open-questions)

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
│   └── shop/                     // cosmetics (Phase 8)
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
    data class OnlySize(val size: BubbleSize, val count: Int) : Objective
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
- [x] Bubbles carry a `BubbleSize` (SMALL/MEDIUM/LARGE = 1/3/5 pts), ready for `OnlySize` goals.
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

### 3.1 Model
- [ ] `StageConfig`, `Objective`, `World` as defined in §2.2.
- [ ] `StageCatalog.kt` — a Kotlin list of all stages (decided: no JSON).

### 3.2 ObjectiveTracker
- [ ] Consumes `GameEvent`s; holds progress per objective (`current / target`).
- [ ] Main goal complete → stage **won** (either immediately, or at time-up for score/survive goals).
- [ ] Time runs out or lives reach 0 before goal → stage **lost**.
- [ ] Stars: 1★ main goal; +1★ per bonus goal met.

### 3.3 Gameplay integration
- [ ] `BubbleGameViewModel.start(config: StageConfig)` replaces `applyDifficulty`.
- [ ] Optional countdown timer in HUD when `durationSec != null`.
- [ ] Lives come from config (not fixed 5).

### 3.4 UI
- [ ] **Stage intro card** (before play): stage number, goal text, bonus goals with ☆ icons, "Play".
- [ ] **HUD goal chip:** e.g. "⭐ 7 / 15 golden" with progress bar; flashes when complete.
- [ ] **Result screen:** stars animate in one by one (sound + haptic each), score counts up,
      coins earned, buttons: Next / Retry / Map.
- [ ] Loss screen: shows how close you got ("12 / 15 — so close!"), Retry is the primary button.

### 3.5 Navigation
- [ ] Routes: `StageMap`, `StageIntro(stageId)`, `Play(mode: GameMode)`, `Result(...)`.
- [ ] `GameMode`: `Stage(id)` | `Endless(difficulty)` | `Daily(date)`.

---

## Phase 4 — Stage map & World 1

### 4.1 Home changes
- [ ] Home shows two big buttons: **Stages** and **Endless** (Endless opens the existing
      three-card difficulty pager, kept as-is for now).

### 4.2 Map screen
- [ ] Vertical, scrolling, winding path (Candy-Crush style), drawn on Canvas.
- [ ] Node states: locked 🔒, unlocked (pulsing), completed with 1–3 ★.
- [ ] Auto-scroll to the current stage; world banner between worlds.
- [ ] Star counter at the top; locked world shows "★ 25 needed".
- [ ] Themed background per world.

### 4.3 World 1 — Sky ☁️ (tutorial world)

| # | Main goal | Bonus goals (2★ / 3★) | Introduces |
|---|---|---|---|
| 1 | Pop 10 bubbles | No misses · Under 20 s | Tapping (tutorial) |
| 2 | Score 30 in 45 s | Score 45 · No lives lost | Size → points |
| 3 | Pop 3 hearts | Score 40 · No misses | Hearts heal |
| 4 | Survive 30 s | Avoid all poison · Score 50 | Poison |
| 5 | Reach ×4 combo | ×6 combo · Score 60 | Combos |
| 6 | Use wind to save 5 bubbles | No misses · Score 50 | Wind |
| 7 | Score 80 in 60 s | ×6 combo · No lives lost | Mixed |
| 8 | Pop 25 small bubbles | Max 35 taps · No misses | Precision |
| 9 | Survive 60 s (fast spawns) | No lives lost · Score 120 | Pressure |
| 10 | 👑 **Boss: Cloud King** | Under 60 s · No lives lost | Bosses (Phase 6) |

Until Phase 6 ships, stage 10 can be a "Score 150" finale.

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

- [ ] Add each to `BubbleType`, a `BubbleScheme` in `BubbleCanvas.kt`, an icon, and behaviour in `GameRules`.
- [ ] Bomb chain reactions: bombs caught in a blast also explode (emit `BombExploded(chainSize)`).
- [ ] Active power-up indicator in HUD (freeze timer, shield icon).

### 5.2 Worlds

| World | Theme | New mechanic | Unlock |
|---|---|---|---|
| 1 ☁️ Sky | Day sky | Basics, combos, wind | — |
| 2 🌊 Ocean | Underwater, slower sway | Golden + Freeze | 20 ★ |
| 3 🌌 Space | Stars, low "gravity" drift | Bomb + chain reactions | 45 ★ |
| 4 🍬 Candy | Pastel, chaotic | Splitter + Shield | 75 ★ |

- [ ] 10–15 stages per world following the World 1 pattern: introduce → practice → combine → boss.
- [ ] World-specific background, music track, and bubble palette.

### 5.3 Wind rework
- [ ] Replace (or complement) the two wind buttons with a **swipe gesture**: swipe direction and speed
      set breeze strength. Keep buttons as an accessibility option in settings.
- [ ] "Wind save" = a bubble near the top that is pushed back down/aside — emits `WindSave`.

---

## Phase 6 — Boss stages

Every 10th stage.

- [ ] Boss = a giant bubble (≈ 3× radius) with **HP** shown as a ring.
- [ ] Each tap deals 1 damage; boss shrinks and wobbles on hit; cracks appear at 66% / 33% HP.
- [ ] Attack phases: releases poison bubbles, then speeds up, then splits into mini-bosses.
- [ ] Defeat = huge pop with slow-motion, confetti and a world-clear banner.

| World | Boss | Gimmick |
|---|---|---|
| Sky | Cloud King | Spawns poison when hit |
| Ocean | Jelly Queen | Freezes your taps briefly |
| Space | Nebula Core | Explodes into bombs at low HP |
| Candy | Gumball Giant | Splits into 3, then 9 |

- [ ] Beating a boss unlocks that world's **cosmetic skin** (usable everywhere).

---

## Phase 7 — Endless mode upgrades

Endless keeps the **three difficulty cards** (Easy / Medium / Hard) for now. Whether it later
becomes a single Endless mode is still open — build waves so they work either way.

- [ ] **Waves:** ~20 s of rising pressure → 3 s calm with "WAVE N" banner → harder wave.
      `WaveDirector` adjusts `SpawnConfig` per wave instead of a pure score ramp.
- [ ] New bubble types appear in Endless once unlocked in Stages.
- [ ] **Persistent best per difficulty**, shown on the difficulty cards.
- [ ] **Unlock difficulties:** Hard unlocks after scoring 150 on Medium (or finishing World 1).
- [ ] Game-over sequence (see Phase 9) with "NEW BEST!" and "X from your best".

---

## Phase 8 — Meta progression & retention

### 8.1 Currency
- [ ] **Coins** earned from: stage stars (first time only), Endless score (score / 10),
      missions, daily rewards, star chests.

### 8.2 Missions
- [ ] 3 active missions at a time; completing one replaces it with a new one.
- [ ] Examples: "Pop 3 golden bubbles in one run", "Reach ×10 combo", "Pop 500 bubbles total",
      "Earn 3★ on any stage", "Survive 90 s in Endless Hard".
- [ ] `MissionTracker` consumes the same `GameEvent` stream.

### 8.3 Daily content
- [ ] **Daily Challenge:** one seeded Endless run per day (`Random(seed = date)`), same for everyone.
      Becomes the **Global** leaderboard tab once a backend exists.
- [ ] **Daily Stage:** a generated stage (random world-appropriate objective) with a reward chest.
- [ ] **Daily login reward:** small coin reward, streak bonus on day 7 (no penalty for missing).

### 8.4 Rewards
- [ ] **Star chests:** every 10 ★ opens a chest (coins or a cosmetic).
- [ ] **Achievements:** long-term goals (Pop 10,000 bubbles, 3★ every Sky stage, beat all bosses).

### 8.5 Cosmetics shop
- [ ] Bubble skins: Classic, Galaxy, Candy, Neon, Gold (each = a `BubbleScheme` set).
- [ ] Pop effects: Rainbow ring (current), Confetti, Stars, Hearts.
- [ ] Backgrounds per world.
- [ ] Shop screen with preview; buy with coins; equip.

### 8.6 Profile & leaderboard
- [x] Profile button on Home → Profile screen (done in Phase 0: games played, bubbles popped, best scores).
- [x] Best combo on the Profile screen (Phase 2).
- [ ] Total stars on the Profile screen (Phase 3).
- [ ] Until sign-in exists: default avatar + editable local player name (stored in DataStore).
- [ ] Leaderboard **Local** tab uses real persisted bests from Room (replace mock data).
- [ ] **Global** tab: keep the tab, show "Coming soon" until the backend phase.

### 8.7 Sign-in & online (later — needs Firebase or Supabase)
- [ ] **Google and Facebook sign-in** (Android: Credential Manager / Facebook SDK; iOS: Google Sign-In / Facebook SDK),
      authenticated through Firebase Auth or Supabase Auth.
- [ ] Profile picture and display name come from the signed-in Google/Facebook account
      (loaded with Coil, as `ProfileButton` already does).
- [ ] Global leaderboard + Daily Challenge rankings stored in the backend.
- [ ] Optional: sync Room/DataStore progress to the cloud so it survives reinstall / new device.

---

## Phase 9 — Polish

- [ ] **Animated home screen:** background bubbles drifting behind the logo (reuse `drawSoapBubble`).
- [ ] **Background music** per world; intensity layer rises with waves/combos. Music + SFX toggles.
- [ ] **Game-over sequence:** slow-mo → 0.5 s pause → score counts up → "NEW BEST!" + confetti
      (or "X from your best!") → big instant **Play Again** button.
- [ ] **First-run tutorial** (World 1 Stage 1): 3 ghost bubbles teaching "pop this", "avoid this",
      "this heals", with a finger hint. Skippable, shown once.
- [ ] **Pause menu:** Resume, Restart, Settings, Quit to map.
- [ ] **Settings screen:** sound, music, haptics, wind controls (swipe/buttons), reset progress.
- [ ] Screen transitions between Home / Map / Play / Result.
- [ ] Accessibility: colour-blind-safe poison marking (skull icon already helps), larger hit-area option.
- [ ] Performance: profile canvas drawing with 40+ bubbles + effects; cache arc gradients if needed.

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
  - `WaveDirector` — wave progression.
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
