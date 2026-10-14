package com.apps.bubblepopping.view.stage

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.stage.ObjectiveProgress
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing

/** [earned] gold stars followed by faded ones, up to [total]. */
@Composable
fun StarRow(
    earned: Int,
    total: Int,
    modifier: Modifier = Modifier,
    starSize: Dp = 18.dp,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(starSize / 8)) {
        repeat(total) { index ->
            Image(
                imageVector = GameIcons.Star,
                contentDescription = null,
                modifier = Modifier
                    .size(starSize)
                    .alpha(if (index < earned) 1f else UNEARNED_ALPHA),
            )
        }
    }
}

private const val UNEARNED_ALPHA = 0.3f

/**
 * One objective line: an icon (target / star before play, ✓ or ✗ after), its
 * description and, for goals in progress, "current / target".
 */
@Composable
fun ObjectiveRow(
    description: String,
    progress: ObjectiveProgress?,
    isBonus: Boolean,
    modifier: Modifier = Modifier,
) {
    val icon = when {
        progress == null -> if (isBonus) GameIcons.Star else GameIcons.Target
        progress.satisfied -> GameIcons.Check
        else -> GameIcons.Close
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Image(
            imageVector = icon,
            contentDescription = when {
                progress == null -> null
                progress.satisfied -> "Done"
                else -> "Missed"
            },
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = description,
            style = if (isBonus) GameTheme.type.body else GameTheme.type.label,
            color = GameColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        if (progress != null && !progress.objective.isConstraint && !progress.satisfied) {
            Text(
                text = "${progress.current} / ${progress.target}",
                style = GameTheme.type.caption,
                color = GameColors.TextSecondary,
            )
        }
    }
}
