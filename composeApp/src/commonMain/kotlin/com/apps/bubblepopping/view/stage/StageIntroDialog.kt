package com.apps.bubblepopping.view.stage

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apps.bubblepopping.data.StageProgress
import com.apps.bubblepopping.meta.Booster
import com.apps.bubblepopping.stage.StageCatalog
import com.apps.bubblepopping.stage.StageConfig
import com.apps.bubblepopping.ui.components.ButtonColor
import com.apps.bubblepopping.ui.components.ButtonSize
import com.apps.bubblepopping.ui.components.GameButton
import com.apps.bubblepopping.ui.components.GameDialog
import com.apps.bubblepopping.ui.components.LocalPlayerResources
import com.apps.bubblepopping.ui.components.OutlinedText
import com.apps.bubblepopping.ui.components.PressableSurface
import com.apps.bubblepopping.ui.components.StatusBadge
import com.apps.bubblepopping.ui.components.enamelPill
import com.apps.bubblepopping.ui.components.formatCount
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.icons.playIcon
import com.apps.bubblepopping.ui.theme.BubbleTheme
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing

/**
 * Shown over the stage map when a stage is tapped, built to be taken in at a
 * glance rather than read: the stars earned, then every target in one gold
 * well (picture, short title, number — the main goal big, springing in last so
 * the eye lands there, the bonus stars below), the power-ups as round buttons,
 * and Play.
 */
@Composable
fun StageIntroDialog(
    stage: StageConfig,
    progress: StageProgress?,
    /** Starts the stage with the boosters picked here (paid for as it starts). */
    onPlay: (Set<Booster>) -> Unit,
    onDismiss: () -> Unit,
) {
    val offered = remember(stage.id) { Booster.offeredFor(stage) }
    var picked by remember(stage.id) { mutableStateOf(emptySet<Booster>()) }
    val coins = LocalPlayerResources.current.coins ?: 0
    val cost = Booster.price(picked)
    GameDialog(title = "Stage ${stage.id}", onDismiss = onDismiss) {
        stage.boss?.let { boss ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Image(GameIcons.Crown, contentDescription = null, modifier = Modifier.size(BOSS_ICON))
                Text("BOSS · ${boss.name}", style = GameTheme.type.label, color = GameColors.Gold)
            }
        }
        // No best score here: a stage is played for its targets, not for a record.
        StageStars(earned = progress?.stars ?: 0, total = stage.maxStars)

        TargetsSection(stage.goal, stage.bonusGoals, stage.world, popIn = true)

        // Head starts for this attempt, paid when PLAY is pressed.
        if (offered.isNotEmpty()) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                SectionTitle("POWER-UPS")
                // Up to five fit side by side on the narrowest phone.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    offered.forEach { booster ->
                        val selected = booster in picked
                        BoosterButton(
                            booster = booster,
                            selected = selected,
                            affordable = selected || cost + booster.price <= coins,
                            onClick = { picked = if (selected) picked - booster else picked + booster },
                        )
                    }
                }
            }
        }

        GameButton(
            text = if (picked.isEmpty()) "PLAY" else "PLAY · ${formatCount(cost)}",
            onClick = { onPlay(picked) },
            // Same green PLAY as everywhere else.
            color = ButtonColor.GREEN,
            size = ButtonSize.LARGE,
            icon = playIcon(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * The stage's stars as one shape, the classic way: the middle star bigger and
 * the side ones a little lower, so the row reads as an arc. Earned stars are
 * gold, the rest faded. An even count has no middle, so its stars stay level.
 */
@Composable
private fun StageStars(earned: Int, total: Int, modifier: Modifier = Modifier) {
    val middle = if (total % 2 == 1 && total >= 3) total / 2 else -1
    Row(modifier, verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        repeat(total) { index ->
            Image(
                imageVector = GameIcons.Star,
                contentDescription = null,
                modifier = Modifier
                    .padding(top = if (index == middle || middle < 0) 0.dp else SIDE_STAR_DROP)
                    .size(if (index == middle) MIDDLE_STAR else SIDE_STAR)
                    .alpha(if (index < earned) 1f else UNEARNED_STAR_ALPHA),
            )
        }
    }
}

/**
 * One booster: a round candy button with its icon, and its price on a coin tag
 * tucked under it. Tapping picks it — the button turns green with a tick on its
 * corner — or drops it. One the player can't afford is greyed out.
 */
@Composable
private fun BoosterButton(
    booster: Booster,
    selected: Boolean,
    affordable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "${booster.title}: ${booster.description}, ${booster.price} coins"
            this.selected = selected
        },
    ) {
        Box {
            PressableSurface(
                onClick = onClick,
                color = if (selected) ButtonColor.GREEN else ButtonColor.GLASS,
                shape = CircleShape,
                lip = BOOSTER_LIP,
                enabled = affordable,
                role = Role.Checkbox,
                modifier = Modifier.size(width = BOOSTER_SIZE, height = BOOSTER_SIZE + BOOSTER_LIP),
            ) {
                Image(booster.icon, contentDescription = null, modifier = Modifier.size(BOOSTER_ICON))
            }
            if (selected) {
                StatusBadge(
                    GameIcons.Tick,
                    ButtonColor.GREEN,
                    size = TICK_SIZE,
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = TICK_SIZE / 4, y = -TICK_SIZE / 4),
                )
            }
        }
        // The price tag overlaps the button's lip, like a label hung on it.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .offset(y = -PRICE_OVERLAP)
                .alpha(if (affordable) 1f else UNAFFORDABLE_ALPHA)
                .height(PRICE_HEIGHT)
                .enamelPill(GameColors.PillCoin)
                .padding(start = 2.dp, end = Spacing.s),
        ) {
            Image(GameIcons.Coin, contentDescription = null, modifier = Modifier.size(PRICE_ICON))
            OutlinedText("${booster.price}", GameTheme.type.caption, outlineWidth = 1.5.dp, maxLines = 1)
        }
    }
}

private val Booster.icon: ImageVector
    get() = when (this) {
        Booster.EXTRA_LIFE -> GameIcons.Heart
        Booster.SHIELD -> GameIcons.Shield
        Booster.ICE -> GameIcons.Snowflake
        Booster.CLOCK -> GameIcons.Clock
        Booster.LIGHTNING -> GameIcons.Bolt
    }

private val BOOSTER_SIZE = 48.dp
private val BOOSTER_LIP = 4.dp
private val BOOSTER_ICON = 28.dp
private val TICK_SIZE = 22.dp
private val PRICE_HEIGHT = 20.dp
private val PRICE_ICON = 16.dp
private val PRICE_OVERLAP = 6.dp
private const val UNAFFORDABLE_ALPHA = 0.45f
private val BOSS_ICON = 28.dp
private val SIDE_STAR = 38.dp
private val MIDDLE_STAR = 50.dp
private val SIDE_STAR_DROP = 10.dp
private const val UNEARNED_STAR_ALPHA = 0.3f

@Preview
@Composable
fun StageIntroDialogPreview() {
    BubbleTheme {
        StageIntroDialog(
            stage = StageCatalog.stage(12),
            progress = StageProgress(12, stars = 2, bestScore = 44),
            onPlay = {},
            onDismiss = {},
        )
    }
}
