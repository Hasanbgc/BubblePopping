package com.apps.bubblepopping.view.stage

import com.apps.bubblepopping.meta.Booster
import com.apps.bubblepopping.view.Decoration
import com.apps.bubblepopping.view.sceneryDecorations
import com.apps.bubblepopping.view.SceneArea
import com.apps.bubblepopping.ui.components.TOP_BAR_HEIGHT
import com.apps.bubblepopping.ui.components.ProvideAmbientClock
import com.apps.bubblepopping.ui.components.ambientTime
import com.apps.bubblepopping.ui.components.ambientLoop
import com.apps.bubblepopping.ui.components.ambientPingPong
import com.apps.bubblepopping.ui.components.cachedLayer
import com.apps.bubblepopping.ui.components.LocalBottomDockHeight
import kotlin.random.Random
import com.apps.bubblepopping.ui.components.EnamelTone
import kotlin.math.cos
import kotlin.math.PI
import com.apps.bubblepopping.ui.components.enamelPill
import com.apps.bubblepopping.ui.components.candyFace
import com.apps.bubblepopping.ui.components.formatCount
import com.apps.bubblepopping.ui.components.ButtonColor
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.layout
import com.apps.bubblepopping.data.StageProgress
import com.apps.bubblepopping.stage.StageCatalog
import com.apps.bubblepopping.stage.StageConfig
import com.apps.bubblepopping.stage.World
import com.apps.bubblepopping.view.MapRoadColor
import com.apps.bubblepopping.view.MapRoadDashColor
import com.apps.bubblepopping.view.MapRoadDoneColor
import com.apps.bubblepopping.view.MapLoadingColor
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.layout.width
import com.apps.bubblepopping.ui.components.GameProgressBar
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sin

/**
 * The stage map: a winding road climbing from stage 1 at the bottom, one
 * themed section per world, and a teaser for the next world at the top.
 */
