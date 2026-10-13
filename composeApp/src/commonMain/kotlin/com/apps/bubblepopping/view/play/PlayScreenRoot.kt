package com.apps.bubblepopping.view.play

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.launch
import com.apps.bubblepopping.heart
import com.apps.bubblepopping.play_screen_bg
import com.apps.bubblepopping.skull
import com.apps.bubblepopping.view.BubbleGameViewModel
import com.apps.bubblepopping.view.play.component.BubbleType
import com.apps.bubblepopping.view.play.component.GameHud
import com.apps.bubblepopping.view.play.component.HUD_CONTENT_HEIGHT
import com.apps.bubblepopping.view.play.component.drawBreezeLines
import com.apps.bubblepopping.view.play.component.drawBubble
import com.apps.bubblepopping.view.play.component.drawHeartBubble
import com.apps.bubblepopping.view.play.component.drawPoisonBubble
import com.apps.bubblepopping.view.play.component.drawPopAnimation
import org.jetbrains.compose.resources.painterResource

/** Pop sounds; one is picked at random per pop. */
private val POP_SAMPLES = listOf("files/pop_1.mp3", "files/pop_2.mp3", "files/pop_3.mp3")

/** Breaking a combo this long or longer plays the combo-break sound. */
private const val COMBO_BREAK_SOUND_MIN = 3

