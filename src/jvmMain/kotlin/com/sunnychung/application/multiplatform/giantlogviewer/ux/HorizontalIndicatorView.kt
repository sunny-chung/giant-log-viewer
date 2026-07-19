package com.sunnychung.application.multiplatform.giantlogviewer.ux

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onGloballyPositioned
import com.sunnychung.application.multiplatform.giantlogviewer.ux.local.LocalColor

@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun HorizontalIndicatorView(
    modifier: Modifier = Modifier,
    value: Float,
    onSelectRatio: (Float) -> Unit = {},
    onScrollByPx: (Float) -> Unit = {},
) {
    val colors = LocalColor.current
    var componentWidth by remember { mutableIntStateOf(0) }
    var componentHeight by remember { mutableIntStateOf(0) }
    var isHovered by remember { mutableStateOf(false) }
    val safeValue = value.coerceIn(0f, 1f)
    val markerRadius by animateFloatAsState(
        targetValue = if (isHovered) {
            (componentHeight * 0.48f)
        } else {
            (componentHeight * 0.34f)
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow * 4f / 3f,
        ),
    )
    val availableTravel = (componentWidth - markerRadius * 2f).coerceAtLeast(0f)
    val markerColor = if (isHovered) {
        lerp(colors.readPositionBarPositiveBackground, Color.White, 0.28f)
    } else {
        colors.readPositionBarPositiveBackground
    }

    fun ratioAtX(x: Float): Float {
        if (availableTravel <= 0f) {
            return 0f
        }
        return ((x - markerRadius) / availableTravel).coerceIn(0f, 1f)
    }

    fun isInsideMarker(position: Offset): Boolean {
        val markerCenter = Offset(
            x = markerRadius + availableTravel * safeValue,
            y = componentHeight.toFloat() / 2f,
        )
        val hitRadius = maxOf(markerRadius, componentHeight * 0.48f)
        return (position - markerCenter).getDistanceSquared() <= hitRadius * hitRadius
    }

    Box(
        modifier
            .onGloballyPositioned {
                componentWidth = it.size.width
                componentHeight = it.size.height
            }
            .onPointerEvent(PointerEventType.Enter) {
                it.changes.firstOrNull()?.position?.let { position ->
                    isHovered = isInsideMarker(position)
                }
            }
            .onPointerEvent(PointerEventType.Exit) {
                isHovered = false
            }
            .onPointerEvent(PointerEventType.Press) {
                onSelectRatio(ratioAtX(it.changes.first().position.x))
                it.changes.forEach { change -> change.consume() }
            }
            .onPointerEvent(PointerEventType.Move) {
                it.changes.firstOrNull()?.position?.let { position ->
                    isHovered = isInsideMarker(position)
                }
                if (it.changes.any { change -> change.pressed }) {
                    onSelectRatio(ratioAtX(it.changes.first().position.x))
                    it.changes.forEach { change -> change.consume() }
                }
            }
            .onPointerEvent(PointerEventType.Scroll) {
                val scrollDelta = it.changes.fold(Offset.Zero) { acc, change ->
                    acc + change.scrollDelta
                }
                val horizontalDelta = when {
                    scrollDelta.x != 0f -> scrollDelta.x
                    it.keyboardModifiers.isShiftPressed && scrollDelta.y != 0f -> scrollDelta.y
                    else -> 0f
                }
                if (horizontalDelta != 0f) {
                    onScrollByPx(horizontalDelta)
                    it.changes.forEach { change -> change.consume() }
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = componentWidth.toFloat()
            val height = componentHeight.toFloat()
            val markerCenter = Offset(
                x = markerRadius + availableTravel * safeValue,
                y = height / 2f,
            )
            drawRect(
                color = colors.readPositionBarNegativeBackground,
                topLeft = Offset(0f, 0f),
                size = Size(width, height),
            )
            drawCircle(
                color = markerColor,
                radius = markerRadius,
                center = markerCenter,
            )
        }
    }
}
