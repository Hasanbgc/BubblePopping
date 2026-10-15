package com.apps.bubblepopping.view.play

import com.apps.bubblepopping.view.play.component.GoalBanner
import kotlinx.coroutines.delay
import com.apps.bubblepopping.ui.components.PanelDivider
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.apps.bubblepopping.HapticFeedback
import com.apps.bubblepopping.Res
import com.apps.bubblepopping.GameSounds
import com.apps.bubblepopping.createSoundManager
import com.apps.bubblepopping.game.BubbleSize
import com.apps.bubblepopping.game.GameEvent
import com.apps.bubblepopping.game.GameOverReason
import com.apps.bubblepopping.game.GameRules
import com.apps.bubblepopping.view.BalooFontFamily
import com.apps.bubblepopping.view.play.component.drawFloatingText
import com.apps.bubblepopping.view.play.component.drawNearMissWarning
import com.apps.bubblepopping.view.play.component.StageGoalBar
import com.apps.bubblepopping.view.play.component.WaveBanner
import com.apps.bubblepopping.view.play.component.WavePill
import androidx.compose.foundation.layout.statusBarsPadding
import kotlinx.coroutines.launch
import com.apps.bubblepopping.heart
import com.apps.bubblepopping.skull
import com.apps.bubblepopping.view.BubbleGameViewModel
import com.apps.bubblepopping.view.play.component.BubbleType
import com.apps.bubblepopping.view.play.component.GameHud
import com.apps.bubblepopping.view.play.component.HUD_CONTENT_HEIGHT
import com.apps.bubblepopping.view.play.component.drawBreezeLines
import com.apps.bubblepopping.view.play.component.drawGameBubble
import com.apps.bubblepopping.view.play.component.drawBoss
import com.apps.bubblepopping.view.play.component.drawConfetti
import com.apps.bubblepopping.view.StingColor
import com.apps.bubblepopping.view.play.component.BubbleIcons
import com.apps.bubblepopping.view.FreezeColor
import com.apps.bubblepopping.game.GameMode
import com.apps.bubblepopping.audio.GameMusic
import com.apps.bubblepopping.data.prefs.SettingsRepository
import com.apps.bubblepopping.view.settings.InGameSettingsDialog
import com.apps.bubblepopping.ui.components.ButtonColor
import androidx.compose.foundation.layout.fillMaxWidth
import com.apps.bubblepopping.ui.components.ButtonSize
import com.apps.bubblepopping.ui.components.GameButton
import com.apps.bubblepopping.ui.components.GameDialogCard
import com.apps.bubblepopping.ui.components.OverlayScrim
import com.apps.bubblepopping.ui.components.formatCount
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.Spacing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.apps.bubblepopping.stage.World
import androidx.compose.foundation.gestures.awaitEachGesture
import kotlin.math.abs
import com.apps.bubblepopping.view.play.component.drawPopAnimation
import org.jetbrains.compose.resources.painterResource

/** Pop sounds; one is picked at random per pop. */
private val POP_SAMPLES = listOf("files/pop_1.mp3", "files/pop_2.mp3", "files/pop_3.mp3")

/** Breaking a combo this long or longer plays the combo-break sound. */
private const val COMBO_BREAK_SOUND_MIN = 3
/** How long a stage's goal is shown big as the round starts. */
private const val GOAL_BANNER_MS = 2_200L

/** Each result star plays a pop pitched as if at this many more combo steps. */
private const val STAR_PITCH_STEP = 4

/** How quickly the music's drum layer follows the action. */
private const val MUSIC_INTENSITY_FADE_MS = 1_500

/** Horizontal movement after a missed press that turns it into a wind swipe. */
private val SWIPE_THRESHOLD = 24.dp

private const val FREEZE_TINT_ALPHA = 0.12f
private const val STING_TINT_ALPHA = 0.15f

