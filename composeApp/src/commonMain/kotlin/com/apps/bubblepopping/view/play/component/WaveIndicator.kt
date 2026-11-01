package com.apps.bubblepopping.view.play.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import com.apps.bubblepopping.ui.components.GameProgressBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.ui.components.OutlinedText
import com.apps.bubblepopping.ui.theme.BubbleTheme
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing

/**
 * The HUD's left side in Endless: "WAVE 3" over a bar filling through the wave
 * ([progress], read only while laying out the bar, so it never recomposes the
 * HUD); "GET READY" during the calm before it.
 */
@Composable
fun WaveMeter(wave: Int, progress: () -> Float, isCalm: Boolean, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedText(if (isCalm) "GET READY" else "WAVE $wave", GameTheme.type.label, color = GameColors.Gold, maxLines = 1)
        GameProgressBar(progress, height = WAVE_BAR_HEIGHT, modifier = Modifier.widthIn(max = WAVE_BAR_WIDTH))
    }
}

private val WAVE_BAR_WIDTH = 88.dp
private val WAVE_BAR_HEIGHT = 10.dp

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
            //Ribbon("Wave ${wave - 1} clear!")
            OutlinedText("WAVE $wave", GameTheme.type.display.copy(fontSize = GameTheme.type.display.fontSize * BANNER_SCALE), color = GameColors.Gold, outlineWidth = 4.dp)
            OutlinedText("Faster bubbles incoming!", GameTheme.type.label)
        }
    }
}

/** The wave number is the biggest text in the game. */
private const val BANNER_SCALE = 1.4f

@Preview
@Composable
fun WaveBannerPreview() {
    BubbleTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
            WaveMeter(wave = 3, progress = { 0.4f }, isCalm = false, modifier = Modifier.width(120.dp))
            WaveBanner(wave = 4, visible = true)
        }
    }
}
