/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ext

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * The largest rectangle with the image's aspect ratio, centered on the image, that fits in an image
 * of [imageSize] rotated by [degrees].
 *
 * @see fitCenteredSize
 */
fun straightenCropBounds(imageSize: Size, degrees: Float): Rect {
    val size = fitCenteredSize(imageSize, imageSize, degrees)

    return Rect(offset = imageSize.center - size.center, size = size)
}

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
