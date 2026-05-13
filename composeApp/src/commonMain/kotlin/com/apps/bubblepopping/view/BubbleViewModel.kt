package com.apps.bubblepopping.view



import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.apps.bubblepopping.view.home.Difficulty
import com.apps.bubblepopping.view.play.component.Bubble
import com.apps.bubblepopping.view.play.component.BubbleType
import com.apps.bubblepopping.view.play.component.PopAnimation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

class BubbleGameViewModel : ViewModel() {

    // ─────────────────────────────────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────────────────────────────────

    companion object {
        private const val BASE_SPAWN_INTERVAL = 0.85f
        private const val MIN_SPAWN_INTERVAL  = 0.30f
        private const val SPAWN_SCALE         = 0.005f   // seconds shaved per point

        private const val BREEZE_STRENGTH = 160f
        private const val BREEZE_DECAY    = 2.0f
        private const val MAX_DELTA       = 0.05f

        private const val MIN_RADIUS    = 22f
        private const val MAX_RADIUS    = 54f
        private const val MIN_SPEED     = 70f
        private const val BASE_MAX_SPEED = 155f
        private const val SPEED_SCALE   = 0.8f           // px/s added per point
        private const val MAX_SPEED_CAP = 320f
        private const val MIN_AMPLITUDE = 18f
        private const val MAX_AMPLITUDE = 62f

        private const val POP_ANIM_SPEED = 2.5f

        private const val MAX_LIVES = 5

        // Spawn weights: 75% normal, 15% poison, 10% heart
        private const val WEIGHT_NORMAL = 75
        private const val WEIGHT_POISON = 15
        // WEIGHT_HEART = 100 - 75 - 15 = 10

        val BUBBLE_COLORS = listOf(
            Color(0xFF81D4FA),
            Color(0xFFB2EBF2),
            Color(0xFF80CBC4),
            Color(0xFFA5D6A7),
            Color(0xFFCE93D8),
            Color(0xFFF48FB1),
            Color(0xFFFFCC80),
            Color(0xFFE0F7FA),
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Difficulty-adjusted values — start at MEDIUM (1× multiplier)
    // ─────────────────────────────────────────────────────────────────────────

    private var adjustedSpawnInterval = BASE_SPAWN_INTERVAL
    private var adjustedBaseMaxSpeed  = BASE_MAX_SPEED
    private var adjustedMaxSpeedCap   = MAX_SPEED_CAP

    // ─────────────────────────────────────────────────────────────────────────
    // Exposed state
    // ─────────────────────────────────────────────────────────────────────────

    val bubbles        = mutableStateListOf<Bubble>()
    val popAnimations  = mutableStateListOf<PopAnimation>()

    var breezeForce    by mutableStateOf(0f);       private set
    var score          by mutableStateOf(0);         private set
    var missedCount    by mutableStateOf(0);         private set
    var isGameOver     by mutableStateOf(false);     private set
    var frameCount     by mutableStateOf(0);         private set

    var _isPaused = MutableStateFlow(false)
    val isPaused  = _isPaused.asStateFlow()



    // ─────────────────────────────────────────────────────────────────────────
    // Private state
    // ─────────────────────────────────────────────────────────────────────────

    private var spawnTimer      = 0f
    private var canvasWidth     = 400f
    private var canvasHeight    = 800f
    private var canvasTopOffset = 0f   // px below which bubbles are in the active game area

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /** Update screen dimensions and the Y-offset where the game area begins (below HUD). */
    fun setCanvasBounds(width: Float, height: Float, topOffset: Float = 0f) {
        if (width > 0f && height > 0f) {
            canvasWidth     = width
            canvasHeight    = height
            canvasTopOffset = topOffset
        }
    }

    fun togglePause() {
       // if (!isGameOver) {
            _isPaused.value = !_isPaused.value

        println("isPaused: ${_isPaused.value}")
       // }
    }

    fun update(delta: Float) {
        if (isGameOver || _isPaused.value) return
        val dt = delta.coerceAtMost(MAX_DELTA)

        spawnStep(dt)
        moveStep(dt)
        cullStep()
        animatePopStep(dt)
        breezeDecayStep(dt)

        frameCount++
    }

    fun tryPop(offset: Offset): Boolean {
        if (isGameOver || isPaused.value) return false
        val minHitRadius = 50f

        val hit = bubbles.firstOrNull { bubble ->
            val dx        = offset.x - bubble.x
            val dy        = offset.y - bubble.y
            val hitRadius = bubble.radius.coerceAtLeast(minHitRadius)
            (dx * dx + dy * dy) <= (hitRadius * hitRadius)
        }

        hit?.let { bubble ->
            bubbles.remove(bubble)
            popAnimations.add(
                PopAnimation(
                    id = "${bubble.id}_pop",
                    x = bubble.x,
                    y = bubble.y,
                    radius = bubble.radius,
                    color = bubble.color,
                )
            )
            when (bubble.type) {
                BubbleType.NORMAL -> score++
                BubbleType.POISON -> isGameOver = true
                BubbleType.HEART  -> {
                    if (missedCount > 0) missedCount--
                    score++
                }
            }
        }
        return hit != null
    }

    fun triggerBreeze(direction: Float) {
        if (isGameOver) return
        breezeForce = direction.coerceIn(-1f, 1f) * BREEZE_STRENGTH
    }

    fun applyDifficulty(difficulty: Difficulty) {
        adjustedSpawnInterval = BASE_SPAWN_INTERVAL * difficulty.spawnIntervalMultiplier
        adjustedBaseMaxSpeed  = BASE_MAX_SPEED * difficulty.speedMultiplier
        adjustedMaxSpeedCap   = MAX_SPEED_CAP * difficulty.maxSpeedCapMultiplier
        restartGame()
    }

    fun restartGame() {
        bubbles.clear()
        popAnimations.clear()
        score       = 0
        missedCount = 0
        isGameOver  = false
        _isPaused.value  = false
        breezeForce = 0f
        spawnTimer  = 0f
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private step functions
    // ─────────────────────────────────────────────────────────────────────────

    private fun spawnStep(dt: Float) {
        val interval = (adjustedSpawnInterval - score * SPAWN_SCALE)
            .coerceAtLeast(MIN_SPAWN_INTERVAL)
        spawnTimer += dt
        if (spawnTimer >= interval) {
            spawnTimer = 0f
            bubbles.add(createBubble())
        }
    }

    private fun moveStep(dt: Float) {
        for (bubble in bubbles) {
            bubble.y -= bubble.speed * dt
            bubble.baseX += breezeForce * dt
            bubble.x = bubble.baseX +
                    sin(bubble.y * 0.05f + bubble.phase) * bubble.amplitude
        }
    }

    private fun cullStep() {
        // Cull bubbles that have risen above the game area (below the HUD, not absolute 0).
        val escaped = bubbles.filter { it.y + it.radius < canvasTopOffset }
        for (bubble in escaped) {
            if (bubble.type == BubbleType.NORMAL) {
                missedCount++
                if (missedCount >= MAX_LIVES) {
                    isGameOver = true
                }
            }
        }
        bubbles.removeAll(escaped.toSet())
    }

    private fun animatePopStep(dt: Float) {
        for (anim in popAnimations) anim.progress += dt * POP_ANIM_SPEED
        popAnimations.removeAll { it.progress >= 1f }
    }

    private fun breezeDecayStep(dt: Float) {
        if (abs(breezeForce) > 0.5f) {
            breezeForce *= (1f - dt * BREEZE_DECAY)
        } else {
            breezeForce = 0f
        }
    }

    private fun createBubble(): Bubble {
        val radius = Random.nextFloat() * (MAX_RADIUS - MIN_RADIUS) + MIN_RADIUS
        val startX = Random.nextFloat() * canvasWidth
        val maxSpeed = (adjustedBaseMaxSpeed + score * SPEED_SCALE).coerceAtMost(adjustedMaxSpeedCap)
        val type = rollBubbleType()
        return Bubble(
            id = Random.nextLong().toString(),
            x = startX,
            y = canvasHeight + radius,
            radius = radius,
            speed = Random.nextFloat() * (maxSpeed - MIN_SPEED) + MIN_SPEED,
            amplitude = Random.nextFloat() * (MAX_AMPLITUDE - MIN_AMPLITUDE) + MIN_AMPLITUDE,
            baseX = startX,
            phase = Random.nextFloat() * (2f * PI.toFloat()),
            color = BUBBLE_COLORS[Random.nextInt(BUBBLE_COLORS.size)],
            shimmerAngle = Random.nextFloat() * 360f,
            type = type,
        )
    }

    private fun rollBubbleType(): BubbleType {
        return when (Random.nextInt(100)) {
            in 0 until WEIGHT_NORMAL -> BubbleType.NORMAL
            in WEIGHT_NORMAL until WEIGHT_NORMAL + WEIGHT_POISON -> BubbleType.POISON
            else -> BubbleType.HEART
        }
    }
}
