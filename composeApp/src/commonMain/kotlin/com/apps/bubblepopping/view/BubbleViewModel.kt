package com.apps.bubblepopping.view

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.bubblepopping.data.MetaRepository
import com.apps.bubblepopping.data.ProgressRepository
import com.apps.bubblepopping.data.StageProgress
import com.apps.bubblepopping.data.prefs.GameSettings
import com.apps.bubblepopping.data.prefs.SettingsRepository
import com.apps.bubblepopping.game.BubbleSize
import com.apps.bubblepopping.game.ComboTracker
import com.apps.bubblepopping.game.EndlessUnlocks
import com.apps.bubblepopping.game.GameEvent
import com.apps.bubblepopping.game.GameMode
import com.apps.bubblepopping.game.GameOverReason
import com.apps.bubblepopping.game.GameRules
import com.apps.bubblepopping.game.ObjectiveTracker
import com.apps.bubblepopping.game.ReviveOffer
import com.apps.bubblepopping.game.RunTracker
import com.apps.bubblepopping.meta.DailyStageGenerator
import com.apps.bubblepopping.meta.RunRewards
import com.apps.bubblepopping.meta.RunSummary
import com.apps.bubblepopping.game.SpawnConfig
import com.apps.bubblepopping.game.WaveDirector
import com.apps.bubblepopping.stage.BossTrick
import com.apps.bubblepopping.meta.Booster
import com.apps.bubblepopping.stage.ObjectiveProgress
import com.apps.bubblepopping.view.play.component.BossBubble
import com.apps.bubblepopping.view.play.component.LightningBolt
import com.apps.bubblepopping.view.play.component.Confetti
import com.apps.bubblepopping.stage.StageCatalog
import com.apps.bubblepopping.stage.StageConfig
import com.apps.bubblepopping.stage.StageResult
import com.apps.bubblepopping.stage.World
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * Runs one round: Endless at a difficulty, a stage with objectives, or one of
 * the daily modes (a seeded Endless run, or a generated stage).
 *
 * [stage] defaults to the catalog entry for a [GameMode.Stage] (or today's
 * generated stage); tests can pass their own. [spawn] defaults to the stage's or
 * difficulty's preset. Pass a [seed] for a deterministic round (same seed → same bubbles).
 */
