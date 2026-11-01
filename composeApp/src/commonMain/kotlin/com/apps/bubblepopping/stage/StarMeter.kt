package com.apps.bubblepopping.stage

/*
 * The HUD's stars: one per objective (main goal first, then the bonuses), side by
 * side. A stage's objectives run at the same time and are met in any order, so
 * each star shows its own objective rather than sharing one bar.
 */

/** How one objective's star looks right now. */
data class StarMeterState(
    /** 0 → 1: how far the objective has got. A constraint is full while it holds, empty once broken. */
    val fill: Float,
    /** Earned if the stage ended now: met, and — for a bonus — the main goal met too. */
    val lit: Boolean,
    /** A constraint that no longer holds (a life too many lost, accuracy too low…). */
    val broken: Boolean,
)

/** One [StarMeterState] per objective, in order; [objectives] are main goal first, then the bonuses. */
fun starMeterStates(objectives: List<ObjectiveProgress>): List<StarMeterState> {
    val mainMet = objectives.firstOrNull()?.satisfied == true
    return objectives.mapIndexed { i, p ->
        StarMeterState(
            fill = p.fraction,
            lit = p.satisfied && (i == 0 || mainMet),
            broken = p.objective.isConstraint && !p.satisfied,
        )
    }
}

/**
 * The objective's live number under its star — short, as the HUD is narrow:
 * "75/120s", "1/2" lives lost (it can pass the limit: "3/2"), "92%", "240/3.1k".
 * [short] (a narrow phone) drops a goal's target — "75s", "240" — since the star's
 * fill already shows how far along it is.
 */
fun ObjectiveProgress.hudReadout(short: Boolean = false): String {
    val reached = if (objective.isConstraint) current else minOf(current, target)
    return when (objective) {
        is Objective.Accuracy -> "$current%"
        is Objective.DefeatBoss -> "${(fraction * 100).toInt()}%"
        is Objective.Survive, is Objective.FinishUnder -> if (short) "${reached}s" else "$reached/${target}s"
        else -> if (short && !objective.isConstraint) compactCount(reached) else "${compactCount(reached)}/${compactCount(target)}"
    }
}

/** 240 → "240", 3100 → "3.1k", 3000 → "3k", 12500 → "12k". */
fun compactCount(n: Int): String {
    if (n < 1000) return "$n"
    if (n >= 10_000) return "${n / 1000}k"
    val tenths = n / 100
    return if (tenths % 10 == 0) "${tenths / 10}k" else "${tenths / 10}.${tenths % 10}k"
}
