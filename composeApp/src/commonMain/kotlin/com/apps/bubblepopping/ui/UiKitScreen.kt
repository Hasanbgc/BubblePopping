package com.apps.bubblepopping.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.ui.components.BadgeDot
import com.apps.bubblepopping.ui.components.BottomDock
import com.apps.bubblepopping.ui.components.ButtonColor
import com.apps.bubblepopping.ui.components.ButtonSize
import com.apps.bubblepopping.ui.components.DockItem
import com.apps.bubblepopping.ui.components.GameButton
import com.apps.bubblepopping.ui.components.GameDialog
import com.apps.bubblepopping.ui.components.GameIconButton
import com.apps.bubblepopping.ui.components.GameProgressBar
import com.apps.bubblepopping.ui.components.GameScreen
import com.apps.bubblepopping.ui.components.GameTabs
import com.apps.bubblepopping.ui.components.GameTextField
import com.apps.bubblepopping.ui.components.GlassPanel
import com.apps.bubblepopping.ui.components.OutlinedText
import com.apps.bubblepopping.ui.components.ResourcePill
import com.apps.bubblepopping.ui.components.Ribbon
import com.apps.bubblepopping.ui.components.ToggleRow
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.BubbleTheme
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing

/**
 * Every "Bubble Glass" component on one screen — the design system's reference.
 * Not part of the app; view it through [UiKitScreenPreview] in Android Studio.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UiKitScreen(onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var music by remember { mutableStateOf(true) }
    var vibration by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    GameScreen(
        title = "UI Kit",
        onBack = onBack,
        actions = {
            ResourcePill(GameIcons.Coin, "1,240")
        },
        bottomBar = {
            BottomDock(
                listOf(
                    DockItem(GameIcons.Shop, "Shop", {}),
                    DockItem(GameIcons.Calendar, "Today", {}, showBadge = true),
                    DockItem(GameIcons.Home, "Home", {}, isCenter = true),
                    DockItem(GameIcons.Trophy, "Ranks", {}),
                    DockItem(GameIcons.Gear, "Settings", {}),
                )
            )
        },
    ) {
        GlassPanel(Modifier.fillMaxWidth(), title = "Text") {
            OutlinedText("WAVE 4", GameTheme.type.display, outlineWidth = 3.dp)
            OutlinedText("Screen title", GameTheme.type.title, outlineWidth = 3.dp)
            OutlinedText("Panel headline", GameTheme.type.headline)
            Text("Body text reads like this. Pop bubbles before they float away!", style = GameTheme.type.body, color = GameColors.TextPrimary)
            Text("Label text", style = GameTheme.type.label, color = GameColors.TextPrimary)
            Text("Caption — hints and fine print", style = GameTheme.type.caption, color = GameColors.TextSecondary)
        }

        GlassPanel(Modifier.fillMaxWidth(), title = "Buttons") {
            GameButton("Play", onClick = {}, size = ButtonSize.LARGE, icon = GameIcons.Play, modifier = Modifier.fillMaxWidth())
            GameButton("Daily Challenge", onClick = {}, color = ButtonColor.VIOLET, modifier = Modifier.fillMaxWidth())
            GameButton("Claim +40", onClick = {}, color = ButtonColor.GREEN, icon = GameIcons.Coin, modifier = Modifier.fillMaxWidth())
            GameButton("300", onClick = {}, color = ButtonColor.GOLD, icon = GameIcons.Coin, modifier = Modifier.fillMaxWidth())
            GameButton("Reset progress", onClick = {}, color = ButtonColor.RED, modifier = Modifier.fillMaxWidth())
            GameButton("Later", onClick = {}, color = ButtonColor.GLASS, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                GameButton("Equip", onClick = {}, color = ButtonColor.GREEN, size = ButtonSize.SMALL)
                GameButton("Locked", onClick = {}, size = ButtonSize.SMALL, enabled = false)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                GameIconButton(GameIcons.Back, "Back", onClick = {})
                GameIconButton(GameIcons.Pause, "Pause", onClick = {}, color = ButtonColor.BLUE)
                GameIconButton(GameIcons.Restart, "Restart", onClick = {}, color = ButtonColor.VIOLET)
                GameIconButton(GameIcons.Close, "Close", onClick = {}, color = ButtonColor.RED)
                GameIconButton(GameIcons.Gear, "Settings", onClick = {})
            }
        }

        GlassPanel(Modifier.fillMaxWidth(), title = "Icons") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                listOf<ImageVector>(
                    GameIcons.Coin, GameIcons.Star, GameIcons.Heart, GameIcons.Gear, GameIcons.Trophy,
                    GameIcons.Calendar, GameIcons.Shop, GameIcons.Lock, GameIcons.Home, GameIcons.Person,
                    GameIcons.Chest, GameIcons.Target, GameIcons.Crown, GameIcons.Music, GameIcons.Sound,
                    GameIcons.Vibrate, GameIcons.Wind, GameIcons.Bubble, GameIcons.Play, GameIcons.Pause,
                    GameIcons.Restart, GameIcons.Check, GameIcons.Back, GameIcons.Close, GameIcons.Pencil, GameIcons.Skull, GameIcons.Clock, GameIcons.Shield, GameIcons.Snowflake, GameIcons.Tap,
                ).forEach { Image(it, contentDescription = it.name, modifier = Modifier.size(40.dp)) }
            }
        }

        GlassPanel(Modifier.fillMaxWidth(), title = "Controls") {
            GameTabs(listOf("Skins", "Pops", "Backgrounds"), selected = tab, onSelect = { tab = it })
            ToggleRow(GameIcons.Music, "Music", "Background music in rounds", music, { music = it })
            ToggleRow(GameIcons.Vibrate, "Vibration", null, vibration, { vibration = it })
            GameTextField(name, { name = it }, placeholder = "Your name", modifier = Modifier.fillMaxWidth())
            GameProgressBar(0.65f)
            GameProgressBar(0.3f, color = GameColors.Success)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m), verticalAlignment = Alignment.CenterVertically) {
                ResourcePill(GameIcons.Coin, "1,240")
                ResourcePill(GameIcons.Star, "34")
                BadgeDot()
            }
        }

        GlassPanel(Modifier.fillMaxWidth(), title = "Dialog") {
            Ribbon("Daily reward", Modifier.align(Alignment.CenterHorizontally))
            GameButton("Open a dialog", onClick = { showDialog = true }, color = ButtonColor.VIOLET, modifier = Modifier.fillMaxWidth())
        }
    }

    if (showDialog) {
        GameDialog(title = "Day 3", onDismiss = { showDialog = false }) {
            Image(GameIcons.Coin, contentDescription = null, modifier = Modifier.size(72.dp))
            OutlinedText("+20", GameTheme.type.display, outlineWidth = 3.dp)
            Text(
                "Welcome back! Day 7 of every week pays 100.",
                style = GameTheme.type.body,
                color = GameColors.TextSecondary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                GameButton("Claim", onClick = { showDialog = false }, color = ButtonColor.GREEN, modifier = Modifier.fillMaxWidth())
                GameButton("Later", onClick = { showDialog = false }, color = ButtonColor.GLASS, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Preview
@Composable
fun UiKitScreenPreview() {
    BubbleTheme { UiKitScreen(onBack = {}) }
}
