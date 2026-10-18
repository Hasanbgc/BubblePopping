package com.apps.bubblepopping.view.endless

import com.apps.bubblepopping.view.play.component.drawSoapBubble
import com.apps.bubblepopping.view.play.component.NORMAL_SCHEME
import com.apps.bubblepopping.view.home.ModeCard
import com.apps.bubblepopping.ui.theme.Corners
import com.apps.bubblepopping.ui.components.sunkenWell
import com.apps.bubblepopping.ui.components.candyFace
import com.apps.bubblepopping.ui.components.OutlinedText
import com.apps.bubblepopping.ui.components.ButtonColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.bubblepopping.data.ProgressRepository
import com.apps.bubblepopping.game.EndlessUnlocks
import com.apps.bubblepopping.ui.components.CoinCounter
import com.apps.bubblepopping.ui.components.GameBackground
import com.apps.bubblepopping.ui.components.cachedLayer
import com.apps.bubblepopping.ui.components.GameTopBar
import com.apps.bubblepopping.ui.components.screenBottomPadding
import com.apps.bubblepopping.ui.components.ResourcePill
import com.apps.bubblepopping.ui.components.formatCount
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.BubbleTheme
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.apps.bubblepopping.view.home.Difficulty
import kotlinx.coroutines.flow.combine
import kotlin.math.absoluteValue

/** What each Endless difficulty card shows. */
data class DifficultyCardState(
    val difficulty: Difficulty,
    val bestScore: Int?,
    val unlocked: Boolean,
)

@Composable
fun EndlessScreenRoot(
    progressRepository: ProgressRepository,
    onDifficultySelected: (Difficulty) -> Unit,
    onBack: () -> Unit,
) {
    // null until loaded, so a locked card never flashes open (or the other way round).
    val cards by remember(progressRepository) {
        combine(
            combine(Difficulty.entries.map(progressRepository::bestEndlessScore)) { it.toList() },
            progressRepository.stageProgress(),
        ) { bests, progress ->
            val mediumBest = bests[Difficulty.MEDIUM.ordinal]
            Difficulty.entries.map { difficulty ->
                DifficultyCardState(
                    difficulty = difficulty,
                    bestScore = bests[difficulty.ordinal],
                    unlocked = EndlessUnlocks.isUnlocked(difficulty, mediumBest, progress),
                )
            }
        }
    }.collectAsStateWithLifecycle(initialValue = null)

    EndlessScreen(
        cards = cards,
        onDifficultySelected = onDifficultySelected,
        onBack = onBack,
    )
}

/**
 * Endless mode: swipe between the Easy / Medium / Hard cards and tap one to
 * play. Each card shows its best score; locked cards show how to unlock them.
 */
@Composable
fun EndlessScreen(
    cards: List<DifficultyCardState>?,
    onDifficultySelected: (Difficulty) -> Unit,
    onBack: () -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = 1,
        pageCount = { Difficulty.entries.size }
    )
    // The difficulty cards' PLAY buttons breathe on the ambient clock, so the content isn't cached.
    GameBackground(animated = true, cacheContent = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .screenBottomPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GameTopBar(title = "Endless", onBack = onBack, actions = { CoinCounter() })
            Spacer(Modifier.weight(1f))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = PAGER_MAX_HEIGHT)
                    .weight(4f, fill = false),
                contentPadding = PaddingValues(horizontal = 80.dp),
                pageSize = PageSize.Fill,
                pageSpacing = 4.dp
            )
            { page ->
                val pageOffset = (
                        (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        ).absoluteValue

                val scale = lerp(
                    start = 0.85f,
                    stop = 1f,
                    fraction = 1f - pageOffset.coerceIn(0f, 1f)
                )

                val alpha = lerp(
                    start = 0.5f,
                    stop = 1f,
                    fraction = 1f - pageOffset.coerceIn(0f, 1f)
                )

                val difficulty = Difficulty.entries[page]
                DifficultyCard(
                    difficulty = difficulty,
                    card = cards?.getOrNull(page),
                    onClick = { onDifficultySelected(difficulty) },
                    // Only the card in the middle breathes; the dimmed ones beside it stay still.
                    breathing = page == pagerState.currentPage,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                        // Kept ready-drawn, so a still card (the dimmed side ones) costs one
                        // image draw a frame instead of being re-rendered behind its alpha.
                        .cachedLayer()
                        // Room inside the layer for the card's outline and shadow, which sit
                        // just outside it (the layer clips at its edges).
                        .padding(horizontal = CARD_EDGE, vertical = Spacing.s),
                )
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

/**
 * One difficulty as a premium card in its colour (green, gold, red): its name and
 * pitch, soap bubbles that get busier and faster with the difficulty, a speed
 * meter, the best score, and PLAY. Locked cards are dimmed with LOCKED and how to
 * unlock; taps wait until progress has loaded.
 */
@Composable
private fun DifficultyCard(
    difficulty: Difficulty,
    card: DifficultyCardState?,
    onClick: () -> Unit,
    breathing: Boolean,
    modifier: Modifier = Modifier,
) {
    val locked = card?.unlocked == false
    ModeCard(
        title = difficulty.label.uppercase(),
        subtitle = difficulty.description,
        color = difficulty.cardColor,
        // Taps wait until progress has loaded.
        onClick = { if (card?.unlocked == true) onClick() },
        locked = locked,
        breathing = breathing,
        artSize = ART_SIZE,
        modifier = modifier,
        art = { artModifier -> DifficultyArt(difficulty, artModifier) },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            SpeedMeter(level = difficulty.ordinal + 1, color = difficulty.cardColor)
            when {
                // Shown once loaded; the space is kept meanwhile, so the card never jumps.
                card == null -> Box(Modifier.alpha(0f)) { ResourcePill(GameIcons.Star, "Best", tint = GameColors.PillStar) }
                card.unlocked.not() -> Text(
                    text = EndlessUnlocks.unlockHint(difficulty),
                    style = GameTheme.type.caption,
                    color = GameColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
                card.bestScore != null -> ResourcePill(GameIcons.Star, "Best ${formatCount(card.bestScore)}", tint = GameColors.PillStar)
                else -> ResourcePill(GameIcons.Bubble, "No best yet", tint = GameColors.PillBubble)
            }
        }
    }
}

/** The card colour of each difficulty. */
private val Difficulty.cardColor: ButtonColor
    get() = when (this) {
        Difficulty.EASY -> ButtonColor.GREEN
        Difficulty.MEDIUM -> ButtonColor.GOLD
        Difficulty.HARD -> ButtonColor.RED
    }

/** "SPEED" and three pips, [level] of them lit as candy, the rest sunken. */
@Composable
private fun SpeedMeter(level: Int, color: ButtonColor) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        OutlinedText("SPEED", GameTheme.type.caption, outlineColor = color.lip, outlineWidth = 1.5.dp)
        repeat(SPEED_LEVELS) { i ->
            Box(
                Modifier
                    .size(width = PIP_WIDTH, height = PIP_HEIGHT)
                    .then(if (i < level) Modifier.candyFace(ButtonColor.GOLD, Corners.pill) else Modifier.sunkenWell(Corners.pill)),
            )
        }
    }
}

