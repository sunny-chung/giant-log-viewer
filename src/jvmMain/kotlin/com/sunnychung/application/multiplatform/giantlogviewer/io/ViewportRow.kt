package com.sunnychung.application.multiplatform.giantlogviewer.io

internal data class ViewportRow(
    val text: String,
    val visibleStartBytePosition: Long,
    val rowStartBytePosition: Long,
    val physicalLineStartBytePosition: Long,
)
