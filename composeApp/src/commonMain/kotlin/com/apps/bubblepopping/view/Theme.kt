package com.apps.bubblepopping.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.apps.bubblepopping.Res
import com.apps.bubblepopping.baloo2_regular
import com.apps.bubblepopping.baloo2_semibold
import org.jetbrains.compose.resources.Font

@Composable
fun BalooFontFamily() = FontFamily(
    fonts = listOf(
        Font(
            resource = Res.font.baloo2_regular,
            weight = FontWeight.Normal
            ),
        Font(
            resource = Res.font.baloo2_semibold,
            weight = FontWeight.SemiBold
        )
    )
)