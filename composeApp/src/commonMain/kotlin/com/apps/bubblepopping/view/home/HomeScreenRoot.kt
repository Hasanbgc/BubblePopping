package com.apps.bubblepopping.view.home

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

// ─────────────────────────────────────────────────────────────────────────────
// DifficultyScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeScreenRoot(
    onBack: (() -> Unit)? = null,
    onNavigateToPlay: (Difficulty) -> Unit,
    onNavigateToLeaderboard: () -> Unit,
) {
    HomeScreen(
        onNavigateToPlay = onNavigateToPlay,
        onNavigateToLeaderboard = onNavigateToLeaderboard,
    )
}

@Composable
fun HomeScreen(
    onNavigateToPlay: (Difficulty) -> Unit,
    onNavigateToLeaderboard: () -> Unit,
) {
    Scaffold() {
        Box(modifier = Modifier.fillMaxSize()) {
            DifficultyScreen(
                onDifficultySelected = onNavigateToPlay,
                onNavigateToLeaderboard = onNavigateToLeaderboard,
            )
        }
    }
}

@Composable
fun DifficultyScreen(
    onDifficultySelected: (Difficulty) -> Unit,
    onNavigateToLeaderboard: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var selectedDifficulty by remember { mutableStateOf<Difficulty?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF020812),
                        Color(0xFF050E1A),
                        Color(0xFF0A1E35),
                        Color(0xFF0D2845),
                    )
                )
            )
    ) {
        BackgroundBubbles()

        IconButton(
            onClick  = onNavigateToLeaderboard,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 8.dp, end = 12.dp),
        ) {
            Text(text = "🏆", fontSize = 24.sp)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            DifficultyTitle()

            Spacer(Modifier.weight(0.8f))

            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Difficulty.entries.forEach { difficulty ->
                    DifficultyButton(
                        difficulty = difficulty,
                        isSelected = selectedDifficulty == difficulty,
                        onClick = {
                            if (selectedDifficulty == null) {
                                selectedDifficulty = difficulty
                                scope.launch {
                                    delay(180)
                                    onDifficultySelected(difficulty)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.weight(0.7f))

            Text(
                text = "Tap to select and start",
                color = Color.White.copy(alpha = 0.22f),
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Title
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DifficultyTitle() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "SELECT",
            color = Color.White.copy(alpha = 0.50f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 8.sp,
        )
        Text(
            text = "DIFFICULTY",
            color = Color.White,
            fontSize = 38.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 3.sp,
            style = TextStyle(
                shadow = Shadow(
                    color = Color(0xFF29B6F6).copy(alpha = 0.75f),
                    offset = Offset(0f, 0f),
                    blurRadius = 28f,
                )
            ),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Choose your challenge",
            color = Color.White.copy(alpha = 0.38f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.5.sp,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DifficultyButton
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DifficultyButton(
    difficulty: Difficulty,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed || isSelected) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "btnScale",
    )

    val staticGlowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.80f else 0.38f,
        animationSpec = tween(220),
        label = "staticGlow",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_${difficulty.name}")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.42f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    val glowAlpha = if (isSelected) pulseAlpha else staticGlowAlpha
    val glowColor = difficulty.glowColor

    Box(
        modifier = modifier
            .scale(scale)
            .drawBehind {
                repeat(5) { layer ->
                    val expand = (layer + 1) * 5f
                    val layerAlpha = glowAlpha * (1f - layer * 0.18f) * 0.28f
                    if (layerAlpha > 0f) {
                        drawRoundRect(
                            color = glowColor.copy(alpha = layerAlpha),
                            topLeft = Offset(-expand, -expand),
                            size = Size(size.width + expand * 2f, size.height + expand * 2f),
                            cornerRadius = CornerRadius(24.dp.toPx() + expand),
                        )
                    }
                }
            }
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        difficulty.gradientStart,
                        difficulty.gradientEnd,
                        difficulty.gradientStart.copy(alpha = 0.85f),
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
    ) {
        // Subtle white inner border
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.14f),
                        style = Stroke(width = 1.dp.toPx()),
                        cornerRadius = CornerRadius(20.dp.toPx()),
                    )
                }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = difficulty.label,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = difficulty.description,
                    color = Color.White.copy(alpha = 0.68f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                )
            }

            DifficultyDots(difficulty = difficulty, isSelected = isSelected)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Difficulty dots indicator  ●●○
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DifficultyDots(difficulty: Difficulty, isSelected: Boolean) {
    val filled = when (difficulty) {
        Difficulty.EASY -> 1
        Difficulty.MEDIUM -> 2
        Difficulty.HARD -> 3
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (index < filled)
                            Color.White.copy(alpha = if (isSelected) 1f else 0.88f)
                        else
                            Color.White.copy(alpha = 0.18f)
                    )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Animated background bubbles
// ─────────────────────────────────────────────────────────────────────────────

private data class BgBubble(
    val startX: Float,
    val startY: Float,
    val radius: Float,
    val speed: Float,
    val amplitude: Float,
    val phase: Float,
    val color: Color,
)

@Composable
private fun BackgroundBubbles() {
    val bubbles = remember {
        val rng = Random(seed = 42)
        val palette = listOf(
            Color(0xFF29B6F6), Color(0xFF4FC3F7),
            Color(0xFF66BB6A), Color(0xFF81C784),
            Color(0xFFFFA726), Color(0xFFFFB74D),
            Color(0xFFEF5350), Color(0xFF7E57C2),
        )
        List(14) { i ->
            BgBubble(
                startX = rng.nextFloat(),
                startY = rng.nextFloat(),
                radius = 8f + rng.nextFloat() * 36f,
                speed = 0.012f + rng.nextFloat() * 0.024f,
                amplitude = 0.018f + rng.nextFloat() * 0.030f,
                phase = rng.nextFloat() * 2f * PI.toFloat(),
                color = palette[i % palette.size],
            )
        }
    }

    var time by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { nanos ->
                val dt = if (lastNanos == 0L) 0f else (nanos - lastNanos) / 1_000_000_000f
                lastNanos = nanos
                time += dt
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        for (bubble in bubbles) {
            val rawY = bubble.startY - bubble.speed * time
            val normY = ((rawY % 1f) + 1f) % 1f
            val normX = bubble.startX + bubble.amplitude * sin(normY * 7f + bubble.phase)
            val cx = normX.coerceIn(0f, 1f) * size.width
            val cy = normY * size.height
            val r = bubble.radius

            drawCircle(
                color = bubble.color.copy(alpha = 0.04f),
                radius = r * 1.8f,
                center = Offset(cx, cy),
            )
            drawCircle(
                color = bubble.color.copy(alpha = 0.07f),
                radius = r,
                center = Offset(cx, cy),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = r * 0.35f,
                center = Offset(cx - r * 0.22f, cy - r * 0.28f),
            )
        }
    }
}