@Composable
fun StageMapScreenRoot(
    /**
     * The player's stage results, collected app-wide so they're ready the moment the
     * map opens; null only before the database first answers (then the map waits,
     * rather than opening on stage 1 and jumping).
     */
    progress: Map<Int, StageProgress>?,
    /** Plays the stage with the boosters picked on its card. */
    onPlayStage: (stageId: Int, boosters: Set<Booster>) -> Unit,
    onBack: () -> Unit,
    /** Opens with this stage's card showing (e.g. after "Next stage"). */
    initialIntroStageId: Int? = null,
) {
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
            onPlay = { boosters ->
                // Closed first, so coming back from the round shows the map.
                introStageId = null
                onPlayStage(stageId, boosters)
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
) = ProvideAmbientClock {
    // The map runs edge to edge; the top bar (and the app's bottom dock) float over it.
    // The map's own top and bottom margins keep every stage reachable from under them.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LOADING_BACKGROUND),
    ) {
        StageMap(
            stages = stages,
            progress = progress,
            onStageSelected = onStageSelected,
            modifier = Modifier.fillMaxSize(),
        )
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

/** What a stage's node shows; a data class, so an unchanged stage skips recomposition. */
private data class MapNode(val stars: Int, val state: NodeState)

/**
 * The map, shown once both the player's progress and every world's scenery are
 * ready, so it never appears half drawn. The scenery is worked out while the
 * progress loads; after the first visit it is cached and the map shows at once.
 * Until then [MapLoading] covers the wait (only if it lasts long enough to notice),
 * and the map fades in.
 */
@Composable
private fun StageMap(
    stages: List<StageConfig>,
    progress: Map<Int, StageProgress>?,
    onStageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // The map runs under the top bar and the bottom dock, so its margins grow by their height.
        // The dock hides (height 0) the moment a round is pushed, while this screen is still
        // fading out: keep the last real height, so the map doesn't re-lay out as it leaves.
        val liveDockHeight = LocalBottomDockHeight.current
        var keptDockHeight by remember { mutableStateOf(liveDockHeight) }
        SideEffect { if (liveDockHeight > 0.dp) keptDockHeight = liveDockHeight }
        val dockHeight = if (liveDockHeight > 0.dp) liveDockHeight else keptDockHeight
        val layout = remember(stages, maxWidth, dockHeight) {
            MapLayout(stages, maxWidth, bottomMargin = BOTTOM_PADDING + dockHeight, topMargin = TOP_PADDING + TOP_BAR_HEIGHT)
        }
        val scenery = rememberMapScenery(layout)
        // Once shown, the map stays: a later layout change (a new screen size) fills its
        // scenery in place rather than going back to the loader.
        var shownOnce by remember { mutableStateOf(false) }
        val ready = progress != null && (shownOnce || layout.sections.all { it.world in scenery })
        // Already ready on the first frame (a later visit): no loader, no fade, the map is just there.
        val readyAtStart = remember { ready }
        // On a first visit the bar fills over at least MIN_LOADER_MS, so it reads as a loader
        // rather than a flicker; if loading is slower, the bar waits for it.
        val clock = remember { Animatable(if (readyAtStart) 1f else 0f) }
        LaunchedEffect(Unit) { clock.animateTo(1f, tween(MIN_LOADER_MS, easing = LinearEasing)) }
        // derivedStateOf: the clock ticks every frame, but this only flips once.
        val showMap by remember { derivedStateOf { clock.value >= 1f } }

        if (ready && showMap) {
            val fade = remember { Animatable(if (readyAtStart) 1f else 0f) }
            LaunchedEffect(Unit) {
                shownOnce = true
                fade.animateTo(1f, tween(MAP_FADE_MS))
            }
            StageList(
                stages = stages,
                progress = progress,
                layout = layout,
                scenery = scenery,
                viewportHeight = maxHeight,
                onStageSelected = onStageSelected,
                modifier = Modifier.graphicsLayer { alpha = fade.value },
            )
        }
        // One step for the progress, one per world's scenery.
        val stepsDone = (if (progress != null) 1 else 0) + layout.sections.count { it.world in scenery }
        val loaded = stepsDone / (layout.sections.size + 1f)
        MapLoading(
            loading = !(ready && showMap),
            // The bar never runs ahead of the real loading, nor of the minimum time.
            fraction = { minOf(clock.value, loaded) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * The map as a lazy list of slices, one per stage, bottom to top: only the stages
 * on screen and [CACHE_WINDOW] around them (about five each way) are built; the
 * rest are built in idle frames as they come near and released when far away.
 * It opens with the current stage in the middle of the screen, with no jump.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StageList(
    stages: List<StageConfig>,
    progress: Map<Int, StageProgress>,
    layout: MapLayout,
    scenery: Map<World, List<Decoration>>,
    viewportHeight: Dp,
    onStageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Each stage's state, worked out once per progress change rather than in every slice.
    val nodes = remember(stages, progress) {
        val currentId = StageCatalog.currentStageId(progress)
        stages.map { stage ->
            val stars = progress[stage.id]?.stars ?: 0
            val state = when {
                !StageCatalog.isUnlocked(stage.id, progress) -> NodeState.LOCKED
                stage.id == currentId && stars == 0 -> NodeState.CURRENT
                else -> NodeState.DONE
            }
            MapNode(stars, state)
        }
    }
    val unlockedCount = nodes.count { it.state != NodeState.LOCKED }
    val worldStarsNeeded = remember(progress) {
        StageCatalog.worlds.associateWith { world -> world.starsToUnlock.takeIf { !StageCatalog.isWorldUnlocked(world, progress) } }
    }
    val density = LocalDensity.current

    // Not saved: every visit opens on the current stage, as the stage you just cleared moves it on.
    val listState = remember(layout) {
        val currentIndex = stages.indexOfFirst { it.id == StageCatalog.currentStageId(progress) }.coerceAtLeast(0)
        val (index, offset) = layout.startPosition(currentIndex, viewportHeight)
        LazyListState(CACHE_WINDOW, index, with(density) { offset.roundToPx() })
    }

    // Stage 1 is the first item, at the bottom.
    LazyColumn(state = listState, reverseLayout = true, modifier = modifier.fillMaxSize()) {
        items(count = stages.size, key = { stages[it].id }, contentType = { "stage" }) { index ->
            val stage = stages[index]
            MapSlice(
                index = index,
                stage = stage,
                node = nodes[index],
                // The gold road only matters up to this slice's top, so slices well
                // below the last unlocked stage keep the same value and skip.
                roadDone = minOf(unlockedCount, index + 2),
                worldStarsNeeded = worldStarsNeeded[stage.world],
                layout = layout,
                scenery = scenery,
                onStageSelected = onStageSelected,
            )
        }
    }
}

/**
 * The map's first-visit loader: a solid blue screen (the Sky world's deep blue)
 * with "Loading map…" over a gold progress bar filled to [fraction] (read here, so
 * its per-frame changes only recompose the loader). It fades out as the map fades in.
 */
@Composable
private fun MapLoading(loading: Boolean, fraction: () -> Float, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = loading,
        enter = fadeIn(),
        exit = fadeOut(tween(MAP_FADE_MS)),
        modifier = modifier,
    ) {
        Box(Modifier.fillMaxSize().background(MapLoadingColor)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
                modifier = Modifier.align(Alignment.Center),
            ) {
                OutlinedText("Loading map…", GameTheme.type.headline)
                GameProgressBar(fraction = fraction(), modifier = Modifier.width(LOADER_BAR_WIDTH))
            }
        }
    }
}

/**
 * One stage's band of the map: its piece of the world colours, scenery and road,
 * drawn once into its own layer (scrolling just moves the layer), with the stage
 * node on top, plus the world banner under a world's first stage and the
 * next-world teaser above the last. Higher slices draw over lower ones, so a node's
 * star pill can hang into the slice below.
 */
@Composable
private fun MapSlice(
    index: Int,
    stage: StageConfig,
    node: MapNode,
    roadDone: Int,
    worldStarsNeeded: Int?,
    layout: MapLayout,
    scenery: Map<World, List<Decoration>>,
    onStageSelected: (Int) -> Unit,
) {
    val top = layout.sliceTop(index)
    val bottom = layout.sliceBottom(index)
    // Borders in whole pixels, rounded once: a slice's bottom is exactly the next one's
    // top, so the list places every slice exactly where its drawing expects to be. With
    // fractional dp heights the list's rounding drifted from the drawing, leaving a
    // thin line at every slice border.
    val density = LocalDensity.current
    val topPx = with(density) { top.roundToPx() }
    val bottomPx = with(density) { bottom.roundToPx() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val height = bottomPx - topPx
                val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
                layout(placeable.width, height) { placeable.place(0, 0) }
            }
            .zIndex(index.toFloat())
            .graphicsLayer(),
    ) {
        // The slice's ground: only changes when its scenery arrives or the road
        // progresses, so it is kept ready-drawn, in a layer of its own that the
        // animating node above doesn't touch.
        Spacer(
            Modifier
                .matchParentSize()
                .cachedLayer()
                .drawWithCache {
                    // The road always starts at stage 1, so the dashes line up across slices.
                    val roadEnd = minOf(index + 1, layout.nodeCount - 1)
                    val centers = (0..roadEnd).map { layout.nodeCenter(it).let { c -> Offset(c.x.toPx(), c.y.toPx()) } }
                    val road = roadPath(centers)
                    val doneRoad = if (roadDone >= 2) roadPath(centers.take(roadDone)) else null
                    val dashes = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 10.dp.toPx()))
                    val background = mapBackground(layout)
                    // Reading the scenery here rebuilds this cache when a world's scenery arrives.
                    val pieces = layout.sections.flatMap { scenery[it.world].orEmpty() }
                        .filter { it.bottom > topPx && it.top < bottomPx }
                    onDrawBehind {
                        clipRect {
                            translate(top = -topPx.toFloat()) {
                                // The map-wide gradient, drawn over this slice's band only.
                                drawRect(background, topLeft = Offset(0f, topPx.toFloat()), size = Size(size.width, (bottomPx - topPx).toFloat()))
                                pieces.forEach { it.draw(this) }
                                drawRoad(road, doneRoad, dashes)
                            }
                        }
                    }
                },
        )
        if (layout.isFirstOfWorld(index)) {
            WorldBanner(
                world = stage.world,
                starsNeeded = worldStarsNeeded,
                comingSoon = false,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = layout.nodeCenter(index).y - top + BANNER_BELOW_NODE - BANNER_HALF_HEIGHT),
            )
        }
        if (index == layout.nodeCount - 1) {
            StageCatalog.upcomingWorld?.let { world ->
                WorldBanner(
                    world = world,
                    starsNeeded = world.starsToUnlock,
                    comingSoon = true,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = layout.teaserCenterY - top - BANNER_HALF_HEIGHT),
                )
            }
        }
        val center = layout.nodeCenter(index)
        StageNode(
            stage = stage,
            stars = node.stars,
            state = node.state,
            onClick = { onStageSelected(stage.id) },
            modifier = Modifier.offset(x = center.x - NODE_SIZE / 2, y = center.y - top - NODE_SIZE / 2),
        )
    }
}

