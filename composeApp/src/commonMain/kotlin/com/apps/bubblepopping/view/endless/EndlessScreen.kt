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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.apps.bubblepopping.Res
import com.apps.bubblepopping.bg
import com.apps.bubblepopping.card_easy
import com.apps.bubblepopping.card_hard
import com.apps.bubblepopping.card_medium
import com.apps.bubblepopping.view.home.BubbleTopAppBar
import com.apps.bubblepopping.view.home.Difficulty
import org.jetbrains.compose.resources.painterResource
import kotlin.math.absoluteValue

/** Endless mode: swipe between the Easy / Medium / Hard cards and tap one to play. */
@Composable
fun EndlessScreen(
    onDifficultySelected: (Difficulty) -> Unit,
    onBack: () -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = 1,
        pageCount = { 3 }
    )
    val cardList = listOf<Painter>(
        painterResource(Res.drawable.card_easy),
        painterResource(Res.drawable.card_medium),
        painterResource(Res.drawable.card_hard),
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(Res.drawable.bg),
            contentDescription = "background_image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BubbleTopAppBar(title = "Endless", onBackPress = onBack)
            Spacer(Modifier.weight(1f))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
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

                Image(
                    painter = cardList[page],
                    contentDescription = "Difficulty Card",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                        .clickable {
                            onDifficultySelected(Difficulty.entries[page])
                        },
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Preview
@Composable
fun EndlessScreenPreview() {
    MaterialTheme {
        EndlessScreen(onDifficultySelected = {}, onBack = {})
    }
}
