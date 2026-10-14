package com.apps.bubblepopping.view.play

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
import androidx.compose.foundation.layout.Column
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
import kotlin.math.sin

/** One tutorial card: a ghost bubble and what to do with it. */
private data class TutorialStep(
    val type: BubbleType,
    val title: String,
    val text: String,
    /** Tap the bubble to continue; otherwise a button continues. */
    val tapToPop: Boolean,
)

private val STEPS = listOf(
    TutorialStep(BubbleType.NORMAL, "Pop this!", "Tap bubbles before they float away.\nBigger bubbles are worth more.", tapToPop = true),
    TutorialStep(BubbleType.POISON, "Avoid this!", "Popping a skull ends the round.\nSwipe sideways to blow skulls away.", tapToPop = false),
    TutorialStep(BubbleType.HEART, "This heals!", "Hearts give back a lost life.", tapToPop = true),
)

/**
 * First-run tutorial before World 1 Stage 1: pop a bubble, learn to leave
 * skulls alone, pop a heart. [onPop] plays the pop sound; [onFinish] fires
 * after the last step or on Skip.
 */
@Composable
fun TutorialOverlay(
    icons: BubbleIcons,
    skinId: String,
    highContrastSkulls: Boolean,
    onPop: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var stepIndex by remember { mutableIntStateOf(0) }
    var popping by remember { mutableStateOf(false) }
    val popProgress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val step = STEPS[stepIndex]

    fun next() {
        if (stepIndex == STEPS.lastIndex) onFinish() else stepIndex++
    }

    fun pop() {
        if (popping) return
        popping = true
        onPop()
        scope.launch {
            popProgress.snapTo(0f)
            popProgress.animateTo(1f, tween(POP_MS))
            popping = false
            next()
        }
    }

    val transition = rememberInfiniteTransition(label = "tutorial")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * kotlin.math.PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(WOBBLE_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "tutorial_time",
    )

    Box(
        modifier = modifier
            .background(GameColors.Scrim.copy(alpha = SCRIM_ALPHA))
            // Swallow taps so nothing reaches the paused game underneath.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        GameButton(
            text = "Skip",
            onClick = onFinish,
            color = ButtonColor.GLASS,
            size = ButtonSize.SMALL,
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(Spacing.l),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
            modifier = Modifier.align(Alignment.Center).padding(horizontal = Spacing.xxl),
        ) {
            OutlinedText(step.title, GameTheme.type.display, outlineWidth = 3.dp)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(BUBBLE_AREA)) {
                Canvas(
                    modifier = Modifier
                        .size(BUBBLE_AREA)
                        .clip(CircleShape)
                        .clickable(enabled = step.tapToPop && !popping) { pop() },
                ) {
                    val center = this.center
                    val radius = size.minDimension * 0.32f
                    if (popping) {
                        drawPopAnimation(
                            PopAnimation(
                                id = "tutorial_$stepIndex",
                                x = center.x,
                                y = center.y,
                                radius = radius,
                                color = Color.White,
                                intensity = 1.5f,
                                progress = popProgress.value,
                            ),
                        )
                    } else {
                        val bob = sin(time) * radius * 0.06f
                        val ghost = Bubble(
                            id = "tutorial",
                            x = center.x,
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
                // Finger hint, bobbing up towards the bubble.
                if (step.tapToPop && !popping) {
                    Image(
                        imageVector = GameIcons.Tap,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(x = 24.dp, y = (12 + 8 * sin(time * 2)).dp)
                            .size(FINGER_SIZE),
                    )
                }
            }
            Text(step.text, style = GameTheme.type.body, color = GameColors.TextPrimary, textAlign = TextAlign.Center)
            if (step.tapToPop) {
                OutlinedText("Tap the bubble", GameTheme.type.label, color = GameColors.BlueTop)
            } else {
                GameButton("Got it", onClick = ::next, icon = GameIcons.Check)
            }
            StepDots(current = stepIndex, total = STEPS.size)
        }
    }
}

@Composable
private fun StepDots(current: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
        repeat(total) { index ->
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (index == current) Color.White else Color.White.copy(alpha = 0.3f))
            )
        }
    }
}

/** The tutorial plays before this stage. */
const val TUTORIAL_STAGE_ID = 1
private val BUBBLE_AREA = 180.dp
private val FINGER_SIZE = 48.dp
private const val SCRIM_ALPHA = 0.7f
private const val POP_MS = 450
private const val WOBBLE_MS = 2_400