/** A world's map section, in dp from the top of the map. */
private class MapSection(val world: World, val top: Dp, val bottom: Dp)

/**
 * Where everything sits on the map, in dp from its top. Stage 1 is at the bottom;
 * each stage is [NODE_SPACING] higher, swinging left and right along a sine wave,
 * with an extra [WORLD_GAP] wherever a new world starts. The map is cut into one
 * slice per stage for the lazy list.
 */
private class MapLayout(
    private val stages: List<StageConfig>,
    val width: Dp,
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

    fun isFirstOfWorld(index: Int) = index == 0 || stages[index].world != stages[index - 1].world

    val teaserCenterY: Dp get() = nodeCenter(stages.lastIndex).y - TEASER_ABOVE_LAST_NODE

    /**
     * Top of stage [index]'s slice: [SLICE_ABOVE_NODE] above its node, so the node's
     * glow fits; the last slice runs to the top of the map (the teaser).
     */
    fun sliceTop(index: Int): Dp = if (index == stages.lastIndex) 0.dp else nodeCenter(index).y - SLICE_ABOVE_NODE

    /** Bottom of stage [index]'s slice: the top of the one below; the first runs to the bottom. */
    fun sliceBottom(index: Int): Dp = if (index == 0) height else sliceTop(index - 1)

    /**
     * The list position — the bottom item and how much of it is scrolled off the
     * bottom — that puts stage [index] in the middle of a [viewportHeight] screen.
     */
    fun startPosition(index: Int, viewportHeight: Dp): Pair<Int, Dp> {
        val viewportBottom = nodeCenter(index).y + viewportHeight / 2
        if (viewportBottom >= height) return 0 to 0.dp
        val bottomItem = stages.indices.first { sliceTop(it) < viewportBottom }
        return bottomItem to (sliceBottom(bottomItem) - viewportBottom)
    }

    /** Vertical span of each world's section, plus the upcoming world's teaser area. */
    val sections: List<MapSection> = buildList {
        val worldsInOrder = stages.map { it.world }.distinct()
        val upcoming = StageCatalog.upcomingWorld
        // With no teaser, the last world's colours run to the top of the map.
        val teaserBottom = if (upcoming != null) nodeCenter(stages.lastIndex).y - NODE_SPACING / 2 else 0.dp
        worldsInOrder.forEachIndexed { i, world ->
            val first = stages.indexOfFirst { it.world == world }
            val last = stages.indexOfLast { it.world == world }
            val bottom = if (i == 0) height else midpoint(nodeCenter(first).y, nodeCenter(first - 1).y)
            val top = if (i == worldsInOrder.lastIndex) teaserBottom else midpoint(nodeCenter(last).y, nodeCenter(last + 1).y)
            add(MapSection(world, top, bottom))
        }
        upcoming?.let { add(MapSection(it, 0.dp, teaserBottom)) }
    }

    private fun midpoint(a: Dp, b: Dp) = (a + b) / 2
}

