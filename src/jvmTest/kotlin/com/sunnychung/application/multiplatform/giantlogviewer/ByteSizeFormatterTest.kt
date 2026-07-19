package com.sunnychung.application.multiplatform.giantlogviewer

import com.sunnychung.application.multiplatform.giantlogviewer.util.formatByteSize
import kotlin.test.Test
import kotlin.test.assertEquals

class ByteSizeFormatterTest {
    @Test
    fun `keeps rounded zero decimals when size is not divisible by displayed unit`() {
        assertEquals("76.00 MB", formatByteSize(76L * 1024L * 1024L + 1L))
    }

    @Test
    fun `omits decimals when size is divisible by displayed unit`() {
        assertEquals("76 MB", formatByteSize(76L * 1024L * 1024L))
    }

    @Test
    fun `uses at most two decimal places for non-divisible sizes`() {
        assertEquals("1.50 KB", formatByteSize(1536L))
    }
}