class BubbleGameViewModel(
    val mode: GameMode,
    private val progressRepository: ProgressRepository,
    settingsRepository: SettingsRepository,
    private val metaRepository: MetaRepository,
    val stage: StageConfig? = defaultStage(mode),
    private val spawn: SpawnConfig = stage?.spawn ?: defaultSpawn(mode),
    private val seed: Long? = stage?.seed ?: (mode as? GameMode.DailyChallenge)?.epochDay,
    /** Head starts bought for this attempt (a restart plays without them). */
    boosters: Set<Booster> = emptySet(),
) : ViewModel() {

    // ─────────────────────────────────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────────────────────────────────

    companion object {
        /** Breeze below this strength snaps to zero. */
        private const val BREEZE_CUTOFF = 0.003f
        /** Bomb blasts draw a much bigger burst than a normal pop. */
        private const val BLAST_INTENSITY = 3f
        /** Bubbles that cost a life when they escape. */
        private val LIFE_BUBBLES = setOf(BubbleType.NORMAL, BubbleType.ARMORED, BubbleType.GHOST, BubbleType.CHAIN)
        /** How soft a fading boss's fade edges are. */
        private const val FADE_EDGE = 0.2f
        private const val BOSS_POP_INTENSITY = 2.5f
        /** "STUNG!" appears this far below the boss's edge. */
        private const val BOSS_TEXT_GAP_DP = 16f
        private const val BOMB_DROP_SPACING_DP = 40f
        /** How quickly bosses catch up with their movement path (per second). */
        private const val BOSS_FOLLOW_RATE = 6f

        private const val CONFETTI_MIN_SPEED_DP = 150f
        private const val CONFETTI_EXTRA_SPEED_DP = 450f
        private const val CONFETTI_LIFT_DP = 250f
        private const val CONFETTI_GRAVITY_DP = 700f
        private const val CONFETTI_DRAG = 1.2f
        /** Endless new-best confetti bursts from this fraction of the screen height. */
        private const val NEW_BEST_CONFETTI_Y = 0.3f
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exposed state
    // ─────────────────────────────────────────────────────────────────────────

    val bubbles        = mutableStateListOf<Bubble>()
    val popAnimations  = mutableStateListOf<PopAnimation>()
    val floatingTexts  = mutableStateListOf<FloatingText>()
    /** Boss stages: every boss copy on screen (several once a boss splits). */
    val bosses         = mutableStateListOf<BossBubble>()
    val confetti       = mutableStateListOf<Confetti>()
    /** Bolts from a lightning bubble to the bubbles it zapped, fading out. */
    val lightningBolts = mutableStateListOf<LightningBolt>()

    /** Current breeze, from -1 (full left) to +1 (full right). */
    var breeze         by mutableStateOf(0f);       private set
    var score          by mutableStateOf(0);         private set
    var missedCount    by mutableStateOf(0);         private set
    var isGameOver     by mutableStateOf(false);     private set
    /** Increments every simulated game frame (not while paused, frozen or over). */
    var frameCount     by mutableStateOf(0);         private set
    /** Increments every frame effects animate — also after game over, so bursts finish. */
    var effectFrame    by mutableStateOf(0);         private set
    /** Set by [update] once the round is over and its slow motion, effects and revive offer are done. */
    private var isSettled by mutableStateOf(false)
    /**
     * True while the round is over and nothing on screen moves (no slow-motion
     * drift, bursts, texts, confetti or shake, and no revive on offer): the screen
     * can stop asking for frames. Observable, so it turns false again as soon as
     * a new effect appears or the round restarts.
     */
    val isAtRest: Boolean
        get() = isGameOver && isSettled && popAnimations.isEmpty() && floatingTexts.isEmpty() && confetti.isEmpty() &&
            lightningBolts.isEmpty()
    /** True when the round that just ended beat the saved best score. */
    var isNewBest      by mutableStateOf(false);     private set

    /** Current combo count; 0 when no combo is running. */
    var combo          by mutableStateOf(0);         private set
    /** Fraction of the combo window left, 1 → 0. Drives the HUD combo meter. */
    var comboTimeLeft  by mutableStateOf(0f);        private set
    /** Screen-shake translation in pixels; [Offset.Zero] when not shaking. */
    var shakeOffset    by mutableStateOf(Offset.Zero); private set

    /** Stage only: live progress of every objective, main goal first. */
    var objectives     by mutableStateOf<List<ObjectiveProgress>>(emptyList()); private set
    /** Stage only: whole seconds left on the timer; null when the round is untimed. */
    var timeLeftSec    by mutableStateOf(stage?.durationSec); private set
    /** Stage only: set when the stage ends. */
    var stageResult    by mutableStateOf<StageResult?>(null); private set

    /** True while a shield is ready to absorb the next skull pop. */
    var shieldActive   by mutableStateOf(false);     private set
    /** Seconds of freeze left; bubbles move slowly while above 0. */
    var freezeTimeLeft by mutableStateOf(0f);        private set
    /** Seconds of double points left (×2 bubbles); every point counts twice while above 0. */
    var doubleTimeLeft by mutableStateOf(0f);        private set
    /** Seconds the player's taps are disabled after a boss sting. */
    var tapLockLeft    by mutableStateOf(0f);        private set

    /** Endless only: the wave being played, or during a calm the one about to start. */
    var wave           by mutableStateOf(1);         private set
    /** Endless only: true during the break between waves (nothing spawns). */
    var isWaveCalm     by mutableStateOf(false);     private set
    /** Endless only: fraction of the current wave played, 0 → 1. */
    var waveProgress   by mutableStateOf(0f);        private set
    /** Coins, missions, chests and achievements this round earned; null until paid out. */
    var rewards        by mutableStateOf<RunRewards?>(null); private set
    /** Endless only: a difficulty this round unlocked, shown on the game-over screen. */
    var unlockedDifficulty by mutableStateOf<Difficulty?>(null); private set
    /** Why the round ended; null while it is running. */
    var gameOverReason by mutableStateOf<GameOverReason?>(null); private set
    /**
     * Set while the round is lost but a coin revive is on offer: [isGameOver] is
     * true, yet nothing is saved until the player declines or the offer runs out.
     */
    var reviveOffer    by mutableStateOf<ReviveOffer?>(null); private set
    /** True once a revive was offered this round (the result then appears without the slow-motion wait). */
    var reviveWasOffered by mutableStateOf(false);   private set
    /** True once this round's one revive was used. */
    var hasRevived     by mutableStateOf(false);     private set
    /** Seconds of protection left after a revive: bubbles are slowed and nothing can end the round. */
    var reviveGraceLeft by mutableStateOf(0f);       private set

    /** Theme for backgrounds and bubble colours. Endless uses the first world. */
    val world: World = stage?.world ?: World.SKY
    private val baseLives: Int = stage?.lives ?: GameRules.MAX_LIVES
    /** Lives this attempt starts with: the stage's, plus one with the +1 life booster. */
    var maxLives       by mutableStateOf(baseLives); private set
    val livesLeft: Int get() = maxLives - missedCount
    val comboMultiplier: Int get() = ComboTracker.multiplierFor(combo)

    /**
     * How intense the music should be, 0 → 1: rises with the combo, later Endless
     * waves, boss fights and a stage timer running out.
     */
    val musicIntensity: Float get() {
        val fromCombo = combo / GameRules.MUSIC_FULL_COMBO.toFloat()
        val fromWave = if (stage == null) (wave - 1) / GameRules.MUSIC_FULL_WAVE.toFloat() else 0f
        val fromBoss = if (bosses.isNotEmpty()) GameRules.MUSIC_BOSS_INTENSITY else 0f
        val fromTimer = timeLeftSec?.let { if (it <= GameRules.MUSIC_URGENT_SECONDS) 1f else 0f } ?: 0f
        return maxOf(fromCombo, fromWave, fromBoss, fromTimer).coerceIn(0f, 1f)
    }

    private val _isPaused = MutableStateFlow(false)
    val isPaused  = _isPaused.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    /** Everything that happens during the round — see [GameEvent]. */
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    /**
     * Saved best score for this difficulty, stage or day's challenge, or null if
     * never played / cleared. Daily Stages have none.
     */
    val bestScore: StateFlow<Int?> = when (mode) {
        is GameMode.Endless -> progressRepository.bestEndlessScore(mode.difficulty)
        is GameMode.Stage -> progressRepository.stageProgress().map { it[mode.stageId]?.bestScore }
        is GameMode.DailyChallenge -> metaRepository.player().map { it.dailyChallengeBest(mode.epochDay) }
        is GameMode.DailyStage -> flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val settings: StateFlow<GameSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, GameSettings())

    /** The player's coins (for the revive offer); null while loading. */
    val coins: StateFlow<Int?> = metaRepository.player().map { it.coins }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Stage progress decides which bubble types and difficulties Endless has unlocked. */
    private val stageProgress: StateFlow<Map<Int, StageProgress>> = progressRepository.stageProgress()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /**
     * Stages use their own mix; Endless adds every special bubble unlocked in Stages.
     * The Daily Challenge keeps the classic mix so every player gets the same round.
     */
    private val typeWeights: StateFlow<Map<BubbleType, Int>> =
        if (mode !is GameMode.Endless) MutableStateFlow(spawn.typeWeights)
        else stageProgress
            .map { EndlessUnlocks.typeWeights(spawn.typeWeights, it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, spawn.typeWeights)

    // ─────────────────────────────────────────────────────────────────────────
    // Private state
    // ─────────────────────────────────────────────────────────────────────────

    private val comboTracker    = ComboTracker()
    private var objectiveTracker = stage?.let { ObjectiveTracker() }
    private var runTracker      = RunTracker()
    /**
     * Endless and paced stages: paces the round in waves instead of ramping with the
     * score. Only Endless announces its waves; a stage's calms just pass quietly.
     */
    private val waveDirector    = when {
        stage == null -> WaveDirector(spawn)
        spawn.inWaves -> WaveDirector.forStage(spawn)
        else -> null
    }
    /** Seconds of slow drift left after the round ends. */
    private var gameOverSlowmoLeft = 0f
    /** Stage only: seconds a revive added to the timer. */
    private var bonusSeconds    = 0
    /** The boosters this attempt was started with, and whether the ice booster's bubble is still to rise. */
    private var activeBoosters  = boosters
    private var iceBoosterPending = false
    private var lightningBoosterPending = false
    /** True while a revive's coins are being taken (the offer stops counting down). */
    private var revivePending   = false
    private var random         = newRandom()
    private var spawnTimer      = 0f
    private var playTime        = 0f
    private var lastTickSec     = 0
    private var bubblesPopped   = 0
    private var nextEffectId    = 0L
    private var canvasWidth     = 400f
    private var canvasHeight    = 800f
    private var canvasTopOffset = 0f   // px below which bubbles are in the active game area
    private var density         = 1f

    // Boss state
    private var bossSpawned     = false
    private var bossClock       = 0f
    /** Play time when the boss arrived, for its enrage timer. */
    private var bossArrivedAt   = 0f
    private var bossEnraged     = false
    /** The fight's current phase (0–2) and the taps on the boss since it began. */
    private var bossPhaseNow    = 0
    private var bossHitsInPhase = 0
    private var bossDamageDealt = 0
    /** Seconds until another ice bubble rises while an ice shield holds. */
    private var iceRespawnLeft  = 0f
    /** Seconds until bomb armor drops its next bombs. */
    private var blastArmorBombsIn = 0f
    /** Slow-motion seconds left after the final blow; the stage ends when it runs out. */
    private var celebrationLeft = 0f
    /** Cosmetic randomness (confetti) — kept apart so it never changes a seeded round. */
    private val effectRandom    = Random.Default

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
        if (isGameOver) {
            gameOverSlowmoStep(dt)
            reviveCountdownStep(dt)
            isSettled = isGameOver && reviveOffer == null && gameOverSlowmoLeft <= 0f &&
                    popAnimations.isEmpty() && floatingTexts.isEmpty() && confetti.isEmpty() && shakeTimeLeft <= 0f
            return
        }
        isSettled = false
        if (hitStopLeft > 0f) {
            hitStopLeft -= dt
            return
        }

        // Freeze and the boss-defeat slow motion slow the bubbles (and spawning), not the clock.
        var bubbleDt = if (freezeTimeLeft > 0f) dt * GameRules.FREEZE_SLOWDOWN else dt
        if (celebrationLeft > 0f) {
            bubbleDt *= GameRules.CELEBRATION_SLOWMO
            celebrationLeft = (celebrationLeft - dt).coerceAtLeast(0f)
        }
        if (reviveGraceLeft > 0f) {
            bubbleDt *= GameRules.REVIVE_GRACE_SLOWDOWN
            reviveGraceLeft = (reviveGraceLeft - dt).coerceAtLeast(0f)
        }
        freezeTimeLeft = (freezeTimeLeft - dt).coerceAtLeast(0f)
        doubleTimeLeft = (doubleTimeLeft - dt).coerceAtLeast(0f)
        tapLockLeft = (tapLockLeft - dt).coerceAtLeast(0f)

        spawnBossIfNeeded()
        bossStep(bubbleDt)
        waveStep(dt)
        spawnStep(bubbleDt)
        if (iceBoosterPending && playTime >= Booster.ICE_DELAY_SECONDS) {
            iceBoosterPending = false
            bubbles.add(createBubble(forcedType = BubbleType.FREEZE))
        }
        if (lightningBoosterPending && playTime >= Booster.ICE_DELAY_SECONDS) {
            lightningBoosterPending = false
            bubbles.add(createBubble(forcedType = BubbleType.LIGHTNING))
        }
        moveStep(bubbleDt)
        fadeStep()
        chainStep()
        windSaveStep()
        cullStep()
        breezeDecayStep(dt)
        tickStep(dt)
        comboStep()
        timerStep()
        checkStageEnd()

        frameCount++
    }

    /**
     * Pops the bubble under [offset], if any. A press that hits nothing is not
     * a miss yet — it may become a swipe; call [onMissedTap] if it doesn't.
     */
    fun tryPop(offset: Offset): Boolean {
        if (isGameOver || isPaused.value || tapLockLeft > 0f) return false
        // Accessibility option: every bubble gets a bigger touch area.
        val hitScale = if (settings.value.largeHitArea) GameRules.LARGE_HIT_AREA_SCALE else 1f
        val minHitRadius = GameRules.MIN_HIT_RADIUS_DP * density * hitScale

        // Bubbles first: they're drawn over the boss, so a skull in front of it gets popped.
        val hit = bubbles.firstOrNull { bubble ->
            // A faded ghost can't be touched: the tap goes through it.
            if (bubble.type == BubbleType.GHOST && bubble.visibility < GameRules.GHOST_TAPPABLE) return@firstOrNull false
            val dx        = offset.x - bubble.x
            val dy        = offset.y - bubble.y
            val hitRadius = (bubble.radius * hitScale).coerceAtLeast(minHitRadius)
            (dx * dx + dy * dy) <= (hitRadius * hitRadius)
        }
        if (hit != null) {
            emit(GameEvent.Tapped(hit = true))
            // An armored bubble only cracks at the first tap.
            if (hit.type == BubbleType.ARMORED && !hit.cracked) {
                crackArmor(hit)
                return true
            }
            bubbles.remove(hit)
            bubblesPopped++
            if (hit.type == BubbleType.POISON) popPoison(hit) else popScoring(hit)
            checkStageEnd()
            return true
        }

        val boss = bosses.lastOrNull {
            it.visibility >= GameRules.GHOST_TAPPABLE && hypot(offset.x - it.x, offset.y - it.y) <= it.radius
        } ?: return false
        emit(GameEvent.Tapped(hit = true))
        hitBossByTap(boss)
        checkStageEnd()
        return true
    }

    /** A tap that hit no bubble and wasn't a swipe. Taps blocked by a sting don't count. */
    fun onMissedTap() {
        if (isGameOver || isPaused.value || tapLockLeft > 0f) return
        emit(GameEvent.Tapped(hit = false))
    }

    /** Push all bubbles sideways. [direction] is -1 (left) to +1 (right). */
    fun triggerBreeze(direction: Float) {
        if (isGameOver) return
        breeze = direction.coerceIn(-1f, 1f)
    }

    /**
     * A horizontal swipe of [dragFraction] of the screen width (negative = left).
     * Stronger swipes give stronger wind; a weaker swipe never weakens current wind
     * in the same direction.
     */
    fun swipeWind(dragFraction: Float) {
        if (isGameOver || isPaused.value) return
        val strength = (dragFraction / GameRules.FULL_WIND_SWIPE_FRACTION).coerceIn(-1f, 1f)
        val sameDirection = strength * breeze > 0f
        if (!sameDirection || abs(strength) > abs(breeze)) breeze = strength
    }

    /**
     * Pays for the [reviveOffer] and plays on. If the coins can't be taken after
     * all, the round ends as if declined.
     */
    fun revive() {
        val offer = reviveOffer ?: return
        if (revivePending) return
        revivePending = true
        viewModelScope.launch {
            val paid = metaRepository.spendCoins(offer.cost)
            revivePending = false
            if (reviveOffer != offer) return@launch
            if (paid) resumeAfterRevive(offer.reason) else declineRevive()
        }
    }

    /** Turns down the [reviveOffer]: the round ends and is saved as usual. */
    fun declineRevive() {
        val offer = reviveOffer ?: return
        if (revivePending) return
        reviveOffer = null
        finishRound(offer.reason)
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
        objectiveTracker = stage?.let { ObjectiveTracker() }
        timeLeftSec   = stage?.durationSec
        stageResult   = null
        shieldActive  = false
        freezeTimeLeft = 0f
        doubleTimeLeft = 0f
        tapLockLeft   = 0f
        bosses.clear()
        confetti.clear()
        bossSpawned   = false
        bossClock     = 0f
        bossArrivedAt = 0f
        bossEnraged   = false
        bossPhaseNow  = 0
        bossHitsInPhase = 0
        bossDamageDealt = 0
        iceRespawnLeft = 0f
        blastArmorBombsIn = 0f
        lightningBolts.clear()
        chainWaiting.clear()
        celebrationLeft = 0f
        gameOverSlowmoLeft = 0f
        waveDirector?.reset()
        wave          = 1
        isWaveCalm    = false
        waveProgress  = 0f
        gameOverReason = null
        reviveOffer   = null
        reviveWasOffered = false
        hasRevived    = false
        reviveGraceLeft = 0f
        revivePending = false
        bonusSeconds  = 0
        rewards       = null
        runTracker    = RunTracker()
        unlockedDifficulty = null
        // Boosters were bought for one attempt.
        activeBoosters = emptySet()
        applyBoosters()
        refreshObjectives()
    }

    init {
        applyBoosters()
        refreshObjectives()
    }

    /** Sets up the round's head starts: an extra life, a shield, an early ice bubble, more time. */
    private fun applyBoosters() {
        maxLives = baseLives + if (Booster.EXTRA_LIFE in activeBoosters) 1 else 0
        if (Booster.SHIELD in activeBoosters) shieldActive = true
        iceBoosterPending = Booster.ICE in activeBoosters
        lightningBoosterPending = Booster.LIGHTNING in activeBoosters
        if (Booster.CLOCK in activeBoosters && stage?.durationSec != null) {
            bonusSeconds = Booster.CLOCK_SECONDS
            timerStep()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Popping
    // ─────────────────────────────────────────────────────────────────────────

    /** Every non-skull bubble: extend the combo, score with its multiplier, apply its effect. */
    private fun popScoring(bubble: Bubble) {
        val previous   = comboTracker.combo
        val newCombo   = comboTracker.onPop(playTime)
        val multiplier = ComboTracker.multiplierFor(newCombo) * scoreBoost
        val points     = GameRules.basePoints(bubble) * multiplier

        score += points
        syncComboState()

        val intensity = 1f + minOf(newCombo, GameRules.MAX_BURST_COMBO) * GameRules.BURST_GROWTH_PER_COMBO
        addPopAnimation(bubble, intensity)
        addFloatingText(
            text = if (multiplier > 1) "+$points ×$multiplier" else "+$points",
            x = bubble.x,
            y = bubble.y - bubble.radius,
            color = popupColor(bubble.type),
        )
        ComboTracker.milestoneText(newCombo)?.let { text ->
            addFloatingText(
                text = text,
                x = bubble.x,
                y = bubble.y - bubble.radius - GameRules.MILESTONE_TEXT_OFFSET_DP * density,
                color = ComboColor,
                isMilestone = true,
            )
            hitStopLeft = GameRules.HIT_STOP_SECONDS
            startShake(GameRules.SHAKE_MILESTONE_DP, GameRules.SHAKE_MILESTONE_SECONDS)
        }
        if (bubble.size == BubbleSize.LARGE) {
            startShake(GameRules.SHAKE_LARGE_POP_DP, GameRules.SHAKE_LARGE_POP_SECONDS)
        }

        emit(GameEvent.Popped(bubble, points, newCombo))
        emit(GameEvent.ComboChanged(newCombo, previous))

        when (bubble.type) {
            BubbleType.BOMB -> explode(bubble, multiplier, newCombo)
            BubbleType.LIGHTNING -> zap(bubble)
            else -> applyEffect(bubble)
        }
    }

    /** What a popped heart / ice / shield / splitter / clock / ×2 bubble does. Bombs are handled by [explode]. */
    private fun applyEffect(bubble: Bubble) {
        when (bubble.type) {
            BubbleType.HEART -> if (missedCount > 0) missedCount--
            BubbleType.FREEZE -> {
                freezeTimeLeft = GameRules.FREEZE_SECONDS
                breakIceShield()
            }
            BubbleType.SHIELD -> shieldActive = true
            BubbleType.SPLITTER -> split(bubble)
            BubbleType.CLOCK -> addTime(bubble)
            BubbleType.DOUBLE -> {
                doubleTimeLeft += GameRules.DOUBLE_SECONDS
                addFloatingText("×${GameRules.DOUBLE_MULTIPLIER} POINTS!", bubble.x, bubble.y - bubble.radius, ComboColor, isMilestone = true)
            }
            BubbleType.NORMAL, BubbleType.GOLDEN, BubbleType.BOMB, BubbleType.POISON,
            BubbleType.CHAIN -> chainPopped(bubble)
            BubbleType.LIGHTNING, BubbleType.ARMORED, BubbleType.GHOST -> Unit
        }
    }

    /** Chain pairs where one bubble is popped and the other must follow in time, by link id. */
    private class ChainWait(val popped: Bubble, val at: Float)
    private val chainWaiting = mutableMapOf<String, ChainWait>()

    /** A chain bubble spawns with its partner beside it, tied to it. */
    private fun addChainPair(first: Bubble) {
        val link = "chain_${nextEffectId++}"
        val gap = GameRules.CHAIN_GAP_DP * density
        // The partner goes on whichever side has room.
        val side = if (first.baseX + gap < canvasWidth - first.radius) 1f else -1f
        bubbles += first.copy(linkId = link)
        bubbles += first.copy(id = "${first.id}_pair", x = first.x + side * gap, baseX = first.baseX + side * gap, linkId = link)
    }

    /** One bubble of a pair popped: the bonus if its partner went just before, otherwise the clock starts. */
    private fun chainPopped(bubble: Bubble) {
        val link = bubble.linkId ?: return
        if (chainWaiting.remove(link) != null) {
            val points = GameRules.CHAIN_BONUS * ComboTracker.multiplierFor(comboTracker.combo) * scoreBoost
            score += points
            addFloatingText("PAIR! +$points", bubble.x, bubble.y - bubble.radius, ComboColor, isMilestone = true)
            emit(GameEvent.ChainPaired(points))
        } else if (bubbles.any { it.linkId == link }) {
            chainWaiting[link] = ChainWait(bubble, playTime)
        }
    }

    /** A pair left half-popped too long: the popped bubble comes back beside its partner. */
    private fun chainStep() {
        if (chainWaiting.isEmpty()) return
        val gap = GameRules.CHAIN_GAP_DP * density
        val expired = chainWaiting.filterValues { playTime - it.at > GameRules.CHAIN_WINDOW_SECONDS }
        for ((link, wait) in expired) {
            chainWaiting.remove(link)
            val partner = bubbles.firstOrNull { it.linkId == link } ?: continue
            val side = if (partner.baseX + gap < canvasWidth - partner.radius) 1f else -1f
            bubbles += wait.popped.copy(
                id = "${wait.popped.id}_back${nextEffectId++}",
                x = partner.x + side * gap,
                y = partner.y,
                baseX = partner.baseX + side * gap,
            )
            addFloatingText("BACK!", partner.x + side * gap, partner.y - partner.radius, ArmorTextColor)
        }
    }

    /**
     * Ghost bubbles fade in and out on their own beat; a fading boss ([BossTrick.Fade])
     * fades on the boss clock — unless a lightning strike has just shown it.
     */
    private fun fadeStep() {
        for (bubble in bubbles) {
            if (bubble.type != BubbleType.GHOST) continue
            val beat = 2f * PI.toFloat() * playTime / GameRules.GHOST_PERIOD_SECONDS + bubble.phase
            bubble.visibility = 0.5f + 0.5f * cos(beat)
        }
        if (bosses.isEmpty()) return
        val fade = stage?.boss?.phases?.get(bossPhaseNow)?.filterIsInstance<BossTrick.Fade>()?.firstOrNull()
        for (boss in bosses) {
            boss.visibility = when {
                fade == null -> 1f
                boss.revealLeft > 0f -> 1f
                else -> {
                    // Visible for visibleShare of each fade, with soft edges either side.
                    val raw = 0.5f + 0.5f * cos(2f * PI.toFloat() * playTime / GameRules.PHANTOM_FADE_SECONDS)
                    ((raw - (1f - fade.visibleShare)) / FADE_EDGE + 0.5f).coerceIn(0f, 1f)
                }
            }
        }
    }

    /** The first tap on an armored bubble: the armor cracks and the combo carries on. */
    private fun crackArmor(bubble: Bubble) {
        bubble.cracked = true
        val previous = comboTracker.combo
        val newCombo = comboTracker.onPop(playTime)
        syncComboState()
        addFloatingText("CRACK!", bubble.x, bubble.y - bubble.radius, ArmorTextColor)
        startShake(GameRules.SHAKE_BOSS_HIT_DP, GameRules.SHAKE_BOSS_HIT_SECONDS)
        emit(GameEvent.ArmorCracked)
        emit(GameEvent.ComboChanged(newCombo, previous))
    }

    /**
     * A lightning bubble: bolts to the [GameRules.LIGHTNING_ZAPS] nearest bubbles
     * (never skulls or bombs), each popping as part of the combo — armor and all — and a
     * strike on every boss.
     */
    private fun zap(source: Bubble) {
        val from = Offset(source.x, source.y)
        val targets = bubbles
            // Never skulls, and never bombs: a zapped bomb could blow up a skull nobody saw coming.
            .filter { it.type != BubbleType.POISON && it.type != BubbleType.BOMB }
            .sortedBy { hypot(it.x - source.x, it.y - source.y) }
            .take(GameRules.LIGHTNING_ZAPS)
        for (target in targets) {

            bubbles.remove(target)
            bubblesPopped++
            lightningBolts += LightningBolt(nextEffectId++, from, Offset(target.x, target.y), GameRules.BOLT_SECONDS)
            val previous = comboTracker.combo
            val newCombo = comboTracker.onPop(playTime)
            val multiplier = ComboTracker.multiplierFor(newCombo) * scoreBoost
            val points = GameRules.basePoints(target) * multiplier
            score += points
            addPopAnimation(target, intensity = 1f + minOf(newCombo, GameRules.MAX_BURST_COMBO) * GameRules.BURST_GROWTH_PER_COMBO)
            emit(GameEvent.Popped(target, points, newCombo, blast = true))
            emit(GameEvent.ComboChanged(newCombo, previous))
            applyEffect(target)
        }
        syncComboState()
        for (boss in bosses.toList()) {
            lightningBolts += LightningBolt(nextEffectId++, from, Offset(boss.x, boss.y), GameRules.BOLT_SECONDS)
            val points = GameRules.BOSS_HIT_POINTS * boss.config.lightningDamage * scoreBoost
            score += points
            // A fading boss shows itself when struck.
            boss.revealLeft = GameRules.PHANTOM_REVEAL_SECONDS
            damageBoss(boss, boss.config.lightningDamage, points, fromBlast = true)
        }
        startShake(GameRules.SHAKE_LARGE_POP_DP * 2f, GameRules.SHAKE_LARGE_POP_SECONDS * 2f)
        emit(GameEvent.LightningZapped(targets.size))
    }

    /** ×2 bubbles: while one is running, every point counts double. */
    private val scoreBoost: Int get() = if (doubleTimeLeft > 0f) GameRules.DOUBLE_MULTIPLIER else 1

    /** A clock bubble: more time on the stage's timer. */
    private fun addTime(bubble: Bubble) {
        if (stage?.durationSec == null) return
        bonusSeconds += GameRules.CLOCK_SECONDS
        timerStep()
        addFloatingText("+${GameRules.CLOCK_SECONDS}s", bubble.x, bubble.y - bubble.radius, ComboColor, isMilestone = true)
    }

    /** A skull ends the round — unless a shield absorbs it or a revive's grace is still on. */
    private fun popPoison(bubble: Bubble) {
        addPopAnimation(bubble, intensity = 1f)
        if (reviveGraceLeft > 0f) return
        if (useShield(bubble)) return
        breakCombo()
        startShake(GameRules.SHAKE_POISON_DP, GameRules.SHAKE_POISON_SECONDS)
        emit(GameEvent.Popped(bubble, points = 0, combo = 0))
        endGame(GameOverReason.POISON)
    }

    /** Spends the shield on [skull] if one is up. Returns true if the skull was absorbed. */
    private fun useShield(skull: Bubble): Boolean {
        if (!shieldActive) return false
        shieldActive = false
        addFloatingText("SHIELD!", skull.x, skull.y - skull.radius, ShieldColor, isMilestone = true)
        emit(GameEvent.Popped(skull, points = 0, combo = comboTracker.combo))
        emit(GameEvent.ShieldUsed)
        return true
    }

    /**
     * Pops everything within reach of [bomb], skulls included. Bombs caught in
     * the blast go off too. Caught bubbles score with the bomb's [multiplier] but
     * don't extend the combo.
     */
    private fun explode(bomb: Bubble, multiplier: Int, combo: Int) {
        val blastRadius = GameRules.BOMB_RADIUS_DP * density
        val pendingBombs = ArrayDeque<Bubble>().apply { add(bomb) }
        var chain = 0
        var chainPoints = 0
        var hitSkull: Bubble? = null

        while (pendingBombs.isNotEmpty()) {
            val center = pendingBombs.removeFirst()
            // Blasts hurt bosses too — unless they're behind an ice shield.
            bosses.toList()
                .filter { !it.shielded }
                .filter { hypot(it.x - center.x, it.y - center.y) - it.radius <= blastRadius }
                .forEach { boss ->
                    val points = GameRules.BOSS_HIT_POINTS * boss.config.blastDamage * multiplier
                    score += points
                    chainPoints += points
                    damageBoss(boss, boss.config.blastDamage, points, fromBlast = true)
                }
            popAnimations.add(
                PopAnimation(
                    id = "blast_${nextEffectId++}",
                    x = center.x,
                    y = center.y,
                    radius = blastRadius / 3f,
                    color = center.color,
                    intensity = BLAST_INTENSITY,
                )
            )
            val caught = bubbles.filter { hypot(it.x - center.x, it.y - center.y) - it.radius <= blastRadius }
            bubbles.removeAll(caught.toSet())

            for (bubble in caught) {
                chain++
                bubblesPopped++
                addPopAnimation(bubble, intensity = 1f)
                if (bubble.type == BubbleType.POISON) {
                    if (!useShield(bubble) && hitSkull == null) hitSkull = bubble
                    continue
                }
                val points = GameRules.basePoints(bubble) * multiplier
                score += points
                chainPoints += points
                emit(GameEvent.Popped(bubble, points, combo, blast = true))
                if (bubble.type == BubbleType.BOMB) pendingBombs.add(bubble) else applyEffect(bubble)
            }
        }

        if (chain > 0) {
            addFloatingText(
                text = "CHAIN ×$chain  +$chainPoints",
                x = bomb.x,
                y = bomb.y - bomb.radius - GameRules.MILESTONE_TEXT_OFFSET_DP * density,
                color = ComboColor,
                isMilestone = true,
            )
        }
        startShake(GameRules.SHAKE_BOMB_DP, GameRules.SHAKE_BOMB_SECONDS)
        emit(GameEvent.BombExploded(chain))

        hitSkull?.takeIf { reviveGraceLeft <= 0f }?.let { skull ->
            breakCombo()
            emit(GameEvent.Popped(skull, points = 0, combo = 0))
            endGame(GameOverReason.POISON)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bosses
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * The boss arrives [GameRules.BOSS_ENTRY_DELAY_SECONDS] into a boss stage — normal
     * bubbles warm the round up first — and glides down into place from above the screen.
     */
    private fun spawnBossIfNeeded() {
        val config = stage?.boss ?: return
        if (bossSpawned || playTime < GameRules.BOSS_ENTRY_DELAY_SECONDS) return
        bossSpawned = true
        bossArrivedAt = playTime
        bosses.add(
            BossBubble(
                id = "boss_${nextEffectId++}",
                config = config,
                generation = 0,
                maxHp = config.hpPerGeneration.first(),
                fullRadius = config.radiusDp * density,
                motionPhase = 0f,
                x = canvasWidth / 2f,
                // Starts above the screen; [bossStep] glides it down to its path.
                y = -config.radiusDp * density,
            )
        )
        enterBossPhase(0, bosses.first())
    }

    /**
     * Bosses drift side to side and bob, faster as they lose HP — and faster still
     * once enraged. While an ice shield holds, fresh ice bubbles keep rising.
     */
    private fun bossStep(dt: Float) {
        if (bosses.isEmpty()) return
        val enrageAfter = stage?.boss?.enrageAfterSec
        if (!bossEnraged && enrageAfter != null && playTime - bossArrivedAt >= enrageAfter) enrage()
        if (bosses.any { it.shielded }) {
            iceRespawnLeft -= dt
            if (iceRespawnLeft <= 0f) riseIce()
        }
        val tricks = stage?.boss?.phases?.get(bossPhaseNow).orEmpty()
        // Bomb armor: fresh bombs keep dropping, the only way through.
        tricks.filterIsInstance<BossTrick.BlastArmor>().firstOrNull()?.let { armor ->
            blastArmorBombsIn -= dt
            if (blastArmorBombsIn <= 0f) {
                dropBombs(bosses.first(), armor.bombs)
                blastArmorBombsIn = armor.everySec * if (bossEnraged) 0.5f else 1f
            }
        }
        // Gravity: every bubble drifts sideways towards the boss.
        tricks.filterIsInstance<BossTrick.Gravity>().firstOrNull()?.let { gravity ->
            val pull = (gravity.strength * dt * if (bossEnraged) GameRules.BOSS_ENRAGE_SPEED else 1f).coerceAtMost(1f)
            val boss = bosses.first()
            for (bubble in bubbles) bubble.baseX += (boss.x - bubble.baseX) * pull
        }
        bossClock += dt * GameRules.BOSS_PHASE_SPEED[bossPhaseNow] * (if (bossEnraged) GameRules.BOSS_ENRAGE_SPEED else 1f)
        val bob = GameRules.BOSS_BOB_DP * density
        // Glide towards the path rather than snapping, so fresh split copies fly apart smoothly.
        val follow = (dt * BOSS_FOLLOW_RATE).coerceAtMost(1f)
        for (boss in bosses) {
            val range = (canvasWidth / 2f - boss.fullRadius).coerceAtLeast(0f)
            val targetX = canvasWidth / 2f + sin(bossClock * 0.7f + boss.motionPhase) * range
            val targetY = bossBaseY() + sin(bossClock * 1.3f + boss.motionPhase * 1.7f) * bob
            boss.x += (targetX - boss.x) * follow
            boss.y += (targetY - boss.y) * follow
            boss.hitFlash = (boss.hitFlash - dt).coerceAtLeast(0f)
            boss.revealLeft = (boss.revealLeft - dt).coerceAtLeast(0f)
        }
    }

    private fun bossBaseY() =
        canvasTopOffset + (canvasHeight - canvasTopOffset) * GameRules.BOSS_HEIGHT_FRACTION

    /** 0 above ⅔ of the boss's total HP, 1 above ⅓, 2 below. */
    private fun bossPhase(): Int {
        val totalHp = stage?.boss?.totalHp ?: return 0
        val damageFraction = bossDamageDealt.toFloat() / totalHp
        return when {
            damageFraction < 1f / 3f -> 0
            damageFraction < 2f / 3f -> 1
            else -> 2
        }
    }

    /** A tap on a boss: 1 damage, extends the combo, and may set off one of the boss's tricks. */
    private fun hitBossByTap(boss: BossBubble) {
        if (boss.shielded) {
            addFloatingText("BLOCKED", boss.x, boss.y - boss.radius, IceTextColor)
            return
        }
        if (boss.blastOnly) {
            addFloatingText("USE BOMBS!", boss.x, boss.y - boss.radius, ArmorTextColor)
            return
        }
        val previous   = comboTracker.combo
        val newCombo   = comboTracker.onPop(playTime)
        val points     = GameRules.BOSS_HIT_POINTS * ComboTracker.multiplierFor(newCombo) * scoreBoost
        score += points
        syncComboState()
        addFloatingText("+$points", boss.x, boss.y - boss.radius, ScorePopupColor)
        emit(GameEvent.ComboChanged(newCombo, previous))

        if (boss.plates > 0) {
            // Plates first: each takes a few taps, and the boss's HP waits behind them.
            boss.plateHits++
            boss.hitFlash = GameRules.BOSS_HIT_FLASH_SECONDS
            val tapsEach = boss.config.phases[bossPhaseNow].filterIsInstance<BossTrick.Plates>().firstOrNull()?.tapsEach ?: 1
            if (boss.plateHits >= tapsEach) breakPlate(boss)
        } else {
            damageBoss(boss, damage = 1, points = points)
        }
        if (boss.hp <= 0 || bosses.isEmpty()) return
        bossHitsInPhase++
        for (trick in boss.config.phases[bossPhaseNow]) {
            val every = when (trick) {
                is BossTrick.ThrowSkulls -> trick.everyHits
                is BossTrick.Sting -> trick.everyHits
                is BossTrick.Gust -> trick.everyHits
                is BossTrick.IceShield -> trick.everyHits
                is BossTrick.ThrowGhosts -> trick.everyHits
                is BossTrick.DropBombs, is BossTrick.BlastArmor, is BossTrick.Gravity,
                is BossTrick.Plates, is BossTrick.HealOnEscape, is BossTrick.Fade -> continue
            }
            // Enraged, every trick comes round twice as often.
            val due = if (bossEnraged) (every + 1) / 2 else every
            if (bossHitsInPhase % due != 0) continue
            when (trick) {
                is BossTrick.ThrowSkulls -> repeat(trick.count) { throwSkull() }
                is BossTrick.Sting -> sting(boss, trick.seconds)
                is BossTrick.Gust -> gust(boss)
                is BossTrick.IceShield -> raiseIceShield(boss)
                is BossTrick.ThrowGhosts -> repeat(trick.count) { bubbles.add(createBubble(forcedType = BubbleType.GHOST)) }
                is BossTrick.DropBombs, is BossTrick.BlastArmor, is BossTrick.Gravity,
                is BossTrick.Plates, is BossTrick.HealOnEscape, is BossTrick.Fade -> Unit
            }
        }
    }

    /** Moves on to the next phase once the damage crosses ⅓ or ⅔. Phases never go back, even when a boss heals. */
    private fun updateBossPhase(boss: BossBubble) {
        val phase = bossPhase()
        if (phase > bossPhaseNow) enterBossPhase(phase, boss)
    }

    /** A phase begins: its count starts over, and its bombs, plates and armor arrive. */
    private fun enterBossPhase(phase: Int, boss: BossBubble) {
        bossPhaseNow = phase
        bossHitsInPhase = 0
        val tricks = boss.config.phases[phase]
        tricks.filterIsInstance<BossTrick.DropBombs>().forEach { dropBombs(boss, it.count) }
        val plates = tricks.filterIsInstance<BossTrick.Plates>().firstOrNull()
        val armored = tricks.any { it is BossTrick.BlastArmor }
        for (copy in bosses) {
            copy.plates = plates?.count ?: 0
            copy.plateHits = 0
            copy.blastOnly = armored
        }
        blastArmorBombsIn = 0f
        when {
            plates != null -> addFloatingText("ARMOR UP!", boss.x, boss.y + boss.radius + BOSS_TEXT_GAP_DP * density, ArmorTextColor, isMilestone = true)
            armored -> addFloatingText("ONLY BOMBS HURT IT!", boss.x, boss.y + boss.radius + BOSS_TEXT_GAP_DP * density, ArmorTextColor, isMilestone = true)
        }
    }

    /** One armor plate breaks, by taps or a blast. */
    private fun breakPlate(boss: BossBubble) {
        boss.plates = (boss.plates - 1).coerceAtLeast(0)
        boss.plateHits = 0
        boss.hitFlash = GameRules.BOSS_HIT_FLASH_SECONDS
        addFloatingText(if (boss.plates == 0) "ARMOR BROKEN!" else "PLATE!", boss.x, boss.y - boss.radius, ArmorTextColor, isMilestone = boss.plates == 0)
        startShake(GameRules.SHAKE_BOSS_HIT_DP * 2f, GameRules.SHAKE_BOSS_HIT_SECONDS * 2f)
    }

    /** Sugar Golem: an escaped bubble heals every copy a little. */
    private fun healBosses(hp: Int) {
        var healed = 0
        for (boss in bosses) {
            val gain = minOf(hp, boss.maxHp - boss.hp)
            boss.hp += gain
            healed += gain
        }
        if (healed == 0) return
        bossDamageDealt = (bossDamageDealt - healed).coerceAtLeast(0)
        bosses.firstOrNull()?.let { addFloatingText("+$hp HP", it.x, it.y - it.radius, BossHealColor) }
    }

    /** The fight ran too long: the boss speeds up and its tricks come twice as often. */
    private fun enrage() {
        bossEnraged = true
        bosses.forEach { it.enraged = true }
        addFloatingText("ENRAGED!", canvasWidth / 2f, bossBaseY(), StingColor, isMilestone = true)
        startShake(GameRules.SHAKE_BOSS_DEFEAT_DP, GameRules.SHAKE_BOSS_HIT_SECONDS * 3)
    }

    /** Storm Cloud: a gust blows every bubble to one side. */
    private fun gust(boss: BossBubble) {
        breeze = if (random.nextBoolean()) 1f else -1f
        addFloatingText("GUST!", boss.x, boss.y + boss.radius + BOSS_TEXT_GAP_DP * density, IceTextColor, isMilestone = true)
    }

    /** Tide Kraken: a shield goes up around every copy until an ice bubble is popped. */
    private fun raiseIceShield(boss: BossBubble) {
        if (bosses.any { it.shielded }) return
        bosses.forEach { it.shielded = true }
        addFloatingText("SHIELDED!", boss.x, boss.y + boss.radius + BOSS_TEXT_GAP_DP * density, IceTextColor, isMilestone = true)
        riseIce()
    }

    private fun riseIce() {
        bubbles.add(createBubble(forcedType = BubbleType.FREEZE))
        iceRespawnLeft = GameRules.ICE_SHIELD_RESPAWN_SECONDS
    }

    /** Popping an ice bubble shatters a boss's ice shield. */
    private fun breakIceShield() {
        val shielded = bosses.filter { it.shielded }
        if (shielded.isEmpty()) return
        shielded.forEach { it.shielded = false }
        val boss = shielded.first()
        addFloatingText("SHIELD BROKEN!", boss.x, boss.y - boss.radius, IceTextColor, isMilestone = true)
        startShake(GameRules.SHAKE_BOSS_HIT_DP, GameRules.SHAKE_BOSS_HIT_SECONDS)
    }

    /** Deals [damage] to [boss] (already scored as [points]). Beaten copies split or vanish. */
    private fun damageBoss(boss: BossBubble, damage: Int, points: Int, fromBlast: Boolean = false) {
        if (boss.hp <= 0) return
        if (boss.shielded) return
        // Bomb armor lets only blasts and lightning through; plates take a blast each before any HP.
        if (boss.blastOnly && !fromBlast) return
        if (boss.plates > 0) {
            if (fromBlast) breakPlate(boss)
            return
        }
        val dealt = minOf(damage, boss.hp)
        boss.hp -= dealt
        bossDamageDealt += dealt
        boss.hitFlash = GameRules.BOSS_HIT_FLASH_SECONDS
        startShake(GameRules.SHAKE_BOSS_HIT_DP, GameRules.SHAKE_BOSS_HIT_SECONDS)
        emit(GameEvent.BossHit(dealt, points))

        updateBossPhase(boss)
        if (boss.hp == 0) beatBossCopy(boss)
    }

    private fun beatBossCopy(boss: BossBubble) {
        bosses.remove(boss)
        popAnimations.add(
            PopAnimation(
                id = "${boss.id}_pop",
                x = boss.x,
                y = boss.y,
                radius = boss.radius,
                color = ScorePopupColor,
                intensity = BOSS_POP_INTENSITY,
            )
        )
        val nextGeneration = boss.generation + 1
        boss.config.hpPerGeneration.getOrNull(nextGeneration)?.let { hp ->
            repeat(GameRules.BOSS_SPLIT_COUNT) { i ->
                bosses.add(
                    BossBubble(
                        id = "boss_${nextEffectId++}",
                        config = boss.config,
                        generation = nextGeneration,
                        maxHp = hp,
                        fullRadius = boss.fullRadius * GameRules.BOSS_SPLIT_SCALE,
                        motionPhase = boss.motionPhase + i * 2f * PI.toFloat() / GameRules.BOSS_SPLIT_COUNT,
                        x = boss.x,
                        y = boss.y,
                    ).apply {
                        enraged = bossEnraged
                        blastOnly = boss.blastOnly
                    }
                )
            }
        }
        if (bosses.isEmpty()) defeatBoss(boss)
    }

    /** The last copy is gone: bonus points, confetti, slow motion — the stage ends afterwards. */
    private fun defeatBoss(last: BossBubble) {
        score += GameRules.BOSS_DEFEAT_POINTS
        celebrationLeft = GameRules.BOSS_CELEBRATION_SECONDS
        startShake(GameRules.SHAKE_BOSS_DEFEAT_DP, GameRules.SHAKE_BOSS_DEFEAT_SECONDS)
        addFloatingText(
            text = "${last.config.name.uppercase()} DEFEATED!",
            x = canvasWidth / 2f,
            y = bossBaseY(),
            color = ComboColor,
            isMilestone = true,
        )
        burstConfetti(last.x, last.y)
        emit(GameEvent.BossDefeated(GameRules.BOSS_DEFEAT_POINTS))
    }

    /** The Cloud King sends a skull bubble up from the bottom, among the others. */
    private fun throwSkull() {
        bubbles.add(createBubble(forcedType = BubbleType.POISON))
    }

    private fun sting(boss: BossBubble, seconds: Float) {
        tapLockLeft = seconds
        addFloatingText("STUNG!", boss.x, boss.y + boss.radius + BOSS_TEXT_GAP_DP * density, StingColor, isMilestone = true)
    }

    /** Nebula Core: bombs drop out from under it as a phase begins. */
    private fun dropBombs(boss: BossBubble, count: Int) {
        val spacing = BOMB_DROP_SPACING_DP * density
        repeat(count) { i ->
            val offsetX = (i - (count - 1) / 2f) * spacing
            bubbles.add(
                createBubble(
                    forcedType = BubbleType.BOMB,
                    at = Offset(boss.x + offsetX, boss.y + boss.radius + spacing),
                )
            )
        }
    }

    private fun burstConfetti(x: Float, y: Float) {
        repeat(GameRules.CONFETTI_COUNT) {
            val angle = effectRandom.nextFloat() * 2f * PI.toFloat()
            val speed = (CONFETTI_MIN_SPEED_DP + effectRandom.nextFloat() * CONFETTI_EXTRA_SPEED_DP) * density
            confetti.add(
                Confetti(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed - CONFETTI_LIFT_DP * density,
                    color = ConfettiColors[effectRandom.nextInt(ConfettiColors.size)],
                    size = (5f + effectRandom.nextFloat() * 5f) * density,
                    rotation = effectRandom.nextFloat() * 360f,
                    spin = (effectRandom.nextFloat() - 0.5f) * 720f,
                    life = 1.6f + effectRandom.nextFloat() * 0.8f,
                )
            )
        }
    }

    /** Replaces a splitter with [GameRules.SPLIT_COUNT] small normal bubbles spread sideways. */
    private fun split(splitter: Bubble) {
        val spread = GameRules.SPLIT_SPREAD_DP * density
        repeat(GameRules.SPLIT_COUNT) { i ->
            val offsetX = (i - (GameRules.SPLIT_COUNT - 1) / 2f) * spread
            bubbles.add(createBubble(forcedType = BubbleType.NORMAL, at = Offset(splitter.x + offsetX, splitter.y)))
        }
    }

    private fun popupColor(type: BubbleType) = when (type) {
        BubbleType.HEART -> HeartPopupColor
        BubbleType.GOLDEN -> ComboColor
        else -> ScorePopupColor
    }

    private fun addFloatingText(text: String, x: Float, y: Float, color: Color, isMilestone: Boolean = false) {
        floatingTexts.add(
            FloatingText(
                id = "text_${nextEffectId++}",
                text = text,
                x = x,
                y = y,
                color = color,
                isMilestone = isMilestone,
            )
        )
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

        for (bolt in lightningBolts) bolt.life -= dt
        lightningBolts.removeAll { it.life <= 0f }

        for (text in floatingTexts) {
            val speed = if (text.isMilestone) GameRules.MILESTONE_TEXT_SPEED else GameRules.FLOATING_TEXT_SPEED
            text.progress += dt * speed
        }
        floatingTexts.removeAll { it.progress >= 1f }

        if (confetti.isNotEmpty()) {
            val gravity = CONFETTI_GRAVITY_DP * density
            for (scrap in confetti) {
                scrap.vy += gravity * dt
                scrap.vx *= 1f - CONFETTI_DRAG * dt
                scrap.x += scrap.vx * dt
                scrap.y += scrap.vy * dt
                scrap.rotation += scrap.spin * dt
                scrap.life -= dt
            }
            confetti.removeAll { it.life <= 0f }
        }

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

    /** Advances the waves. Endless announces each calm and each new wave; stages don't. */
    private fun waveStep(dt: Float) {
        val director = waveDirector ?: return
        val endless = stage == null
        when (director.update(dt)) {
            WaveDirector.Change.CALM_STARTED -> {
                spawnTimer = 0f
                if (endless) emit(GameEvent.WaveCleared(director.wave - 1))
            }
            WaveDirector.Change.WAVE_STARTED -> if (endless) emit(GameEvent.WaveStarted(director.wave))
            null -> Unit
        }
        if (!endless) return
        wave         = director.wave
        isWaveCalm   = director.isCalm
        waveProgress = director.waveProgress
    }

    /** After the round ends, bubbles keep drifting in slow motion for a moment. */
    private fun gameOverSlowmoStep(dt: Float) {
        if (gameOverSlowmoLeft <= 0f) return
        gameOverSlowmoLeft = (gameOverSlowmoLeft - dt).coerceAtLeast(0f)
        moveStep(dt * GameRules.GAME_OVER_SLOWMO)
    }

    private fun spawnStep(dt: Float) {
        // Waves: nothing spawns during a calm, and the wave sets the pace.
        if (waveDirector?.isCalm == true) return
        val interval = (waveDirector?.spawnInterval
            ?: (spawn.spawnInterval - score * spawn.spawnRampPerPoint).coerceAtLeast(spawn.minSpawnInterval)) *
            // Boss stages: the bubbles around the boss come faster as the fight goes on.
            (if (stage?.boss != null) GameRules.BOSS_SPAWN_PACE[bossPhaseNow] else 1f) *
            (if (bossEnraged) GameRules.BOSS_ENRAGE_SPAWN_PACE else 1f)
        spawnTimer += dt
        if (spawnTimer >= interval) {
            spawnTimer = 0f
            val bubble = createBubble()
            if (bubble.type == BubbleType.CHAIN) addChainPair(bubble) else bubbles.add(bubble)
        }
    }

    private fun moveStep(dt: Float) {
        val breezeVelocity = breeze * GameRules.BREEZE_SPEED * canvasWidth
        val windy = abs(breeze) > GameRules.WIND_SAVE_MIN_BREEZE
        for (bubble in bubbles) {
            bubble.y -= bubble.speed * dt
            bubble.baseX += breezeVelocity * dt
            // Wind can blow skulls off the side; every other bubble stays on screen.
            val canLeave = windy && bubble.type == BubbleType.POISON
            if (!canLeave && canvasWidth > bubble.radius * 2) {
                bubble.baseX = bubble.baseX.coerceIn(bubble.radius, canvasWidth - bubble.radius)
            }
            bubble.x = bubble.baseX +
                    sin(bubble.y / density * GameRules.WOBBLE_FREQUENCY + bubble.phase) * bubble.amplitude
        }
    }

    /** Skulls blown fully off either side are gone for good. */
    private fun windSaveStep() {
        val blownAway = bubbles.filter {
            it.type == BubbleType.POISON && (it.x + it.radius < 0f || it.x - it.radius > canvasWidth)
        }
        if (blownAway.isEmpty()) return
        bubbles.removeAll(blownAway.toSet())
        blownAway.forEach { emit(GameEvent.WindSave(it)) }
    }

    private fun cullStep() {
        // Cull bubbles that have risen above the game area (below the HUD, not absolute 0).
        val escaped = bubbles.filter { it.y + it.radius < canvasTopOffset }
        if (escaped.isEmpty()) return
        bubbles.removeAll(escaped.toSet())
        for (bubble in escaped) {
            // Right after a revive, escaping bubbles cost nothing.
            val lifeLost = bubble.type in LIFE_BUBBLES && reviveGraceLeft <= 0f
            if (lifeLost) {
                missedCount++
                breakCombo()
                stage?.boss?.phases?.get(bossPhaseNow)?.filterIsInstance<BossTrick.HealOnEscape>()?.firstOrNull()
                    ?.takeIf { bosses.isNotEmpty() }?.let { healBosses(it.hp) }
            }
            emit(GameEvent.Missed(bubble, lifeLost))
        }
        if (missedCount >= maxLives) endGame(GameOverReason.OUT_OF_LIVES)
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

    private fun timerStep() {
        val duration = stage?.durationSec ?: return
        timeLeftSec = ceil(duration + bonusSeconds - playTime).toInt().coerceAtLeast(0)
    }

    /**
     * Stage only. Ends the stage when its objectives are met (untimed: main goal;
     * timed: every objective) or when the timer runs out.
     */
    private fun checkStageEnd() {
        val stage = stage ?: return
        val tracker = objectiveTracker ?: return
        if (isGameOver) return
        // Let the boss-defeat slow motion play out before the result appears.
        if (celebrationLeft > 0f) return

        val goalMet = tracker.isSatisfied(stage.goal)
        val duration = stage.durationSec
        when {
            goalMet && (duration == null || stage.allObjectives.all(tracker::isSatisfied)) ->
                endGame(GameOverReason.GOAL_COMPLETE)
            duration != null && playTime >= duration + bonusSeconds ->
                endGame(GameOverReason.TIME_UP)
        }
    }

    /**
     * Stops the round. A loss the player can afford to undo first offers a revive
     * ([reviveOffer]); otherwise — or once that is declined — [finishRound] saves it.
     */
    private fun endGame(reason: GameOverReason) {
        if (isGameOver) return
        isGameOver = true
        gameOverReason = reason
        gameOverSlowmoLeft = GameRules.GAME_OVER_SLOWMO_SECONDS
        if (canRevive(reason)) {
            reviveOffer = ReviveOffer(reason, GameRules.REVIVE_COST, GameRules.REVIVE_OFFER_SECONDS)
            reviveWasOffered = true
            emit(GameEvent.ReviveOffered(reason))
        } else {
            finishRound(reason)
        }
    }

    /** Once per round, after a loss (a stage with a star already earned is a win), with coins to pay. */
    private fun canRevive(reason: GameOverReason): Boolean {
        if (hasRevived || reason == GameOverReason.GOAL_COMPLETE) return false
        if ((coins.value ?: 0) < GameRules.REVIVE_COST) return false
        val stars = stage?.let { objectiveTracker?.stars(it) } ?: 0
        return stars == 0
    }

    private fun reviveCountdownStep(dt: Float) {
        val offer = reviveOffer ?: return
        if (revivePending) return
        val left = offer.timeLeft - dt
        if (left <= 0f) declineRevive() else reviveOffer = offer.copy(timeLeft = left)
    }

    /** Undoes what ended the round, then plays on with a short grace period. */
    private fun resumeAfterRevive(reason: GameOverReason) {
        when (reason) {
            GameOverReason.OUT_OF_LIVES -> missedCount = (maxLives - GameRules.REVIVE_LIVES).coerceAtLeast(0)
            // Every skull on screen bursts harmlessly.
            GameOverReason.POISON -> bubbles.filter { it.type == BubbleType.POISON }.forEach { skull ->
                bubbles.remove(skull)
                addPopAnimation(skull, intensity = 1f)
            }
            GameOverReason.TIME_UP -> bonusSeconds += GameRules.REVIVE_EXTRA_SECONDS
            GameOverReason.GOAL_COMPLETE -> Unit
        }
        reviveOffer = null
        hasRevived = true
        isGameOver = false
        gameOverReason = null
        gameOverSlowmoLeft = 0f
        reviveGraceLeft = GameRules.REVIVE_GRACE_SECONDS
        timerStep()
        addFloatingText("REVIVED!", canvasWidth / 2f, canvasHeight / 2f, ComboColor, isMilestone = true)
        emit(GameEvent.Revived(reason))
    }

    /** Saves the round and pays out its rewards. */
    private fun finishRound(reason: GameOverReason) {
        // Stars come from the objectives at this moment, whatever ended the round.
        val stars = stage?.let { objectiveTracker?.stars(it) } ?: 0
        stage?.let { stageResult = StageResult(won = stars > 0, stars = stars, objectives = objectives) }
        isNewBest = score > 0 && score > (bestScore.value ?: 0) && (stage == null || stars > 0) &&
                mode !is GameMode.DailyStage
        unlockedDifficulty = newlyUnlockedDifficulty()
        // Stages celebrate on their result card; Endless throws confetti for a new best.
        if (isNewBest && stage == null) burstConfetti(canvasWidth / 2f, canvasHeight * NEW_BEST_CONFETTI_Y)
        emit(GameEvent.GameOver(reason, score, isNewBest))

        val finalScore = score
        val popped = bubblesPopped
        val bestCombo = comboTracker.best
        val summary = RunSummary(
            mode = mode,
            score = finalScore,
            stars = stars,
            previousStars = (mode as? GameMode.Stage)?.let { stageProgress.value[it.stageId]?.stars } ?: 0,
            stats = runTracker.stats(),
        )
        viewModelScope.launch {
            // Save the round first: chests and achievements are judged on the updated progress.
            when (mode) {
                is GameMode.Endless ->
                    progressRepository.recordEndlessGame(mode.difficulty, finalScore, popped, bestCombo)
                is GameMode.Stage ->
                    progressRepository.recordStageGame(mode.stageId, stars, finalScore, popped, bestCombo)
                is GameMode.DailyChallenge, is GameMode.DailyStage ->
                    progressRepository.recordDailyGame(popped, bestCombo)
            }
            rewards = metaRepository.onRunFinished(summary)
        }
    }

    /** Endless: a difficulty that was locked before this round's score and is open now. */
    private fun newlyUnlockedDifficulty(): Difficulty? {
        val endless = mode as? GameMode.Endless ?: return null
        // Only the Medium best counts towards unlocks.
        if (endless.difficulty != Difficulty.MEDIUM) return null
        val before = bestScore.value
        val after = maxOf(score, before ?: 0)
        val progress = stageProgress.value
        return Difficulty.entries.firstOrNull {
            !EndlessUnlocks.isUnlocked(it, before, progress) && EndlessUnlocks.isUnlocked(it, after, progress)
        }
    }

    private fun emit(event: GameEvent) {
        _events.tryEmit(event)
        runTracker.onEvent(event)
        objectiveTracker?.let { tracker ->
            tracker.onEvent(event)
            refreshObjectives()
        }
    }

    private fun refreshObjectives() {
        val stage = stage ?: return
        val tracker = objectiveTracker ?: return
        objectives = stage.allObjectives.map(tracker::progress)
    }

    private fun newRandom(): Random = seed?.let { Random(it) } ?: Random.Default

    /**
     * A new bubble rising from below the screen, or — with [at] — a small one
     * appearing at that point (splitter fragments). [forcedType] skips the
     * weighted type roll.
     */
    private fun createBubble(forcedType: BubbleType? = null, at: Offset? = null): Bubble {
        val type = forcedType ?: rollBubbleType()
        // Golden bubbles and splitter fragments are always small.
        val maxRadiusDp = if (type == BubbleType.GOLDEN || at != null) GameRules.SMALL_RADIUS_DP else GameRules.MAX_RADIUS_DP
        val radiusDp = random.nextFloat() * (maxRadiusDp - GameRules.MIN_RADIUS_DP) + GameRules.MIN_RADIUS_DP
        val radius   = radiusDp * density
        val randomX  = random.nextFloat() * canvasWidth
        val startX   = at?.x ?: randomX
        val maxSpeed = waveDirector?.maxSpeed
            ?: (spawn.maxSpeed + score * spawn.speedRampPerPoint).coerceAtMost(spawn.maxSpeedCap)
        var speed    = (random.nextFloat() * (maxSpeed - spawn.minSpeed) + spawn.minSpeed) * canvasHeight
        if (type == BubbleType.GOLDEN) speed *= GameRules.GOLDEN_SPEED_MULTIPLIER
        val amplitudeDp = random.nextFloat() * (GameRules.MAX_AMPLITUDE_DP - GameRules.MIN_AMPLITUDE_DP) +
                GameRules.MIN_AMPLITUDE_DP
        return Bubble(
            id = random.nextLong().toString(),
            x = startX,
            y = at?.y ?: (canvasHeight + radius),
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
        val weights = typeWeights.value
        var roll = random.nextInt(weights.values.sum())
        for ((type, weight) in weights) {
            if (roll < weight) return type
            roll -= weight
        }
        error("unreachable: roll exceeded total weight")
    }
}

/** The catalog stage, or today's generated stage; null for Endless modes. */
private fun defaultStage(mode: GameMode): StageConfig? = when (mode) {
    is GameMode.Stage -> StageCatalog.stage(mode.stageId)
    is GameMode.DailyStage -> DailyStageGenerator.generate(mode.epochDay, mode.world)
    is GameMode.Endless, is GameMode.DailyChallenge -> null
}

/** Endless modes' spawn preset. The Daily Challenge is always played at Medium. */
private fun defaultSpawn(mode: GameMode): SpawnConfig = when (mode) {
    is GameMode.Endless -> mode.difficulty.spawn
    is GameMode.DailyChallenge -> Difficulty.MEDIUM.spawn
    is GameMode.Stage, is GameMode.DailyStage -> error("Stage modes take the stage's spawn")
}
