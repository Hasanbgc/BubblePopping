package com.apps.bubblepopping.view.stage

import com.apps.bubblepopping.view.drawScenery
import com.apps.bubblepopping.view.SceneArea
import com.apps.bubblepopping.ui.components.TOP_BAR_HEIGHT
import com.apps.bubblepopping.ui.components.LocalBottomDockHeight
import kotlin.random.Random
import com.apps.bubblepopping.ui.components.EnamelTone
import kotlin.math.cos
import kotlin.math.PI
import com.apps.bubblepopping.ui.components.enamelPill
import com.apps.bubblepopping.ui.components.candyFace
import com.apps.bubblepopping.ui.components.ResourcePill
import com.apps.bubblepopping.ui.components.ButtonColor
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.apps.bubblepopping.view.MapRoadColor
import com.apps.bubblepopping.view.MapRoadDashColor
import com.apps.bubblepopping.view.MapRoadDoneColor
import com.apps.bubblepopping.ui.components.GameTopBar
import com.apps.bubblepopping.ui.components.LocalUiFeedback
import com.apps.bubblepopping.ui.components.OutlinedText
import com.apps.bubblepopping.ui.icons.GameIcons
import com.apps.bubblepopping.ui.theme.BubbleTheme
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
    onPlayStage: (Int) -> Unit,
    onBack: () -> Unit,
    /** Opens with this stage's card showing (e.g. after "Next stage"). */
    initialIntroStageId: Int? = null,
) {
    // null until the first load, so the map doesn't scroll to stage 1 and then jump.
    val progress by remember(progressRepository) { progressRepository.stageProgress() }
        .collectAsStateWithLifecycle(initialValue = null)
    // The tapped stage, whose card is showing over the map.
    var introStageId by rememberSaveable { mutableStateOf(initialIntroStageId) }

    StageMapScreen(
        stages = StageCatalog.stages,
        progress = progress,
        onStageSelected = { introStageId = it },
        onBack = onBack,
    )

    introStageId?.let { stageId ->
        StageIntroDialog(
            stage = StageCatalog.stage(stageId),
            progress = progress?.get(stageId),
            onPlay = {
                // Closed first, so coming back from the round shows the map.
                introStageId = null
                onPlayStage(stageId)
            },
            onDismiss = { introStageId = null },
        )
    }
}

