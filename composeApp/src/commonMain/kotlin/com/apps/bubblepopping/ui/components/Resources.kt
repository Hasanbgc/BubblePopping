package com.apps.bubblepopping.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.GameColors

/** The player's coins and stars, shown in top bars. Null while loading. */
@Immutable
data class PlayerResources(
    val coins: Int? = null,
    val stars: Int? = null,
    val maxStars: Int? = null,
)

/** Provided once at the app root, so any top bar can show the counters. */
val LocalPlayerResources = compositionLocalOf { PlayerResources() }

/** Coin counter for a top bar (hidden until loaded). */
@Composable
fun CoinCounter(modifier: Modifier = Modifier) {
    val coins = LocalPlayerResources.current.coins ?: return
    ResourcePill(GameIcons.Coin, formatCount(coins), modifier)
}

/** Star counter for a top bar; with [showMax] it reads "34/120". */
@Composable
fun StarCounter(modifier: Modifier = Modifier, showMax: Boolean = false) {
    val resources = LocalPlayerResources.current
    val stars = resources.stars ?: return
    val text = if (showMax && resources.maxStars != null) "$stars/${resources.maxStars}" else formatCount(stars)
    ResourcePill(GameIcons.Star, text, modifier)
}

/** 1240 → "1,240". */
fun formatCount(value: Int): String =
    value.toString().reversed().chunked(3).joinToString(",").reversed()

/**
 * Round avatar for the Home top bar. Shows the signed-in photo once sign-in
 * exists ([imageUrl]); until then the drawn person icon.
 */
@Composable
fun AvatarButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageUrl: String = "",
    size: Dp = 48.dp,
) {
    Box(modifier, contentAlignment = Alignment.TopCenter) {
        GameIconButton(GameIcons.Person, contentDescription = "Profile", onClick = onClick, size = size)
        if (imageUrl.isNotBlank()) {
            // The photo covers the icon; it takes no touches, so taps reach the button below.
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size * PHOTO_FRACTION)
                    .align(Alignment.TopCenter)
                    .offset(y = size * (1 - PHOTO_FRACTION) / 2)
                    .clip(CircleShape)
                    .background(GameColors.Ink)
                    .border(2.dp, GameColors.GlassRim, CircleShape),
            )
        }
    }
}

private const val PHOTO_FRACTION = 0.86f
