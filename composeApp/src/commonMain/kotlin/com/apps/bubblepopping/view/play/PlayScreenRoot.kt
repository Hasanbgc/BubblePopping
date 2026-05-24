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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apps.bubblepopping.HapticFeedback
import com.apps.bubblepopping.Res
import com.apps.bubblepopping.SoundManager
import com.apps.bubblepopping.bg
import com.apps.bubblepopping.createSoundManager
import com.apps.bubblepopping.heart
import com.apps.bubblepopping.play_screen_bg
import com.apps.bubblepopping.skull
import com.apps.bubblepopping.view.BubbleGameViewModel
import com.apps.bubblepopping.view.home.Difficulty
import com.apps.bubblepopping.view.play.component.BubbleType
import com.apps.bubblepopping.view.play.component.GameHud
import com.apps.bubblepopping.view.play.component.HUD_CONTENT_HEIGHT
import com.apps.bubblepopping.view.play.component.drawBreezeLines
import com.apps.bubblepopping.view.play.component.drawBubble
import com.apps.bubblepopping.view.play.component.drawHeartBubble
import com.apps.bubblepopping.view.play.component.drawPoisonBubble
import com.apps.bubblepopping.view.play.component.drawPopAnimation
import org.jetbrains.compose.resources.painterResource

// ─────────────────────────────────────────────────────────────────────────────
// BubblePoppingScreen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PlayScreenRoot(
    difficulty: Difficulty = Difficulty.EASY,
    viewModel: BubbleGameViewModel = viewModel(),
    hapticFeedback: HapticFeedback,
    onBack: (() -> Unit)? = null,
    onHome: (() -> Unit)? = null,
) {
    BubblePoppingScreen(
        difficulty = difficulty,
        viewModel = viewModel,
        hapticFeedback = hapticFeedback,
        onBack = onBack,
        onHome = onHome,
    )
}
@Composable
fun BubblePoppingScreen(
    difficulty: Difficulty,
    viewModel: BubbleGameViewModel = viewModel(),
    hapticFeedback: HapticFeedback,
    onBack: (() -> Unit)? = null,
    onHome: (() -> Unit)? = null,
) {
    LaunchedEffect(difficulty) { viewModel.applyDifficulty(difficulty) }


    var elapsedTime by remember { mutableStateOf(0f) }

    val heartBitmap = rememberIconBitmap(painterResource(Res.drawable.heart))
    val skullBitmap = rememberIconBitmap(painterResource(Res.drawable.skull))

    val isPaused by viewModel.isPaused.collectAsStateWithLifecycle()


    val soundManager by produceState<SoundManager?>(initialValue = null) {
        try {
            val bytes = Res.readBytes("files/pop_3.mp3")
            value = createSoundManager(bytes)
        } catch (e: Exception) {
            println("Failed to load sound: ${e.message}")
        }
    }

    DisposableEffect(Unit) {
        onDispose { soundManager?.dispose() }
    }

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
                    viewModel.update(delta)
                }
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
            val frameCount = viewModel.frameCount

            // ── Game canvas ──────────────────────────────────────────────────────
            // Full-screen so the background gradient is always seamless.
            // Touch events use requireUnconsumed = true (the default), so taps the
            // HUD consumed at the Initial pass never trigger a bubble pop here.
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown() // requireUnconsumed = true
                                val popped = viewModel.tryPop(down.position)
                                if (popped) {
                                    hapticFeedback.popVibration()
                                    soundManager?.playPop()
                                    down.consume()
                                }
                            }
                        }
                    }
            )  {
                viewModel.setCanvasBounds(
                    width     = size.width,
                    height    = size.height,
                    topOffset = topOffsetPx,
                )

                drawBreezeLines(viewModel.breezeForce, elapsedTime)

                for (bubble in viewModel.bubbles) {
                    when (bubble.type) {
                        BubbleType.NORMAL -> drawBubble(bubble)
                        BubbleType.POISON -> drawPoisonBubble(bubble, skullBitmap)
                        BubbleType.HEART  -> drawHeartBubble(bubble, heartBitmap)
                    }
                }

                for (anim in viewModel.popAnimations) {
                    drawPopAnimation(anim)
                }
            }

            // ── Pause overlay ────────────────────────────────────────────────────
            // Sits above the canvas but below HUD and controls so both remain
            // interactive. Tapping the dim layer also resumes the game.
            if (isPaused && !viewModel.isGameOver) {
                PauseOverlay(
                    score    = viewModel.score,
                    onResume = { viewModel.togglePause() },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // ── Breeze controls ──────────────────────────────────────────────────
            // navigationBarsPadding() keeps buttons above the gesture bar on all
            // device configurations (3-button nav, gesture nav, etc.).
            BreezeControls(
                onBreezeLeft  = { viewModel.triggerBreeze(-1f) },
                onBreezeRight = { viewModel.triggerBreeze(+1f) },
                breezeForce   = viewModel.breezeForce,
                modifier      = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp),
            )

            // ── HUD — drawn last, always on top ──────────────────────────────────
            // Manages its own statusBarsPadding; no external top padding needed.
            GameHud(
                score = viewModel.score,
                lives = 5 - viewModel.missedCount,
                isPaused = isPaused,
                onPlayPauseClick = {
                    viewModel.togglePause()
                    println("paused clicked")
                },
                modifier = Modifier.align(Alignment.TopStart),
            )

            if (viewModel.isGameOver) {
                GameOverOverlay(
                    score     = viewModel.score,
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
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GameOverOverlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GameOverOverlay(
    score: Int,
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
    breezeForce: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment     = Alignment.CenterVertically,
        modifier              = modifier,
    ) {
        BreezeButton(label = "← Wind", onClick = onBreezeLeft,  isActive = breezeForce < -5f)
        BreezeButton(label = "Wind →", onClick = onBreezeRight, isActive = breezeForce > +5f)
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