/** [progress] is null while loading. */
@Composable
fun StageMapScreen(
    stages: List<StageConfig>,
    progress: Map<Int, StageProgress>?,
    onStageSelected: (Int) -> Unit,
    onBack: () -> Unit,
) {
    // The map runs edge to edge; the top bar (and the app's bottom dock) float over it.
    // The map's own top and bottom margins keep every stage reachable from under them.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LOADING_BACKGROUND),
    ) {
        if (progress != null) {
            StageMap(
                stages = stages,
                progress = progress,
                onStageSelected = onStageSelected,
                modifier = Modifier.fillMaxSize(),
            )
        }
        GameTopBar(
            title = "Stages",
            onBack = onBack,
            actions = { StarCounter(showMax = true) },
            modifier = Modifier.align(Alignment.TopCenter),
        )
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
        // The map runs under the top bar and the bottom dock, so its margins grow by their height.
        val dockHeight = LocalBottomDockHeight.current
        val layout = remember(stages, maxWidth, dockHeight) {
            MapLayout(stages, maxWidth, bottomMargin = BOTTOM_PADDING + dockHeight, topMargin = TOP_PADDING + TOP_BAR_HEIGHT)
        }
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
                    drawWorldScenery(layout)
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
private class MapLayout(
    private val stages: List<StageConfig>,
    width: Dp,
    private val bottomMargin: Dp,
    topMargin: Dp,
) {
    private val centerX = width / 2
    private val amplitude = (width / 2 - NODE_SIZE) * 0.8f

    val nodeCount: Int get() = stages.size

    /** Number of world changes before each stage. */
    private val worldBreaks: IntArray = IntArray(stages.size).also { breaks ->
        for (i in 1 until stages.size) {
            breaks[i] = breaks[i - 1] + if (stages[i].world != stages[i - 1].world) 1 else 0
        }
    }

    val height: Dp = bottomMargin +
            NODE_SPACING * (stages.size - 1) +
            WORLD_GAP * worldBreaks.last() +
            topMargin

    fun nodeCenter(index: Int) = DpOffset(
        x = centerX + amplitude * sin(index * WAVE_STEP),
        y = height - bottomMargin - NODE_SPACING * index - WORLD_GAP * worldBreaks[index],
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

/**
 * Each world's section in its colours, with a soft blend where one world meets
 * the next instead of a hard line.
 */
private fun DrawScope.drawWorldBackgrounds(layout: MapLayout) {
    val sections = layout.worldSections()
    for ((world, top, bottom) in sections) {
        val topPx = top.toPx()
        val bottomPx = bottom.toPx()
        drawRect(
            brush = Brush.verticalGradient(worldMapColors(world), startY = topPx, endY = bottomPx),
            topLeft = Offset(0f, topPx),
            size = Size(size.width, bottomPx - topPx),
        )
    }
    // Blend each border: the upper world's bottom colour fades into the lower world's top colour.
    val blend = WORLD_BLEND.toPx()
    sections.zipWithNext().forEach { (lower, upper) ->
        val edge = lower.second.toPx()
        drawRect(
            brush = Brush.verticalGradient(
                listOf(worldMapColors(upper.first).last(), worldMapColors(lower.first).first()),
                startY = edge - blend,
                endY = edge + blend,
            ),
            topLeft = Offset(0f, edge - blend),
            size = Size(size.width, blend * 2f),
        )
    }
}

/**
 * Each world's scenery, at fixed seeded spots so it never moves between visits:
 * Sky — sun glow, fluffy clouds, bubble rings; Ocean — light rays, wave lines,
 * rising bubbles; Space — nebulae, stars, a ringed planet; Candy — polka dots,
 * sprinkles, lollipop swirls.
 */
private fun DrawScope.drawWorldScenery(layout: MapLayout) {
    for ((world, top, bottom) in layout.worldSections()) {
        val area = SceneArea(top.toPx(), bottom.toPx(), size.width)
        if (area.height <= 0f) continue
        val random = Random(SCENERY_SEED + world.ordinal)
        drawScenery(world, area, random)
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
    // A darker edge under the road, so it reads as a raised path.
    drawPath(fullRoad, GameColors.ButtonOutline.copy(alpha = ROAD_EDGE_ALPHA), style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Round))
    drawPath(fullRoad, MapRoadColor, style = Stroke(width = 22.dp.toPx(), cap = StrokeCap.Round))
    drawPath(fullRoad, MapRoadDashColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, pathEffect = dashes))

    if (unlockedCount >= 2) {
        val done = pathThrough(centers.take(unlockedCount))
        // Soft gold glow under the gold dashes.
        drawPath(done, GameColors.Gold.copy(alpha = ROAD_GLOW_ALPHA), style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round))
        drawPath(done, MapRoadDoneColor, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, pathEffect = dashes))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Nodes & banners
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A stage on the map: a candy bubble (blue for the next stage, violet once
 * cleared, slate while locked) with a dark outline and a soft shadow. Cleared
 * stages get a gold ring, the next stage pulses with a glow, and a boss stage is
 * bigger with a slowly turning gold starburst inside, behind its crown. Its stars sit on a small
 * pill underneath.
 */
@Composable
private fun StageNode(
    stage: StageConfig,
    stars: Int,
    state: NodeState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isBoss = stage.boss != null
    val transition = rememberInfiniteTransition(label = "stage_node")
    // The stage to play next gently pulses.
    val pulse = if (state == NodeState.CURRENT) {
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 700), RepeatMode.Reverse),
            label = "pulse",
        ).value
    } else 1f
    // Only bosses turn a starburst; read only while drawing, so it never recomposes the node.
    val spin = if (isBoss) {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(BURST_TURN_MS, easing = LinearEasing), RepeatMode.Restart),
            label = "burst_spin",
        )
    } else null
    val feedback = LocalUiFeedback.current
    val color = when (state) {
        NodeState.CURRENT -> ButtonColor.BLUE
        NodeState.DONE -> ButtonColor.VIOLET
        NodeState.LOCKED -> ButtonColor.SLATE
    }
    val bubbleSize = if (isBoss) BOSS_NODE_SIZE else NODE_SIZE

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(NODE_SIZE)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .requiredSize(bubbleSize)
                    .scale(pulse)
                    .drawBehind {
                        val r = size.minDimension / 2f
                        if (state == NodeState.CURRENT) {
                            drawCircle(Brush.radialGradient(listOf(GameColors.BlueTop.copy(alpha = 0.6f), Color.Transparent), center, r * 1.6f), r * 1.6f)
                        }
                        // Soft shadow on the ground, then the dark outline ring.
                        drawOval(
                            GameColors.ButtonShadow,
                            topLeft = Offset(size.width * 0.12f, size.height * 0.78f),
                            size = Size(size.width * 0.76f, size.height * 0.32f),
                        )
                        val outline = NODE_OUTLINE.toPx()
                        drawCircle(GameColors.ButtonOutline, radius = r + outline / 2f, style = Stroke(outline))
                    }
                    .candyFace(color, CircleShape)
                    .then(
                        if (state == NodeState.DONE) {
                            Modifier.border(NODE_GOLD_RING, Brush.verticalGradient(listOf(GameColors.GoldTop, GameColors.GoldLip)), CircleShape)
                        } else Modifier
                    )
                    .semantics { contentDescription = "Stage ${stage.id}" + (if (isBoss) ", boss" else "") + if (state == NodeState.LOCKED) ", locked" else "" }
                    .clickable(enabled = state != NodeState.LOCKED) {
                        feedback.click()
                        onClick()
                    },
            ) {
                // Boss: a gold starburst turning inside the bubble, behind the crown (or lock).
                if (spin != null) {
                    Canvas(Modifier.matchParentSize()) {
                        val r = size.minDimension / 2f
                        rotate(spin.value) { drawStarburst(r * BURST_REACH, r * BURST_INNER, dimmed = state == NodeState.LOCKED) }
                    }
                }
                when {
                    state == NodeState.LOCKED -> Image(GameIcons.Lock, contentDescription = null, modifier = Modifier.size(NODE_ICON))
                    isBoss -> Image(GameIcons.Crown, contentDescription = null, modifier = Modifier.size(BOSS_ICON))
                    else -> OutlinedText(stage.id.toString(), GameTheme.type.title, outlineColor = color.lip, outlineWidth = 2.5.dp)
                }
            }
        }
        if (state != NodeState.LOCKED) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = if (isBoss) Spacing.m else Spacing.s)
                    .enamelPill(GameColors.PillStar)
                    .padding(horizontal = Spacing.s, vertical = 2.dp),
            ) {
                StarRow(earned = stars, total = stage.maxStars, starSize = STAR_SIZE)
            }
        }
    }
}

