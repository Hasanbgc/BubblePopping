package com.apps.bubblepopping.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.apps.bubblepopping.HapticFeedback
import com.apps.bubblepopping.data.AppContainer
import com.apps.bubblepopping.game.GameMode
import com.apps.bubblepopping.stage.StageCatalog
import com.apps.bubblepopping.view.BubbleGameViewModel
import com.apps.bubblepopping.view.endless.EndlessScreenRoot
import com.apps.bubblepopping.view.stage.StageMapScreenRoot
import com.apps.bubblepopping.view.home.HomeScreenRoot
import com.apps.bubblepopping.view.leaderboard.LeaderboardScreenRoot
import com.apps.bubblepopping.view.play.PlayScreenRoot
import com.apps.bubblepopping.view.profile.ProfileScreenRoot
import com.apps.bubblepopping.view.meta.ShopScreenRoot
import com.apps.bubblepopping.view.meta.TodayScreenRoot
import com.apps.bubblepopping.view.settings.SettingsScreenRoot
import com.apps.bubblepopping.ui.components.UiFeedbackProvider
import com.apps.bubblepopping.ui.components.BottomDock
import com.apps.bubblepopping.ui.components.DockItem
import com.apps.bubblepopping.ui.components.LocalBottomDockHeight
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.meta.currentEpochDay
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.ui.components.LocalPlayerResources
import com.apps.bubblepopping.ui.components.PlayerResources
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.combine
import com.apps.bubblepopping.ui.theme.BubbleTheme
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass


val config = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Routes.Home::class)
            subclass(Routes.Play::class)
            subclass(Routes.Ranking::class)
            subclass(Routes.Profile::class)
            subclass(Routes.Stages::class)
            subclass(Routes.Endless::class)
            subclass(Routes.Today::class)
            subclass(Routes.Shop::class)
            subclass(Routes.Settings::class)
        }
    }
}

@Composable
fun NavGraph(
    container: AppContainer,
    hapticFeedback: HapticFeedback,
    /** Closes the app from Home; null where an app can't close itself (iOS). */
    onExitApp: (() -> Unit)? = null,
) {
    BubbleTheme {
        UiFeedbackProvider(container.settingsRepository, hapticFeedback) {
            PlayerResourcesProvider(container) {
                AppNavigation(container, hapticFeedback, onExitApp)
            }
        }
    }
}

/** Keeps coins and stars up to date for every top bar. */
@Composable
private fun PlayerResourcesProvider(container: AppContainer, content: @Composable () -> Unit) {
    val today = remember { currentEpochDay() }
    val resources by remember(container) {
        combine(container.metaRepository.player(), container.progressRepository.stageProgress()) { player, progress ->
            PlayerResources(
                coins = player.coins,
                stars = StageCatalog.totalStars(progress),
                maxStars = StageCatalog.maxStars,
                dailyRewardReady = player.canClaimLogin(today),
            )
        }
    }.collectAsStateWithLifecycle(PlayerResources())
    CompositionLocalProvider(LocalPlayerResources provides resources, content = content)
}

