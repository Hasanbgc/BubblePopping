package com.apps.bubblepopping.view.play

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.game.GameOverReason
import com.apps.bubblepopping.game.GameRules
import com.apps.bubblepopping.game.ReviveOffer
import com.apps.bubblepopping.ui.components.ButtonColor
import com.apps.bubblepopping.ui.components.ButtonSize
import com.apps.bubblepopping.ui.components.DialogButtons
import com.apps.bubblepopping.ui.components.GameButton
import com.apps.bubblepopping.ui.components.GameDialogCard
import com.apps.bubblepopping.ui.components.OutlinedText
import com.apps.bubblepopping.ui.components.OverlayScrim
import com.apps.bubblepopping.ui.components.ResourcePill
import com.apps.bubblepopping.ui.components.formatCount
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.BubbleTheme
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import kotlin.math.ceil

/**
 * "Continue?" after a lost round: a countdown ring, what the revive gives back,
 * and a gold coin button. Running out of time counts as [onDecline].
 */
@Composable
fun ReviveOverlay(
    offer: ReviveOffer,
    /** The player's coins, shown under the buttons; null while loading. */
    coins: Int?,
    onRevive: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OverlayScrim(modifier = modifier) {
        GameDialogCard(title = "Continue?", onClose = onDecline) {
            Text(
                reasonLine(offer.reason),
                style = GameTheme.type.body,
                color = GameColors.TextSecondary,
                textAlign = TextAlign.Center,
            )
            CountdownRing(offer)
            OutlinedText(benefitLine(offer.reason), GameTheme.type.headline, textAlign = TextAlign.Center)
            DialogButtons {
                GameButton(
                    text = "Revive · ${formatCount(offer.cost)}",
                    onClick = onRevive,
                    color = ButtonColor.GOLD,
                    size = ButtonSize.LARGE,
                    icon = GameIcons.Coin,
                    modifier = Modifier.fillMaxWidth(),
                )
                GameButton(
                    text = "No thanks",
                    onClick = onDecline,
                    color = ButtonColor.GLASS,
                    size = ButtonSize.SMALL,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            coins?.let { ResourcePill(GameIcons.Coin, formatCount(it), tint = GameColors.PillCoin) }
        }
    }
}

/** Whole seconds left in the middle of a gold ring that empties as the offer runs out. */
@Composable
private fun CountdownRing(offer: ReviveOffer) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(RING_SIZE)
            .drawBehind {
                val stroke = RING_STROKE.toPx()
                val inset = stroke / 2f
                val arcSize = size.copy(width = size.width - stroke, height = size.height - stroke)
                val topLeft = Offset(inset, inset)
                drawArc(
                    color = GameColors.Ink.copy(alpha = TRACK_ALPHA),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke),
                )
                drawArc(
                    color = GameColors.Gold,
                    startAngle = -90f,
                    sweepAngle = 360f * offer.progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            },
    ) {
        OutlinedText(
            text = ceil(offer.timeLeft).toInt().coerceAtLeast(0).toString(),
            style = GameTheme.type.display,
            outlineWidth = 3.dp,
            maxLines = 1,
        )
    }
}

private fun reasonLine(reason: GameOverReason): String = when (reason) {
    GameOverReason.POISON -> "You popped a skull!"
    GameOverReason.OUT_OF_LIVES -> "Too many bubbles got away"
    GameOverReason.TIME_UP -> "Time's up!"
    GameOverReason.GOAL_COMPLETE -> ""
}

private fun benefitLine(reason: GameOverReason): String = when (reason) {
    GameOverReason.POISON -> "Clear the skulls and play on"
    GameOverReason.OUT_OF_LIVES -> "Get ${GameRules.REVIVE_LIVES} lives back"
    GameOverReason.TIME_UP -> "Get ${GameRules.REVIVE_EXTRA_SECONDS} more seconds"
    GameOverReason.GOAL_COMPLETE -> ""
}

private val RING_SIZE = 96.dp
private val RING_STROKE = 8.dp
private const val TRACK_ALPHA = 0.45f

@Preview
@Composable
fun ReviveOverlayPreview() {
    BubbleTheme {
        ReviveOverlay(
            offer = ReviveOffer(GameOverReason.OUT_OF_LIVES, cost = GameRules.REVIVE_COST, timeLeft = 3.4f, duration = 5f),
            coins = 1240,
            onRevive = {},
            onDecline = {},
        )
    }
}