// ─────────────────────────────────────────────────────────────────────────────
// BubblePoppingScreen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PlayScreenRoot(
    viewModel: BubbleGameViewModel,
    hapticFeedback: HapticFeedback,
    onBack: (() -> Unit)? = null,
    onHome: (() -> Unit)? = null,
) {
    BubblePoppingScreen(
        viewModel = viewModel,
        hapticFeedback = hapticFeedback,
        onBack = onBack,
        onHome = onHome,
    )
}
@Composable
fun BubblePoppingScreen(
    viewModel: BubbleGameViewModel,
    hapticFeedback: HapticFeedback,
    onBack: (() -> Unit)? = null,
    onHome: (() -> Unit)? = null,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val bestScore by viewModel.bestScore.collectAsStateWithLifecycle()

    var elapsedTime by remember { mutableStateOf(0f) }

    val heartBitmap = rememberIconBitmap(painterResource(Res.drawable.heart))
    val skullBitmap = rememberIconBitmap(painterResource(Res.drawable.skull))

    val isPaused by viewModel.isPaused.collectAsStateWithLifecycle()

    // ── System back ──────────────────────────────────────────────────────────
    // Playing → pause. Paused → leave. After game over the handler is disabled,
    // so back falls through to the navigation and leaves straight away.
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = !viewModel.isGameOver,
        onBackCompleted = {
            if (isPaused) onBack?.invoke() else viewModel.togglePause()
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

    // ── Sound & haptics ──────────────────────────────────────────────────────
    // Driven by game events so feedback matches exactly what the game decided.
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val sounds  = gameSounds.takeIf { settings.soundEnabled }
            val haptics = hapticFeedback.takeIf { settings.hapticsEnabled }
            when (event) {
                is GameEvent.Popped -> when (event.bubble.type) {
                    BubbleType.NORMAL -> {
                        sounds?.pop(event.combo)
                        if (event.bubble.size == BubbleSize.SMALL) haptics?.light() else haptics?.medium()
                    }
                    BubbleType.HEART -> {
                        sounds?.heart()
                        haptics?.medium()
                    }
                    BubbleType.POISON -> {
                        sounds?.poison()
                        haptics?.heavy()
                    }
                }
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
                // Always called: the ViewModel ignores pause itself, and keeps
                // effects (bursts, floating text, shake) animating after game over.
                viewModel.update(delta)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()){
        Image(
            painter = painterResource(Res.drawable.play_screen_bg),
            contentDescription = "background_image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
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
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown() // requireUnconsumed = true
                                // Sound and haptics follow from the Popped event.
                                if (viewModel.tryPop(down.position)) down.consume()
                            }
                        }
                    }
            )  {
                drawBreezeLines(viewModel.breeze, elapsedTime)

                // Normal bubbles this close to the top pulse red — they're about to cost a life.
                val dangerY = topOffsetPx + (size.height - topOffsetPx) * GameRules.NEAR_MISS_ZONE
                for (bubble in viewModel.bubbles) {
                    when (bubble.type) {
                        BubbleType.NORMAL -> {
                            drawBubble(bubble)
                            if (bubble.y - bubble.radius < dangerY) drawNearMissWarning(bubble, elapsedTime)
                        }
                        BubbleType.POISON -> drawPoisonBubble(bubble, skullBitmap)
                        BubbleType.HEART  -> drawHeartBubble(bubble, heartBitmap)
                    }
                }

                for (anim in viewModel.popAnimations) {
                    drawPopAnimation(anim)
                }

                for (text in viewModel.floatingTexts) {
                    drawFloatingText(text, textMeasurer, fontFamily)
                }
            }

            // ── Pause overlay ────────────────────────────────────────────────────
            // Sits above the canvas but below HUD and controls so both remain
            // interactive. Tapping the dim layer also resumes the game.
            if (isPaused && !viewModel.isGameOver) {
                PauseOverlay(
                    score    = viewModel.score,
                    onResume = { viewModel.togglePause() },
                    onQuit   = onBack,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // ── Breeze controls ──────────────────────────────────────────────────
            // navigationBarsPadding() keeps buttons above the gesture bar on all
            // device configurations (3-button nav, gesture nav, etc.).
            BreezeControls(
                onBreezeLeft  = { viewModel.triggerBreeze(-1f) },
                onBreezeRight = { viewModel.triggerBreeze(+1f) },
                breeze        = viewModel.breeze,
                modifier      = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp),
            )

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
            )

            if (viewModel.isGameOver) {
                GameOverOverlay(
                    score     = viewModel.score,
                    bestScore = bestScore,
                    isNewBest = viewModel.isNewBest,
                    onRestart = { viewModel.restartGame() },
                    onHome    = onHome,
                    modifier  = Modifier.fillMaxSize(),
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
    onQuit: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier         = modifier
            .background(Color.Black.copy(alpha = 0.50f))
            .clickable(onClick = onResume),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text          = "PAUSED",
                color         = Color.White,
                fontSize      = 32.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 6.sp,
            )
            Text(
                text          = "SCORE",
                color         = Color.White.copy(alpha = 0.50f),
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Medium,
                letterSpacing = 4.sp,
            )
            Text(
                text       = score.toString(),
                color      = Color.White,
                fontSize   = 48.sp,
                fontWeight = FontWeight.Light,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text     = "tap to resume",
                color    = Color.White.copy(alpha = 0.40f),
                fontSize = 13.sp,
            )
            // iOS has no system back button, so quitting mid-round needs a button.
            if (onQuit != null) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick  = onQuit,
                    shape    = RoundedCornerShape(50),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.12f),
                        contentColor   = Color.White,
                    ),
                    modifier = Modifier.height(46.dp),
                ) {
                    Text(text = "Quit", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GameOverOverlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GameOverOverlay(
    score: Int,
    bestScore: Int?,
    isNewBest: Boolean,
    onRestart: () -> Unit,
    onHome: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier         = modifier.background(Color.Black.copy(alpha = 0.72f)),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text          = "GAME OVER",
                color         = Color.White,
                fontSize      = 36.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 4.sp,
            )
            Text(
                text          = "SCORE",
                color         = Color.White.copy(alpha = 0.50f),
                fontSize      = 12.sp,
                fontWeight    = FontWeight.Medium,
                letterSpacing = 4.sp,
            )
            Text(
                text       = score.toString(),
                color      = Color.White,
                fontSize   = 56.sp,
                fontWeight = FontWeight.Light,
            )
            if (isNewBest) {
                Text(
                    text          = "NEW BEST!",
                    color         = Color(0xFFFFD700),
                    fontSize      = 18.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 3.sp,
                )
            } else if (bestScore != null) {
                Text(
                    text          = "BEST  $bestScore",
                    color         = Color.White.copy(alpha = 0.60f),
                    fontSize      = 14.sp,
                    fontWeight    = FontWeight.Medium,
                    letterSpacing = 2.sp,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick  = onRestart,
                shape    = RoundedCornerShape(50),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF29B6F6).copy(alpha = 0.88f),
                    contentColor   = Color.White,
                ),
                modifier = Modifier.height(52.dp),
            ) {
                Text(text = "Play Again", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            if (onHome != null) {
                Button(
                    onClick  = onHome,
                    shape    = RoundedCornerShape(50),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.12f),
                        contentColor   = Color.White,
                    ),
                    modifier = Modifier.height(52.dp),
                ) {
                    Text(text = "Home", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BreezeControls
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BreezeControls(
    onBreezeLeft: () -> Unit,
    onBreezeRight: () -> Unit,
    breeze: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment     = Alignment.CenterVertically,
        modifier              = modifier,
    ) {
        BreezeButton(label = "← Wind", onClick = onBreezeLeft,  isActive = breeze < -0.03f)
        BreezeButton(label = "Wind →", onClick = onBreezeRight, isActive = breeze > +0.03f)
    }
}

@Composable
private fun BreezeButton(label: String, onClick: () -> Unit, isActive: Boolean) {
    Button(
        onClick  = onClick,
        shape    = RoundedCornerShape(50),
        colors   = ButtonDefaults.buttonColors(
            containerColor = if (isActive) Color(0xFF29B6F6).copy(alpha = 0.88f)
                             else          Color.White.copy(alpha = 0.12f),
            contentColor   = Color.White,
        ),
        modifier = Modifier.height(46.dp),
    ) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

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
