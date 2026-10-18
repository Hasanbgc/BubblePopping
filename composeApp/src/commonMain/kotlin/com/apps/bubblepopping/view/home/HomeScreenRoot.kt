package com.apps.bubblepopping.view.home

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.paint
import com.apps.bubblepopping.ui.components.formatCount
import com.apps.bubblepopping.ui.components.ResourcePill
import com.apps.bubblepopping.ui.components.EnamelTone
import com.apps.bubblepopping.stage.StageCatalog
import com.apps.bubblepopping.data.ProgressRepository
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.apps.bubblepopping.Res
import com.apps.bubblepopping.bp_logo
import com.apps.bubblepopping.infinity
import com.apps.bubblepopping.milestones
import com.apps.bubblepopping.data.MetaRepository
import com.apps.bubblepopping.meta.Economy
import com.apps.bubblepopping.meta.PlayerState
import com.apps.bubblepopping.meta.currentEpochDay
import com.apps.bubblepopping.ribbon
import com.apps.bubblepopping.ui.components.AvatarButton
import com.apps.bubblepopping.ui.theme.BubbleTheme
import com.apps.bubblepopping.ui.components.CoinCounter
import com.apps.bubblepopping.ui.components.GameBackground
import com.apps.bubblepopping.ui.components.BackgroundStyle
import com.apps.bubblepopping.ui.components.GameTopBar
import com.apps.bubblepopping.ui.components.screenBottomPadding
import com.apps.bubblepopping.ui.components.StarCounter
import com.apps.bubblepopping.view.meta.LoginWeek
import com.apps.bubblepopping.ui.components.ButtonColor
import com.apps.bubblepopping.ui.components.ButtonSize
import com.apps.bubblepopping.ui.components.DialogButtons
import com.apps.bubblepopping.ui.components.GameButton
import com.apps.bubblepopping.ui.components.GameDialog
import com.apps.bubblepopping.ui.components.GameIconButton
import androidx.compose.ui.text.style.TextAlign
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
    progressRepository: ProgressRepository,
    onNavigateToStages: () -> Unit,
    onNavigateToEndless: () -> Unit,
    onNavigateToProfile: () -> Unit,
    /** Closes the app; null where an app can't close itself (iOS), which hides the close button. */
    onExitApp: (() -> Unit)? = null,
) {
    val today = remember { currentEpochDay() }
    // Close button and system back both ask first.
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = onExitApp != null,
        onBackCompleted = { confirmExit = true },
    )
    val scope = rememberCoroutineScope()
    // null until loaded, so the daily-reward popup never flashes for a claimed day.
    val player by remember(metaRepository) { metaRepository.player() }.collectAsStateWithLifecycle(null)
    var loginDismissed by rememberSaveable { mutableStateOf(false) }
    // null until loaded, so the cards never flash a zero.
    val stageProgress by remember(progressRepository) { progressRepository.stageProgress() }.collectAsStateWithLifecycle(null)
    val bestRun by remember(progressRepository) { progressRepository.topEndlessRuns(1) }.collectAsStateWithLifecycle(null)

    HomeContent(
        progress = stageProgress?.let { progress ->
            HomeProgress(
                currentStage = StageCatalog.currentStageId(progress),
                stars = progress.values.sumOf { it.stars },
                bestEndless = bestRun?.firstOrNull()?.score,
            )
        },
        onStagesClick = onNavigateToStages,
        onEndlessClick = onNavigateToEndless,
        onProfileClick = onNavigateToProfile,
        onCloseClick = onExitApp?.let { { confirmExit = true } },
    )

    if (confirmExit && onExitApp != null) {
        ExitDialog(onQuit = onExitApp, onStay = { confirmExit = false })
    }

    val current = player
    if (current != null && current.canClaimLogin(today) && !loginDismissed) {
        DailyRewardDialog(
            player = current,
            onClaim = { scope.launch { metaRepository.claimDailyLogin(today) } },
            onDismiss = { loginDismissed = true },
        )
    }
}