// ─────────────────────────────────────────────────────────────────────────────
// BubblePoppingScreen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PlayScreenRoot(
    viewModel: BubbleGameViewModel,
    hapticFeedback: HapticFeedback,
    settingsRepository: SettingsRepository,
    onBack: (() -> Unit)? = null,
    onHome: (() -> Unit)? = null,
    onNextStage: (() -> Unit)? = null,
) {
    BubblePoppingScreen(
        viewModel = viewModel,
        hapticFeedback = hapticFeedback,
        settingsRepository = settingsRepository,
        onBack = onBack,
        onHome = onHome,
        onNextStage = onNextStage,
    )
}

/**
 * [onHome] leaves after the round (Endless → Home, stage → stage list).
 * [onNextStage] is shown on a cleared stage's result when a next stage exists.
 * [settingsRepository] backs the pause menu's settings and the tutorial flag.
 */
@Composable
fun BubblePoppingScreen(
    viewModel: BubbleGameViewModel,
    hapticFeedback: HapticFeedback,
    settingsRepository: SettingsRepository,
    onBack: (() -> Unit)? = null,
    onHome: (() -> Unit)? = null,
    onNextStage: (() -> Unit)? = null,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val bestScore by viewModel.bestScore.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // ── First-run tutorial (World 1 Stage 1) ─────────────────────────────────
    // null until DataStore has loaded, so the round waits rather than starting
    // and then being interrupted by the tutorial.
    val storedSettings by remember(settingsRepository) { settingsRepository.settings }
        .collectAsStateWithLifecycle(null)
    var tutorialFinished by rememberSaveable { mutableStateOf(false) }
    val isTutorialStage = viewModel.mode == GameMode.Stage(TUTORIAL_STAGE_ID)
    val showTutorial = isTutorialStage && !tutorialFinished && storedSettings?.tutorialSeen == false
    val holdRound = isTutorialStage && !tutorialFinished && (storedSettings == null || showTutorial)
    val currentHoldRound by rememberUpdatedState(holdRound)
    // A stage's goal is shown big for a moment whenever a round starts (after the tutorial).
    var goalBannerVisible by remember { mutableStateOf(false) }
    LaunchedEffect(viewModel.isGameOver, holdRound) {
        if (viewModel.stage != null && !viewModel.isGameOver && !holdRound) {
            goalBannerVisible = true
            delay(GOAL_BANNER_MS)
            goalBannerVisible = false
        }
    }
    var showSettings by remember { mutableStateOf(false) }

    var elapsedTime by remember { mutableStateOf(0f) }

    val heartBitmap = rememberIconBitmap(painterResource(Res.drawable.heart))
    val skullBitmap = rememberIconBitmap(painterResource(Res.drawable.skull))
    val bubbleIcons = remember(heartBitmap, skullBitmap) { BubbleIcons(heart = heartBitmap, skull = skullBitmap) }

    val isPaused by viewModel.isPaused.collectAsStateWithLifecycle()

    // ── System back ──────────────────────────────────────────────────────────
    // Playing → pause. Paused → leave. After game over the handler is disabled,
    // so back falls through to the navigation and leaves straight away.
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = !viewModel.isGameOver,
        onBackCompleted = {
            // Before the round starts (tutorial), or when already paused, back leaves.
            if (isPaused || holdRound) onBack?.invoke() else viewModel.togglePause()
        },
    )

    val gameSounds by produceState<GameSounds?>(initialValue = null) {
        try {
            val samples = POP_SAMPLES.map { Res.readBytes(it) }
            value = GameSounds(createSoundManager(samples))
        } catch (_: Exception) {
            // Missing or unreadable audio: play silently rather than crash.
        }
    }

    DisposableEffect(Unit) {
        onDispose { gameSounds?.dispose() }
    }

    // ── Music ────────────────────────────────────────────────────────────────
    // Synthesised per world off the main thread; plays while the round is live
    // and the app is in the foreground. The drum layer follows the action.
    val music by produceState<GameMusic?>(initialValue = null, viewModel.world) {
        value = try {
            GameMusic.create(withContext(Dispatchers.Default) { GameMusic.synthesize(viewModel.world) })
        } catch (_: Exception) {
            null // no music rather than a crash
        }
    }
    DisposableEffect(music) {
        val current = music
        onDispose { current?.release() }
    }
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val musicPlaying = settings.musicEnabled && !isPaused && !viewModel.isGameOver && !holdRound &&
            lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    LaunchedEffect(music, musicPlaying) {
        if (musicPlaying) music?.play() else music?.pause()
    }
    val musicIntensity by animateFloatAsState(
        targetValue = viewModel.musicIntensity,
        animationSpec = tween(MUSIC_INTENSITY_FADE_MS),
        label = "music_intensity",
    )
    LaunchedEffect(music, musicIntensity) { music?.setIntensity(musicIntensity) }

    // ── Sound & haptics ──────────────────────────────────────────────────────
    // Driven by game events so feedback matches exactly what the game decided.
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val sounds  = gameSounds.takeIf { settings.soundEnabled }
            val haptics = hapticFeedback.takeIf { settings.hapticsEnabled }
            when (event) {
                is GameEvent.Popped -> when (event.bubble.type) {
                    BubbleType.NORMAL, BubbleType.SPLITTER -> {
                        sounds?.pop(event.combo)
                        if (event.bubble.size == BubbleSize.SMALL) haptics?.light() else haptics?.medium()
                    }
                    BubbleType.HEART -> {
                        sounds?.heart()
                        haptics?.medium()
                    }
                    BubbleType.GOLDEN -> {
                        sounds?.golden()
                        haptics?.medium()
                    }
                    BubbleType.FREEZE -> {
                        sounds?.freeze()
                        haptics?.medium()
                    }
                    BubbleType.SHIELD -> {
                        sounds?.shield()
                        haptics?.medium()
                    }
                    // The blast itself plays on BombExploded.
                    BubbleType.BOMB -> Unit
                    BubbleType.POISON -> {
                        sounds?.poison()
                        haptics?.heavy()
                    }
                }
                is GameEvent.BombExploded -> {
                    sounds?.bomb()
                    haptics?.heavy()
                }
                is GameEvent.ShieldUsed -> {
                    sounds?.shield()
                    haptics?.success()
                }
                is GameEvent.WindSave -> {
                    sounds?.windSave()
                    haptics?.light()
                }
                is GameEvent.BossHit -> {
                    sounds?.bossHit()
                    haptics?.medium()
                }
                is GameEvent.BossDefeated -> {
                    haptics?.success()
                    sounds?.let { launch { it.newBest() } }
                }
                is GameEvent.WaveCleared -> {
                    sounds?.golden()
                    haptics?.success()
                }
                is GameEvent.WaveStarted -> haptics?.medium()
                is GameEvent.ComboChanged -> {
                    if (event.broken && event.previous >= COMBO_BREAK_SOUND_MIN) sounds?.comboBreak()
                }
                is GameEvent.GameOver -> {
                    if (event.reason == GameOverReason.OUT_OF_LIVES) haptics?.heavy()
                    if (event.isNewBest) {
                        haptics?.success()
                        sounds?.let { launch { it.newBest() } }
                    }
                }
                else -> Unit
            }
        }
    }

    val textMeasurer = rememberTextMeasurer()
    val fontFamily   = BalooFontFamily()

    // ── Game-area top offset from live WindowInsets ──────────────────────────
    // Tells the ViewModel the Y boundary at which bubbles should be culled
    // (the bottom edge of the HUD), so they never vanish mid-screen.
    val density      = LocalDensity.current
    val statusBarPx  = WindowInsets.statusBars.getTop(density)
    val hudContentPx = with(density) { HUD_CONTENT_HEIGHT.roundToPx() }
    val topOffsetPx  = (statusBarPx + hudContentPx).toFloat()

    // ── Frame loop ───────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        var lastFrameNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                val delta = if (lastFrameNanos == 0L) 0f
                            else (frameNanos - lastFrameNanos) / 1_000_000_000f
                lastFrameNanos = frameNanos
                if (!viewModel.isGameOver && !viewModel.isPaused.value) {
                    elapsedTime += delta
                }
                // Always called (except while the tutorial holds the round): the
                // ViewModel ignores pause itself, and keeps effects (bursts,
                // floating text, shake) animating after game over.
                if (!currentHoldRound) viewModel.update(delta)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()){
        // The world's colours and scenery. Outside stages the equipped background
        // cosmetic brings its own.
        PlayBackdrop(
            world = viewModel.world,
            backgroundId = settings.equippedBackground.takeIf { viewModel.stage == null },
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(0.2f),

                        )
                    )
                )
        )
        {
            // Establish state dependency so the Canvas recomposes every frame even
            // when bubble positions mutate in-place (not observed individually).
            // effectFrame also ticks after game over, so bursts and texts finish.
            val effectFrame = viewModel.effectFrame

            // ── Game canvas ──────────────────────────────────────────────────────
            // Full-screen so the background gradient is always seamless.
            // Touch events use requireUnconsumed = true (the default), so taps the
            // HUD consumed at the Initial pass never trigger a bubble pop here.
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    // Screen shake moves only the play field, not the HUD or buttons.
                    .graphicsLayer {
                        translationX = viewModel.shakeOffset.x
                        translationY = viewModel.shakeOffset.y
                    }
                    .onSizeChanged { intSize ->
                        viewModel.setCanvasBounds(
                            width     = intSize.width.toFloat(),
                            height    = intSize.height.toFloat(),
                            topOffset = topOffsetPx,
                            density   = density.density,
                        )
                    }
                    // A press on a bubble pops it. A press that misses is either a
                    // horizontal swipe (wind) or, if released without swiping, a missed tap.
                    .pointerInput(Unit) {
                        val swipeThreshold = SWIPE_THRESHOLD.toPx()
                        awaitEachGesture {
                            val down = awaitFirstDown() // requireUnconsumed = true
                            // Sound and haptics follow from the Popped event.
                            if (viewModel.tryPop(down.position)) {
                                down.consume()
                                return@awaitEachGesture
                            }
                            var swiped = false
                            while (true) {
                                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                val dx = change.position.x - down.position.x
                                if (!swiped && abs(dx) > swipeThreshold) swiped = true
                                if (swiped) viewModel.swipeWind(dx / size.width)
                                if (!change.pressed) break
                            }
                            if (!swiped) viewModel.onMissedTap()
                        }
                    }
            )  {
                drawBreezeLines(viewModel.breeze, elapsedTime)

                // Bosses sit behind the bubbles (a skull in front of a boss gets tapped first).
                for (boss in viewModel.bosses) {
                    drawBoss(boss, viewModel.world, elapsedTime)
                }

                // Normal bubbles this close to the top pulse red — they're about to cost a life.
                val dangerY = topOffsetPx + (size.height - topOffsetPx) * GameRules.NEAR_MISS_ZONE
                for (bubble in viewModel.bubbles) {
                    drawGameBubble(bubble, viewModel.world, bubbleIcons, settings.equippedSkin, settings.highContrastSkulls)
                    if (bubble.type == BubbleType.NORMAL && bubble.y - bubble.radius < dangerY) {
                        drawNearMissWarning(bubble, elapsedTime)
                    }
                }

                for (anim in viewModel.popAnimations) {
                    drawPopAnimation(anim, settings.equippedPopEffect)
                }

                for (text in viewModel.floatingTexts) {
                    drawFloatingText(text, textMeasurer, fontFamily)
                }

                for (scrap in viewModel.confetti) {
                    drawConfetti(scrap)
                }
            }

            // ── Sting tint (taps locked) ─────────────────────────────────────────
            if (viewModel.tapLockLeft > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(StingColor.copy(alpha = STING_TINT_ALPHA))
                )
            }

            // ── Freeze tint ──────────────────────────────────────────────────────
            if (viewModel.freezeTimeLeft > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(FreezeColor.copy(alpha = FREEZE_TINT_ALPHA))
                )
            }

            // ── Pause overlay ────────────────────────────────────────────────────
            // Sits above the canvas but below HUD and controls so both remain
            // interactive. Tapping the dim layer also resumes the game.
            if (isPaused && !viewModel.isGameOver) {
                PauseOverlay(
                    score     = viewModel.score,
                    onResume  = { viewModel.togglePause() },
                    onRestart = { viewModel.restartGame() },
                    onSettings = { showSettings = true },
                    onQuit    = onBack,
                    quitLabel = when (viewModel.mode) {
                        is GameMode.Stage -> "Quit to map"
                        is GameMode.DailyChallenge, is GameMode.DailyStage -> "Quit to Today"
                        is GameMode.Endless -> "Quit"
                    },
                    modifier  = Modifier.fillMaxSize(),
                )
            }

            // ── Breeze controls ──────────────────────────────────────────────────
            // navigationBarsPadding() keeps buttons above the gesture bar on all
            // device configurations (3-button nav, gesture nav, etc.).
            // Swiping always works; the buttons can be turned off in settings.
            if (settings.windButtonsEnabled) {
                BreezeControls(
                    onBreezeLeft  = { viewModel.triggerBreeze(-1f) },
                    onBreezeRight = { viewModel.triggerBreeze(+1f) },
                    breeze        = viewModel.breeze,
                    modifier      = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 20.dp),
                )
            }

            // ── HUD — drawn last, always on top ──────────────────────────────────
            // Manages its own statusBarsPadding; no external top padding needed.
            GameHud(
                score = viewModel.score,
                lives = viewModel.livesLeft,
                isPaused = isPaused,
                onPlayPauseClick = {
                    viewModel.togglePause()
                },
                modifier = Modifier.align(Alignment.TopStart),
                comboTimeLeft = viewModel.comboTimeLeft,
                comboMultiplier = viewModel.comboMultiplier,
                shieldActive = viewModel.shieldActive,
                freezeTimeLeft = viewModel.freezeTimeLeft,
            )

            // ── Stage: timer + goal progress under the HUD ──────────────────────
            if (viewModel.stage != null) {
                StageGoalBar(
                    goal = viewModel.objectives.firstOrNull(),
                    timeLeftSec = viewModel.timeLeftSec,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = HUD_CONTENT_HEIGHT + 4.dp),
                )
                // The goal in full, big in the middle as the round starts.
                viewModel.objectives.firstOrNull()?.let { goal ->
                    GoalBanner(
                        text = goal.objective.description,
                        visible = goalBannerVisible,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            } else {
                // ── Endless: wave progress under the HUD, banner between waves ──
                WavePill(
                    wave     = viewModel.wave,
                    progress = viewModel.waveProgress,
                    isCalm   = viewModel.isWaveCalm,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = HUD_CONTENT_HEIGHT + 4.dp),
                )
                WaveBanner(
                    wave     = viewModel.wave,
                    visible  = viewModel.isWaveCalm && !viewModel.isGameOver,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            if (viewModel.isGameOver) {
                val stage = viewModel.stage
                val stageResult = viewModel.stageResult
                if (stage != null && stageResult != null) {
                    val isDaily = viewModel.mode is GameMode.DailyStage
                    StageResultOverlay(
                        backLabel   = if (isDaily) "Today" else "Stages",
                        rewards     = viewModel.rewards,
                        result      = stageResult,
                        maxStars    = stage.maxStars,
                        score       = viewModel.score,
                        bestScore   = bestScore,
                        isNewBest   = viewModel.isNewBest,
                        onRetry     = { viewModel.restartGame() },
                        onStages    = onHome,
                        onNext      = onNextStage,
                        onStarShown = { star ->
                            if (settings.soundEnabled) gameSounds?.pop(combo = star * STAR_PITCH_STEP)
                            if (settings.hapticsEnabled) hapticFeedback.medium()
                        },
                        modifier    = Modifier.fillMaxSize(),
                    )
                } else {
                    GameOverOverlay(
                        score     = viewModel.score,
                        bestScore = bestScore,
                        isNewBest = viewModel.isNewBest,
                        reason    = viewModel.gameOverReason,
                        wave      = viewModel.wave,
                        unlockedDifficulty = viewModel.unlockedDifficulty,
                        rewards   = viewModel.rewards,
                        label     = if (viewModel.mode is GameMode.DailyChallenge) "DAILY CHALLENGE" else null,
                        onRestart = { viewModel.restartGame() },
                        onHome    = onHome,
                        modifier  = Modifier.fillMaxSize(),
                    )
                }
            }

            // ── First-run tutorial — over everything, the round waits behind it ──
            if (showTutorial) {
                TutorialOverlay(
                    icons = bubbleIcons,
                    skinId = settings.equippedSkin,
                    highContrastSkulls = settings.highContrastSkulls,
                    onPop = {
                        if (settings.soundEnabled) gameSounds?.pop(combo = 0)
                        if (settings.hapticsEnabled) hapticFeedback.medium()
                    },
                    onFinish = {
                        tutorialFinished = true
                        scope.launch { settingsRepository.setTutorialSeen(true) }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (showSettings) {
                InGameSettingsDialog(
                    settings = settings,
                    onToggle = { toggle, on -> scope.launch { toggle.set(settingsRepository, on) } },
                    onDismiss = { showSettings = false },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PauseOverlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PauseOverlay(
    score: Int,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onSettings: () -> Unit,
    onQuit: (() -> Unit)?,
    quitLabel: String,
    modifier: Modifier = Modifier,
) {
    // Tapping the dim area outside the card also resumes.
    OverlayScrim(modifier = modifier, onScrimClick = onResume) {
        GameDialogCard(title = "Paused", onClose = onResume) {
            ScorePlaque(score = formatCount(score), celebrate = false)
            GameButton("Resume", onClick = onResume, size = ButtonSize.LARGE, icon = GameIcons.Play, modifier = Modifier.fillMaxWidth())
            // Secondary actions under a divider, as round buttons with labels, so Resume stays the obvious one.
            PanelDivider()
            RoundActions {
                RoundAction(GameIcons.Restart, "Restart", ButtonColor.VIOLET, onRestart)
                RoundAction(GameIcons.Gear, "Settings", ButtonColor.GLASS, onSettings)
                // iOS has no system back button, so quitting mid-round needs a button.
                if (onQuit != null) RoundAction(GameIcons.Home, quitLabel, ButtonColor.RED, onQuit)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BreezeControls
// ─────────────────────────────────────────────────────────────────────────────

/** Optional on-screen wind buttons (swiping always works). The active side lights up blue. */
@Composable
private fun BreezeControls(
    onBreezeLeft: () -> Unit,
    onBreezeRight: () -> Unit,
    breeze: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.xl),
        verticalAlignment     = Alignment.CenterVertically,
        modifier              = modifier,
    ) {
        GameButton(
            text = "← Wind",
            onClick = onBreezeLeft,
            color = if (breeze < -WIND_ACTIVE) ButtonColor.BLUE else ButtonColor.GLASS,
            size = ButtonSize.SMALL,
            icon = GameIcons.Wind,
        )
        GameButton(
            text = "Wind →",
            onClick = onBreezeRight,
            color = if (breeze > WIND_ACTIVE) ButtonColor.BLUE else ButtonColor.GLASS,
            size = ButtonSize.SMALL,
            icon = GameIcons.Wind,
        )
    }
}

/** Breeze above this strength lights up its button. */
private const val WIND_ACTIVE = 0.03f

// ─────────────────────────────────────────────────────────────────────────────
// Icon helper
// ─────────────────────────────────────────────────────────────────────────────

// Rasterises a Painter into an ImageBitmap once, then caches it.
// drawImage in a DrawScope is reliable on all Compose Multiplatform targets;
// painter.draw() directly in a Canvas DrawScope is not.
@Composable
private fun rememberIconBitmap(painter: Painter, sizePx: Int = 256): ImageBitmap {
    val density = LocalDensity.current
    return remember(painter, sizePx) {
        val bitmap = ImageBitmap(sizePx, sizePx)
        CanvasDrawScope().draw(
            density         = density,
            layoutDirection = LayoutDirection.Ltr,
            canvas          = Canvas(bitmap),
            size            = Size(sizePx.toFloat(), sizePx.toFloat()),
        ) {
            with(painter) { draw(Size(sizePx.toFloat(), sizePx.toFloat())) }
        }
        bitmap
    }
}
