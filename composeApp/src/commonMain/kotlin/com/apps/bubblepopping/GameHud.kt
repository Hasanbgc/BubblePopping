package com.apps.bubblepopping

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bubblepopping.composeapp.generated.resources.Res
import bubblepopping.composeapp.generated.resources.live
import org.jetbrains.compose.resources.painterResource

// ─────────────────────────────────────────────────────────────────────────────
// Dimension constant — shared with BubblePoppinScreen for inset arithmetic
// ─────────────────────────────────────────────────────────────────────────────

/** Height of the HUD content row (below the status bar). */
internal val HUD_CONTENT_HEIGHT: Dp = 56.dp

// ─────────────────────────────────────────────────────────────────────────────
// GameHud
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full-width overlay HUD that blends seamlessly into the game background.
 *
 * The outer Box carries a vertical gradient (opaque dark → transparent) so the
 * bar looks native to the game instead of a separate UI layer. Content sits in
 * the Row below the status bar so it never overlaps system icons.
 *
 * No touch-blocking is needed: bubbles are culled at canvasTopOffset (the HUD
 * bottom edge), so tryPop always returns false for taps in this area.
 */
@Composable
fun GameHud(
    score: Int,
    lives: Int,
    isPaused: Boolean,
    onPlayPauseClick: () -> Unit,
    onRankingClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            // Solid dark-blue over the status bar; fades to transparent so the
            // game canvas shows through at the bottom edge of the HUD.
            .background(
                Brush.verticalGradient(
                    0.00f to Color(0xFF050E1A),
                    0.60f to Color(0xFF050E1A).copy(alpha = 0.82f),
                    1.00f to Color.Transparent,
                )
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(HUD_CONTENT_HEIGHT)
                .padding(horizontal = 20.dp)
        ) {

            // Left + Right Row
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LivesView(lives = lives)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayPauseButton(isPaused = isPaused, onClick = onPlayPauseClick)
                    RankingButton(onClick = onRankingClick)
                }
            }

            // Center Score (true center)
            ScoreView(
                score = score,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LivesView — ❤ icon + "x N"
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Compact lives indicator: a single heart icon followed by "x [lives]".
 */
@Composable
fun LivesView(
    lives: Int,
    modifier: Modifier = Modifier,
) {
    val heartTint = if (lives > 0) Color(0xFFFF1744) else Color.White.copy(alpha = 0.30f)

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier              = modifier,
    ) {
        Icon(
            painter            = painterResource(Res.drawable.live),
            contentDescription = "Lives",
            tint               = heartTint,
            modifier           = Modifier.size(20.dp),
        )
        Text(
            text       = "x $lives",
            color      = Color.White,
            fontSize   = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ScoreView — spring-animated score counter
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Centered score display. The number spring-animates toward [score] on each
 * increment, giving a satisfying count-up feel.
 */
@Composable
fun ScoreView(
    score: Int,
    modifier: Modifier = Modifier,
) {
    val displayed by animateIntAsState(
        targetValue   = score,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label         = "score",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = modifier,
    ) {
        Text(
            text          = "SCORE",
            color         = Color.White.copy(alpha = 0.45f),
            fontSize      = 10.sp,
            fontWeight    = FontWeight.Medium,
            letterSpacing = 4.sp,
        )
        Text(
            text       = displayed.toString(),
            color      = Color.White,
            fontSize   = 28.sp,
            fontWeight = FontWeight.Light,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PlayPauseButton
// ─────────────────────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────────────────────
// RankingButton
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RankingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick  = onClick,
        modifier = modifier.size(40.dp),
    ) {
        Text(
            text     = "🏆",
            fontSize = 20.sp,
            color    = Color.White,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PlayPauseButton
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Toggle button with a [Crossfade] dissolve between "▶" (play) and "⏸" (pause).
 * A [MutableInteractionSource]-driven bouncy scale animates on press.
 */
@Composable
fun PlayPauseButton(
    isPaused: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.75f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium,
        ),
        label = "play_pause_scale",
    )

    IconButton(
        onClick           = {
            println("pause icon click")
            onClick.invoke() },
        enabled = true,
        interactionSource = interactionSource,
        modifier          = modifier
            .size(40.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        Crossfade(
            targetState   = isPaused,
            animationSpec = tween(durationMillis = 200),
            label         = "play_pause_icon",
        ) { paused ->
            Text(
                text     = if (isPaused) "▶" else "⏸",
                fontSize = 20.sp,
                color    = Color.White,
            )
        }
    }
}
