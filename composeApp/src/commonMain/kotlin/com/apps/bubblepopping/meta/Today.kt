package com.apps.bubblepopping.meta

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Today's date in the device's time zone, as days since 1970-01-01. Daily content resets at local midnight. */
@OptIn(ExperimentalTime::class)
fun currentEpochDay(): Long =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toEpochDays()
