package com.apps.bubblepopping.view.meta

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.meta.Economy
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.Corners
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing

/**
 * The 7-day login calendar: claimed days ticked, the next claim glowing gold,
 * day 7 shown as a chest.
 *
 * [claimedDays] is the total claimed so far; [canClaim] is whether today's
 * reward is still waiting (then the next day is highlighted).
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
                    .clip(Corners.small)
                    .background(
                        when {
                            next -> GameColors.Gold.copy(alpha = 0.35f)
                            claimed -> GameColors.Success.copy(alpha = 0.25f)
                            else -> GameColors.Ink.copy(alpha = 0.35f)
                        }
                    )
                    .border(
                        width = if (next) 2.dp else 1.dp,
                        color = if (next) GameColors.Gold else GameColors.GlassRim.copy(alpha = 0.4f),
                        shape = Corners.small,
                    )
                    .padding(vertical = Spacing.xs),
            ) {
                Text("${index + 1}", style = GameTheme.type.caption, color = GameColors.TextSecondary)
                Image(
                    imageVector = when {
                        claimed -> GameIcons.Check
                        index == week - 1 -> GameIcons.Chest
                        else -> GameIcons.Coin
                    },
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = "$coins",
                    style = GameTheme.type.caption,
                    color = if (claimed) GameColors.TextMuted else Color.White,
                )
            }
        }
    }
}