/**
 * Soap bubbles showing the pace: Easy one calm bubble; Medium a big one with two
 * small ones and short speed streaks; Hard a crowd with long streaks.
 */
@Composable
private fun DifficultyArt(difficulty: Difficulty, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val streak = Color.White.copy(alpha = 0.55f)
        fun bubble(fx: Float, fy: Float, fr: Float, rot: Float, streakLength: Float) {
            val c = Offset(w * fx, w * fy)
            val r = w * fr
            if (streakLength > 0f) {
                // Speed streaks below the bubble: it's rising fast.
                for (k in -1..1) {
                    val x = c.x + k * r * 0.45f
                    drawLine(
                        streak,
                        Offset(x, c.y + r * 1.15f),
                        Offset(x, c.y + r * 1.15f + w * streakLength * (1f - kotlin.math.abs(k) * 0.3f)),
                        strokeWidth = (r * 0.12f).coerceAtLeast(1.5f),
                        cap = StrokeCap.Round,
                    )
                }
            }
            drawSoapBubble(c.x, c.y, r, rot, NORMAL_SCHEME)
        }
        when (difficulty) {
            Difficulty.EASY -> bubble(0.5f, 0.48f, 0.34f, 30f, 0f)
            Difficulty.MEDIUM -> {
                bubble(0.24f, 0.3f, 0.12f, 120f, 0.08f)
                bubble(0.8f, 0.52f, 0.1f, 220f, 0.08f)
                bubble(0.5f, 0.45f, 0.28f, 30f, 0.12f)
            }
            Difficulty.HARD -> {
                bubble(0.18f, 0.22f, 0.1f, 120f, 0.12f)
                bubble(0.84f, 0.26f, 0.09f, 200f, 0.14f)
                bubble(0.16f, 0.62f, 0.08f, 300f, 0.14f)
                bubble(0.86f, 0.64f, 0.11f, 60f, 0.12f)
                bubble(0.5f, 0.42f, 0.24f, 30f, 0.18f)
            }
        }
    }
}

/** Taller than the screen allows on small phones; the pager shrinks it to fit. */
private val PAGER_MAX_HEIGHT = 440.dp
/** Room round each card for its outline and shadow. */
private val CARD_EDGE = 4.dp
private val ART_SIZE = 150.dp
private const val SPEED_LEVELS = 3
private val PIP_WIDTH = 18.dp
private val PIP_HEIGHT = 9.dp

@Preview
@Composable
fun EndlessScreenPreview() {
    BubbleTheme {
        EndlessScreen(
            cards = listOf(
                DifficultyCardState(Difficulty.EASY, bestScore = 212, unlocked = true),
                DifficultyCardState(Difficulty.MEDIUM, bestScore = 96, unlocked = true),
                DifficultyCardState(Difficulty.HARD, bestScore = null, unlocked = false),
            ),
            onDifficultySelected = {},
            onBack = {},
        )
    }
}
