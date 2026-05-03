package com.apps.bubblepopping


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import bubblepopping.composeapp.generated.resources.Res
import bubblepopping.composeapp.generated.resources.heart
import bubblepopping.composeapp.generated.resources.live
import bubblepopping.composeapp.generated.resources.live_outline
import bubblepopping.composeapp.generated.resources.skull
import org.jetbrains.compose.resources.painterResource

// ─────────────────────────────────────────────────────────────────────────────
// MainScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BubblePoppingScreen(
    viewModel: BubbleGameViewModel = viewModel(),
    hapticFeedback: HapticFeedback
) {
    var elapsedTime by remember { mutableStateOf(0f) }

    // Rasterise icon painters into bitmaps once. drawImage is used inside
    // the Canvas DrawScope — it's reliable across all Compose Multiplatform
    // targets unlike calling painter.draw() directly in a DrawScope.
    val heartBitmap = rememberIconBitmap(painterResource(Res.drawable.heart))
    val skullBitmap = rememberIconBitmap(painterResource(Res.drawable.skull))

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

    // Frame loop — pauses automatically when isGameOver
    LaunchedEffect(Unit) {
        var lastFrameNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                val delta = if (lastFrameNanos == 0L) 0f
                else (frameNanos - lastFrameNanos) / 1_000_000_000f
                lastFrameNanos = frameNanos
                if (!viewModel.isGameOver) {
                    elapsedTime += delta
                    viewModel.update(delta)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF050E1A),
                        Color(0xFF0A1E35),
                        Color(0xFF0E2F4F),
                        Color(0xFF144B6E),
                    )
                )
            )
    ) {
        val frameCount = viewModel.frameCount

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val popped = viewModel.tryPop(down.position)
                            if (popped) {
                                hapticFeedback.popVibration()
                                soundManager?.playPop()
                                down.consume()
                            }
                        }
                    }
                }
        ) {
            viewModel.setCanvasSize(size.width, size.height)

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

        // Score + lives HUD
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 52.dp),
        ) {
            ScoreDisplay(score = viewModel.score)
            Spacer(modifier = Modifier.height(8.dp))
            LivesDisplay(missedCount = viewModel.missedCount)
        }

        BreezeControls(
            onBreezeLeft  = { viewModel.triggerBreeze(-1f) },
            onBreezeRight = { viewModel.triggerBreeze(+1f) },
            breezeForce   = viewModel.breezeForce,
            modifier      = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 44.dp),
        )

        if (viewModel.isGameOver) {
            GameOverOverlay(
                score   = viewModel.score,
                onRestart = { viewModel.restartGame() },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HUD components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ScoreDisplay(
    score: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = modifier,
    ) {
        Text(
            text          = "SCORE",
            color         = Color.White.copy(alpha = 0.45f),
            fontSize      = 11.sp,
            fontWeight    = FontWeight.Medium,
            letterSpacing = 4.sp,
        )
        Text(
            text       = score.toString(),
            color      = Color.White,
            fontSize   = 44.sp,
            fontWeight = FontWeight.Light,
        )
    }
}

@Composable
private fun LivesDisplay(
    missedCount: Int,
    modifier: Modifier = Modifier,
) {
    val maxLives = 5
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        modifier              = modifier,
    ) {
        repeat(maxLives) { index ->
            val isFilled = index < (maxLives - missedCount)
            if(isFilled) {
                Icon(
                    painter = painterResource(Res.drawable.live),
                    contentDescription = null,
                    tint = Color(0xFFFF1744),
                    modifier = Modifier.size(20.dp)
                )
            }else{
                Icon(
                    painter = painterResource(Res.drawable.live_outline),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            /*Canvas(modifier = Modifier.size(20.dp)) {
                val r  = size.minDimension / 2f
                val cx = size.width / 2f
                val cy = size.height / 2f
                val hs = r * 0.85f
                val heartPath = Path().apply {
                    moveTo(cx, cy + hs)
                    cubicTo(
                        cx - hs * 1.8f, cy + hs * 0.1f,
                        cx - hs * 2.0f, cy - hs * 1.2f,
                        cx,             cy - hs * 0.5f,
                    )
                    cubicTo(
                        cx + hs * 2.0f, cy - hs * 1.2f,
                        cx + hs * 1.8f, cy + hs * 0.1f,
                        cx,             cy + hs,
                    )
                    close()
                }
                if (isFilled) {
                    drawPath(path = heartPath, color = Color(0xFFFF1744).copy(alpha = 0.90f))
                } else {
                    drawPath(
                        path  = heartPath,
                        color = Color.White.copy(alpha = 0.25f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()),
                    )
                }
            }*/
        }
    }
}

@Composable
private fun GameOverOverlay(
    score: Int,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.72f)),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text       = "GAME OVER",
                color      = Color.White,
                fontSize   = 36.sp,
                fontWeight = FontWeight.Bold,
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
                onClick = onRestart,
                shape   = RoundedCornerShape(50),
                colors  = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF29B6F6).copy(alpha = 0.88f),
                    contentColor   = Color.White,
                ),
                modifier = Modifier.height(52.dp),
            ) {
                Text(
                    text       = "Play Again",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

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
        BreezeButton(
            label    = "← Wind",
            onClick  = onBreezeLeft,
            isActive = breezeForce < -5f,
        )
        BreezeButton(
            label    = "Wind →",
            onClick  = onBreezeRight,
            isActive = breezeForce > +5f,
        )
    }
}

@Composable
private fun BreezeButton(
    label: String,
    onClick: () -> Unit,
    isActive: Boolean,
) {
    Button(
        onClick = onClick,
        shape   = RoundedCornerShape(50),
        colors  = ButtonDefaults.buttonColors(
            containerColor = if (isActive)
                Color(0xFF29B6F6).copy(alpha = 0.88f)
            else
                Color.White.copy(alpha = 0.12f),
            contentColor = Color.White,
        ),
        modifier = Modifier.height(46.dp),
    ) {
        Text(
            text       = label,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Icon helper
// ─────────────────────────────────────────────────────────────────────────────

// Rasterises a Painter into an ImageBitmap once, then caches it.
// Using drawImage in a DrawScope is reliable on all Compose Multiplatform
// targets; calling painter.draw() directly in a Canvas DrawScope is not.
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
            with(painter) {
                draw(Size(sizePx.toFloat(), sizePx.toFloat()))
            }
        }
        bitmap
    }
}
