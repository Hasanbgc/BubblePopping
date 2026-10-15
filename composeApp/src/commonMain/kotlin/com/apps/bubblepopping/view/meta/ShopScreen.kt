package com.apps.bubblepopping.view.meta

import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.components.sunkenWell
import com.apps.bubblepopping.ui.components.ResourcePill
import com.apps.bubblepopping.ui.components.OutlinedText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.Image
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.bubblepopping.data.MetaRepository
import com.apps.bubblepopping.data.ProgressRepository
import com.apps.bubblepopping.data.prefs.GameSettings
import com.apps.bubblepopping.data.prefs.SettingsRepository
import com.apps.bubblepopping.meta.Cosmetic
import com.apps.bubblepopping.meta.CosmeticKind
import com.apps.bubblepopping.meta.CosmeticRarity
import com.apps.bubblepopping.meta.Cosmetics
import com.apps.bubblepopping.meta.PlayerState
import com.apps.bubblepopping.ui.components.CoinCounter
import com.apps.bubblepopping.ui.components.ButtonColor
import com.apps.bubblepopping.ui.components.ButtonSize
import com.apps.bubblepopping.ui.components.CardTier
import com.apps.bubblepopping.ui.components.EquippedLabel
import com.apps.bubblepopping.ui.components.ShopCard
import com.apps.bubblepopping.ui.components.GameButton
import com.apps.bubblepopping.ui.components.GameScreen
import com.apps.bubblepopping.ui.components.GameTabs
import com.apps.bubblepopping.ui.components.formatCount
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.BubbleTheme
import com.apps.bubblepopping.ui.theme.Spacing
import androidx.compose.foundation.layout.size
import com.apps.bubblepopping.view.play.component.PopAnimation
import com.apps.bubblepopping.view.play.component.drawPopAnimation
import com.apps.bubblepopping.view.play.component.drawSkinPreview
import com.apps.bubblepopping.view.playBackgroundColors
import com.apps.bubblepopping.view.worldMapColors
import com.apps.bubblepopping.stage.World
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@Composable
fun ShopScreenRoot(
    metaRepository: MetaRepository,
    progressRepository: ProgressRepository,
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val player by remember(metaRepository) { metaRepository.player() }.collectAsStateWithLifecycle(PlayerState())
    val owned by remember(metaRepository, progressRepository) {
        combine(metaRepository.purchasedCosmetics(), progressRepository.stageProgress()) { purchased, progress ->
            Cosmetics.owned(purchased, progress)
        }
    }.collectAsStateWithLifecycle(Cosmetics.owned(emptySet(), emptyMap()))
    val settings by remember(settingsRepository) { settingsRepository.settings }.collectAsStateWithLifecycle(GameSettings())

    ShopScreen(
        coins = player.coins,
        owned = owned,
        settings = settings,
        onBuy = { cosmetic ->
            scope.launch {
                // Equip straight away — that's what the player wants to see.
                if (metaRepository.buy(cosmetic)) settingsRepository.equip(cosmetic)
            }
        },
        onEquip = { cosmetic -> scope.launch { settingsRepository.equip(cosmetic) } },
        onBack = onBack,
    )
}