@Composable
private fun AppNavigation(container: AppContainer, hapticFeedback: HapticFeedback, onExitApp: (() -> Unit)?) {
    val backStack = rememberNavBackStack(config, Routes.Home)
    // Collected for the whole session, not per visit (it keeps updating during a round), so the
    // stage map has up-to-date progress on its first frame instead of waiting for the database.
    val stageProgress by remember(container) { container.progressRepository.stageProgress() }
        .collectAsStateWithLifecycle(null)

    // Menu screens share one bottom dock; it hides during a round.
    val tab = dockTabFor(backStack.lastOrNull())
    var dockHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalBottomDockHeight provides if (tab != null) dockHeight else 0.dp) {
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.fillMaxSize(),
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                onBack = {
                    backStack.removeLastOrNull()
                },
                // Forward: the new screen slides in a little and fades up. Back: the reverse.
                transitionSpec = {
                    (slideInHorizontally(tween(TRANSITION_MS)) { it / SLIDE_FRACTION } + fadeIn(tween(TRANSITION_MS))) togetherWith
                        fadeOut(tween(TRANSITION_MS))
                },
                popTransitionSpec = { popTransition() },
                predictivePopTransitionSpec = { popTransition() },
                entryProvider = entryProvider {
                    entry<Routes.Home> {
                        HomeScreenRoot(
                            metaRepository = container.metaRepository,
                            progressRepository = container.progressRepository,
                            onNavigateToStages = {
                                backStack.add(Routes.Stages())
                            },
                            onNavigateToEndless = {
                                backStack.add(Routes.Endless)
                            },
                            onNavigateToProfile = {
                                backStack.add(Routes.Profile)
                            },
                            onExitApp = onExitApp,
                        )
                    }
                    entry<Routes.Endless> {
                        EndlessScreenRoot(
                            progressRepository = container.progressRepository,
                            onDifficultySelected = {
                                backStack.add(Routes.Play(GameMode.Endless(it)))
                            },
                            onBack = {
                                backStack.removeLastOrNull()
                            },
                        )
                    }
                    entry<Routes.Play> { args ->
                        val stageMode = args.mode as? GameMode.Stage
                        val nextStage = stageMode?.let { StageCatalog.next(it.stageId) }
                        PlayScreenRoot(
                            // Scoped to this back-stack entry: survives rotation, cleared on leaving.
                            viewModel = viewModel {
                                BubbleGameViewModel(
                                    mode = args.mode,
                                    progressRepository = container.progressRepository,
                                    settingsRepository = container.settingsRepository,
                                    metaRepository = container.metaRepository,
                                )
                            },
                            hapticFeedback = hapticFeedback,
                            settingsRepository = container.settingsRepository,
                            onBack = {
                                backStack.removeLastOrNull()
                            },
                            // Endless: back to the cards. Stage: back to the map. Daily: back to Today.
                            onHome = {
                                backStack.removeLastOrNull()
                            },
                            // Back to the map with the next stage's card open.
                            onNextStage = nextStage?.let { next ->
                                {
                                    backStack.removeLastOrNull()
                                    if (backStack.lastOrNull() is Routes.Stages) backStack.removeLastOrNull()
                                    backStack.add(Routes.Stages(introStageId = next.id))
                                }
                            },
                        )
                    }
                    entry<Routes.Stages> { args ->
                        StageMapScreenRoot(
                            progress = stageProgress,
                            initialIntroStageId = args.introStageId,
                            onPlayStage = { stageId ->
                                backStack.add(Routes.Play(GameMode.Stage(stageId)))
                            },
                            onBack = {
                                backStack.removeLastOrNull()
                            },
                        )
                    }
                    entry<Routes.Ranking> {
                        LeaderboardScreenRoot(
                            progressRepository = container.progressRepository,
                            settingsRepository = container.settingsRepository,
                            onBack = {
                                backStack.removeLastOrNull()
                            },
                        )
                    }
                    entry<Routes.Today> {
                        TodayScreenRoot(
                            metaRepository = container.metaRepository,
                            progressRepository = container.progressRepository,
                            onPlay = { mode ->
                                backStack.add(Routes.Play(mode))
                            },
                            onBack = {
                                backStack.removeLastOrNull()
                            },
                        )
                    }
                    entry<Routes.Settings> {
                        SettingsScreenRoot(
                            settingsRepository = container.settingsRepository,
                            onResetProgress = container::resetProgress,
                            onBack = {
                                backStack.removeLastOrNull()
                            },
                        )
                    }
                    entry<Routes.Shop> {
                        ShopScreenRoot(
                            metaRepository = container.metaRepository,
                            progressRepository = container.progressRepository,
                            settingsRepository = container.settingsRepository,
                            onBack = {
                                backStack.removeLastOrNull()
                            },
                        )
                    }
                    entry<Routes.Profile> {
                        ProfileScreenRoot(
                            progressRepository = container.progressRepository,
                            metaRepository = container.metaRepository,
                            settingsRepository = container.settingsRepository,
                            onBack = {
                                backStack.removeLastOrNull()
                            },
                        )
                    }
                }
            )
        }

        if (tab != null) {
            val resources = LocalPlayerResources.current
            BottomDock(
                items = DockTab.entries.map { entry ->
                    DockItem(
                        icon = entry.icon,
                        label = entry.label,
                        onClick = { backStack.selectTab(entry) },
                        showBadge = entry == DockTab.TODAY && resources.dailyRewardReady,
                    )
                },
                selectedIndex = tab.ordinal,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    // Measured with the system bar under it, so screens can clear both.
                    .onSizeChanged { dockHeight = with(density) { it.height.toDp() } }
                    .navigationBarsPadding(),
            )
        }
    }
}

/** The bottom dock's tabs, in order. Every menu screen belongs to one. */
private enum class DockTab(val icon: ImageVector, val label: String, val root: NavKey) {
    SHOP(GameIcons.Shop, "Shop", Routes.Shop),
    TODAY(GameIcons.Calendar, "Today", Routes.Today),
    HOME(GameIcons.Home, "Home", Routes.Home),
    RANKS(GameIcons.Trophy, "Ranks", Routes.Ranking),
    SETTINGS(GameIcons.Gear, "Settings", Routes.Settings),
}

/** The tab highlighted for [route]; null in a round, where the dock hides. */
private fun dockTabFor(route: NavKey?): DockTab? = when (route) {
    null, is Routes.Play -> null
    Routes.Shop -> DockTab.SHOP
    Routes.Today -> DockTab.TODAY
    Routes.Ranking -> DockTab.RANKS
    Routes.Settings -> DockTab.SETTINGS
    // Stages, Endless and Profile are reached from Home.
    else -> DockTab.HOME
}

/**
 * Switches to [tab]: back to Home, then the tab's screen on top, so Back from
 * any tab returns Home. Tapping the tab you're on (at its root) does nothing.
 */
private fun MutableList<NavKey>.selectTab(tab: DockTab) {
    if (lastOrNull() == tab.root) return
    while (size > 1) removeAt(lastIndex)
    if (tab.root != Routes.Home) add(tab.root)
}

private fun popTransition(): ContentTransform =
    fadeIn(tween(TRANSITION_MS)) togetherWith
        (slideOutHorizontally(tween(TRANSITION_MS)) { it / SLIDE_FRACTION } + fadeOut(tween(TRANSITION_MS)))

private const val TRANSITION_MS = 280
/** Screens slide by 1/4 of the width — a hint of movement, not a full slide. */
private const val SLIDE_FRACTION = 4
