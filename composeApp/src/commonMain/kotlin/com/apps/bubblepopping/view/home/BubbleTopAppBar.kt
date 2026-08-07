package com.apps.bubblepopping.view.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.apps.bubblepopping.Res
import com.apps.bubblepopping.ic_arrow
import com.apps.bubblepopping.ic_close
import org.jetbrains.compose.resources.painterResource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BubbleTopAppBar(
    profileUrl:String = "",
    title: String = "",
    onBackPress: () -> Unit,
) {
    CenterAlignedTopAppBar(
        modifier = Modifier.padding(horizontal = 16.dp),
        title = {
            if (title.isNotEmpty()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        navigationIcon = {
            if (title.isEmpty()) {
                ProfileButton(profileUrl,onProfileClick = {})
            } else {
                BackButton { onBackPress.invoke() }
            }
        },
        actions = {
            if (title.isEmpty()) {
                CloseButton {
                    onBackPress.invoke()
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
        )
    )

}

@Composable
fun BackButton(
    onBackPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(100.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(100.dp)
            )
            .clickable {
                onBackPress()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_arrow),
            contentDescription = "Back",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ProfileButton(
    imageUrl: String = "",
    onProfileClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .wrapContentSize()
            .border(
                width = 3.dp,
                color = Color(0x590A0A0A),
                shape = RoundedCornerShape(100.dp)
            )
            .dropShadow(
                shape = RoundedCornerShape(100.dp), shadow = Shadow(
                    color = Color.Black.copy(0.20f),
                    offset = DpOffset(0.dp, 1.dp),
                    radius = 5.dp,
                    spread = 2.dp,
                    blendMode = BlendMode.Color,
                )
            )
            .clip(RoundedCornerShape(100.dp)),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Profile",
            modifier = Modifier
                .size(48.dp)
                .padding(3.dp)
                .clickable {
                    onProfileClick()
                }
                .border(
                    border = BorderStroke(
                        width = 4.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFE0FE00),
                                Color(0xFFD38000),
                            )
                        )
                    ),
                    shape = RoundedCornerShape(100.dp)
                )
                .clip(shape = RoundedCornerShape(100.dp))
        )
    }
}

@Composable
fun CloseButton(
    onBackPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(52.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_close),
            contentDescription = "Profile",
            modifier = Modifier.matchParentSize().clickable {
                onBackPress()
            },
        )
    }
}