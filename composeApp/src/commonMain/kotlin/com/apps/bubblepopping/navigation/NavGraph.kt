package com.apps.bubblepopping.navigation

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
import com.apps.bubblepopping.view.endless.EndlessScreen
import com.apps.bubblepopping.view.stage.StageIntroScreenRoot
import com.apps.bubblepopping.view.stage.StageMapScreenRoot
import com.apps.bubblepopping.view.home.HomeScreenRoot
import com.apps.bubblepopping.view.leaderboard.LeaderboardScreenRoot
import com.apps.bubblepopping.view.play.PlayScreenRoot
import com.apps.bubblepopping.view.profile.ProfileScreenRoot
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
        }
    }
}

@Composable
fun NavGraph(container: AppContainer, hapticFeedback: HapticFeedback) {

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
        entryProvider = entryProvider {
            entry<Routes.Home> {
                HomeScreenRoot(
                    onNavigateToStages = {
                        backStack.add(Routes.Stages)
                    },
                    onNavigateToEndless = {
                        backStack.add(Routes.Endless)
                    },
                    onNavigateToProfile = {
                        backStack.add(Routes.Profile)
                    },
                )
            }
            entry<Routes.Endless> {
                EndlessScreen(
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
                        )
                    },
                    hapticFeedback = hapticFeedback,
                    onBack = {
                        backStack.removeLastOrNull()
                    },
                    // Endless: back to Home. Stage: back to the stage list.
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
                    onBack = {
                        backStack.removeLastOrNull()
                    },
                )
            }
            entry<Routes.Profile> {
                ProfileScreenRoot(
                    progressRepository = container.progressRepository,
                    onBack = {
                        backStack.removeLastOrNull()
                    },
                )
            }
        }
    )

}