package com.sunnychung.application.multiplatform.giantlogviewer.util

import com.sunnychung.lib.multiplatform.bigtext.core.layout.CharMeasurer

class FixedWidthCharMeasurer(private val charWidth: Float) : CharMeasurer<Unit> {
    override fun measureFullText(text: CharSequence) {
        // Nothing
    }

    override fun findCharWidth(char: CharSequence, style: Unit?): Float = charWidth

    override fun findCharYOffset(char: CharSequence, style: Unit?): Float = throw NotImplementedError()
}
