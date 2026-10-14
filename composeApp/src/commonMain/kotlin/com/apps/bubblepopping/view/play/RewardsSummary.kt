package com.apps.bubblepopping.view.play

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.meta.Achievement
import com.apps.bubblepopping.meta.ChestReward
import com.apps.bubblepopping.meta.Cosmetics
import com.apps.bubblepopping.meta.RewardLine
import com.apps.bubblepopping.meta.RunRewards
import com.apps.bubblepopping.ui.components.CoinAmount
import com.apps.bubblepopping.ui.components.OutlinedText
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.BubbleTheme
import com.apps.bubblepopping.ui.theme.Corners
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing

/**
 * What the round earned: coin lines, cosmetics won from chests and new
 * achievements, with the total at the bottom. Slides in once paid out.
 */
@Composable
fun RewardsSummary(rewards: RunRewards?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = rewards != null && !rewards.isEmpty,
        enter = fadeIn() + expandVertically(),
        modifier = modifier,
    ) {
        val shown = rewards ?: return@AnimatedVisibility
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Corners.medium)
                .background(GameColors.Ink.copy(alpha = 0.35f))
                .border(1.5.dp, GameColors.Gold.copy(alpha = 0.6f), Corners.medium)
                .padding(horizontal = Spacing.m, vertical = Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            shown.lines.take(MAX_LINES).forEach { line ->
                RewardRow(icon = iconFor(line), text = line.label) { CoinAmount("+${line.coins}") }
            }
            shown.chests.filterIsInstance<ChestReward.Item>().forEach { item ->
                RewardRow(icon = GameIcons.Chest, text = "Chest: ${item.cosmetic.name}") {
                    OutlinedText("NEW!", GameTheme.type.label, color = GameColors.Gold)
                }
            }
            if (shown.coins > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Total  ", style = GameTheme.type.label, color = GameColors.TextSecondary)
                    Image(GameIcons.Coin, contentDescription = null, modifier = Modifier.size(26.dp))
                    OutlinedText(" +${shown.coins}", GameTheme.type.headline, color = GameColors.Gold)
                }
            }
        }
    }
}

private fun iconFor(line: RewardLine): ImageVector = when {
    line.label.startsWith(MISSION_PREFIX) -> GameIcons.Check
    line.label.startsWith(ACHIEVEMENT_PREFIX) -> GameIcons.Trophy
    line.label.contains(CHEST_WORD) -> GameIcons.Chest
    line.label.contains(STAR_WORD) -> GameIcons.Star
    else -> GameIcons.Coin
}

@Composable
private fun RewardRow(icon: ImageVector, text: String, value: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(
            text = text,
            style = GameTheme.type.caption,
            color = GameColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        value()
    }
}

private const val MAX_LINES = 6
private const val MISSION_PREFIX = "Mission"
private const val ACHIEVEMENT_PREFIX = "Achievement"
private const val CHEST_WORD = "chest"
private const val STAR_WORD = "★"

@Preview
@Composable
fun RewardsSummaryPreview() {
    BubbleTheme {
        RewardsSummary(
            RunRewards(
                lines = listOf(
                    RewardLine("Score 152", 15),
                    RewardLine("Mission: Reach a 10 combo", 40),
                    RewardLine("Achievement: Bubble Buster", 100),
                ),
                chests = listOf(ChestReward.Item(Cosmetics.all.first { it.id == "skin_neon" })),
                achievements = listOf(Achievement.POP_1000),
            )
        )
    }
}
