package com.apps.bubblepopping.view.stage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apps.bubblepopping.stage.ObjectiveProgress
import com.apps.bubblepopping.view.ObjectiveFailedColor
import com.apps.bubblepopping.view.ObjectiveMetColor
import com.apps.bubblepopping.view.StarColor

/** [earned] gold stars followed by dim ones, up to [total]. */
@Composable
fun StarRow(
    earned: Int,
    total: Int,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(total) { index ->
            Text(
                text = "★",
                fontSize = fontSize,
                color = if (index < earned) StarColor else StarColor.copy(alpha = 0.25f),
            )
        }
    }
}

/**
 * One objective line: ✓/✗ (or ☆ before play), its description and, for goals
 * in progress, "current / target".
 */
@Composable
fun ObjectiveRow(
    description: String,
    progress: ObjectiveProgress?,
    isBonus: Boolean,
    modifier: Modifier = Modifier,
) {
    val (icon, iconColor) = when {
        progress == null -> (if (isBonus) "☆" else "●") to StarColor
        progress.satisfied -> "✓" to ObjectiveMetColor
        else -> "✗" to ObjectiveFailedColor
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = icon, color = iconColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(
            text = description,
            color = Color.White,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f),
        )
        if (progress != null && !progress.objective.isConstraint && !progress.satisfied) {
            Text(
                text = "${progress.current} / ${progress.target}",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
            )
        }
    }
}
