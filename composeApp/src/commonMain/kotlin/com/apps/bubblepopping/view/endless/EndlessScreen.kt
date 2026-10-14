package com.apps.bubblepopping.view.endless

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.bubblepopping.Res
import com.apps.bubblepopping.card_easy
import com.apps.bubblepopping.card_hard
import com.apps.bubblepopping.card_medium
import com.apps.bubblepopping.data.ProgressRepository
import com.apps.bubblepopping.game.EndlessUnlocks
import com.apps.bubblepopping.ui.components.CoinCounter
import com.apps.bubblepopping.ui.components.GameBackground
import com.apps.bubblepopping.ui.components.GameTopBar
import com.apps.bubblepopping.ui.components.screenBottomPadding
import com.apps.bubblepopping.ui.components.GlassPanel
import com.apps.bubblepopping.ui.components.LocalUiFeedback
import com.apps.bubblepopping.ui.components.ResourcePill
import com.apps.bubblepopping.ui.components.formatCount
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.BubbleTheme
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import com.apps.bubblepopping.view.home.Difficulty
import kotlinx.coroutines.flow.combine
import org.jetbrains.compose.resources.painterResource
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
    val cardImages = listOf<Painter>(
        painterResource(Res.drawable.card_easy),
        painterResource(Res.drawable.card_medium),
        painterResource(Res.drawable.card_hard),
    )

    GameBackground {
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
                    .heightIn(max = 480.dp)
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
                    image = cardImages[page],
                    difficulty = difficulty,
                    card = cards?.getOrNull(page),
                    onClick = { onDifficultySelected(difficulty) },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        },
                )
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

/** One difficulty: the card artwork, its best score below, and a lock while locked. */
@Composable
private fun DifficultyCard(
    image: Painter,
    difficulty: Difficulty,
    card: DifficultyCardState?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locked = card?.unlocked == false
    val feedback = LocalUiFeedback.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                // Taps wait until progress has loaded, and locked cards don't open.
                .clickable(enabled = card?.unlocked == true) {
                    feedback.click()
                    onClick()
                },
        ) {
            Image(
                painter = image,
                contentDescription = "${difficulty.label} card",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = if (locked) LOCKED_CARD_ALPHA else 1f },
            )
            if (locked) {
                GlassPanel(strong = true) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.s),
                    ) {
                        Image(GameIcons.Lock, contentDescription = "Locked", modifier = Modifier.size(48.dp))
                        Text(
                            text = EndlessUnlocks.unlockHint(difficulty),
                            style = GameTheme.type.label,
                            color = GameColors.TextPrimary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        // Kept in the layout while loading or locked, so the cards don't shift.
        Box(Modifier.graphicsLayer { alpha = if (card == null || locked) 0f else 1f }) {
            val best = card?.bestScore
            if (best != null) {
                ResourcePill(GameIcons.Star, "Best ${formatCount(best)}")
            } else {
                ResourcePill(GameIcons.Bubble, "No best yet")
            }
        }
    }
}

private const val LOCKED_CARD_ALPHA = 0.35f

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
