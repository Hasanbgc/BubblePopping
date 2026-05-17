package com.apps.bubblepopping.view.home

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apps.bubblepopping.Res
import com.apps.bubblepopping.blue_card
import com.apps.bubblepopping.bp_logo
import com.apps.bubblepopping.bubble_popping_bg
import com.apps.bubblepopping.ribbon
import com.apps.bubblepopping.view.BalooFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

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
    Scaffold() {
        Box(modifier = Modifier.fillMaxSize()) {
            HomeContent(
                onDifficultySelected = onNavigateToPlay,
                onNavigateToLeaderboard = onNavigateToLeaderboard,
            )
        }
    }
}

@Composable
fun HomeContent(
    onDifficultySelected: (Difficulty) -> Unit,
    onNavigateToLeaderboard: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var selectedDifficulty by remember { mutableStateOf<Difficulty?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Image(
            painter = painterResource(Res.drawable.bubble_popping_bg),
            contentDescription = "background_image",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )

        //BackgroundBubbles()

        IconButton(
            onClick  = onNavigateToLeaderboard,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 8.dp, end = 12.dp),
        ) {
            Text(text = "🏆", fontSize = 24.sp)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Image(
                painter = painterResource(Res.drawable.bp_logo),
                contentDescription = "",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentScale = ContentScale.FillBounds

            )
            Image(
                painter = painterResource(Res.drawable.ribbon),
                contentDescription = "ribbon",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(95.dp)
                    .offset(x = 0.dp, y=(-35).dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier
                .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ){

                DifficultyCard(
                    modifier = Modifier.weight(1f),
                    card = painterResource(Res.drawable.blue_card),
                    onClick = {}
                )
                DifficultyCard(
                    modifier = Modifier.weight(1f),
                    card = painterResource(Res.drawable.blue_card),
                    onClick = {}
                )
                DifficultyCard(
                    modifier = Modifier.weight(1f),
                    card = painterResource(Res.drawable.blue_card),
                    onClick = {}
                )

            }


            /*Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Difficulty.entries.forEach { difficulty ->
                    DifficultyButton(
                        difficulty = difficulty,
                        isSelected = selectedDifficulty == difficulty,
                        onClick = {
                            if (selectedDifficulty == null) {
                                selectedDifficulty = difficulty
                                scope.launch {
                                    delay(180)
                                    onDifficultySelected(difficulty)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }*/



            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun DifficultyCard(
    modifier: Modifier,
    card: Painter,
    onClick : () -> Unit
){

    Box(modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ){
        Image(
            painter = card,
            contentDescription = "",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.height(200.dp)
        )
        Column(
            modifier = Modifier.wrapContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Easy",
                fontFamily = BalooFontFamily(),
                fontWeight = FontWeight.SemiBold,
                fontSize = 36.sp,
                modifier = Modifier.wrapContentSize().padding(0.dp)

            )
        }

    }
}


@Composable
@Preview
fun DifficultyCardPreview(){
    MaterialTheme {
        DifficultyCard(
            modifier = Modifier.wrapContentSize(),
            painterResource(Res.drawable.blue_card),
            onClick = {}
        )
    }
}