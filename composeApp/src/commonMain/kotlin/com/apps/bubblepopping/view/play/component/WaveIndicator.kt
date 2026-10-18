package com.apps.bubblepopping.view.play.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.ui.components.ButtonColor
import com.apps.bubblepopping.ui.components.EnamelTone
import com.apps.bubblepopping.ui.components.GameProgressBar
import com.apps.bubblepopping.ui.components.OutlinedText
import com.apps.bubblepopping.ui.components.Ribbon
import com.apps.bubblepopping.ui.components.enamelPill
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.BubbleTheme
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing

/**
 * Endless, below the HUD: a light violet enamel pill with the wave number on a candy
 * medallion at its left, then "WAVE 3" over a progress bar for the wave
 * ([progress], read only while laying out the bar, so it never recomposes the pill).
 * During the calm between waves the medallion shows a bubble and the label reads
 * "GET READY".
 */
@Composable
fun WavePill(
    wave: Int,
    progress: () -> Float,
    isCalm: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        modifier = modifier
            .enamelPill(GameColors.VioletTop, EnamelTone.LIGHT)
            .padding(start = PILL_INSET, end = Spacing.l, top = PILL_INSET, bottom = PILL_INSET),
    ) {
        Medallion(ButtonColor.VIOLET) {
            if (isCalm) {
                Image(GameIcons.Bubble, contentDescription = null, modifier = Modifier.size(MEDALLION_SIZE * 0.6f))
            } else {
                OutlinedText("$wave", GameTheme.type.headline, outlineColor = GameColors.VioletLip, maxLines = 1)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            OutlinedText(if (isCalm) "GET READY" else "WAVE $wave", GameTheme.type.label, maxLines = 1)
            GameProgressBar(progress, modifier = Modifier.width(PROGRESS_BAR_WIDTH), height = PROGRESS_BAR_HEIGHT)
        }
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
            //Ribbon("Wave ${wave - 1} clear!")
            OutlinedText("WAVE $wave", GameTheme.type.display.copy(fontSize = GameTheme.type.display.fontSize * BANNER_SCALE), color = GameColors.Gold, outlineWidth = 4.dp)
            OutlinedText("Faster bubbles incoming!", GameTheme.type.label)
        }
    }
}

/** Gap between the pill's rim and the medallion (and top / bottom). */
private val PILL_INSET = 5.dp
private val PROGRESS_BAR_WIDTH = 120.dp
private val PROGRESS_BAR_HEIGHT = 10.dp
/** The wave number is the biggest text in the game. */
private const val BANNER_SCALE = 1.4f

@Preview
@Composable
fun WavePillPreview() {
    BubbleTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.m), modifier = Modifier.padding(Spacing.l)) {
            WavePill(wave = 3, progress = { 0.4f }, isCalm = false)
            WavePill(wave = 4, progress = { 0f }, isCalm = true)
        }
    }
}

@Preview
@Composable
fun WaveBannerPreview() {
    BubbleTheme {
        WaveBanner(wave = 4, visible = true)
    }
}
