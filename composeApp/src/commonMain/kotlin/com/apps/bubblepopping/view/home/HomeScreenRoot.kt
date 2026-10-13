package com.apps.bubblepopping.view.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apps.bubblepopping.Res
import com.apps.bubblepopping.bg
import com.apps.bubblepopping.bp_logo
import com.apps.bubblepopping.ribbon
import com.apps.bubblepopping.view.BalooFontFamily
import com.apps.bubblepopping.view.EndlessButtonColor
import com.apps.bubblepopping.view.StageAccentColor
import org.jetbrains.compose.resources.painterResource

@Composable
fun HomeScreenRoot(
    onNavigateToStages: () -> Unit,
    onNavigateToEndless: () -> Unit,
    onNavigateToProfile: () -> Unit,
) {
    HomeScreen(
        onNavigateToStages = onNavigateToStages,
        onNavigateToEndless = onNavigateToEndless,
        onNavigateToProfile = onNavigateToProfile,
    )
}

@Composable
fun HomeScreen(
    onNavigateToStages: () -> Unit,
    onNavigateToEndless: () -> Unit,
    onNavigateToProfile: () -> Unit,
) {
    // No Scaffold: the background image is drawn edge-to-edge and HomeContent
    // applies the status/navigation bar insets itself.
    HomeContent(
        onStagesClick = onNavigateToStages,
        onEndlessClick = onNavigateToEndless,
        onProfileClick = onNavigateToProfile,
    )
}

@Composable
fun HomeContent(
    onStagesClick: () -> Unit,
    onEndlessClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Image(
            painter = painterResource(Res.drawable.bg),
            contentDescription = "background_image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // Empty profileUrl → default avatar until Google/Facebook sign-in exists.
            BubbleTopAppBar(
                onProfileClick = onProfileClick,
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

                Spacer(Modifier.weight(1f))
                ModeButton(
                    title = "STAGES",
                    subtitle = "Clear goals, collect stars",
                    color = StageAccentColor,
                    onClick = onStagesClick,
                )
                Spacer(Modifier.height(16.dp))
                ModeButton(
                    title = "ENDLESS",
                    subtitle = "How long can you last?",
                    color = EndlessButtonColor,
                    onClick = onEndlessClick,
                )
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ModeButton(
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
) {
    val font = BalooFontFamily()
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
        modifier = Modifier
            .fillMaxWidth(0.75f)
            .height(84.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontFamily = font,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp,
            )
            Text(
                text = subtitle,
                fontFamily = font,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
@Preview
fun HomeContentPreview() {
    MaterialTheme {
        HomeContent(
            {}, {}, {}
        )
    }
}
