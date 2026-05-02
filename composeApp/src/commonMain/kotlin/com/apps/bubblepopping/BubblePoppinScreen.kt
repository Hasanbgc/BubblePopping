package com.apps.bubblepopping


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import bubblepopping.composeapp.generated.resources.Res

// ─────────────────────────────────────────────────────────────────────────────
// MainScreen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Root screen of the game.
 *
 * Responsibilities:
 *  - Runs the frame loop via [LaunchedEffect] + [withFrameNanos]
 *  - Lays out the full-screen [Canvas] for game rendering
 *  - Overlays the score display and breeze buttons
 *
 * The [Canvas] re-executes its draw block whenever [viewModel.frameCount]
 * changes (which happens every frame), so it always reads the latest
 * (in-place mutated) bubble positions without creating any new objects.
 */
@Composable
fun BubblePoppingScreen(
    viewModel: BubbleGameViewModel = viewModel(),
    hapticFeedback: HapticFeedback
) {
    // Elapsed game time in seconds — passed to the canvas for animating
    // the breeze streaks.
    var elapsedTime by remember { mutableStateOf(0f) }
    // Load audio bytes once from composeResources
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

    // ── Frame loop ──────────────────────────────────────────────────────────
    // withFrameNanos suspends until the next vsync, giving a true 60 fps
    // loop that is tightly coupled to the display refresh rate.
    // Cancellation is automatic when the composable leaves the tree.
    LaunchedEffect(Unit) {
        var lastFrameNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                val delta = if (lastFrameNanos == 0L) 0f
                else (frameNanos - lastFrameNanos) / 1_000_000_000f
                lastFrameNanos = frameNanos
                elapsedTime   += delta
                viewModel.update(delta)     // delta is capped inside the VM
            }
        }
    }

    // ── Root container ───────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF050E1A),  // deep ocean — top
                        Color(0xFF0A1E35),
                        Color(0xFF0E2F4F),
                        Color(0xFF144B6E),  // lighter water — bottom
                    )
                )
            )
    ) {

        // ── Game canvas ──────────────────────────────────────────────────────
        // Reading frameCount here (in composition scope) makes Compose
        // re-run this block — and therefore the draw lambda — every frame.
        val frameCount = viewModel.frameCount

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    /*detectTapGestures { tapOffset ->
                       val popped = viewModel.tryPop(tapOffset)
                        if (popped) {
                            hapticFeedback.popVibration()
                            soundManager?.playPop()
                        }
                    }*/
                    awaitPointerEventScope {
                        while (true) {
                            // Fires the instant finger touches screen — no wait
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val popped = viewModel.tryPop(down.position)
                            if (popped) {
                                hapticFeedback.popVibration()
                                soundManager?.playPop()
                                down.consume()   // prevent event bubbling up
                            }
                        }
                    }
                }
        ) {
            // Report canvas dimensions to the ViewModel on every draw.
            // This is cheap (two float assignments) and ensures the VM
            // always has the correct size even after rotation / resize.
            viewModel.setCanvasSize(size.width, size.height)

            // Breeze visual (streaking lines)
            drawBreezeLines(viewModel.breezeForce, elapsedTime)

            // Bubbles
            for (bubble in viewModel.bubbles) {
                drawBubble(bubble)
            }

            // Pop burst animations
            for (anim in viewModel.popAnimations) {
                drawPopAnimation(anim)
            }
        }

        // ── HUD ──────────────────────────────────────────────────────────────
        ScoreDisplay(
            score    = viewModel.score,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 52.dp),
        )

        BreezeControls(
            onBreezeLeft  = { viewModel.triggerBreeze(-1f) },
            onBreezeRight = { viewModel.triggerBreeze(+1f) },
            breezeForce   = viewModel.breezeForce,
            modifier      = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 44.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HUD components
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Displays the current score at the top of the screen.
 */
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

/**
 * Two breeze trigger buttons at the bottom of the screen.
 * The active button lights up while a breeze is running.
 */
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

/**
 * A single rounded breeze button.
 * [isActive] highlights it in accent blue while the gust is blowing.
 */
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
                Color(0xFF29B6F6).copy(alpha = 0.88f)   // active  — light blue
            else
                Color.White.copy(alpha = 0.12f),         // idle    — ghost
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