// ─────────────────────────────────────────────────────────────────────────────
// Scenery
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Each world's scenery pieces, worked out on a background thread and kept for the
 * session, so the next visit has them on its first frame.
 */
@Composable
private fun rememberMapScenery(layout: MapLayout): Map<World, List<Decoration>> {
    val density = LocalDensity.current
    val keys = remember(layout, density) {
        val widthPx = with(density) { layout.width.toPx() }
        layout.sections.map { section ->
            with(density) { SceneryKey(section.world, section.top.toPx(), section.bottom.toPx(), widthPx, density.density) }
        }
    }
    val scenery = remember(keys) {
        mutableStateMapOf<World, List<Decoration>>().apply {
            keys.forEach { key -> MapSceneryCache[key]?.let { put(key.world, it) } }
        }
    }
    LaunchedEffect(keys) {
        for (key in keys) {
            if (key.world in scenery) continue
            val pieces = withContext(Dispatchers.Default) { key.build(density) }
            MapSceneryCache[key] = pieces
            scenery[key.world] = pieces
        }
    }
    return scenery
}

/** A world section's scenery inputs, in px. */
private data class SceneryKey(val world: World, val top: Float, val bottom: Float, val width: Float, val density: Float) {
    fun build(density: Density): List<Decoration> {
        val area = SceneArea(top, bottom, width)
        if (area.height <= 0f) return emptyList()
        return density.sceneryDecorations(world, area, Random(SCENERY_SEED + world.ordinal))
    }
}

