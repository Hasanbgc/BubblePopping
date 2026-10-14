package com.apps.bubblepopping.view.stage

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.bubblepopping.data.ProgressRepository
import com.apps.bubblepopping.data.StageProgress
import com.apps.bubblepopping.stage.StageCatalog
import com.apps.bubblepopping.stage.StageConfig
import com.apps.bubblepopping.stage.World
import com.apps.bubblepopping.view.MapCloudColor
import com.apps.bubblepopping.view.MapRoadColor
import com.apps.bubblepopping.view.MapRoadDashColor
import com.apps.bubblepopping.view.MapRoadDoneColor
import com.apps.bubblepopping.ui.components.GameTopBar
import com.apps.bubblepopping.ui.components.screenBottomPadding
import com.apps.bubblepopping.ui.components.LocalUiFeedback
import com.apps.bubblepopping.ui.components.OutlinedText
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.BubbleTheme
import com.apps.bubblepopping.ui.theme.Corners
import com.apps.bubblepopping.ui.theme.GameColors
import com.apps.bubblepopping.ui.theme.GameTheme
import com.apps.bubblepopping.ui.theme.Spacing
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.apps.bubblepopping.ui.components.StarCounter
import com.apps.bubblepopping.view.worldMapColors
import kotlinx.coroutines.flow.first
import kotlin.math.sin

/**
 * The stage map: a winding road climbing from stage 1 at the bottom, one
 * themed section per world, and a teaser for the next world at the top.
 */
@Composable
fun StageMapScreenRoot(
    progressRepository: ProgressRepository,
    onStageSelected: (Int) -> Unit,
    onBack: () -> Unit,
) {
    // null until the first load, so the map doesn't scroll to stage 1 and then jump.
    val progress by remember(progressRepository) { progressRepository.stageProgress() }
        .collectAsStateWithLifecycle(initialValue = null)

    StageMapScreen(
        stages = StageCatalog.stages,
        progress = progress,
        onStageSelected = onStageSelected,
        onBack = onBack,
    )
}

/** [progress] is null while loading. */
@Composable
fun StageMapScreen(
    stages: List<StageConfig>,
    progress: Map<Int, StageProgress>?,
    onStageSelected: (Int) -> Unit,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameColors.Scrim)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().screenBottomPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GameTopBar(title = "Stages", onBack = onBack, actions = { StarCounter(showMax = true) })
            if (progress != null) {
                StageMap(
                    stages = stages,
                    progress = progress,
                    onStageSelected = onStageSelected,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Map
// ─────────────────────────────────────────────────────────────────────────────

private enum class NodeState { LOCKED, CURRENT, DONE }

@Composable
private fun StageMap(
    stages: List<StageConfig>,
    progress: Map<Int, StageProgress>,
    onStageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val layout = remember(stages, maxWidth) { MapLayout(stages, maxWidth) }
        val viewportHeight = maxHeight
        val scrollState = rememberScrollState()
        val density = LocalDensity.current
        val currentId = StageCatalog.currentStageId(progress)
        val currentIndex = stages.indexOfFirst { it.id == currentId }

        // Start with the current stage in the middle of the screen.
        LaunchedEffect(layout) {
            snapshotFlow { scrollState.maxValue }.first { it in 1 until Int.MAX_VALUE }
            val target = with(density) {
                (layout.nodeCenter(currentIndex).y - viewportHeight / 2).toPx()
            }
            scrollState.scrollTo(target.toInt().coerceIn(0, scrollState.maxValue))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(layout.height)
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawWorldBackgrounds(layout)
                    drawClouds(layout)
                    drawRoad(layout, unlockedCount = stages.count { StageCatalog.isUnlocked(it.id, progress) })
                }

                // World banners sit just below each world's first stage.
                StageCatalog.worlds.forEach { world ->
                    val firstIndex = stages.indexOfFirst { it.world == world }
                    WorldBanner(
                        world = world,
                        starsNeeded = world.starsToUnlock
                            .takeIf { !StageCatalog.isWorldUnlocked(world, progress) },
                        comingSoon = false,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = layout.nodeCenter(firstIndex).y + BANNER_BELOW_NODE - BANNER_HALF_HEIGHT),
                    )
                }
                // Teaser for the next world, above the last stage.
                StageCatalog.upcomingWorld?.let { world ->
                    WorldBanner(
                        world = world,
                        starsNeeded = world.starsToUnlock,
                        comingSoon = true,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = layout.teaserCenterY - BANNER_HALF_HEIGHT),
                    )
                }

                stages.forEachIndexed { index, stage ->
                    val center = layout.nodeCenter(index)
                    val stars = progress[stage.id]?.stars ?: 0
                    val state = when {
                        !StageCatalog.isUnlocked(stage.id, progress) -> NodeState.LOCKED
                        stage.id == currentId && stars == 0 -> NodeState.CURRENT
                        else -> NodeState.DONE
                    }
                    StageNode(
                        stage = stage,
                        stars = stars,
                        state = state,
                        onClick = { onStageSelected(stage.id) },
                        modifier = Modifier.offset(x = center.x - NODE_SIZE / 2, y = center.y - NODE_SIZE / 2),
                    )
                }
            }
        }
    }
}

