package com.apps.bubblepopping.view.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.bubblepopping.Res
import com.apps.bubblepopping.bp_logo
import com.apps.bubblepopping.data.MetaRepository
import com.apps.bubblepopping.meta.Economy
import com.apps.bubblepopping.meta.PlayerState
import com.apps.bubblepopping.meta.currentEpochDay
import com.apps.bubblepopping.ribbon
import com.apps.bubblepopping.ui.components.AvatarButton
import com.apps.bubblepopping.ui.components.BottomDock
import com.apps.bubblepopping.ui.components.CoinCounter
import com.apps.bubblepopping.ui.components.DockItem
import com.apps.bubblepopping.ui.components.GameBackground
import com.apps.bubblepopping.ui.components.GameTopBar
import com.apps.bubblepopping.ui.components.StarCounter
import androidx.compose.ui.graphics.vector.ImageVector
import com.apps.bubblepopping.view.meta.LoginWeek
import com.apps.bubblepopping.ui.components.ButtonColor
import com.apps.bubblepopping.ui.components.ButtonSize
import com.apps.bubblepopping.ui.components.DialogButtons
import com.apps.bubblepopping.ui.components.GameButton
import com.apps.bubblepopping.ui.components.GameDialog
import com.apps.bubblepopping.ui.components.OutlinedText
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@Composable
fun HomeScreenRoot(
    metaRepository: MetaRepository,
    onNavigateToStages: () -> Unit,
    onNavigateToEndless: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToToday: () -> Unit,
    onNavigateToShop: () -> Unit,
    onNavigateToRanking: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val today = remember { currentEpochDay() }
    val scope = rememberCoroutineScope()
    // null until loaded, so the daily-reward popup never flashes for a claimed day.
    val player by remember(metaRepository) { metaRepository.player() }.collectAsStateWithLifecycle(null)
    var loginDismissed by rememberSaveable { mutableStateOf(false) }

    HomeContent(
        todayHasNews = player?.canClaimLogin(today) == true,
        onStagesClick = onNavigateToStages,
        onEndlessClick = onNavigateToEndless,
        onProfileClick = onNavigateToProfile,
        onTodayClick = onNavigateToToday,
        onShopClick = onNavigateToShop,
        onRankingClick = onNavigateToRanking,
        onSettingsClick = onNavigateToSettings,
    )

    val current = player
    if (current != null && current.canClaimLogin(today) && !loginDismissed) {
        DailyRewardDialog(
            player = current,
            onClaim = { scope.launch { metaRepository.claimDailyLogin(today) } },
            onDismiss = { loginDismissed = true },
        )
    }
}

@Composable
fun HomeContent(
    todayHasNews: Boolean,
    onStagesClick: () -> Unit,
    onEndlessClick: () -> Unit,
    onProfileClick: () -> Unit,
    onTodayClick: () -> Unit,
    onShopClick: () -> Unit,
    onRankingClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GameBackground(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Avatar on the left (the photo arrives with Google / Facebook sign-in), counters on the right.
            GameTopBar(
                title = null,
                onBack = null,
                leading = { AvatarButton(onClick = onProfileClick) },
                actions = {
                    StarCounter()
                    CoinCounter()
                },
            )

            Image(
                painter = painterResource(Res.drawable.bp_logo),
                contentDescription = "Bubble Popping",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LOGO_HEIGHT),
            )
            Image(
                painter = painterResource(Res.drawable.ribbon),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(RIBBON_HEIGHT)
                    .offset(y = -Spacing.xl),
            )

            Spacer(Modifier.weight(1f))
            ModeButton(
                title = "STAGES",
                subtitle = "Clear goals, collect stars",
                icon = GameIcons.Star,
                color = ButtonColor.BLUE,
                onClick = onStagesClick,
            )
            Spacer(Modifier.height(Spacing.l))
            ModeButton(
                title = "ENDLESS",
                subtitle = "How long can you last?",
                icon = GameIcons.Bubble,
                color = ButtonColor.VIOLET,
                onClick = onEndlessClick,
            )
            Spacer(Modifier.weight(1f))

            BottomDock(
                items = listOf(
                    DockItem(GameIcons.Shop, "Shop", onShopClick),
                    DockItem(GameIcons.Calendar, "Today", onTodayClick, showBadge = todayHasNews),
                    DockItem(GameIcons.Home, "Home", onClick = {}, isCenter = true),
                    DockItem(GameIcons.Trophy, "Ranks", onRankingClick),
                    DockItem(GameIcons.Gear, "Settings", onSettingsClick),
                ),
            )
        }
    }
}

/** A big mode button with its one-line pitch underneath. */
@Composable
private fun ModeButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: ButtonColor,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        GameButton(
            text = title,
            onClick = onClick,
            color = color,
            size = ButtonSize.LARGE,
            icon = icon,
            modifier = Modifier.fillMaxWidth(MODE_BUTTON_WIDTH),
        )
        Text(subtitle, style = GameTheme.type.caption, color = GameColors.TextSecondary)
    }
}

private val LOGO_HEIGHT = 190.dp
private val RIBBON_HEIGHT = 90.dp
private const val MODE_BUTTON_WIDTH = 0.75f

/** Welcome-back popup: the week's calendar, today's coins, Claim / Later. */
@Composable
private fun DailyRewardDialog(player: PlayerState, onClaim: () -> Unit, onDismiss: () -> Unit) {
    val coins = Economy.loginReward(player.loginDaysClaimed)
    GameDialog(title = "Daily reward", onDismiss = onDismiss) {
        Text(
            text = "Welcome back! Day ${Economy.loginWeekDay(player.loginDaysClaimed)} of 7",
            style = GameTheme.type.body,
            color = GameColors.TextSecondary,
        )
        LoginWeek(claimedDays = player.loginDaysClaimed, canClaim = true)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Image(GameIcons.Coin, contentDescription = null, modifier = Modifier.size(48.dp))
            OutlinedText("+$coins", GameTheme.type.display, outlineWidth = 3.dp)
        }
        DialogButtons {
            GameButton("Claim", onClick = onClaim, color = ButtonColor.GREEN, size = ButtonSize.LARGE, modifier = Modifier.fillMaxWidth())
            GameButton("Later", onClick = onDismiss, color = ButtonColor.GLASS, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
@Preview
fun HomeContentPreview() {
    MaterialTheme {
        HomeContent(
            todayHasNews = true,
            onStagesClick = {},
            onEndlessClick = {},
            onProfileClick = {},
            onTodayClick = {},
            onShopClick = {},
            onRankingClick = {},
            onSettingsClick = {},
        )
    }
}
