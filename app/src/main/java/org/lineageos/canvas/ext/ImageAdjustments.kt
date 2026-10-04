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

fun brightnessColorFilter(value: Float): ColorFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, value * 255f,
            0f, 1f, 0f, 0f, value * 255f,
            0f, 0f, 1f, 0f, value * 255f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),
)

fun contrastColorFilter(value: Float): ColorFilter {
    val offset = 0.5f * (1f - value) * 255f
    return ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                value, 0f, 0f, 0f, offset,
                0f, value, 0f, 0f, offset,
                0f, 0f, value, 0f, offset,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    )
}

fun sepiaColorFilter(amount: Float): ColorFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            1f - 0.607f * amount, 0.769f * amount, 0.189f * amount, 0f, 0f,
            0.349f * amount, 1f - 0.314f * amount, 0.168f * amount, 0f, 0f,
            0.272f * amount, 0.534f * amount, 1f - 0.869f * amount, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),
)

fun greyColorFilter(saturation: Float): ColorFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            0.213f + 0.787f * saturation, 0.715f - 0.715f * saturation,
            0.072f - 0.072f * saturation, 0f, 0f,
            0.213f - 0.213f * saturation, 0.715f + 0.285f * saturation,
            0.072f - 0.072f * saturation, 0f, 0f,
            0.213f - 0.213f * saturation, 0.715f - 0.715f * saturation,
            0.072f + 0.928f * saturation, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),
)

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