/** Scenery kept across visits to the map. Only touched on the main thread. */
private object MapSceneryCache {
    private val entries = LinkedHashMap<SceneryKey, List<Decoration>>()

    operator fun get(key: SceneryKey): List<Decoration>? = entries[key]

    operator fun set(key: SceneryKey, pieces: List<Decoration>) {
        // A new screen size or dock height means new keys; drop the oldest instead of growing.
        if (entries.size >= MAX_CACHED_SECTIONS) entries.remove(entries.keys.first())
        entries[key] = pieces
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Drawing
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The whole map's colours as one continuous top-to-bottom gradient: each world's
 * colours spread over its section, and across each border the colour runs
 * straight from the upper world's bottom colour to the lower world's top colour
 * over [WORLD_BLEND] each side. One gradient has no seams, so there's no visible
 * edge (the "bump" separately drawn blend strips left) where worlds meet.
 */
private fun Density.mapBackground(layout: MapLayout): Brush {
    val height = layout.height.toPx()
    val blend = WORLD_BLEND.toPx()
    val sections = layout.sections.sortedBy { it.top }
    val stops = sections.flatMapIndexed { i, section ->
        val colors = worldMapColors(section.world)
        // Inset from the borders it shares with a neighbour, leaving room for the blend.
        val start = section.top.toPx() + if (i > 0) blend else 0f
        val end = section.bottom.toPx() - if (i < sections.lastIndex) blend else 0f
        colors.mapIndexed { k, color ->
            val y = start + (end - start) * k / (colors.size - 1)
            (y / height).coerceIn(0f, 1f) to color
        }
    }
    return Brush.verticalGradient(*stops.toTypedArray(), startY = 0f, endY = height)
}

/** A smooth road through [points], bending between each pair. */
private fun roadPath(points: List<Offset>) = Path().apply {
    moveTo(points.first().x, points.first().y)
    for (i in 1 until points.size) {
        val from = points[i - 1]
        val to = points[i]
        val midY = (from.y + to.y) / 2
        cubicTo(from.x, midY, to.x, midY, to.x, to.y)
    }
}

/**
 * The road: a wide soft band with dashes on top. The stretch up to the last
 * unlocked stage ([done]) is dashed in gold.
 */
private fun DrawScope.drawRoad(road: Path, done: Path?, dashes: PathEffect) {
    // A darker edge under the road, so it reads as a raised path.
    drawPath(road, GameColors.ButtonOutline.copy(alpha = ROAD_EDGE_ALPHA), style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Round))
    drawPath(road, MapRoadColor, style = Stroke(width = 22.dp.toPx(), cap = StrokeCap.Round))
    drawPath(road, MapRoadDashColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, pathEffect = dashes))
    if (done != null) {
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
 *
 * Only the current stage and bosses animate, and both animations are read in a
 * graphics layer, so they move the layer without recomposing or redrawing anything.
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
    // The stage to play next gently pulses, and bosses turn a starburst, on the
    // map's ambient clock (read only in layers, so nothing recomposes or redraws).
    val clock = ambientTime()
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
                    // Always its own layer, so the pulse and the starburst never redraw the slice.
                    .graphicsLayer {
                        if (state == NodeState.CURRENT) {
                            val pulse = 1f + PULSE_GROW * ambientPingPong(clock.value, PULSE_MS)
                            scaleX = pulse
                            scaleY = pulse
                        }
                    }
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
                // Drawn once; the layer rotates it.
                if (isBoss) {
                    Canvas(Modifier.matchParentSize().graphicsLayer { rotationZ = ambientLoop(clock.value, BURST_TURN_MS) * 360f }) {
                        val r = size.minDimension / 2f
                        drawStarburst(r * BURST_REACH, r * BURST_INNER, dimmed = state == NodeState.LOCKED)
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
 * "Sky World" on a compact light-cyan enamel name plate. When [starsNeeded] is
 * set the world is still locked: a lock sits in front of the name and the stars
 * it takes (the score pill's star and the number) at the end, all on the one
 * plate. [comingSoon] marks a world without stages yet, with a clock instead of the lock.
 */
@Composable
private fun WorldBanner(
    world: World,
    starsNeeded: Int?,
    comingSoon: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        modifier = modifier
            .height(PLATE_HEIGHT)
            .enamelPill(GameColors.OceanTint, EnamelTone.OCEAN)
            .padding(horizontal = Spacing.l),
    ) {
        if (starsNeeded != null) {
            Image(
                if (comingSoon) GameIcons.Clock else GameIcons.Lock,
                contentDescription = if (comingSoon) "Coming soon" else "Locked",
                modifier = Modifier.size(PLATE_ICON),
            )
        }
        OutlinedText("${world.displayName} World", GameTheme.type.headline, maxLines = 1)
        if (starsNeeded != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                modifier = Modifier.semantics { contentDescription = "$starsNeeded stars to unlock" },
            ) {
                Image(GameIcons.Star, contentDescription = null, modifier = Modifier.size(PLATE_ICON))
                OutlinedText(formatCount(starsNeeded), GameTheme.type.headline, maxLines = 1)
            }
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
/** The current stage's pulse: how much it grows, and how long each way takes. */
private const val PULSE_GROW = 0.1f
private const val PULSE_MS = 700
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
private val PLATE_ICON = 24.dp
private val TEASER_ABOVE_LAST_NODE = 130.dp
/**
 * How far a stage's slice reaches above its node: room for the current boss's
 * pulsing glow (84 dp × 1.6 / 2 × 1.1 ≈ 74 dp), so nothing pokes into the slice above.
 */
private val SLICE_ABOVE_NODE = 76.dp
/** Stages kept built beyond the screen, each way (about five). */
@OptIn(ExperimentalFoundationApi::class)
private val CACHE_WINDOW = LazyLayoutCacheWindow(ahead = NODE_SPACING * 5, behind = NODE_SPACING * 5)
/** World sections whose scenery is kept between visits (a few screen sizes' worth). */
private const val MAX_CACHED_SECTIONS = 16
/** The map fades in this fast after a first-visit load (and the loader fades out). */
private const val MAP_FADE_MS = 250
/** A first visit's loader fills over at least this long. */
private const val MIN_LOADER_MS = 700
private val LOADER_BAR_WIDTH = 220.dp
/** Height of the soft blend where one world meets the next. */
private val WORLD_BLEND = 90.dp
private const val SCENERY_SEED = 7
/** Under everything: dark, so no frame outside the loader shows the loader's blue. */
private val LOADING_BACKGROUND = GameColors.NightTop/** Radians between consecutive stages on the left–right sine wave. */
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