/**
 * A faint starburst inside a boss stage, behind its crown: [BURST_POINTS] points
 * between [inner] and [outer], a light gold wash fading out towards the tips with
 * no outline — barely there, just enough to mark the stage as special. Fainter
 * still while the boss is [dimmed] (locked).
 */
private fun DrawScope.drawStarburst(outer: Float, inner: Float, dimmed: Boolean) {
    val path = Path()
    for (i in 0 until BURST_POINTS * 2) {
        val r = if (i % 2 == 0) outer else inner
        val angle = (i * 180f / BURST_POINTS - 90f) * (PI.toFloat() / 180f)
        val x = center.x + r * cos(angle)
        val y = center.y + r * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    val strength = if (dimmed) BURST_ALPHA_LOCKED else BURST_ALPHA
    drawPath(
        path,
        Brush.radialGradient(
            listOf(GameColors.CardGlint.copy(alpha = strength), GameColors.GoldTop.copy(alpha = strength * 0.4f)),
            center = center,
            radius = outer,
        ),
    )
}

/**
 * "Sky World" on a compact light-cyan enamel name plate. When
 * [starsNeeded] is set the world is still locked, and a pill under the plate shows
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
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(PLATE_HEIGHT)
                .enamelPill(GameColors.OceanTint, EnamelTone.OCEAN)
                .padding(horizontal = Spacing.l),
        ) {
            OutlinedText("${world.displayName} World", GameTheme.type.headline, maxLines = 1)
        }
        if (starsNeeded != null) {
            ResourcePill(
                icon = if (comingSoon) GameIcons.Clock else GameIcons.Lock,
                value = if (comingSoon) "Coming soon · $starsNeeded ★" else "$starsNeeded ★ needed",
                tint = GameColors.PillStar,
            )
        }
    }
}

private val NODE_ICON = 34.dp
private val NODE_SIZE = 68.dp
private val NODE_OUTLINE = 2.5.dp
private val NODE_GOLD_RING = 3.dp
/** Boss stages are bigger, with a bigger crown. */
private val BOSS_NODE_SIZE = 84.dp
private val BOSS_ICON = 44.dp
private val STAR_SIZE = 14.dp
private const val BURST_POINTS = 14
/** Starburst point tips and notches, as fractions of the boss bubble's radius (inside it). */
private const val BURST_REACH = 0.92f
private const val BURST_INNER = 0.66f
private const val BURST_TURN_MS = 16_000
/** How visible the starburst is: faint on purpose. */
private const val BURST_ALPHA = 0.22f
private const val BURST_ALPHA_LOCKED = 0.1f
private val NODE_SPACING = 130.dp
/** Extra room between worlds, for the banner. */
private val WORLD_GAP = 120.dp
/** Room below stage 1 for the first world's banner. */
private val BOTTOM_PADDING = 190.dp
/** Room above the last stage for the next-world teaser. */
private val TOP_PADDING = 240.dp
private val BANNER_BELOW_NODE = 100.dp
private val BANNER_HALF_HEIGHT = 20.dp
private val PLATE_HEIGHT = 40.dp
private val TEASER_ABOVE_LAST_NODE = 130.dp
/** Height of the soft blend where one world meets the next. */
private val WORLD_BLEND = 90.dp
private const val SCENERY_SEED = 7
/** Shown for the moment before progress loads; close to the map's own colours. */
private val LOADING_BACKGROUND = Color(0xFF0D47A1)
/** Radians between consecutive stages on the left–right sine wave. */
private const val WAVE_STEP = 0.95f
private const val ROAD_EDGE_ALPHA = 0.25f
private const val ROAD_GLOW_ALPHA = 0.3f

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
