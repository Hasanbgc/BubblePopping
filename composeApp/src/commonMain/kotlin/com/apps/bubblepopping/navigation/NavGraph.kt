package com.apps.bubblepopping.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.apps.bubblepopping.HapticFeedback
import com.apps.bubblepopping.view.home.HomeScreenRoot
import com.apps.bubblepopping.view.play.PlayScreenRoot
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass


val config = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Routes.Home::class)
            subclass(Routes.Play::class)
        }
    }
}

@Composable
fun NavGraph(hapticFeedback: HapticFeedback) {

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
                    onBack = {
                        backStack.removeLastOrNull()
                    },
                    onNavigateToPlay = {
                        backStack.add(Routes.Play(difficulty = it))
                    }
                )
            }
            entry<Routes.Play> { args ->
                PlayScreenRoot(
                    difficulty = args.difficulty,
                    hapticFeedback = hapticFeedback,
                    onBack = {
                        backStack.removeLastOrNull()
                    },
                    onNavigateToRanking = {
                        backStack.add(Routes.Ranking)
                    }
                )
            }
        }
    )

}