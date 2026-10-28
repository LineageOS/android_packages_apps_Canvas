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
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.util.lerp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

private const val FIT_TOLERANCE = 0.01f
private const val SEARCH_ITERATIONS = 24

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

/**
 * The largest rectangle with the image's aspect ratio, centered on the image, that fits in an image
 * of [imageSize] rotated by [degrees].
 *
 * @see fitCenteredSize
 */
fun straightenCropBounds(imageSize: Size, degrees: Float) =
    centeredRect(imageSize, fitCenteredSize(imageSize, imageSize, degrees))

/**
 * Scale [size] down, keeping its aspect ratio, so that a rectangle of that size centered on the
 * image fits in an image of [imageSize] rotated by [degrees]. Port of Gallery2's
 * ImageStraighten.getUntranslatedStraightenCropBounds.
 */
fun fitCenteredSize(size: Size, imageSize: Size, degrees: Float): Size {
    if (size.width <= 0f || size.height <= 0f) return Size.Zero

    val radians = degrees * PI.toFloat() / 180f
    val cosine = abs(cos(radians))
    val sine = abs(sin(radians))
    val aspectRatio = size.width / size.height

    val height =
        minOf(
            size.height,
            imageSize.width / (aspectRatio * cosine + sine),
            imageSize.height / (aspectRatio * sine + cosine),
        )

    return Size(height * aspectRatio, height)
}

/**
 * Bring [crop] inside an image of [imageSize] rotated by [degrees], moving it towards the image
 * center and shrinking it as little as needed.
 */
fun constrainCrop(crop: Rect, imageSize: Size, degrees: Float): Rect {
    if (crop.fitsInRotatedImage(imageSize, degrees)) return crop

    val target = centeredRect(imageSize, fitCenteredSize(crop.size, imageSize, degrees))
    val fraction = searchFraction { lerp(target, crop, it).fitsInRotatedImage(imageSize, degrees) }

    return lerp(target, crop, fraction)
}

/**
 * Move a valid [crop] by [offset], stopping at the edges of an image of [imageSize] rotated by
 * [degrees]. Movement blocked by an edge continues along it, so the crop slides along the edges.
 */
fun moveCrop(crop: Rect, offset: Offset, imageSize: Size, degrees: Float): Rect {
    val start = constrainCrop(crop, imageSize, degrees)
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
 * Resize a valid crop from [old] towards [new], stopping each edge at the edges of an image of
 * [imageSize] rotated by [degrees].
 */
fun resizeCrop(old: Rect, new: Rect, imageSize: Size, degrees: Float): Rect {
    var result = constrainCrop(old, imageSize, degrees)

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

private fun centeredRect(imageSize: Size, size: Size) =
    Rect(offset = imageSize.center - Offset(size.width / 2f, size.height / 2f), size = size)