/** Cosmetics shop: one tab per kind, each item with a live preview. */
@Composable
fun ShopScreen(
    coins: Int,
    owned: Set<String>,
    settings: GameSettings,
    onBuy: (Cosmetic) -> Unit,
    onEquip: (Cosmetic) -> Unit,
    onBack: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(0) }
    val kind = CosmeticKind.entries[tab]

    // The grid scrolls itself, so the screen column doesn't.
    GameScreen(title = "Shop", onBack = onBack, actions = { CoinCounter() }, scrollable = false) {
        GameTabs(
            options = CosmeticKind.entries.map { it.title },
            selected = tab,
            onSelect = { tab = it },
        )
        // How much of this tab's collection is owned.
        val items = Cosmetics.of(kind)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedText("Collection", GameTheme.type.headline, maxLines = 1, modifier = Modifier.weight(1f))
            ResourcePill(GameIcons.Check, "${items.count { it.id in owned }}/${items.size} owned", tint = GameColors.Success)
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            // Room at the sides and ends for the cards' outlines and shadows, which sit outside them.
            contentPadding = PaddingValues(horizontal = GRID_EDGE, vertical = Spacing.s),
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
            modifier = Modifier.weight(1f),
        ) {
            items(items, key = { it.id }) { cosmetic ->
                ShopItem(
                    cosmetic = cosmetic,
                    owned = cosmetic.id in owned,
                    equipped = settings.equipped(kind) == cosmetic.id,
                    affordable = (cosmetic.price ?: 0) <= coins,
                    onBuy = { onBuy(cosmetic) },
                    onEquip = { onEquip(cosmetic) },
                )
            }
        }
        // A short note in a sunken strip.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            modifier = Modifier
                .fillMaxWidth()
                .sunkenWell()
                .padding(horizontal = Spacing.m, vertical = Spacing.s),
        ) {
            Image(GameIcons.Bubble, contentDescription = null, modifier = Modifier.size(NOTE_ICON))
            Hint(
                text = "Bubble skins change normal bubbles only. Backgrounds show in Endless and the Daily Challenge.",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ShopItem(
    cosmetic: Cosmetic,
    owned: Boolean,
    equipped: Boolean,
    affordable: Boolean,
    onBuy: () -> Unit,
    onEquip: () -> Unit,
) {
    val price = cosmetic.price
    ShopCard(
        name = cosmetic.name,
        tier = cosmetic.rarity.cardTier,
        owned = owned,
        equipped = equipped,
        // Boss rewards can't be bought: until won, the window shows a lock.
        locked = !owned && price == null,
        preview = { glow, modifier -> CosmeticPreview(cosmetic, modifier, glow) },
        action = {
            when {
                equipped -> EquippedLabel()
                owned -> GameButton("Equip", onClick = onEquip, color = ButtonColor.GREEN, size = ButtonSize.SMALL, modifier = Modifier.fillMaxWidth())
                price != null -> GameButton(
                    text = formatCount(price),
                    onClick = onBuy,
                    color = ButtonColor.GOLD,
                    size = ButtonSize.SMALL,
                    icon = GameIcons.Coin,
                    enabled = affordable,
                    modifier = Modifier.fillMaxWidth(),
                )
                else -> Hint(Cosmetics.unlockHint(cosmetic) ?: "Locked")
            }
        },
    )
}

private val CosmeticRarity.cardTier: CardTier
    get() = when (this) {
        CosmeticRarity.COMMON -> CardTier.COMMON
        CosmeticRarity.RARE -> CardTier.RARE
        CosmeticRarity.EPIC -> CardTier.EPIC
        CosmeticRarity.LEGENDARY -> CardTier.LEGENDARY
    }

/**
 * A looping preview: a turning bubble, a repeating pop, or the background colours.
 * [glow] lights the item from behind (the shop card's tier colour).
 */
@Composable
fun CosmeticPreview(cosmetic: Cosmetic, modifier: Modifier = Modifier, glow: Color = Color.Transparent) {
    val transition = rememberInfiniteTransition(label = "preview")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(PREVIEW_LOOP_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "preview_phase",
    )
    val background = when (cosmetic.kind) {
        CosmeticKind.BACKGROUND -> playBackgroundColors(cosmetic.id) ?: worldMapColors(World.SKY)
        else -> listOf(Color(0xFF0D47A1), Color(0xFF1976D2))
    }
    Canvas(modifier.background(Brush.verticalGradient(background))) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.28f
        if (glow.alpha > 0f) {
            drawCircle(Brush.radialGradient(listOf(glow, Color.Transparent), center, radius * 2.2f), radius * 2.2f, center)
        }
        when (cosmetic.kind) {
            CosmeticKind.SKIN -> drawSkinPreview(cosmetic.id, center, radius, rotation = phase * 360f)
            CosmeticKind.POP_EFFECT -> {
                // First half: the bubble; second half: it pops.
                if (phase < 0.4f) {
                    drawSkinPreview(Cosmetics.CLASSIC.id, center, radius * 0.7f, rotation = phase * 200f)
                } else {
                    val pop = PopAnimation(
                        id = cosmetic.id,
                        x = center.x,
                        y = center.y,
                        radius = radius * 0.7f,
                        color = Color.White,
                        progress = (phase - 0.4f) / 0.6f,
                    )
                    drawPopAnimation(pop, cosmetic.id)
                }
            }
            CosmeticKind.BACKGROUND -> drawSkinPreview(Cosmetics.CLASSIC.id, center, radius * 0.6f, rotation = phase * 360f)
        }
    }
}

private const val PREVIEW_LOOP_MS = 1_600
/** Outline + shadow room around the grid's outer cards. */
private val GRID_EDGE = 4.dp
private val NOTE_ICON = 22.dp

@Preview
@Composable
fun ShopScreenPreview() {
    BubbleTheme {
        ShopScreen(
            coins = 420,
            owned = setOf("skin_classic", "skin_cloud", "pop_rainbow", "bg_default"),
            settings = GameSettings(equippedSkin = "skin_cloud"),
            onBuy = {},
            onEquip = {},
            onBack = {},
        )
    }
}
