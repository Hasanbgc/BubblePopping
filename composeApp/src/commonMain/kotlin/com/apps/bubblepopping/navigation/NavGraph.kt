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
import com.apps.bubblepopping.view.stage.StageIntroScreenRoot
import com.apps.bubblepopping.view.stage.StageMapScreenRoot
import com.apps.bubblepopping.view.home.HomeScreenRoot
import com.apps.bubblepopping.view.leaderboard.LeaderboardScreenRoot
import com.apps.bubblepopping.view.play.PlayScreenRoot
import com.apps.bubblepopping.view.profile.ProfileScreenRoot
import com.apps.bubblepopping.view.meta.ShopScreenRoot
import com.apps.bubblepopping.view.meta.TodayScreenRoot
import com.apps.bubblepopping.view.settings.SettingsScreenRoot
import com.apps.bubblepopping.ui.components.UiFeedbackProvider
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
            subclass(Routes.StageIntro::class)
            subclass(Routes.Endless::class)
            subclass(Routes.Today::class)
            subclass(Routes.Shop::class)
            subclass(Routes.Settings::class)
        }
    }
}

@Composable
fun NavGraph(container: AppContainer, hapticFeedback: HapticFeedback) {
    BubbleTheme {
        UiFeedbackProvider(container.settingsRepository, hapticFeedback) {
            PlayerResourcesProvider(container) {
                AppNavigation(container, hapticFeedback)
            }
        }
    }
}

/** Keeps coins and stars up to date for every top bar. */
@Composable
private fun PlayerResourcesProvider(container: AppContainer, content: @Composable () -> Unit) {
    val resources by remember(container) {
        combine(container.metaRepository.player(), container.progressRepository.stageProgress()) { player, progress ->
            PlayerResources(
                coins = player.coins,
                stars = StageCatalog.totalStars(progress),
                maxStars = StageCatalog.maxStars,
            )
        }
    }.collectAsStateWithLifecycle(PlayerResources())
    CompositionLocalProvider(LocalPlayerResources provides resources, content = content)
}

@Composable
private fun AppNavigation(container: AppContainer, hapticFeedback: HapticFeedback) {
    val backStack = rememberNavBackStack(config, Routes.Home)

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
                    onNavigateToStages = {
                        backStack.add(Routes.Stages)
                    },
                    onNavigateToEndless = {
                        backStack.add(Routes.Endless)
                    },
                    onNavigateToProfile = {
                        backStack.add(Routes.Profile)
                    },
                    onNavigateToToday = {
                        backStack.add(Routes.Today)
                    },
                    onNavigateToShop = {
                        backStack.add(Routes.Shop)
                    },
                    onNavigateToRanking = {
                        backStack.add(Routes.Ranking)
                    },
                    onNavigateToSettings = {
                        backStack.add(Routes.Settings)
                    },
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
                    // The play screen replaces itself with the next stage's card.
                    onNextStage = nextStage?.let { next ->
                        {
                            backStack.removeLastOrNull()
                            backStack.add(Routes.StageIntro(next.id))
                        }
                    },
                )
            }
            entry<Routes.Stages> {
                StageMapScreenRoot(
                    progressRepository = container.progressRepository,
                    onStageSelected = { stageId ->
                        backStack.add(Routes.StageIntro(stageId))
                    },
                    onBack = {
                        backStack.removeLastOrNull()
                    },
                )
            }
            entry<Routes.StageIntro> { args ->
                StageIntroScreenRoot(
                    stage = StageCatalog.stage(args.stageId),
                    progressRepository = container.progressRepository,
                    // The card is replaced by the round, so leaving the round returns to the list.
                    onPlay = {
                        backStack.removeLastOrNull()
                        backStack.add(Routes.Play(GameMode.Stage(args.stageId)))
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
private fun popTransition(): ContentTransform =
    fadeIn(tween(TRANSITION_MS)) togetherWith
        (slideOutHorizontally(tween(TRANSITION_MS)) { it / SLIDE_FRACTION } + fadeOut(tween(TRANSITION_MS)))

private const val TRANSITION_MS = 280
/** Screens slide by 1/4 of the width — a hint of movement, not a full slide. */
private const val SLIDE_FRACTION = 4
