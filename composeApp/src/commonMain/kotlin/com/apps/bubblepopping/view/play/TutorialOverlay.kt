package com.apps.bubblepopping.view.play

import com.apps.bubblepopping.ui.theme.Corners
import com.apps.bubblepopping.ui.components.goldInset
import com.apps.bubblepopping.ui.components.candyFace
import com.apps.bubblepopping.ui.components.GameDialogCard
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.border
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.game.BubbleSize
import com.apps.bubblepopping.ui.components.ButtonColor
import com.apps.bubblepopping.ui.components.ButtonSize
import com.apps.bubblepopping.ui.components.GameButton
import com.apps.bubblepopping.ui.components.OutlinedText
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing
import androidx.compose.foundation.Image
import com.apps.bubblepopping.view.play.component.Bubble
import com.apps.bubblepopping.view.play.component.BubbleIcons
import com.apps.bubblepopping.view.play.component.BubbleType
import com.apps.bubblepopping.view.play.component.PopAnimation
import com.apps.bubblepopping.stage.World
import com.apps.bubblepopping.view.play.component.drawGameBubble
import com.apps.bubblepopping.view.play.component.drawPopAnimation
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sin
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity

/** What a tutorial step asks the player to do to move on. */
private enum class StepAction {
    /** Tap the bubble. */
    TAP,
    /** Swipe sideways anywhere: wind blows the bubble off the side. */
    SWIPE,
    /** Read, then press "Got it". */
    READ,
}

/** One tutorial card: a ghost bubble and what to do with it. */
private data class TutorialStep(
    val type: BubbleType,
    val title: String,
    val text: String,
    val action: StepAction,
    /** Light behind the bubble in its socket. */
    val glow: Color,
)

private val STEPS = listOf(
    TutorialStep(BubbleType.NORMAL, "Pop this!", "Tap bubbles before they float away.\nBigger bubbles are worth more.", StepAction.TAP, glow = GameColors.BlueTop),
    TutorialStep(BubbleType.POISON, "Avoid this!", "Popping a skull ends the round.\nNever tap one!", StepAction.READ, glow = GameColors.Danger),
    TutorialStep(BubbleType.POISON, "Blow it away!", "Swipe sideways to make wind.\nWind blows skulls off the screen.", StepAction.SWIPE, glow = GameColors.BlueTop),
    TutorialStep(BubbleType.BOMB, "Boom!", "Bombs pop every bubble nearby.\nKeep skulls out of the blast!", StepAction.TAP, glow = GameColors.GoldBottom),
    TutorialStep(BubbleType.HEART, "This heals!", "Hearts give back a lost life.", StepAction.TAP, glow = GameColors.PillHeart),
)

/**
 * First-run tutorial before World 1 Stage 1: pop a bubble, learn to leave
 * skulls alone and blow them away with wind, set off a bomb, pop a heart.
 * [onPop] plays the popped type's sound, [onWind] the wind's; [onFinish] fires
 * after the last step or on Skip.
 */