/**
 * Where everything sits on the map, in dp. Stage 1 is at the bottom; each
 * stage is [NODE_SPACING] higher, swinging left and right along a sine wave,
 * with an extra [WORLD_GAP] wherever a new world starts.
 */
private class MapLayout(private val stages: List<StageConfig>, width: Dp) {
    private val centerX = width / 2
    private val amplitude = (width / 2 - NODE_SIZE) * 0.8f

    val nodeCount: Int get() = stages.size

    /** Number of world changes before each stage. */
    private val worldBreaks: IntArray = IntArray(stages.size).also { breaks ->
        for (i in 1 until stages.size) {
            breaks[i] = breaks[i - 1] + if (stages[i].world != stages[i - 1].world) 1 else 0
        }
    }

    val height: Dp = BOTTOM_PADDING +
            NODE_SPACING * (stages.size - 1) +
            WORLD_GAP * worldBreaks.last() +
            TOP_PADDING

    fun nodeCenter(index: Int) = DpOffset(
        x = centerX + amplitude * sin(index * WAVE_STEP),
        y = height - BOTTOM_PADDING - NODE_SPACING * index - WORLD_GAP * worldBreaks[index],
    )

    val teaserCenterY: Dp get() = nodeCenter(stages.lastIndex).y - TEASER_ABOVE_LAST_NODE

    /** Vertical span (top, bottom) of each world's section, plus the upcoming world's teaser area. */
    fun worldSections(): List<Triple<World, Dp, Dp>> {
        val sections = mutableListOf<Triple<World, Dp, Dp>>()
        val worldsInOrder = stages.map { it.world }.distinct()
        val upcoming = StageCatalog.upcomingWorld
        // With no teaser, the last world's colours run to the top of the map.
        val teaserBottom = if (upcoming != null) nodeCenter(stages.lastIndex).y - NODE_SPACING / 2 else 0.dp
        worldsInOrder.forEachIndexed { i, world ->
            val first = stages.indexOfFirst { it.world == world }
            val last = stages.indexOfLast { it.world == world }
            val bottom = if (i == 0) height else midpoint(nodeCenter(first).y, nodeCenter(first - 1).y)
            val top = if (i == worldsInOrder.lastIndex) teaserBottom else midpoint(nodeCenter(last).y, nodeCenter(last + 1).y)
            sections += Triple(world, top, bottom)
        }
        upcoming?.let { sections += Triple(it, 0.dp, teaserBottom) }
        return sections
    }

    private fun midpoint(a: Dp, b: Dp) = (a + b) / 2
}

// ─────────────────────────────────────────────────────────────────────────────
// Drawing
// ─────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawWorldBackgrounds(layout: MapLayout) {
    for ((world, top, bottom) in layout.worldSections()) {
        val topPx = top.toPx()
        val bottomPx = bottom.toPx()
        drawRect(
            brush = Brush.verticalGradient(worldMapColors(world), startY = topPx, endY = bottomPx),
            topLeft = Offset(0f, topPx),
            size = Size(size.width, bottomPx - topPx),
        )
    }
}

/** Soft cloud puffs at fixed pseudo-random spots, so they don't move between visits. */
private fun DrawScope.drawClouds(layout: MapLayout) {
    val step = CLOUD_STEP.toPx()
    val count = (layout.height.toPx() / step).toInt()
    for (i in 0..count) {
        val x = size.width * ((i * 37 % 100) / 100f)
        val y = i * step + step / 3
        val r = (26 + i * 13 % 20).dp.toPx()
        drawCircle(MapCloudColor, r, Offset(x, y))
        drawCircle(MapCloudColor, r * 0.75f, Offset(x + r * 0.9f, y + r * 0.2f))
        drawCircle(MapCloudColor, r * 0.6f, Offset(x - r * 0.85f, y + r * 0.25f))
    }
}

/**
 * The road through every stage: a wide soft band with dashes on top. The
 * stretch up to the last unlocked stage is dashed in gold.
 */
