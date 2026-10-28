/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ext

import androidx.compose.ui.geometry.Size
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Scale this size down, keeping its aspect ratio, so that a rectangle of
 * that size centered on the image fits in an image of [imageSize] rotated by [degrees].
 * Port of Gallery2's ImageStraighten.getUntranslatedStraightenCropBounds.
 */
fun Size.fitInCenter(imageSize: Size, degrees: Float): Size {
    if (width <= 0f || height <= 0f) return Size.Zero

    val radians = degrees * PI.toFloat() / 180f
    val cosine = abs(cos(radians))
    val sine = abs(sin(radians))
    val aspectRatio = width / height

    val fittedHeight =
        minOf(
            height,
            imageSize.width / (aspectRatio * cosine + sine),
            imageSize.height / (aspectRatio * sine + cosine),
        )

    return Size(fittedHeight * aspectRatio, fittedHeight)
}
