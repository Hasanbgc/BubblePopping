package com.apps.bubblepopping.view.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.bubblepopping.data.prefs.GameSettings
import com.apps.bubblepopping.data.prefs.SettingsRepository
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import com.apps.bubblepopping.ui.components.ButtonColor
import com.apps.bubblepopping.ui.components.DialogButtons
import com.apps.bubblepopping.ui.components.GameButton
import com.apps.bubblepopping.ui.components.GameDialog
import com.apps.bubblepopping.ui.components.ToggleRow
import com.apps.bubblepopping.ui.components.GameScreen
import com.apps.bubblepopping.ui.components.GlassPanel
import com.apps.bubblepopping.view.meta.Hint
import com.apps.bubblepopping.ui.theme.BubbleTheme
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing
import kotlinx.coroutines.launch

/** One switchable setting. */
enum class Toggle(val label: String, val hint: String, val icon: ImageVector) {
    SOUND("Sound effects", "Pops, bombs, bosses", GameIcons.Sound),
    MUSIC("Music", "Background music in rounds", GameIcons.Music),
    HAPTICS("Vibration", "Haptic feedback on pops and hits", GameIcons.Vibrate),
    WIND_BUTTONS("Wind buttons", "On-screen wind buttons (swiping always works)", GameIcons.Wind),
    LARGE_HIT_AREA("Larger tap area", "Easier to hit small bubbles", GameIcons.Target),
    HIGH_CONTRAST_SKULLS("Skull warning ring", "Striped ring around skulls, easy to spot without colour", GameIcons.Skull);

    fun isOn(settings: GameSettings): Boolean = when (this) {
        SOUND -> settings.soundEnabled
        MUSIC -> settings.musicEnabled
        HAPTICS -> settings.hapticsEnabled
        WIND_BUTTONS -> settings.windButtonsEnabled
        LARGE_HIT_AREA -> settings.largeHitArea
        HIGH_CONTRAST_SKULLS -> settings.highContrastSkulls
    }

    suspend fun set(repository: SettingsRepository, on: Boolean) = when (this) {
        SOUND -> repository.setSoundEnabled(on)
        MUSIC -> repository.setMusicEnabled(on)
        HAPTICS -> repository.setHapticsEnabled(on)
        WIND_BUTTONS -> repository.setWindButtonsEnabled(on)
        LARGE_HIT_AREA -> repository.setLargeHitArea(on)
        HIGH_CONTRAST_SKULLS -> repository.setHighContrastSkulls(on)
    }

    companion object {
        val AUDIO_AND_CONTROLS = listOf(SOUND, MUSIC, HAPTICS, WIND_BUTTONS)
        val ACCESSIBILITY = listOf(LARGE_HIT_AREA, HIGH_CONTRAST_SKULLS)
    }
}

@Composable
fun SettingsScreenRoot(
    settingsRepository: SettingsRepository,
    onResetProgress: suspend () -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val settings by remember(settingsRepository) { settingsRepository.settings }
        .collectAsStateWithLifecycle(GameSettings())

    SettingsScreen(
        settings = settings,
        onToggle = { toggle, on -> scope.launch { toggle.set(settingsRepository, on) } },
        onReplayTutorial = { scope.launch { settingsRepository.setTutorialSeen(false) } },
        onResetProgress = { scope.launch { onResetProgress() } },
        onBack = onBack,
    )
}

@Composable
fun SettingsScreen(
    settings: GameSettings,
    onToggle: (Toggle, Boolean) -> Unit,
    onReplayTutorial: () -> Unit,
    onResetProgress: () -> Unit,
    onBack: () -> Unit,
) {
    var confirmReset by remember { mutableStateOf(false) }
    var resetDone by remember { mutableStateOf(false) }

    GameScreen(title = "Settings", onBack = onBack) {
        GlassPanel(Modifier.fillMaxWidth(), title = "Sound & controls", icon = GameIcons.Sound) {
            SettingsToggles(settings, Toggle.AUDIO_AND_CONTROLS, onToggle)
        }
        GlassPanel(Modifier.fillMaxWidth(), title = "Accessibility", icon = GameIcons.Target) {
            SettingsToggles(settings, Toggle.ACCESSIBILITY, onToggle)
        }
        GlassPanel(Modifier.fillMaxWidth(), title = "Help", icon = GameIcons.Bubble) {
            Hint(
                if (settings.tutorialSeen) "Replay the tutorial at the start of stage 1."
                else "The tutorial will play at the start of stage 1."
            )
            GameButton(
                text = "Replay tutorial",
                onClick = onReplayTutorial,
                color = ButtonColor.GLASS,
                icon = GameIcons.Restart,
                enabled = settings.tutorialSeen,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        GlassPanel(Modifier.fillMaxWidth(), title = "Progress", icon = GameIcons.Skull) {
            Hint(
                if (resetDone) "Progress reset."
                else "Deletes stars, scores, coins, missions, cosmetics and achievements. Settings and your name stay."
            )
            GameButton(
                text = "Reset progress",
                onClick = { confirmReset = true },
                color = ButtonColor.RED,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (confirmReset) {
        ResetProgressDialog(
            onConfirm = {
                confirmReset = false
                resetDone = true
                onResetProgress()
            },
            onDismiss = { confirmReset = false },
        )
    }
}

/** Last check before deleting everything. Red action, glass "Keep" as the easy way out. */
@Composable
private fun ResetProgressDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    GameDialog(title = "Reset progress?", onDismiss = onDismiss) {
        Image(GameIcons.Skull, contentDescription = null, modifier = Modifier.size(DIALOG_ICON))
        Text(
            text = "Every star, score, coin, mission, cosmetic and achievement will be deleted.",
            style = GameTheme.type.body,
            color = GameColors.TextPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "This can't be undone. Your settings and name stay.",
            style = GameTheme.type.caption,
            color = GameColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        DialogButtons {
            GameButton("Reset everything", onClick = onConfirm, color = ButtonColor.RED, modifier = Modifier.fillMaxWidth())
            GameButton("Keep my progress", onClick = onDismiss, color = ButtonColor.GLASS, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** A switch row per [toggles] entry. Also used by the in-game pause menu. */
@Composable
fun SettingsToggles(
    settings: GameSettings,
    toggles: List<Toggle>,
    onToggle: (Toggle, Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        toggles.forEach { toggle ->
            ToggleRow(
                icon = toggle.icon,
                label = toggle.label,
                hint = toggle.hint,
                checked = toggle.isOn(settings),
                onCheckedChange = { onToggle(toggle, it) },
            )
        }
    }
}

/** In-game settings, opened from the pause menu. Scrolls on short screens. */
@Composable
fun InGameSettingsDialog(
    settings: GameSettings,
    onToggle: (Toggle, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    GameDialog(title = "Settings", onDismiss = onDismiss) {
        Column(
            Modifier
                .heightIn(max = IN_GAME_SETTINGS_MAX_HEIGHT)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsToggles(settings, Toggle.AUDIO_AND_CONTROLS + Toggle.ACCESSIBILITY, onToggle)
        }
        DialogButtons {
            GameButton("Done", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}

private val DIALOG_ICON = 64.dp
private val IN_GAME_SETTINGS_MAX_HEIGHT = 380.dp

@Preview
@Composable
fun SettingsScreenPreview() {
    BubbleTheme {
        SettingsScreen(
            settings = GameSettings(tutorialSeen = true, largeHitArea = true),
            onToggle = { _, _ -> },
            onReplayTutorial = {},
            onResetProgress = {},
            onBack = {},
        )
    }
}
