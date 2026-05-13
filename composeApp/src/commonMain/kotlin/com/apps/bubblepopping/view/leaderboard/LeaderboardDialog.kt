package com.apps.bubblepopping.view.leaderboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// ─────────────────────────────────────────────────────────────────────────────
// Data model
// ─────────────────────────────────────────────────────────────────────────────

data class LeaderboardEntry(
    val rank: Int,
    val name: String,
    val score: Int,
    val isCurrentUser: Boolean = false,
)

enum class LeaderboardTab { LOCAL, GLOBAL }

// ─────────────────────────────────────────────────────────────────────────────
// Game-themed color tokens — mirrors the dark blue game palette
// ─────────────────────────────────────────────────────────────────────────────

private val DialogBg       = Color(0xFF08172A)
private val DialogSurface  = Color(0xFF0D2240)
private val Accent         = Color(0xFF29B6F6)
private val MedalGold      = Color(0xFFFFD700)
private val MedalSilver    = Color(0xFFCCCCCC)
private val MedalBronze    = Color(0xFFCD7F32)
private val UserHighlight  = Color(0xFF29B6F6).copy(alpha = 0.15f)
private val SubtleWhite    = Color.White.copy(alpha = 0.50f)
private val DividerLine    = Color.White.copy(alpha = 0.07f)

// ─────────────────────────────────────────────────────────────────────────────
// Mock leaderboard data — swap these lists for a real data source
// ─────────────────────────────────────────────────────────────────────────────

private val localBaseline = listOf(
    "Alex" to 87,  "Jordan" to 74, "Sam" to 61,
    "Morgan" to 55, "Taylor" to 48, "Casey" to 39,
    "Riley" to 32,  "Avery" to 21,  "Quinn" to 14,
)

private val globalBaseline = listOf(
    "BubbleMaster" to 312, "PopKing"  to 285, "BurstQueen" to 261,
    "ArcadeAce"    to 234, "FastPop"  to 198, "BubblePro"  to 176,
    "PopStar"      to 154, "QuickBurst" to 132, "BubbleNovice" to 67,
)

private fun buildEntries(
    baseline: List<Pair<String, Int>>,
    currentScore: Int,
): List<LeaderboardEntry> =
    (baseline + ("You" to currentScore))
        .sortedByDescending { (_, score) -> score }
        .mapIndexed { i, (name, score) ->
            LeaderboardEntry(
                rank          = i + 1,
                name          = name,
                score         = score,
                isCurrentUser = name == "You",
            )
        }

// ─────────────────────────────────────────────────────────────────────────────
// LeaderboardDialog
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full-screen leaderboard dialog with personal score, LOCAL/GLOBAL tabs,
 * and an animated ranked list highlighting the current user.
 *
 * @param currentScore Score from the current (or most recent) session.
 * @param onDismiss    Called when the user taps outside or the close button.
 */
@Composable
fun LeaderboardDialog(
    currentScore: Int,
    onDismiss: () -> Unit,
) {
    var activeTab by remember { mutableStateOf(LeaderboardTab.LOCAL) }

    // Recompute list whenever the tab or the user's score changes.
    val entries = remember(activeTab, currentScore) {
        when (activeTab) {
            LeaderboardTab.LOCAL  -> buildEntries(localBaseline,  currentScore)
            LeaderboardTab.GLOBAL -> buildEntries(globalBaseline, currentScore)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape          = RoundedCornerShape(24.dp),
            color          = DialogBg,
            contentColor   = Color.White,
            tonalElevation = 0.dp,
            modifier       = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Column {
                DialogHeader(onDismiss = onDismiss)
                PersonalScore(score = currentScore)
                Spacer(Modifier.height(4.dp))
                TabSelector(
                    activeTab     = activeTab,
                    onTabSelected = { activeTab = it },
                )
                Spacer(Modifier.height(8.dp))
                LeaderboardList(entries = entries)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dialog sections
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DialogHeader(onDismiss: () -> Unit) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 18.dp, bottom = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "🏆", fontSize = 22.sp)
            Text(
                text       = "Leaderboard",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        IconButton(onClick = onDismiss) {
            Text(text = "✕", fontSize = 16.sp, color = SubtleWhite)
        }
    }
}

@Composable
private fun PersonalScore(score: Int) {
    Surface(
        color    = DialogSurface,
        shape    = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text       = "Your Best Score",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color      = SubtleWhite,
                )
                Text(
                    text       = score.toString(),
                    fontSize   = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Accent,
                )
            }
            Text(text = "⭐", fontSize = 30.sp)
        }
    }
}

@Composable
private fun TabSelector(
    activeTab: LeaderboardTab,
    onTabSelected: (LeaderboardTab) -> Unit,
) {
    TabRow(
        selectedTabIndex = activeTab.ordinal,
        containerColor   = Color.Transparent,
        contentColor     = Accent,
        divider          = { HorizontalDivider(color = DividerLine) },
        modifier         = Modifier.padding(horizontal = 16.dp),
    ) {
        LeaderboardTab.entries.forEach { tab ->
            Tab(
                selected             = activeTab == tab,
                onClick              = { onTabSelected(tab) },
                selectedContentColor   = Accent,
                unselectedContentColor = SubtleWhite,
                text = {
                    Text(
                        text       = tab.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontWeight = if (activeTab == tab) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
            )
        }
    }
}

@Composable
private fun LeaderboardList(entries: List<LeaderboardEntry>) {
    LazyColumn(
        modifier            = Modifier.heightIn(max = 340.dp),
        contentPadding      = PaddingValues(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(items = entries, key = { it.rank }) { entry ->
            LeaderboardItem(entry = entry)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LeaderboardItem
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Single leaderboard row. Top-3 rows show medal emojis and rank-specific
 * colors. The current user's row fades in a highlight background.
 */
@Composable
fun LeaderboardItem(entry: LeaderboardEntry) {
    // Animate the highlight background so the user's row pops in smoothly.
    val bgColor by animateColorAsState(
        targetValue   = if (entry.isCurrentUser) UserHighlight else Color.Transparent,
        animationSpec = tween(durationMillis = 400),
        label         = "row_bg",
    )

    val rankEmoji = when (entry.rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> null }
    val rankColor = when (entry.rank) { 1 -> MedalGold; 2 -> MedalSilver; 3 -> MedalBronze; else -> SubtleWhite }
    val nameColor = when {
        entry.rank in 1..3   -> rankColor
        entry.isCurrentUser  -> Accent
        else                 -> Color.White
    }

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // ── Rank badge + name ───────────────────────────────────────────────
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (rankEmoji != null) {
                Text(
                    text     = rankEmoji,
                    fontSize = 20.sp,
                    modifier = Modifier.width(28.dp),
                )
            } else {
                Text(
                    text       = "${entry.rank}",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color      = rankColor,
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier.width(28.dp),
                )
            }
            Text(
                text       = entry.name,
                fontSize   = 15.sp,
                fontWeight = if (entry.isCurrentUser) FontWeight.Bold else FontWeight.Normal,
                color      = nameColor,
            )
        }

        // ── Score ───────────────────────────────────────────────────────────
        Text(
            text       = entry.score.toString(),
            fontSize   = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color      = if (entry.rank in 1..3) rankColor else Color.White,
        )
    }
}
