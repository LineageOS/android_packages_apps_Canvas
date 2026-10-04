/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ext

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Adjusts brightness.
 * @param value Range [-1f..1f]. -1f is pure black, 0f is identity, 1f is pure white.
 */
fun brightnessColorFilter(value: Float): ColorFilter {
    val offset = value.coerceIn(-1f, 1f) * 255f
    val matrix = ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, offset,
            0f, 1f, 0f, 0f, offset,
            0f, 0f, 1f, 0f, offset,
            0f, 0f, 0f, 1f, 0f
        )
    )
    return ColorFilter.colorMatrix(matrix)
}

/**
 * Adjusts contrast.
 * @param value Range [0f..2f]. 0f is solid mid-grey, 1f is identity, 2f is high contrast.
 */
fun contrastColorFilter(value: Float): ColorFilter {
    val scale = value.coerceIn(0f, 2f)
    val offset = (1f - scale) * 128f
    val matrix = ColorMatrix(
        floatArrayOf(
            scale, 0f, 0f, 0f, offset,
            0f, scale, 0f, 0f, offset,
            0f, 0f, scale, 0f, offset,
            0f, 0f, 0f, 1f, 0f
        )
    )
    return ColorFilter.colorMatrix(matrix)
}


/**
 * Applies a sepia tone.
 * @param value Range [0f..1f]. 0f is identity, 1f is full sepia tone.
 */
fun sepiaColorFilter(value: Float): ColorFilter {
    val t = value.coerceIn(0f, 1f)
    val inv = 1f - t

    // Standard W3C / Rec.601 sepia transformation coefficients blended with identity
    val matrix = ColorMatrix(
        floatArrayOf(
            inv + t * 0.393f, t * 0.769f, t * 0.189f, 0f, 0f,
            t * 0.349f, inv + t * 0.686f, t * 0.168f, 0f, 0f,
            t * 0.272f, t * 0.534f, inv + t * 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
    return ColorFilter.colorMatrix(matrix)
}

/**
 * Desaturates colors to greyscale.
 * @param value Range [0f..1f]. 0f is identity, 1f is full greyscale.
 */
fun greyColorFilter(value: Float): ColorFilter {
    val t = value.coerceIn(0f, 1f)
    val inv = 1f - t

    // ITU-R BT.601 luma weights (0.299 R, 0.587 G, 0.114 B) linearly interpolated with identity
    val r = 0.299f * t
    val g = 0.587f * t
    val b = 0.114f * t

    val matrix = ColorMatrix(
        floatArrayOf(
            inv + r, g, b, 0f, 0f,
            r, inv + g, b, 0f, 0f,
            r, g, inv + b, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
    return ColorFilter.colorMatrix(matrix)
}

fun ImageBitmap.adjustBrightness(value: Float): ImageBitmap {
    if (value == 0f) return this

    val output = ImageBitmap(
        width = width,
        height = height,
        config = config,
        hasAlpha = hasAlpha,
        colorSpace = colorSpace,
    )

    CanvasDrawScope().draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = Canvas(output),
        size = Size(width.toFloat(), height.toFloat()),
    ) {
        drawImage(
            image = this@adjustBrightness,
            colorFilter = brightnessColorFilter(value),
        )
    }

    return output
}

fun ImageBitmap.adjustContrast(value: Float): ImageBitmap {
    if (value == 1f) return this

    val output = ImageBitmap(
        width = width,
        height = height,
        config = config,
        hasAlpha = hasAlpha,
        colorSpace = colorSpace,
    )

    CanvasDrawScope().draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = Canvas(output),
        size = Size(width.toFloat(), height.toFloat()),
    ) {
        drawImage(
            image = this@adjustContrast,
            colorFilter = contrastColorFilter(value),
        )
    }

    return output
}

fun ImageBitmap.applySepia(amount: Float): ImageBitmap =
    if (amount == 0f) this else applyColorFilter(sepiaColorFilter(amount))

fun ImageBitmap.applyGrey(saturation: Float): ImageBitmap =
    if (saturation == 1f) this else applyColorFilter(greyColorFilter(saturation))

private fun ImageBitmap.applyColorFilter(colorFilter: ColorFilter): ImageBitmap {
    val output = ImageBitmap(
        width = width,
        height = height,
        config = config,
        hasAlpha = hasAlpha,
        colorSpace = colorSpace,
    )

    CanvasDrawScope().draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = Canvas(output),
        size = Size(width.toFloat(), height.toFloat()),
    ) {
        drawImage(
            image = this@applyColorFilter,
            colorFilter = colorFilter,
        )
    }

    return output
}
