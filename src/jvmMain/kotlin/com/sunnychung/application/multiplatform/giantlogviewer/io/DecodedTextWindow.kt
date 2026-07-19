package com.sunnychung.application.multiplatform.giantlogviewer.io

interface DecodedTextWindow {
    val text: String
    val byteRange: LongRange

    fun bytePositionAtCharIndex(charIndex: Int): Long
}
