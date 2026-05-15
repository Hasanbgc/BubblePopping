package com.apps.androidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.HapticFeedback
import com.apps.bubblepopping.navigation.NavGraph
import com.apps.bubblepopping.view.leaderboard.LeaderboardEntry
import com.apps.bubblepopping.view.leaderboard.LeaderboardItem
import com.apps.bubblepopping.view.leaderboard.LeaderboardScreenRoot
import com.apps.bubblepopping.view.play.component.GameHud

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Dark style forces white (light) icons in the status bar and navigation
        // bar — correct for the game's dark-blue background.
        enableEdgeToEdge(
            statusBarStyle     = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        setContent {
            val hapticFeedback = HapticFeedback(this)
            //App(hapticFeedback)
            NavGraph(hapticFeedback)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@Preview
@Composable
fun AppAndroidPreview() {
    val hapticFeedback = HapticFeedback(LocalContext.current)
    //App(hapticFeedback)
}

@Preview(name = "Game HUD — playing", showBackground = true, backgroundColor = 0xFF0A1E35)
@Composable
private fun GameHudPlayingPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0A1E35))
                .padding(top = 8.dp),
        ) {
            GameHud(
                score = 42,
                lives = 3,
                isPaused = false,
                onPlayPauseClick = {},
            )
        }
    }
}

@Preview(name = "Game HUD — paused", showBackground = true, backgroundColor = 0xFF0A1E35)
@Composable
private fun GameHudPausedPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0A1E35))
                .padding(top = 8.dp),
        ) {
            GameHud(
                score = 42,
                lives = 1,
                isPaused = true,
                onPlayPauseClick = {},
            )
        }
    }
}

@Preview(name = "Leaderboard Dialog", showBackground = true, backgroundColor = 0xFF050E1A)
@Composable
private fun LeaderboardDialogPreview() {
    MaterialTheme {
        LeaderboardScreenRoot( currentScore = 55){}
    }
}

@Preview(name = "Leaderboard Items", showBackground = true, backgroundColor = 0xFF08172A)
@Composable
private fun LeaderboardItemsPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .background(Color(0xFF08172A))
                .padding(8.dp),
        ) {
            LeaderboardItem(LeaderboardEntry(rank = 1, name = "BubbleMaster", score = 312))
            LeaderboardItem(LeaderboardEntry(rank = 2, name = "PopKing", score = 285))
            LeaderboardItem(LeaderboardEntry(rank = 3, name = "BurstQueen", score = 261))
            LeaderboardItem(
                LeaderboardEntry(
                    rank = 9,
                    name = "You",
                    score = 55,
                    isCurrentUser = true
                )
            )
        }
    }
}
