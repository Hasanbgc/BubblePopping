package com.apps.bubblepopping.view.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.apps.bubblepopping.bp_logo
import com.apps.bubblepopping.card_easy
import com.apps.bubblepopping.card_hard
import com.apps.bubblepopping.card_medium
import com.apps.bubblepopping.ribbon
import org.jetbrains.compose.resources.painterResource
import kotlin.math.absoluteValue

@Composable
fun HomeScreenRoot(
    onBack: (() -> Unit)? = null,
    onNavigateToPlay: (Difficulty) -> Unit,
    onNavigateToLeaderboard: () -> Unit,
) {
    HomeScreen(
        onNavigateToPlay = onNavigateToPlay,
        onNavigateToLeaderboard = onNavigateToLeaderboard,
    )
}

@Composable
fun HomeScreen(
    onNavigateToPlay: (Difficulty) -> Unit,
    onNavigateToLeaderboard: () -> Unit,
) {
    Scaffold(
    ) {
        HomeContent(
            onDifficultySelected = onNavigateToPlay,
            onNavigateToLeaderboard = onNavigateToLeaderboard,
        )
    }
}

@Composable
fun HomeContent(
    onDifficultySelected: (Difficulty) -> Unit,
    onNavigateToLeaderboard: () -> Unit,
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

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Image(
            painter = painterResource(Res.drawable.bg),
            contentDescription = "background_image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            BubbleTopAppBar(
                profileUrl = "https://i.pravatar.cc/300",
                title = "",
                onBackPress = {}
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(top = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                Image(
                    painter = painterResource(Res.drawable.bp_logo),
                    contentDescription = "",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentScale = ContentScale.Fit

                )
                Image(
                    painter = painterResource(Res.drawable.ribbon),
                    contentDescription = "ribbon",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(95.dp)
                        .offset(x = 0.dp, y = (-25).dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    contentPadding = PaddingValues(horizontal = 100.dp),
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
                                val difficulty = when (page) {
                                    0 -> Difficulty.EASY
                                    1 -> Difficulty.MEDIUM
                                    2 -> Difficulty.HARD
                                    else -> Difficulty.MEDIUM
                                }
                                onDifficultySelected(difficulty)
                            },
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}


@Composable
@Preview
fun DifficultyCardPreview() {
    MaterialTheme {
        HomeContent(
            {}, {}
        )
    }
}