private fun DrawScope.drawRoad(layout: MapLayout, unlockedCount: Int) {
    val centers = (0 until layout.nodeCount).map { layout.nodeCenter(it).let { c -> Offset(c.x.toPx(), c.y.toPx()) } }
    if (centers.size < 2) return

    fun pathThrough(points: List<Offset>) = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            val from = points[i - 1]
            val to = points[i]
            val midY = (from.y + to.y) / 2
            cubicTo(from.x, midY, to.x, midY, to.x, to.y)
        }
    }

    val dashes = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 10.dp.toPx()))
    val fullRoad = pathThrough(centers)
    drawPath(fullRoad, MapRoadColor, style = Stroke(width = 22.dp.toPx(), cap = StrokeCap.Round))
    drawPath(fullRoad, MapRoadDashColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, pathEffect = dashes))

    if (unlockedCount >= 2) {
        drawPath(
            pathThrough(centers.take(unlockedCount)),
            MapRoadDoneColor,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, pathEffect = dashes),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Nodes & banners
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StageNode(
    stage: StageConfig,
    stars: Int,
    state: NodeState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The stage to play next gently pulses.
    val pulse = if (state == NodeState.CURRENT) {
        val transition = rememberInfiniteTransition(label = "current_stage")
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 700), RepeatMode.Reverse),
            label = "pulse",
        ).value
    } else 1f
    val feedback = LocalUiFeedback.current

    // Candy bubbles: blue for the next stage, violet for cleared ones, dark glass when locked.
    val (top, bottom) = when (state) {
        NodeState.CURRENT -> GameColors.BlueTop to GameColors.BlueBottom
        NodeState.DONE -> GameColors.VioletTop to GameColors.VioletBottom
        NodeState.LOCKED -> GameColors.Ink.copy(alpha = 0.55f) to GameColors.Ink.copy(alpha = 0.75f)
    }
    val rim = when (state) {
        NodeState.CURRENT -> Color.White
        NodeState.DONE -> GameColors.Gold
        NodeState.LOCKED -> GameColors.GlassRim.copy(alpha = 0.5f)
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .scale(pulse)
                .size(NODE_SIZE)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(top, bottom)))
                .drawBehind {
                    // Gloss highlight across the top.
                    drawOval(
                        color = Color.White.copy(alpha = 0.35f),
                        topLeft = Offset(size.width * 0.2f, size.height * 0.08f),
                        size = Size(size.width * 0.6f, size.height * 0.3f),
                    )
                }
                .border(3.dp, rim, CircleShape)
                .semantics { contentDescription = "Stage ${stage.id}" + if (state == NodeState.LOCKED) ", locked" else "" }
                .clickable(enabled = state != NodeState.LOCKED) {
                    feedback.click()
                    onClick()
                },
        ) {
            when {
                state == NodeState.LOCKED -> Image(GameIcons.Lock, contentDescription = null, modifier = Modifier.size(NODE_ICON))
                stage.boss != null -> Image(GameIcons.Crown, contentDescription = null, modifier = Modifier.size(NODE_ICON))
                else -> OutlinedText(stage.id.toString(), GameTheme.type.title, outlineWidth = 2.5.dp)
            }
        }
        if (state != NodeState.LOCKED) {
            StarRow(earned = stars, total = stage.maxStars, starSize = 16.dp, modifier = Modifier.padding(top = Spacing.xs))
        }
    }
}

/**
 * "Sky World". When [starsNeeded] is set the world is still locked and shows
 * its star requirement; [comingSoon] marks a world without stages yet.
 */
@Composable
private fun WorldBanner(
    world: World,
    starsNeeded: Int?,
    comingSoon: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .height(BANNER_HALF_HEIGHT * 2)
            .clip(Corners.large)
            .background(GameColors.Ink.copy(alpha = 0.55f))
            .border(2.dp, GameColors.GlassRim, Corners.large)
            .padding(horizontal = Spacing.xl),
    ) {
        OutlinedText("${world.displayName} World", GameTheme.type.headline)
        if (starsNeeded != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Image(if (comingSoon) GameIcons.Clock else GameIcons.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                Image(GameIcons.Star, contentDescription = null, modifier = Modifier.size(14.dp))
                Text(
                    text = if (comingSoon) "Coming soon · $starsNeeded needed" else "$starsNeeded needed",
                    style = GameTheme.type.caption,
                    color = GameColors.Gold,
                )
            }
        }
    }
}

private val NODE_ICON = 34.dp
private val NODE_SIZE = 68.dp
private val NODE_SPACING = 130.dp
/** Extra room between worlds, for the banner. */
private val WORLD_GAP = 120.dp
/** Room below stage 1 for the first world's banner. */
private val BOTTOM_PADDING = 190.dp
/** Room above the last stage for the next-world teaser. */
private val TOP_PADDING = 240.dp
private val BANNER_BELOW_NODE = 100.dp
private val BANNER_HALF_HEIGHT = 28.dp
private val TEASER_ABOVE_LAST_NODE = 130.dp
private val CLOUD_STEP = 170.dp
/** Radians between consecutive stages on the left–right sine wave. */
private const val WAVE_STEP = 0.95f

@Preview
@Composable
fun StageMapScreenPreview() {
    BubbleTheme {
        StageMapScreen(
            stages = StageCatalog.stages,
            progress = mapOf(
                1 to StageProgress(1, stars = 3, bestScore = 40),
                2 to StageProgress(2, stars = 2, bestScore = 31),
                3 to StageProgress(3, stars = 1, bestScore = 25),
            ),
            onStageSelected = {},
            onBack = {},
        )
    }
}