/** Home: avatar and counters, the logo, and the two mode buttons. The bottom dock is drawn by the app frame. */
@Composable
fun HomeContent(
    progress: HomeProgress?,
    onStagesClick: () -> Unit,
    onEndlessClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Shows the close button; null hides it. */
    onCloseClick: (() -> Unit)? = null,
) {
    // The mode cards' PLAY buttons breathe on the ambient clock, so the content isn't cached.
    GameBackground(modifier, style = BackgroundStyle.ART, animated = true, cacheContent = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .screenBottomPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Avatar on the left (the photo arrives with Google / Facebook sign-in), the star and
            // coin counters centred, and the close button on the right.
            GameTopBar(
                title = null,
                onBack = null,
                leading = { AvatarButton(onClick = onProfileClick) },
                center = {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                        StarCounter()
                        CoinCounter()
                    }
                },
                actions = {
                    if (onCloseClick != null) {
                        GameIconButton(GameIcons.Close, contentDescription = "Quit game", onClick = onCloseClick, color = ButtonColor.RED)
                    }
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
            // The ribbon art is taller than the room it takes up (it has clear space above and
            // below), so it's painted without a clip: Image would slice off the ribbon's tails.
            Box(
                Modifier
                    .fillMaxWidth(0.85f)
                    .height(RIBBON_HEIGHT)
                    .offset(y = -Spacing.xl)
                    .paint(painterResource(Res.drawable.ribbon), contentScale = ContentScale.FillWidth),
            )

            // The two modes as cards side by side, centred in the space left: as tall as
            // [MODE_CARD_MAX_HEIGHT], shorter on small phones so nothing is cut off.
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.l),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = MODE_CARD_MAX_HEIGHT)
                        .fillMaxHeight()
                        .padding(horizontal = Spacing.screen, vertical = Spacing.m),
                ) {
                    ModeCard(
                        title = "STAGES",
                        art = painterResource(Res.drawable.milestones),
                        color = ButtonColor.BLUE,
                        onClick = onStagesClick,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    ) {
                        ModeInfo(
                            line = progress?.let { "Stage ${it.currentStage}" },
                            pill = progress?.let { "${it.stars}/${StageCatalog.maxStars}" },
                            tone = EnamelTone.DARK,
                            tint = GameColors.PillStar,
                        )
                    }
                    ModeCard(
                        title = "ENDLESS",
                        art = painterResource(Res.drawable.infinity),
                        color = ButtonColor.VIOLET,
                        onClick = onEndlessClick,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    ) {
                        ModeInfo(
                            line = "Best score",
                            pill = progress?.let { it.bestEndless?.let(::formatCount) ?: "—" },
                            tone = EnamelTone.DARK,
                            tint = GameColors.VioletTop,
                        )
                    }
                }
            }
        }
    }
}

/** What Home shows on the mode cards; null while loading. */
data class HomeProgress(val currentStage: Int, val stars: Int, val bestEndless: Int?)

/** A card's progress: a short line and a star pill. Space is kept while loading, so the card never jumps. */
@Composable
private fun ModeInfo(line: String?, pill: String?, tone: EnamelTone, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        OutlinedText(line ?: " ", GameTheme.type.label, maxLines = 1)
        Box(Modifier.alpha(if (pill == null) 0f else 1f)) {
            ResourcePill(GameIcons.Star, pill ?: "0", tint = tint, tone = tone)
        }
    }
}

private val LOGO_HEIGHT = 190.dp
private val RIBBON_HEIGHT = 90.dp
private val MODE_CARD_MAX_HEIGHT = 280.dp

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

/** Asked before the app closes (close button or system back on Home). Staying is the big, easy choice. */
@Composable
private fun ExitDialog(onQuit: () -> Unit, onStay: () -> Unit) {
    GameDialog(title = "Leaving?", onDismiss = onStay) {
        Image(GameIcons.Bubble, contentDescription = null, modifier = Modifier.size(64.dp))
        Text(
            text = "The bubbles will miss you! Your progress is saved.",
            style = GameTheme.type.body,
            color = GameColors.TextPrimary,
            textAlign = TextAlign.Center,
        )
        DialogButtons {
            GameButton("Keep playing", onClick = onStay, color = ButtonColor.GREEN, size = ButtonSize.LARGE, modifier = Modifier.fillMaxWidth())
            GameButton("Quit", onClick = onQuit, color = ButtonColor.RED, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
@Preview
fun HomeContentPreview() {
    BubbleTheme {
        HomeContent(
            progress = HomeProgress(currentStage = 12, stars = 34, bestEndless = 1520),
            onStagesClick = {},
            onEndlessClick = {},
            onProfileClick = {},
        )
    }
}
