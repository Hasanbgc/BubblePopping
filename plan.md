# Feature Implementation Plan

## Overview of 4 Features

1. **5-miss game over** — track escaped bubbles; 5 misses ends the game
2. **Rising difficulty** — spawn rate and bubble speed scale up with score
3. **Poison bubble** — dark bubble; immediate game over on tap
4. **Heart bubble** — restores one missed life on tap

---

## Files to change

| File | What changes |
|------|-------------|
| `Bubble.kt` | Add `BubbleType` enum; add `type` field to `Bubble` |
| `BubbleViewModel.kt` | Add `missedCount`, `isGameOver`; update spawn/cull/pop logic; difficulty scaling |
| `BubblePoppingScreen.kt` | Pause loop on game over; show lives HUD; show game-over overlay |
| `BubbleCanvas.kt` | Add draw functions for poison and heart bubbles; dispatch on type |

---

## Feature 1 — 5-miss game over

### ViewModel changes
- Add `var missedCount by mutableStateOf(0)` (exposed, private set)
- Add `var isGameOver by mutableStateOf(false)` (exposed, private set)
- In `cullStep()`: before removing bubbles that exited the top, for each that escaped check:
  - **NORMAL** → `missedCount++`; if `missedCount >= 5` → `isGameOver = true`
  - **POISON** → quietly remove (no penalty — player didn't tap it, they dodged it)
  - **HEART** → quietly remove (missed opportunity, no penalty)
- Add `fun restartGame()` to reset all state back to initial values

### Screen changes
- In the frame loop `LaunchedEffect`: wrap the `viewModel.update(delta)` call — skip updates when `isGameOver`
- Add a `GameOverOverlay` composable that covers the screen when `isGameOver`:
  - Shows "GAME OVER", final score, and a Restart button that calls `viewModel.restartGame()`
- Add a `LivesDisplay` composable (top-left or below score) showing 5 heart icons, filled/empty based on `missedCount`

---

## Feature 2 — Rising difficulty

### ViewModel changes
- Replace the fixed `SPAWN_INTERVAL` and fixed speed range with dynamic computed values based on `score`:
  ```
  currentSpawnInterval = (BASE_SPAWN_INTERVAL - score * SPAWN_SCALE).coerceAtLeast(MIN_SPAWN_INTERVAL)
  currentMaxSpeed      = BASE_MAX_SPEED + score * SPEED_SCALE
  ```
- Constants:
  - `BASE_SPAWN_INTERVAL = 0.85f`, `MIN_SPAWN_INTERVAL = 0.30f`, `SPAWN_SCALE = 0.005f`
  - `BASE_MAX_SPEED = 155f`, `SPEED_SCALE = 0.8f` (caps naturally because bubble size limits stay the same)
- No new exposed state needed — difficulty is invisible to the UI

---

## Feature 3 — Poison bubble

### Bubble model
```kotlin
enum class BubbleType { NORMAL, POISON, HEART }
```
Add `val type: BubbleType` to `Bubble` data class (default `NORMAL`).

### ViewModel changes
- In `createBubble()`: roll a random type — suggested weights:
  - 75% NORMAL, 15% POISON, 10% HEART
- In `tryPop()`: after hit detection, branch on `bubble.type`:
  - `NORMAL` → `score++` (existing behaviour)
  - `POISON` → `isGameOver = true` (do NOT increment score)
  - `HEART`  → restore one life (see Feature 4)

### Canvas changes — `drawPoisonBubble(bubble: Bubble)`
- Dark translucent fill: near-black / deep purple `Color(0xFF1A0033)` at alpha 0.35
- Outer rim: sickly green `Color(0xFF39FF14)` stroke
- Inner X mark (two diagonal lines crossing the centre) in bright green
- Faint pulsing glow effect via a large semi-transparent circle behind it
- No rainbow arcs — deliberately looks different from normal bubbles

---

## Feature 4 — Heart bubble

### ViewModel changes
- In `tryPop()` for HEART type:
  ```kotlin
  if (missedCount > 0) missedCount--
  score++ // optional: reward the player
  ```
- Heart bubbles that escape the top are quietly removed (no penalty in `cullStep`)

### Canvas changes — `drawHeartBubble(bubble: Bubble)`
- Translucent pink/red fill: `Color(0xFFFF1744)` at alpha 0.25
- White/pink outer rim stroke
- Heart shape drawn at the centre using two arcs + a triangle (or approximated with a `Path`)
- Soft red radial glow behind the bubble to make it stand out

---

## Restart flow

`viewModel.restartGame()` resets:
- `bubbles.clear()`
- `popAnimations.clear()`
- `score = 0`
- `missedCount = 0`
- `isGameOver = false`
- `breezeForce = 0f`
- `spawnTimer = 0f`

The frame loop in the screen restarts automatically because `isGameOver` flips back to `false`.

---

## Summary of new state exposed by ViewModel

| Property | Type | Description |
|----------|------|-------------|
| `missedCount` | `Int` | 0–5 bubbles missed so far |
| `isGameOver` | `Boolean` | true = game ended |

Everything else (difficulty, bubble types) is internal to the ViewModel.
