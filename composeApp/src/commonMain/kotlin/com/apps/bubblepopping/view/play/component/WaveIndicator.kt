package com.apps.bubblepopping.view.play.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.ui.components.GameProgressBar
import com.apps.bubblepopping.ui.components.OutlinedText
import com.apps.bubblepopping.ui.components.Ribbon
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.BubbleTheme
import com.apps.bubblepopping.ui.theme.Corners
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing

/** Endless, below the HUD: the current wave and how far through it the player is. */
@Composable
fun WavePill(
    wave: Int,
    progress: Float,
    isCalm: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(Corners.medium)
            .background(GameColors.Ink.copy(alpha = 0.5f))
            .border(1.5.dp, GameColors.GlassRim, Corners.medium)
            .padding(horizontal = Spacing.m, vertical = Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Image(GameIcons.Bubble, contentDescription = null, modifier = Modifier.size(18.dp))
            OutlinedText(if (isCalm) "GET READY" else "WAVE $wave", GameTheme.type.caption)
        }
        GameProgressBar(progress, modifier = Modifier.width(PROGRESS_BAR_WIDTH), height = 8.dp)
    }
}

/** Endless: the announcement shown during the calm before wave [wave]. */
@Composable
fun WaveBanner(
    wave: Int,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.6f),
        exit = fadeOut() + scaleOut(targetScale = 1.3f),
        modifier = modifier,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Ribbon("Wave ${wave - 1} clear!")
            OutlinedText("WAVE $wave", GameTheme.type.display.copy(fontSize = GameTheme.type.display.fontSize * BANNER_SCALE), color = GameColors.Gold, outlineWidth = 4.dp)
            OutlinedText("Faster bubbles incoming!", GameTheme.type.label)
        }
    }
}

private val PROGRESS_BAR_WIDTH = 110.dp
/** The wave number is the biggest text in the game. */
private const val BANNER_SCALE = 1.4f

@Preview
@Composable
fun WavePillPreview() {
    BubbleTheme {
        WavePill(wave = 3, progress = 0.4f, isCalm = false)
    }
}

@Preview
@Composable
fun WaveBannerPreview() {
    BubbleTheme {
        WaveBanner(wave = 4, visible = true)
    }
}
