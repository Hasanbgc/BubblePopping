package com.apps.bubblepopping

import com.apps.bubblepopping.ui.components.formatCount
import kotlin.test.Test
import kotlin.test.assertEquals

/** Small rules inside the Bubble Glass UI components. */
class UiTest {

    @Test
    fun counters_groupThousands() {
        assertEquals("0", formatCount(0))
        assertEquals("999", formatCount(999))
        assertEquals("1,240", formatCount(1_240))
        assertEquals("1,000,000", formatCount(1_000_000))
    }
}
