package com.apps.bubblepopping.view

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.bubblepopping.data.ProgressRepository
import com.apps.bubblepopping.data.prefs.GameSettings
import com.apps.bubblepopping.data.prefs.SettingsRepository
import com.apps.bubblepopping.game.BubbleSize
import com.apps.bubblepopping.game.ComboTracker
import com.apps.bubblepopping.game.GameEvent
import com.apps.bubblepopping.game.GameOverReason
import com.apps.bubblepopping.game.GameRules
import com.apps.bubblepopping.game.SpawnConfig
import com.apps.bubblepopping.view.home.Difficulty
import com.apps.bubblepopping.view.play.component.Bubble
import com.apps.bubblepopping.view.play.component.BubbleType
import com.apps.bubblepopping.view.play.component.FloatingText
import com.apps.bubblepopping.view.play.component.PopAnimation
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Runs one Endless round at [difficulty].
 *
 * Pass a [seed] for a deterministic round (same seed → same bubbles), e.g. the
 * daily challenge or tests. [spawn] defaults to the difficulty's preset.
 */
class BubbleGameViewModel(
    val difficulty: Difficulty,
    private val progressRepository: ProgressRepository,
    settingsRepository: SettingsRepository,
    private val spawn: SpawnConfig = difficulty.spawn,
    private val seed: Long? = null,
) : ViewModel() {

    // ─────────────────────────────────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────────────────────────────────

    companion object {
        /** Breeze below this strength snaps to zero. */
        private const val BREEZE_CUTOFF = 0.003f
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exposed state
    // ─────────────────────────────────────────────────────────────────────────

    val bubbles        = mutableStateListOf<Bubble>()
    val popAnimations  = mutableStateListOf<PopAnimation>()
    val floatingTexts  = mutableStateListOf<FloatingText>()

    /** Current breeze, from -1 (full left) to +1 (full right). */
    var breeze         by mutableStateOf(0f);       private set
    var score          by mutableStateOf(0);         private set
    var missedCount    by mutableStateOf(0);         private set
    var isGameOver     by mutableStateOf(false);     private set
    /** Increments every simulated game frame (not while paused, frozen or over). */
    var frameCount     by mutableStateOf(0);         private set
    /** Increments every frame effects animate — also after game over, so bursts finish. */
    var effectFrame    by mutableStateOf(0);         private set
    /** True when the round that just ended beat the saved best score. */
    var isNewBest      by mutableStateOf(false);     private set

    /** Current combo count; 0 when no combo is running. */
    var combo          by mutableStateOf(0);         private set
    /** Fraction of the combo window left, 1 → 0. Drives the HUD combo meter. */
    var comboTimeLeft  by mutableStateOf(0f);        private set
    /** Screen-shake translation in pixels; [Offset.Zero] when not shaking. */
    var shakeOffset    by mutableStateOf(Offset.Zero); private set

    val livesLeft: Int get() = GameRules.MAX_LIVES - missedCount
    val comboMultiplier: Int get() = ComboTracker.multiplierFor(combo)

    private val _isPaused = MutableStateFlow(false)
    val isPaused  = _isPaused.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    /** Everything that happens during the round — see [GameEvent]. */
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    /** Saved best score for [difficulty], or null if never played. */
    val bestScore: StateFlow<Int?> = progressRepository.bestEndlessScore(difficulty)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val settings: StateFlow<GameSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, GameSettings())

    // ─────────────────────────────────────────────────────────────────────────
    // Private state
    // ─────────────────────────────────────────────────────────────────────────

    private val comboTracker    = ComboTracker()
    private var random          = newRandom()
    private var spawnTimer      = 0f
    private var playTime        = 0f
    private var lastTickSec     = 0
    private var bubblesPopped   = 0
    private var nextEffectId    = 0L
    private var canvasWidth     = 400f
    private var canvasHeight    = 800f
    private var canvasTopOffset = 0f   // px below which bubbles are in the active game area
    private var density         = 1f

    // Effects run on real time, separate from game time.
    private var hitStopLeft     = 0f
    private var shakeTimeLeft   = 0f
    private var shakeDuration   = 0f
    private var shakeStrengthPx = 0f
    private var shakeClock      = 0f

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Update screen dimensions, the Y-offset where the game area begins (below
     * the HUD), and the screen density used to convert dp sizes to pixels.
     */
    fun setCanvasBounds(width: Float, height: Float, topOffset: Float = 0f, density: Float = 1f) {
        if (width > 0f && height > 0f && density > 0f) {
            canvasWidth     = width
            canvasHeight    = height
            canvasTopOffset = topOffset
            this.density    = density
        }
    }

    fun togglePause() {
        if (!isGameOver) {
            _isPaused.value = !_isPaused.value
        }
    }

    /** Advance one frame. Call every frame, including after game over (effects keep animating). */
    fun update(delta: Float) {
        if (_isPaused.value) return
        val dt = delta.coerceAtMost(GameRules.MAX_DELTA)

        effectsStep(dt)
        if (isGameOver) return
        if (hitStopLeft > 0f) {
            hitStopLeft -= dt
            return
        }

        spawnStep(dt)
        moveStep(dt)
        cullStep()
        breezeDecayStep(dt)
        tickStep(dt)
        comboStep()

        frameCount++
    }

    fun tryPop(offset: Offset): Boolean {
        if (isGameOver || isPaused.value) return false
        val minHitRadius = GameRules.MIN_HIT_RADIUS_DP * density

        val hit = bubbles.firstOrNull { bubble ->
            val dx        = offset.x - bubble.x
            val dy        = offset.y - bubble.y
            val hitRadius = bubble.radius.coerceAtLeast(minHitRadius)
            (dx * dx + dy * dy) <= (hitRadius * hitRadius)
        }
        emit(GameEvent.Tapped(hit = hit != null))

        hit?.let { bubble ->
            bubbles.remove(bubble)
            bubblesPopped++
            if (bubble.type == BubbleType.POISON) popPoison(bubble) else popScoring(bubble)
        }
        return hit != null
    }

    /** Push all bubbles sideways. [direction] is -1 (left) to +1 (right). */
    fun triggerBreeze(direction: Float) {
        if (isGameOver) return
        breeze = direction.coerceIn(-1f, 1f)
    }

    fun restartGame() {
        bubbles.clear()
        popAnimations.clear()
        floatingTexts.clear()
        comboTracker.reset()
        score         = 0
        missedCount   = 0
        isGameOver    = false
        isNewBest     = false
        _isPaused.value = false
        breeze        = 0f
        combo         = 0
        comboTimeLeft = 0f
        shakeOffset   = Offset.Zero
        spawnTimer    = 0f
        playTime      = 0f
        lastTickSec   = 0
        bubblesPopped = 0
        hitStopLeft   = 0f
        shakeTimeLeft = 0f
        random        = newRandom()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Popping
    // ─────────────────────────────────────────────────────────────────────────

    /** Normal and heart bubbles: extend the combo and score with its multiplier. */
    private fun popScoring(bubble: Bubble) {
        val previous   = comboTracker.combo
        val newCombo   = comboTracker.onPop(playTime)
        val multiplier = ComboTracker.multiplierFor(newCombo)
        val points     = bubble.size.points * multiplier

        if (bubble.type == BubbleType.HEART && missedCount > 0) missedCount--
        score += points
        syncComboState()

        val intensity = 1f + minOf(newCombo, GameRules.MAX_BURST_COMBO) * GameRules.BURST_GROWTH_PER_COMBO
        addPopAnimation(bubble, intensity)
        floatingTexts.add(
            FloatingText(
                id = "text_${nextEffectId++}",
                text = if (multiplier > 1) "+$points ×$multiplier" else "+$points",
                x = bubble.x,
                y = bubble.y - bubble.radius,
                color = if (bubble.type == BubbleType.HEART) HeartPopupColor else ScorePopupColor,
            )
        )
        ComboTracker.milestoneText(newCombo)?.let { text ->
            floatingTexts.add(
                FloatingText(
                    id = "text_${nextEffectId++}",
                    text = text,
                    x = bubble.x,
                    y = bubble.y - bubble.radius - GameRules.MILESTONE_TEXT_OFFSET_DP * density,
                    color = ComboColor,
                    isMilestone = true,
                )
            )
            hitStopLeft = GameRules.HIT_STOP_SECONDS
            startShake(GameRules.SHAKE_MILESTONE_DP, GameRules.SHAKE_MILESTONE_SECONDS)
        }
        if (bubble.size == BubbleSize.LARGE) {
            startShake(GameRules.SHAKE_LARGE_POP_DP, GameRules.SHAKE_LARGE_POP_SECONDS)
        }

        emit(GameEvent.Popped(bubble, points, newCombo))
        emit(GameEvent.ComboChanged(newCombo, previous))
    }

    private fun popPoison(bubble: Bubble) {
        breakCombo()
        addPopAnimation(bubble, intensity = 1f)
        startShake(GameRules.SHAKE_POISON_DP, GameRules.SHAKE_POISON_SECONDS)
        emit(GameEvent.Popped(bubble, points = 0, combo = 0))
        endGame(GameOverReason.POISON)
    }

    private fun addPopAnimation(bubble: Bubble, intensity: Float) {
        popAnimations.add(
            PopAnimation(
                id = "${bubble.id}_pop",
                x = bubble.x,
                y = bubble.y,
                radius = bubble.radius,
                color = bubble.color,
                intensity = intensity,
            )
        )
    }

    private fun breakCombo() {
        val broken = comboTracker.breakCombo()
        if (broken > 0) {
            syncComboState()
            emit(GameEvent.ComboChanged(combo = 0, previous = broken, broken = true))
        }
    }

    private fun syncComboState() {
        combo = comboTracker.combo
        comboTimeLeft = comboTracker.timeLeftFraction(playTime)
    }

    private fun startShake(strengthDp: Float, seconds: Float) {
        val strengthPx = strengthDp * density
        val currentPx = if (shakeDuration > 0f) shakeStrengthPx * shakeTimeLeft / shakeDuration else 0f
        if (strengthPx < currentPx) return   // a stronger shake is already running
        shakeStrengthPx = strengthPx
        shakeDuration   = seconds
        shakeTimeLeft   = seconds
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private step functions
    // ─────────────────────────────────────────────────────────────────────────

    private fun effectsStep(dt: Float) {
        for (anim in popAnimations) anim.progress += dt * GameRules.POP_ANIM_SPEED
        popAnimations.removeAll { it.progress >= 1f }

        for (text in floatingTexts) {
            val speed = if (text.isMilestone) GameRules.MILESTONE_TEXT_SPEED else GameRules.FLOATING_TEXT_SPEED
            text.progress += dt * speed
        }
        floatingTexts.removeAll { it.progress >= 1f }

        if (shakeTimeLeft > 0f) {
            shakeTimeLeft = (shakeTimeLeft - dt).coerceAtLeast(0f)
            shakeClock += dt
            val strength = shakeStrengthPx * shakeTimeLeft / shakeDuration
            shakeOffset = Offset(sin(shakeClock * 90f) * strength, cos(shakeClock * 73f) * strength)
        } else if (shakeOffset != Offset.Zero) {
            shakeOffset = Offset.Zero
        }

        effectFrame++
    }

    private fun spawnStep(dt: Float) {
        val interval = (spawn.spawnInterval - score * spawn.spawnRampPerPoint)
            .coerceAtLeast(spawn.minSpawnInterval)
        spawnTimer += dt
        if (spawnTimer >= interval) {
            spawnTimer = 0f
            bubbles.add(createBubble())
        }
    }

    private fun moveStep(dt: Float) {
        val breezeVelocity = breeze * GameRules.BREEZE_SPEED * canvasWidth
        for (bubble in bubbles) {
            bubble.y -= bubble.speed * dt
            bubble.baseX += breezeVelocity * dt
            bubble.x = bubble.baseX +
                    sin(bubble.y / density * GameRules.WOBBLE_FREQUENCY + bubble.phase) * bubble.amplitude
        }
    }

    private fun cullStep() {
        // Cull bubbles that have risen above the game area (below the HUD, not absolute 0).
        val escaped = bubbles.filter { it.y + it.radius < canvasTopOffset }
        if (escaped.isEmpty()) return
        bubbles.removeAll(escaped.toSet())
        for (bubble in escaped) {
            val lifeLost = bubble.type == BubbleType.NORMAL
            if (lifeLost) {
                missedCount++
                breakCombo()
            }
            emit(GameEvent.Missed(bubble, lifeLost))
        }
        if (missedCount >= GameRules.MAX_LIVES) endGame(GameOverReason.OUT_OF_LIVES)
    }

    private fun breezeDecayStep(dt: Float) {
        breeze = if (abs(breeze) > BREEZE_CUTOFF) breeze * (1f - dt * GameRules.BREEZE_DECAY) else 0f
    }

    private fun tickStep(dt: Float) {
        playTime += dt
        val seconds = playTime.toInt()
        if (seconds > lastTickSec) {
            lastTickSec = seconds
            emit(GameEvent.Tick(seconds))
        }
    }

    private fun comboStep() {
        val previous = comboTracker.combo
        if (comboTracker.expireIfStale(playTime)) emit(GameEvent.ComboChanged(combo = 0, previous = previous))
        syncComboState()
    }

    private fun endGame(reason: GameOverReason) {
        if (isGameOver) return
        isGameOver = true
        isNewBest  = score > 0 && score > (bestScore.value ?: 0)
        emit(GameEvent.GameOver(reason, score, isNewBest))

        val finalScore = score
        val popped = bubblesPopped
        val bestCombo = comboTracker.best
        viewModelScope.launch {
            progressRepository.recordEndlessGame(difficulty, finalScore, popped, bestCombo)
        }
    }

    private fun emit(event: GameEvent) {
        _events.tryEmit(event)
    }

    private fun newRandom(): Random = seed?.let { Random(it) } ?: Random.Default

    private fun createBubble(): Bubble {
        val radiusDp = random.nextFloat() * (GameRules.MAX_RADIUS_DP - GameRules.MIN_RADIUS_DP) +
                GameRules.MIN_RADIUS_DP
        val radius   = radiusDp * density
        val startX   = random.nextFloat() * canvasWidth
        val maxSpeed = (spawn.maxSpeed + score * spawn.speedRampPerPoint).coerceAtMost(spawn.maxSpeedCap)
        val speed    = (random.nextFloat() * (maxSpeed - spawn.minSpeed) + spawn.minSpeed) * canvasHeight
        val amplitudeDp = random.nextFloat() * (GameRules.MAX_AMPLITUDE_DP - GameRules.MIN_AMPLITUDE_DP) +
                GameRules.MIN_AMPLITUDE_DP
        val type = rollBubbleType()
        return Bubble(
            id = random.nextLong().toString(),
            x = startX,
            y = canvasHeight + radius,
            radius = radius,
            size = GameRules.sizeForRadius(radiusDp),
            speed = speed,
            amplitude = amplitudeDp * density,
            baseX = startX,
            phase = random.nextFloat() * (2f * PI.toFloat()),
            color = BubbleColors[random.nextInt(BubbleColors.size)],
            shimmerAngle = random.nextFloat() * 360f,
            type = type,
        )
    }

    private fun rollBubbleType(): BubbleType {
        var roll = random.nextInt(spawn.typeWeights.values.sum())
        for ((type, weight) in spawn.typeWeights) {
            if (roll < weight) return type
            roll -= weight
        }
        error("unreachable: roll exceeded total weight")
    }
}
