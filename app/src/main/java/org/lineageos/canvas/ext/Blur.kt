/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ext

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntRect
import androidx.core.graphics.scale
import org.lineageos.canvas.models.RedactMode
import kotlin.math.roundToInt

private const val BLUR_RADIUS_DIVISOR = 50
private const val MIN_BLUR_RADIUS = 8
private const val BLUR_PASSES = 3
private const val PREVIEW_MAX_SIZE = 1024

val ImageBitmap.blurRadius
    get() = maxOf(MIN_BLUR_RADIUS, maxOf(width, height) / BLUR_RADIUS_DIVISOR)

fun ImageBitmap.redact(rect: IntRect, mode: RedactMode): ImageBitmap {
    val bounds = IntRect(0, 0, width, height)
    val target = rect.intersect(bounds)
    if (target.width <= 0 || target.height <= 0) return this

    val output = asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(output)

    when (mode) {
        RedactMode.BLUR -> {
            val region = target.inflate(blurRadius * BLUR_PASSES).intersect(bounds)
            val blurred = output.blurred(region, blurRadius)
            canvas.drawBitmap(
                blurred,
                target.translate(-region.left, -region.top).toAndroidRect(),
                target.toAndroidRect(),
                null,
            )
        }

        RedactMode.MASK -> canvas.drawRect(
            target.toAndroidRect(),
            Paint().apply { color = android.graphics.Color.BLACK },
        )
    }

    return output.asImageBitmap()
}

fun ImageBitmap.blurredPreview(): ImageBitmap {
    val scale = minOf(1f, PREVIEW_MAX_SIZE.toFloat() / maxOf(width, height))
    val source = asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false).let {
        when (scale < 1f) {
            true -> it.scale(
                (width * scale).roundToInt().coerceAtLeast(1),
                (height * scale).roundToInt().coerceAtLeast(1),
            )

            false -> it
        }
    }
    val radius = (blurRadius * scale).roundToInt().coerceAtLeast(1)

    return source.blurred(IntRect(0, 0, source.width, source.height), radius).asImageBitmap()
}

private fun Bitmap.blurred(region: IntRect, radius: Int): Bitmap {
    val width = region.width
    val height = region.height
    val pixels = IntArray(width * height)
    val buffer = IntArray(width * height)

    getPixels(pixels, 0, width, region.left, region.top, width, height)

    repeat(BLUR_PASSES) {
        boxBlur(pixels, buffer, width, height, radius, horizontal = true)
        boxBlur(buffer, pixels, width, height, radius, horizontal = false)
    }

    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
}

private fun boxBlur(
    source: IntArray,
    destination: IntArray,
    width: Int,
    height: Int,
    radius: Int,
    horizontal: Boolean,
) {
    val length = if (horizontal) width else height
    val lines = if (horizontal) height else width
    val step = if (horizontal) 1 else width
    val window = radius * 2 + 1

    for (line in 0 until lines) {
        val base = if (horizontal) line * width else line

        fun pixelAt(index: Int) = source[base + index.coerceIn(0, length - 1) * step]

        var a = 0
        var r = 0
        var g = 0
        var b = 0
        for (index in -radius..radius) {
            val pixel = pixelAt(index)
            a += pixel ushr 24
            r += (pixel shr 16) and 0xFF
            g += (pixel shr 8) and 0xFF
            b += pixel and 0xFF
        }

        for (index in 0 until length) {
            destination[base + index * step] = ((a / window) shl 24) or
                    ((r / window) shl 16) or
                    ((g / window) shl 8) or
                    (b / window)

            val outgoing = pixelAt(index - radius)
            val incoming = pixelAt(index + radius + 1)
            a += (incoming ushr 24) - (outgoing ushr 24)
            r += ((incoming shr 16) and 0xFF) - ((outgoing shr 16) and 0xFF)
            g += ((incoming shr 8) and 0xFF) - ((outgoing shr 8) and 0xFF)
            b += (incoming and 0xFF) - (outgoing and 0xFF)
        }
    }
}

private fun IntRect.inflate(delta: Int) = IntRect(left - delta, top - delta, right + delta, bottom + delta)

private fun IntRect.toAndroidRect() = android.graphics.Rect(left, top, right, bottom)
