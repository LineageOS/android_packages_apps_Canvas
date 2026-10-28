/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ext

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.util.lerp
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import org.lineageos.canvas.models.Handle

private const val FIT_TOLERANCE = 0.01f
private const val SEARCH_ITERATIONS = 24

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

/** Whether this crop rectangle lies inside an image of [imageSize] rotated by [degrees]. */
fun Rect.fitsInRotatedImage(imageSize: Size, degrees: Float): Boolean {
    val radians = -degrees * PI.toFloat() / 180f
    val cosine = cos(radians)
    val sine = sin(radians)
    val center = imageSize.center

    return listOf(topLeft, topRight, bottomRight, bottomLeft).all { corner ->
        val dx = corner.x - center.x
        val dy = corner.y - center.y
        val x = center.x + dx * cosine - dy * sine
        val y = center.y + dx * sine + dy * cosine
        x >= -FIT_TOLERANCE &&
            x <= imageSize.width + FIT_TOLERANCE &&
            y >= -FIT_TOLERANCE &&
            y <= imageSize.height + FIT_TOLERANCE
    }
}

/**
 * Bring this crop inside an image of [imageSize] rotated by [degrees], moving it towards the image
 * center and shrinking it as little as needed.
 */
fun Rect.constrainCrop(imageSize: Size, degrees: Float): Rect {
    if (fitsInRotatedImage(imageSize, degrees)) return this

    val fitted = fitCenteredSize(size, imageSize, degrees)
    val target = Rect(offset = imageSize.center - fitted.center, size = fitted)
    val fraction = searchFraction { lerp(target, this, it).fitsInRotatedImage(imageSize, degrees) }

    return lerp(target, this, fraction)
}

/**
 * Move this valid crop by [offset], stopping at the edges of an image of [imageSize] rotated by
 * [degrees]. Movement blocked by an edge continues along it, so the crop slides along the edges.
 */
fun Rect.moveCrop(offset: Offset, imageSize: Size, degrees: Float): Rect {
    val start = constrainCrop(imageSize, degrees)
    val fraction = searchFraction {
        start.translate(offset * it).fitsInRotatedImage(imageSize, degrees)
    }
    if (fraction == 1f) return start.translate(offset)

    val radians = degrees * PI.toFloat() / 180f
    val remaining = offset * (1f - fraction)
    var result = start.translate(offset * fraction)

    for (axis in listOf(Offset(cos(radians), sin(radians)), Offset(-sin(radians), cos(radians)))) {
        val current = result
        val step = axis * (remaining.x * axis.x + remaining.y * axis.y)
        result = current.translate(
            step * searchFraction {
                current.translate(step * it).fitsInRotatedImage(imageSize, degrees)
            }
        )
    }

    return result
}

/**
 * Resize this valid crop towards [new], stopping each edge at the edges of an image of [imageSize]
 * rotated by [degrees].
 */
fun Rect.resizeCrop(new: Rect, imageSize: Size, degrees: Float): Rect {
    var result = constrainCrop(imageSize, degrees)

    val edges =
        listOf<(Rect, Float) -> Rect>(
            { rect, fraction -> rect.copy(left = lerp(rect.left, new.left, fraction)) },
            { rect, fraction -> rect.copy(top = lerp(rect.top, new.top, fraction)) },
            { rect, fraction -> rect.copy(right = lerp(rect.right, new.right, fraction)) },
            { rect, fraction -> rect.copy(bottom = lerp(rect.bottom, new.bottom, fraction)) },
        )

    for (moveEdge in edges) {
        val current = result
        val fraction = searchFraction {
            moveEdge(current, it).fitsInRotatedImage(imageSize, degrees)
        }
        result = moveEdge(current, fraction)
    }

    return result
}

/**
 * Find the largest fraction in [0, 1] for which [fits] holds, assuming the valid fractions form an
 * interval starting at 0.
 */
private fun searchFraction(fits: (Float) -> Boolean): Float {
    if (fits(1f)) return 1f

    var valid = 0f
    var invalid = 1f
    repeat(SEARCH_ITERATIONS) {
        val middle = (valid + invalid) / 2f
        if (fits(middle)) valid = middle else invalid = middle
    }

    return valid
}

/**
 * Move the edges of this rectangle selected by [handle] by [drag], without making it smaller than
 * [requestedMinimumSize] (or its current size, if already smaller).
 */
fun Rect.resizedBy(handle: Handle, drag: Offset, requestedMinimumSize: Float): Rect {
    val minWidth = minOf(requestedMinimumSize, width)
    val minHeight = minOf(requestedMinimumSize, height)

    return Rect(
        left = if (handle.movesLeft) minOf(left + drag.x, right - minWidth) else left,
        top = if (handle.movesTop) minOf(top + drag.y, bottom - minHeight) else top,
        right = if (handle.movesRight) maxOf(right + drag.x, left + minWidth) else right,
        bottom = if (handle.movesBottom) maxOf(bottom + drag.y, top + minHeight) else bottom,
    )
}

/**
 * Round this valid crop rectangle to whole pixels. Edges are rounded inwards, so the result is
 * still valid.
 */
fun Rect.roundInwards() =
    IntRect(
        left = ceil(left - FIT_TOLERANCE).toInt(),
        top = ceil(top - FIT_TOLERANCE).toInt(),
        right = floor(right + FIT_TOLERANCE).toInt(),
        bottom = floor(bottom + FIT_TOLERANCE).toInt(),
    )
