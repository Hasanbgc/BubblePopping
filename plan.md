# Difficulty Selection Screen — Implementation Plan

## Status
- [x] `Difficulty.kt` — enum with label, description, tuning multipliers, glow/gradient colors
- [x] `DifficultyScreen.kt` — full premium UI composable
- [x] `BubbleViewModel.kt` — add `applyDifficulty(difficulty: Difficulty)` method
- [x] `BubblePoppinScreen.kt` — add `difficulty` param + `LaunchedEffect` to apply it
- [x] `App.kt` — replace single-screen with state-based navigation

## Approach

### Navigation
No new dependency. Use `var selectedDifficulty by mutableStateOf<Difficulty?>(null)` in App.kt.
Wrap screens in `AnimatedContent` for a slide-up transition.

### Difficulty → ViewModel wiring
`BubbleGameViewModel` stays no-arg. Add instance vars `adjustedSpawnInterval`,
`adjustedBaseMaxSpeed`, `adjustedMaxSpeedCap`. `applyDifficulty()` sets them and calls `restartGame()`.
`BubblePoppingScreen` calls `LaunchedEffect(difficulty) { viewModel.applyDifficulty(difficulty) }`.

### DifficultyScreen UI layers (bottom to top)
1. Dark navy vertical gradient background
2. BackgroundBubbles() — 14 semi-transparent circles floating upward (Canvas + frame loop)
3. Title — "SELECT / DIFFICULTY" with cyan Shadow glow via TextStyle
4. Three DifficultyButton cards — gradient bg, multi-layer glow via drawBehind, white border stroke, dots badge
5. Bottom hint text

### DifficultyButton behaviour
- collectIsPressedAsState() -> spring scale to 0.97f on press
- Selected state -> rememberInfiniteTransition pulse on glow alpha
- 180 ms delay after tap before calling onDifficultySelected so animation plays
