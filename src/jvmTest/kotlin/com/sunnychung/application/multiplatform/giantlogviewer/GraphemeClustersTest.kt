package com.sunnychung.application.multiplatform.giantlogviewer

import com.sunnychung.application.multiplatform.giantlogviewer.util.GraphemeClusters
import kotlin.test.Test
import kotlin.test.assertEquals

class GraphemeClustersTest {
    @Test
    fun iteratesEmojiSequencesAsSingleClusters() {
        val text = "a👆🏿b👨‍👩‍👧‍👦c🇭🇰d1️⃣e"
        val clusters = mutableListOf<String>()

        GraphemeClusters.forEach(text) { start, end ->
            clusters += text.substring(start, end)
        }

        assertEquals(
            listOf("a", "👆🏿", "b", "👨‍👩‍👧‍👦", "c", "🇭🇰", "d", "1️⃣", "e"),
            clusters,
        )
    }

    @Test
    fun snapsIndexesToGraphemeBoundaries() {
        val text = "a👆🏿b"

        assertEquals(1, GraphemeClusters.boundaryAtOrBefore(text, 3))
        assertEquals(5, GraphemeClusters.boundaryAtOrAfter(text, 3))
        assertEquals(5, GraphemeClusters.nextBoundary(text, 1))
        assertEquals(1, GraphemeClusters.previousBoundary(text, 5))
    }
}
