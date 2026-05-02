package com.apps.bubblepopping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val hapticFeedback = HapticFeedback(this)
            App(hapticFeedback)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    val hapticFeedback = HapticFeedback(LocalContext.current)
    App(hapticFeedback)
}