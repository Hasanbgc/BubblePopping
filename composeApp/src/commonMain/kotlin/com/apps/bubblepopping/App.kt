package com.apps.bubblepopping

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.painterResource

import bubblepopping.composeapp.generated.resources.Res
import bubblepopping.composeapp.generated.resources.compose_multiplatform

@Composable
fun App(hapticFeedback: HapticFeedback) {
    MaterialTheme {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primaryContainer)
                /*.safeContentPadding()*/,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            BubblePoppingScreen(
                hapticFeedback = hapticFeedback
            )
        }
    }
}

