package com.apps.bubblepopping



import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

class BubbleGameViewModel : ViewModel() {

    // ─────────────────────────────────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────────────────────────────────

    companion object {
        /** Seconds between each new bubble spawn */
        private const val SPAWN_INTERVAL = 0.85f

        /** How fast a triggered breeze moves bubbles sideways (px / s) */
        private const val BREEZE_STRENGTH = 160f

        /**
         * Exponential decay multiplier applied to breezeForce every second.
         * Higher = breeze fades faster.
         */
        private const val BREEZE_DECAY = 2.0f

        /** Safety cap on delta (seconds) to survive app-to-background spikes */
        private const val MAX_DELTA = 0.05f

        private const val MIN_RADIUS = 22f
        private const val MAX_RADIUS = 54f
        private const val MIN_SPEED  = 70f
        private const val MAX_SPEED  = 155f
        private const val MIN_AMPLITUDE = 18f
        private const val MAX_AMPLITUDE = 62f

        /**
         * Speed multiplier applied to [PopAnimation.progress].
         * 2.5 means each animation finishes in ~0.4 seconds.
         */
        private const val POP_ANIM_SPEED = 2.5f

        /** Watery, translucent bubble colours */
        val BUBBLE_COLORS = listOf(
            Color(0xFF81D4FA),  // light blue
            Color(0xFFB2EBF2),  // cyan
            Color(0xFF80CBC4),  // teal
            Color(0xFFA5D6A7),  // mint
            Color(0xFFCE93D8),  // lavender
            Color(0xFFF48FB1),  // pink
            Color(0xFFFFCC80),  // peach
            Color(0xFFE0F7FA),  // ice
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exposed state  (observed by Compose)
    // ─────────────────────────────────────────────────────────────────────────

    /** Live bubble list.  Structural changes (add/remove) notify Compose. */
    val bubbles = mutableStateListOf<Bubble>()

    /** Short-lived pop burst animations */
    val popAnimations = mutableStateListOf<PopAnimation>()

    /**
     * Current lateral wind force (px / s).
     * Positive = rightward, Negative = leftward.
     * Observed by the canvas to draw wind streak lines.
     */
    var breezeForce by mutableStateOf(0f)
        private set

    /** Player score — increments on each successful pop */
    var score by mutableStateOf(0)
        private set

    /**
     * Incremented every frame so the Canvas composable re-executes and
     * picks up the latest (mutated) bubble positions.
     * See MainScreen.kt for the read-pattern.
     */
    var frameCount by mutableStateOf(0)
        private set

    // ─────────────────────────────────────────────────────────────────────────
    // Private state
    // ─────────────────────────────────────────────────────────────────────────

    private var spawnTimer  = 0f
    private var canvasWidth  = 400f
    private var canvasHeight = 800f

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called once the Canvas measures itself.
     * Must be set before the first [update] call or bubbles will spawn
     * at wrong positions.
     */
    fun setCanvasSize(width: Float, height: Float) {
        if (width > 0f && height > 0f) {
            canvasWidth  = width
            canvasHeight = height
        }
    }

    /**
     * Main game-loop tick.  Called every frame from a [withFrameNanos] loop
     * in MainScreen.  [delta] is elapsed time in seconds (already capped).
     *
     * Order matters:
     *   1. Spawn → 2. Move → 3. Cull → 4. Animate pops → 5. Decay breeze
     */
    fun update(delta: Float) {
        val dt = delta.coerceAtMost(MAX_DELTA)

        spawnStep(dt)
        moveStep(dt)
        cullStep()
        animatePopStep(dt)
        breezeDecayStep(dt)

        // Signal Compose to redraw the canvas
        frameCount++
    }

    /**
     * Hit-test a tap/click at [offset].
     * If it lands inside a bubble that bubble is removed and a
     * [PopAnimation] is queued in its place.
     */
    fun tryPop(offset: Offset) : Boolean{
        val minHitRadius = 40f

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
                    id     = "${bubble.id}_pop",
                    x      = bubble.x,
                    y      = bubble.y,
                    radius = bubble.radius,
                    color  = bubble.color,
                )
            )
            score++
        }
        return hit!= null
    }

    /**
     * Trigger a breeze gust.
     * @param direction -1f for leftward, +1f for rightward.
     */
    fun triggerBreeze(direction: Float) {
        breezeForce = direction.coerceIn(-1f, 1f) * BREEZE_STRENGTH
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private step functions
    // ─────────────────────────────────────────────────────────────────────────

    private fun spawnStep(dt: Float) {
        spawnTimer += dt
        if (spawnTimer >= SPAWN_INTERVAL) {
            spawnTimer = 0f
            bubbles.add(createBubble())
        }
    }

    private fun moveStep(dt: Float) {
        for (bubble in bubbles) {
            // Rise upward
            bubble.y -= bubble.speed * dt

            // Breeze pushes the oscillation centre sideways
            bubble.baseX += breezeForce * dt

            // Sine-wave lateral position relative to baseX.
            // Using y as the sine input means the wave shape is spatial,
            // not time-based — the bubble traces a curve as it rises.
            bubble.x = bubble.baseX +
                    sin(bubble.y * 0.05f + bubble.phase) * bubble.amplitude
        }
    }

    private fun cullStep() {
        // Remove bubbles that have fully exited through the top
        bubbles.removeAll { it.y + it.radius < 0f }
    }

    private fun animatePopStep(dt: Float) {
        for (anim in popAnimations) {
            anim.progress += dt * POP_ANIM_SPEED
        }
        popAnimations.removeAll { it.progress >= 1f }
    }

    private fun breezeDecayStep(dt: Float) {
        if (abs(breezeForce) > 0.5f) {
            // Exponential decay: force halves every (1/BREEZE_DECAY) seconds
            breezeForce *= (1f - dt * BREEZE_DECAY)
        } else {
            breezeForce = 0f
        }
    }

    private fun createBubble(): Bubble {
        val radius = Random.nextFloat() * (MAX_RADIUS - MIN_RADIUS) + MIN_RADIUS
        val startX = Random.nextFloat() * canvasWidth
        return Bubble(
            id           = Random.nextLong().toString(),
            x            = startX,
            y            = canvasHeight + radius,       // just below the bottom edge
            radius       = radius,
            speed        = Random.nextFloat() * (MAX_SPEED - MIN_SPEED) + MIN_SPEED,
            amplitude    = Random.nextFloat() * (MAX_AMPLITUDE - MIN_AMPLITUDE) + MIN_AMPLITUDE,
            baseX        = startX,
            phase        = Random.nextFloat() * (2f * PI.toFloat()),
            color        = BUBBLE_COLORS[Random.nextInt(BUBBLE_COLORS.size)],
            shimmerAngle = Random.nextFloat() * 360f,
        )
    }
}