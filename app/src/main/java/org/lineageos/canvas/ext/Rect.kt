/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ext

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ScaleFactor
import org.lineageos.canvas.models.Handle
import kotlin.math.abs

operator fun Rect.times(scaleFactor: ScaleFactor) = Rect(
    left = left * scaleFactor.scaleX,
    top = top * scaleFactor.scaleY,
    right = right * scaleFactor.scaleX,
    bottom = bottom * scaleFactor.scaleY,
)

operator fun Rect.div(scaleFactor: ScaleFactor) = Rect(
    left = left / scaleFactor.scaleX,
    top = top / scaleFactor.scaleY,
    right = right / scaleFactor.scaleX,
    bottom = bottom / scaleFactor.scaleY,
)

fun updateRectWithDrag(
    rect: Rect,
    handle: Handle,
    drag: Offset,
    bounds: Rect,
    requestedMinimumSize: Float,
): Rect {
    val minWidth = minOf(requestedMinimumSize, bounds.width)
    val minHeight = minOf(requestedMinimumSize, bounds.height)

    if (handle == Handle.CENTER) {
        val width = rect.width.coerceIn(0f, bounds.width)
        val height = rect.height.coerceIn(0f, bounds.height)
        val maxLeft = maxOf(bounds.left, minOf(bounds.right, bounds.right - width))
        val maxTop = maxOf(bounds.top, minOf(bounds.bottom, bounds.bottom - height))
        val left = (rect.left + drag.x).coerceIn(bounds.left, maxLeft)
        val top = (rect.top + drag.y).coerceIn(bounds.top, maxTop)

        return Rect(
            left = left,
            top = top,
            right = minOf(bounds.right, left + width),
            bottom = minOf(bounds.bottom, top + height),
        )
    }

    val left = if (handle.movesLeft) {
        (rect.left + drag.x).coerceIn(
            bounds.left,
            maxOf(bounds.left, minOf(bounds.right, rect.right - minWidth)),
        )
    } else {
        rect.left
    }
    val right = if (handle.movesRight) {
        (rect.right + drag.x).coerceIn(
            minOf(bounds.right, maxOf(bounds.left, rect.left + minWidth)),
            bounds.right,
        )
    } else {
        rect.right
    }
    val top = if (handle.movesTop) {
        (rect.top + drag.y).coerceIn(
            bounds.top,
            maxOf(bounds.top, minOf(bounds.bottom, rect.bottom - minHeight)),
        )
    } else {
        rect.top
    }
    val bottom = if (handle.movesBottom) {
        (rect.bottom + drag.y).coerceIn(
            minOf(bounds.bottom, maxOf(bounds.top, rect.top + minHeight)),
            bounds.bottom,
        )
    } else {
        rect.bottom
    }

    return Rect(left, top, right, bottom)
}

fun updateRectWithDragKeepingRatio(
    rect: Rect,
    handle: Handle,
    drag: Offset,
    bounds: Rect,
    requestedMinimumSize: Float,
    ratio: Float,
): Rect {
    val movesHorizontally = handle.movesLeft || handle.movesRight
    val movesVertically = handle.movesTop || handle.movesBottom

    val draggedWidth = when {
        handle.movesRight -> rect.width + drag.x
        handle.movesLeft -> rect.width - drag.x
        else -> rect.width
    }
    val draggedHeight = when {
        handle.movesBottom -> rect.height + drag.y
        handle.movesTop -> rect.height - drag.y
        else -> rect.height
    }
    val width = when {
        movesHorizontally && movesVertically -> when (abs(drag.x) >= abs(drag.y) * ratio) {
            true -> draggedWidth
            false -> draggedHeight * ratio
        }

        movesHorizontally -> draggedWidth
        else -> draggedHeight * ratio
    }

    val availableWidth = when {
        handle.movesRight -> bounds.right - rect.left
        handle.movesLeft -> rect.right - bounds.left
        else -> bounds.width
    }
    val availableHeight = when {
        handle.movesBottom -> bounds.bottom - rect.top
        handle.movesTop -> rect.bottom - bounds.top
        else -> bounds.height
    }
    val maxWidth = minOf(availableWidth, availableHeight * ratio)
    val minWidth = maxOf(requestedMinimumSize, requestedMinimumSize * ratio).coerceAtMost(maxWidth)

    val newWidth = width.coerceIn(minWidth, maxWidth)
    val newHeight = newWidth / ratio

    val left = when {
        handle.movesRight -> rect.left
        handle.movesLeft -> rect.right - newWidth
        else -> (rect.center.x - newWidth / 2).coerceIn(
            bounds.left,
            maxOf(bounds.left, bounds.right - newWidth),
        )
    }
    val top = when {
        handle.movesBottom -> rect.top
        handle.movesTop -> rect.bottom - newHeight
        else -> (rect.center.y - newHeight / 2).coerceIn(
            bounds.top,
            maxOf(bounds.top, bounds.bottom - newHeight),
        )
    }

    return Rect(left, top, left + newWidth, top + newHeight)
}

fun getHandleForOffset(offset: Offset, rect: Rect, hitRadius: Float): Handle? {
    val handles = listOf(
        Handle.TOP_LEFT to Offset(rect.left, rect.top),
        Handle.TOP to Offset(rect.center.x, rect.top),
        Handle.TOP_RIGHT to Offset(rect.right, rect.top),
        Handle.LEFT to Offset(rect.left, rect.center.y),
        Handle.RIGHT to Offset(rect.right, rect.center.y),
        Handle.BOTTOM_LEFT to Offset(rect.left, rect.bottom),
        Handle.BOTTOM to Offset(rect.center.x, rect.bottom),
        Handle.BOTTOM_RIGHT to Offset(rect.right, rect.bottom),
    )
    val hitRadiusSquared = hitRadius * hitRadius
    val closestHandle = handles.minByOrNull { (_, position) ->
        val dx = offset.x - position.x
        val dy = offset.y - position.y
        dx * dx + dy * dy
    }

    if (closestHandle != null) {
        val (handle, position) = closestHandle
        val dx = offset.x - position.x
        val dy = offset.y - position.y
        if (dx * dx + dy * dy <= hitRadiusSquared) return handle
    }

    return Handle.CENTER.takeIf { rect.contains(offset) }
}
