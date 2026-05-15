package com.apps.bubblepopping.view.leaderboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// Data model
// ─────────────────────────────────────────────────────────────────────────────

data class LeaderboardEntry(
    val rank: Int,
    val name: String,
    val easyScore: Int?,
    val mediumScore: Int?,
    val hardScore: Int?,
    val isCurrentUser: Boolean = false,
) {
    val totalScore: Int get() = (easyScore ?: 0) + (mediumScore ?: 0) + (hardScore ?: 0)
}

enum class LeaderboardTab { LOCAL, GLOBAL }

// ─────────────────────────────────────────────────────────────────────────────
// Color tokens
// ─────────────────────────────────────────────────────────────────────────────

private val ScreenSurface = Color(0xFF0D2240)
private val Accent        = Color(0xFF29B6F6)
private val MedalGold     = Color(0xFFFFD700)
private val MedalSilver   = Color(0xFFCCCCCC)
private val MedalBronze   = Color(0xFFCD7F32)
private val UserHighlight = Color(0xFF29B6F6).copy(alpha = 0.15f)
private val SubtleWhite   = Color.White.copy(alpha = 0.50f)
private val DividerLine   = Color.White.copy(alpha = 0.07f)

private val EasyColor   = Color(0xFF66BB6A)
private val MediumColor = Color(0xFFFFA726)
private val HardColor   = Color(0xFFEF5350)

// ─────────────────────────────────────────────────────────────────────────────
// Mock data — swap with a real data source when ready
// ─────────────────────────────────────────────────────────────────────────────

private data class PlayerData(val name: String, val easy: Int?, val medium: Int?, val hard: Int?)

private val localPlayers = listOf(
    PlayerData("Alex",    87,   55,   null),
    PlayerData("Jordan",  74,   74,   32  ),
    PlayerData("Sam",     61,   null, null),
    PlayerData("Morgan",  null, 55,   18  ),
    PlayerData("Taylor",  48,   48,   null),
    PlayerData("Casey",   39,   null, null),
    PlayerData("Riley",   32,   32,   12  ),
    PlayerData("Avery",   21,   21,   null),
    PlayerData("Quinn",   14,   null, null),
)

private val globalPlayers = listOf(
    PlayerData("BubbleMaster", 212,  312,  156 ),
    PlayerData("PopKing",      185,  285,  198 ),
    PlayerData("BurstQueen",   161,  261,  null),
    PlayerData("ArcadeAce",    134,  234,  112 ),
    PlayerData("FastPop",      null, 198,  145 ),
    PlayerData("BubblePro",    176,  176,  null),
    PlayerData("PopStar",      154,  null, 98  ),
    PlayerData("QuickBurst",   null, 132,  null),
    PlayerData("BubbleNovice", 67,   null, null),
)

private fun buildEntries(players: List<PlayerData>): List<LeaderboardEntry> =
    players
        .sortedByDescending { (it.easy ?: 0) + (it.medium ?: 0) + (it.hard ?: 0) }
        .mapIndexed { i, p ->
            LeaderboardEntry(
                rank        = i + 1,
                name        = p.name,
                easyScore   = p.easy,
                mediumScore = p.medium,
                hardScore   = p.hard,
            )
        }

// ─────────────────────────────────────────────────────────────────────────────
// LeaderboardScreenRoot
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LeaderboardScreenRoot(onBack: () -> Unit) {
    var activeTab by remember { mutableStateOf(LeaderboardTab.LOCAL) }

    val entries = remember(activeTab) {
        when (activeTab) {
            LeaderboardTab.LOCAL  -> buildEntries(localPlayers)
            LeaderboardTab.GLOBAL -> buildEntries(globalPlayers)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF050E1A),
                        Color(0xFF0A1E35),
                        Color(0xFF0E2F4F),
                        Color(0xFF144B6E),
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            ScreenHeader(onBack = onBack)
            TabSelector(activeTab = activeTab, onTabSelected = { activeTab = it })
            Spacer(Modifier.height(4.dp))
            DifficultyHeader()
            HorizontalDivider(
                color    = DividerLine,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            LeaderboardList(entries = entries, modifier = Modifier.weight(1f))
            Spacer(Modifier.height(12.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Screen sections
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ScreenHeader(onBack: () -> Unit) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 20.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onBack) {
            Text(text = "←", fontSize = 22.sp, color = Color.White)
        }
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "🏆", fontSize = 22.sp)
            Text(
                text       = "Leaderboard",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
            )
        }
        Spacer(Modifier.width(48.dp))
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
                selected               = activeTab == tab,
                onClick                = { onTabSelected(tab) },
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
private fun DifficultyHeader() {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(28.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            text       = "Player",
            fontSize   = 11.sp,
            fontWeight = FontWeight.Medium,
            color      = SubtleWhite,
            modifier   = Modifier.weight(1f),
        )
        Text(
            text       = "Easy",
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
            color      = EasyColor,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.width(52.dp),
        )
        Text(
            text       = "Med",
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
            color      = MediumColor,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.width(52.dp),
        )
        Text(
            text       = "Hard",
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
            color      = HardColor,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.width(52.dp),
        )
    }
}

@Composable
private fun LeaderboardList(entries: List<LeaderboardEntry>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier            = modifier,
        contentPadding      = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
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

@Composable
fun LeaderboardItem(entry: LeaderboardEntry) {
    val bgColor by animateColorAsState(
        targetValue   = if (entry.isCurrentUser) UserHighlight else Color.Transparent,
        animationSpec = tween(durationMillis = 400),
        label         = "row_bg",
    )

    val rankEmoji = when (entry.rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> null }
    val rankColor = when (entry.rank) { 1 -> MedalGold; 2 -> MedalSilver; 3 -> MedalBronze; else -> SubtleWhite }
    val nameColor = when {
        entry.rank in 1..3  -> rankColor
        entry.isCurrentUser -> Accent
        else                -> Color.White
    }

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (rankEmoji != null) {
            Text(
                text      = rankEmoji,
                fontSize  = 18.sp,
                textAlign = TextAlign.Center,
                modifier  = Modifier.width(28.dp),
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

        Spacer(Modifier.width(12.dp))

        Text(
            text       = entry.name,
            fontSize   = 14.sp,
            fontWeight = if (entry.isCurrentUser) FontWeight.Bold else FontWeight.Normal,
            color      = nameColor,
            modifier   = Modifier.weight(1f),
        )

        ScoreCell(score = entry.easyScore,   color = EasyColor)
        ScoreCell(score = entry.mediumScore, color = MediumColor)
        ScoreCell(score = entry.hardScore,   color = HardColor)
    }
}

@Composable
private fun ScoreCell(score: Int?, color: Color) {
    Text(
        text       = score?.toString() ?: "—",
        fontSize   = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color      = if (score != null) color else SubtleWhite.copy(alpha = 0.35f),
        textAlign  = TextAlign.Center,
        modifier   = Modifier.width(52.dp),
    )
}