@Composable
fun TutorialOverlay(
    icons: BubbleIcons,
    skinId: String,
    highContrastSkulls: Boolean,
    onPop: (BubbleType) -> Unit,
    onWind: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var stepIndex by remember { mutableIntStateOf(0) }
    // Set while the step's ending plays: the pop, or the bubble blowing away.
    var acting by remember { mutableStateOf(false) }
    val actProgress = remember { Animatable(0f) }
    // Which way the wind blows the bubble: -1 left, +1 right.
    var windSide by remember { mutableFloatStateOf(1f) }
    val scope = rememberCoroutineScope()
    val step = STEPS[stepIndex]

    fun next() {
        if (stepIndex == STEPS.lastIndex) onFinish() else stepIndex++
    }

    fun act(durationMs: Int) {
        acting = true
        scope.launch {
            actProgress.snapTo(0f)
            actProgress.animateTo(1f, tween(durationMs, easing = FastOutLinearInEasing))
            acting = false
            next()
        }
    }

    fun pop() {
        if (acting) return
        onPop(step.type)
        act(POP_MS)
    }

    fun blow(side: Float) {
        if (acting) return
        windSide = side
        onWind()
        act(BLOW_MS)
    }

    val transition = rememberInfiniteTransition(label = "tutorial")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * kotlin.math.PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(WOBBLE_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "tutorial_time",
    )
    val swipeDistance = with(LocalDensity.current) { SWIPE_DISTANCE.toPx() }

    Box(
        modifier = modifier
            .background(GameColors.Scrim.copy(alpha = SCRIM_ALPHA))
            // Swallow taps so nothing reaches the paused game underneath.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            // On the wind step a sideways swipe anywhere makes wind, as in the game.
            .pointerInput(step.action) {
                if (step.action != StepAction.SWIPE) return@pointerInput
                var dragged = 0f
                detectHorizontalDragGestures(onDragStart = { dragged = 0f }) { change, amount ->
                    change.consume()
                    dragged += amount
                    if (abs(dragged) >= swipeDistance) blow(sign(dragged))
                }
            },
    ) {
        GameButton(
            text = "Skip",
            onClick = onFinish,
            color = ButtonColor.GLASS,
            size = ButtonSize.SMALL,
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(Spacing.l),
        )

        // The step on the game's dialog card: its title on the ribbon.
        GameDialogCard(title = step.title, onClose = null, modifier = Modifier.align(Alignment.Center)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(BUBBLE_AREA)) {
                // Gold-framed socket, lit from behind in the step's colour.
                Box(
                    Modifier
                        .matchParentSize()
                        .goldInset(CircleShape)
                        .drawBehind {
                            drawCircle(
                                Brush.radialGradient(listOf(step.glow.copy(alpha = SOCKET_GLOW_ALPHA), Color.Transparent), center, size.minDimension / 2f),
                            )
                        },
                )
                Canvas(
                    modifier = Modifier
                        .size(BUBBLE_AREA)
                        .clip(CircleShape)
                        .clickable(enabled = step.action == StepAction.TAP && !acting) { pop() },
                ) {
                    val center = this.center
                    val radius = size.minDimension * 0.32f
                    if (acting && step.action == StepAction.TAP) {
                        drawPopAnimation(
                            PopAnimation(
                                id = "tutorial_$stepIndex",
                                x = center.x,
                                y = center.y,
                                radius = radius,
                                color = Color.White,
                                // A bomb goes off with a bigger burst.
                                intensity = if (step.type == BubbleType.BOMB) BOMB_POP_INTENSITY else POP_INTENSITY,
                                progress = actProgress.value,
                            ),
                        )
                    } else {
                        // Blown away: the bubble slides out of the socket with the wind, gusts behind it.
                        val blown = if (acting) actProgress.value else 0f
                        val drift = windSide * blown * (size.width / 2f + radius * 1.2f)
                        if (blown > 0f) drawWindStreaks(center, radius, windSide, blown)
                        val bob = sin(time) * radius * 0.06f
                        val ghost = Bubble(
                            id = "tutorial",
                            x = center.x + drift,
                            y = center.y + bob,
                            radius = radius,
                            size = BubbleSize.LARGE,
                            speed = 0f,
                            amplitude = 0f,
                            baseX = center.x,
                            phase = 0f,
                            color = Color.White,
                            shimmerAngle = time * 20f,
                            type = step.type,
                        )
                        drawGameBubble(ghost, World.SKY, icons, skinId, highContrastSkulls)
                    }
                }
                if (!acting) {
                    when (step.action) {
                        // Finger hint, bobbing up towards the bubble.
                        StepAction.TAP -> Image(
                            imageVector = GameIcons.Tap,
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(x = 24.dp, y = (12 + 8 * sin(time * 2)).dp)
                                .size(FINGER_SIZE),
                        )
                        // Finger hint, sliding side to side under the bubble.
                        StepAction.SWIPE -> Image(
                            imageVector = GameIcons.Tap,
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(x = (SWIPE_HINT_REACH * sin(time * 2)).dp, y = 16.dp)
                                .size(FINGER_SIZE),
                        )
                        StepAction.READ -> Unit
                    }
                }
            }
            Text(step.text, style = GameTheme.type.body, color = GameColors.TextPrimary, textAlign = TextAlign.Center)
            // Same height whichever it is, so the card doesn't jump between steps.
            Box(Modifier.fillMaxWidth().heightIn(min = ACTION_HEIGHT), contentAlignment = Alignment.Center) {
                when (step.action) {
                    StepAction.TAP -> OutlinedText("Tap the bubble", GameTheme.type.headline, color = GameColors.BlueTop)
                    StepAction.SWIPE -> Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(GameIcons.Wind, contentDescription = null, modifier = Modifier.size(WIND_ICON))
                        OutlinedText("Swipe sideways", GameTheme.type.headline, color = GameColors.BlueTop)
                    }
                    StepAction.READ -> GameButton("Got it", onClick = ::next, icon = GameIcons.Check, modifier = Modifier.fillMaxWidth())
                }
            }
            StepDots(current = stepIndex, total = STEPS.size)
        }
    }
}

/** Gusts trailing the blown bubble: short white lines that stretch out and fade as it goes. */
private fun DrawScope.drawWindStreaks(center: Offset, radius: Float, side: Float, progress: Float) {
    val alpha = (1f - progress) * WIND_STREAK_ALPHA
    for (i in 0 until WIND_STREAKS) {
        val y = center.y + (i - (WIND_STREAKS - 1) / 2f) * radius * 0.45f
        val length = radius * (0.8f + 0.25f * (i % 2)) * (0.4f + progress)
        val start = center.x - side * radius * (1.4f - progress)
        drawLine(
            color = Color.White.copy(alpha = alpha),
            start = Offset(start, y),
            end = Offset(start + side * length, y),
            strokeWidth = radius * 0.08f,
            cap = StrokeCap.Round,
        )
    }
}

/** Progress through the steps: the current one a gold candy pill, the rest small sunken dots. */
@Composable
private fun StepDots(current: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { index ->
            if (index == current) {
                Box(Modifier.size(width = DOT_CURRENT_WIDTH, height = DOT_SIZE).candyFace(ButtonColor.GOLD, Corners.pill))
            } else {
                Box(
                    Modifier
                        .size(DOT_SIZE)
                        .clip(CircleShape)
                        .background(GameColors.ButtonOutline.copy(alpha = 0.6f))
                        .border(1.dp, GameColors.GlassFill, CircleShape)
                )
            }
        }
    }
}

/** The tutorial plays before this stage. */
const val TUTORIAL_STAGE_ID = 1
private val BUBBLE_AREA = 180.dp
private val FINGER_SIZE = 48.dp
private val ACTION_HEIGHT = 50.dp
private val DOT_SIZE = 10.dp
private val DOT_CURRENT_WIDTH = 26.dp
private const val SOCKET_GLOW_ALPHA = 0.55f
private const val SCRIM_ALPHA = 0.7f
private const val POP_MS = 450
private const val POP_INTENSITY = 1.5f
private const val BOMB_POP_INTENSITY = 2.5f
private const val BLOW_MS = 650
/** A sideways drag this long makes wind. */
private val SWIPE_DISTANCE = 40.dp
/** How far, in dp, the swipe hint slides each way. */
private const val SWIPE_HINT_REACH = 36f
private val WIND_ICON = 28.dp
private const val WIND_STREAKS = 5
private const val WIND_STREAK_ALPHA = 0.8f
private const val WOBBLE_MS = 2_400
