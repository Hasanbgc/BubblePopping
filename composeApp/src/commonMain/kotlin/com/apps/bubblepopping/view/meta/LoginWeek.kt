package com.apps.bubblepopping.view.meta

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.meta.Economy
import com.apps.bubblepopping.ui.components.ButtonColor
import com.apps.bubblepopping.ui.components.OutlinedText
import com.apps.bubblepopping.ui.components.candyFace
import com.apps.bubblepopping.ui.components.sunkenWell
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.Corners
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing

/**
 * The 7-day login calendar. Today's reward (when [canClaim]) is a gold candy
 * tile with a dark outline; claimed days are sunken tiles with a tick; days to
 * come are sunken tiles with their coins (day 7: a chest).
 *
 * [claimedDays] is the total claimed so far; [canClaim] is whether today's
 * reward is still waiting.
 */
@Composable
fun LoginWeek(claimedDays: Int, canClaim: Boolean, modifier: Modifier = Modifier) {
    val week = Economy.LOGIN_REWARDS.size
    // Right after claiming day 7 the whole week shows as done until tomorrow.
    val claimedThisWeek =
        if (!canClaim && claimedDays > 0 && claimedDays % week == 0) week else claimedDays % week
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Economy.LOGIN_REWARDS.forEachIndexed { index, coins ->
            val claimed = index < claimedThisWeek
            val next = canClaim && index == claimedThisWeek
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .then(if (next) Modifier.todayTile() else Modifier.sunkenWell(Corners.small))
                    .padding(vertical = Spacing.xs),
            ) {
                OutlinedText(
                    text = "${index + 1}",
                    style = GameTheme.type.caption,
                    outlineColor = if (next) GameColors.GoldLip else GameColors.Ink,
                    outlineWidth = 1.5.dp,
                )
                Image(
                    imageVector = when {
                        claimed -> GameIcons.Check
                        index == week - 1 -> GameIcons.Chest
                        else -> GameIcons.Coin
                    },
                    contentDescription = null,
                    modifier = Modifier.size(TILE_ICON),
                )
                OutlinedText(
                    text = "$coins",
                    style = GameTheme.type.caption,
                    color = if (next) GameColors.TextPrimary else GameColors.Gold,
                    outlineColor = if (next) GameColors.GoldLip else GameColors.Ink,
                    outlineWidth = 1.5.dp,
                    modifier = Modifier.alpha(if (claimed) CLAIMED_ALPHA else 1f),
                )
            }
        }
    }
}

/** Today's reward: a gold candy tile with the dark outline used across the UI. */
private fun Modifier.todayTile(): Modifier = this
    .drawBehind {
        val outline = 2.dp.toPx()
        val radius = TILE_CORNER.toPx()
        drawRoundRect(
            GameColors.ButtonOutline,
            topLeft = Offset(-outline / 2f, -outline / 2f),
            size = Size(size.width + outline, size.height + outline),
            cornerRadius = CornerRadius(radius + outline / 2f),
            style = Stroke(outline),
        )
    }
    .candyFace(ButtonColor.GOLD, Corners.small)

/** Corner of [Corners.small], needed in px for the outline. */
private val TILE_CORNER = 12.dp
private val TILE_ICON = 24.dp
private const val CLAIMED_ALPHA = 0.5f
