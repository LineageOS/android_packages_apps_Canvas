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
import org.lineageos.canvas.models.FilterPreset

/**
 * Adjusts brightness.
 * @param value Range [-1f..1f]. -1f is pure black, 0f is identity, 1f is pure white.
 */
fun brightnessColorFilter(value: Float): ColorFilter {
    val offset = value.coerceIn(-1f, 1f) * 255f
    return channelColorFilter(offset = offset)
}

/**
 * Adjusts contrast.
 * @param value Range [0f..2f]. 0f is solid mid-grey, 1f is identity, 2f is high contrast.
 */
fun contrastColorFilter(value: Float): ColorFilter {
    val scale = value.coerceIn(0f, 2f)
    val offset = (1f - scale) * 128f
    return channelColorFilter(red = scale, green = scale, blue = scale, offset = offset)
}

/**
 * Adjusts saturation.
 * @param value Range [0f..2f]. 0f is pure grey, 1f is identity, 2f is full saturation.
 */
fun saturationColorFilter(value: Float): ColorFilter {
    val saturation = value.coerceIn(0f, 2f)
    return ColorFilter.colorMatrix(ColorMatrix(saturationMatrix(saturation)))
}

/**
 * Adjusts warmth.
 * @param value Range [-1f..1f]. -1f is cool, 0f is identity, 1f is red.
 */
fun warmthColorFilter(value: Float): ColorFilter {
    val shift = value.coerceIn(-1f, 1f) * COLOR_BALANCE_STRENGTH
    return channelColorFilter(red = 1f + shift, blue = 1f - shift)
}

/**
 * Adjusts tint.
 * @param value Range [-1f..1f]. -1f is green, 0f is identity, 1f is magenta.
 */
fun tintColorFilter(value: Float): ColorFilter {
    val shift = value.coerceIn(-1f, 1f) * COLOR_BALANCE_STRENGTH
    return channelColorFilter(
        red = 1f + shift / 2f,
        green = 1f - shift,
        blue = 1f + shift / 2f,
    )
}

private const val COLOR_BALANCE_STRENGTH = 0.3f

/**
 * Blends a preset's color matrix with identity.
 * @param preset The filter preset to apply.
 * @param amount Range [0f..1f]. 0f is identity, 1f is the full preset.
 */
fun presetColorFilter(preset: FilterPreset, amount: Float): ColorFilter {
    val t = amount.coerceIn(0f, 1f)
    val target = preset.matrix
    val identity = ColorMatrix().values
    return ColorFilter.colorMatrix(
        ColorMatrix(FloatArray(20) { identity[it] + (target[it] - identity[it]) * t })
    )
}

private val FilterPreset.matrix: FloatArray
    get() = when (this) {
        FilterPreset.PUNCH -> saturationMatrix(1.4f, contrast = 1.15f)
        FilterPreset.VINTAGE -> floatArrayOf(
            0.62f, 0.32f, 0.08f, 0f, 22f,
            0.18f, 0.66f, 0.07f, 0f, 18f,
            0.13f, 0.27f, 0.42f, 0f, 14f,
            0f, 0f, 0f, 1f, 0f
        )

        FilterPreset.COOL -> floatArrayOf(
            0.92f, 0.02f, 0.01f, 0f, 0f,
            0.01f, 0.98f, 0.01f, 0f, 2f,
            0.01f, 0.02f, 1.04f, 0f, 8f,
            0f, 0f, 0f, 1f, 0f
        )

        FilterPreset.NOIR -> saturationMatrix(0f, contrast = 1.45f)
        FilterPreset.FADE -> saturationMatrix(0.75f, contrast = 0.78f, lift = 24f)
        FilterPreset.NEGATIVE -> floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        )
    }

/**
 * Creates a saturation matrix using BT.601 luma weights, followed by contrast and lift.
 * @param saturation 0f is greyscale, 1f preserves saturation, values above 1f increase it.
 * @param contrast Contrast scale around 128f. 1f preserves contrast.
 * @param lift Offset added equally to RGB channels in the [0f..255f] color space.
 */
private fun saturationMatrix(
    saturation: Float,
    contrast: Float = 1f,
    lift: Float = 0f,
): FloatArray {
    val r = 0.299f * (1f - saturation) * contrast
    val g = 0.587f * (1f - saturation) * contrast
    val b = 0.114f * (1f - saturation) * contrast
    val s = saturation * contrast
    val offset = (1f - contrast) * 128f + lift
    return floatArrayOf(
        r + s, g, b, 0f, offset,
        r, g + s, b, 0f, offset,
        r, g, b + s, 0f, offset,
        0f, 0f, 0f, 1f, 0f
    )
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
fun greyColorFilter(value: Float) =
    saturationColorFilter(1f - value.coerceIn(0f, 1f))

/**
 * Scales RGB channels independently, then adds a shared offset.
 * @param red Red channel scale. 1f is identity.
 * @param green Green channel scale. 1f is identity.
 * @param blue Blue channel scale. 1f is identity.
 * @param offset Offset added to each RGB channel in the [0f..255f] color space.
 */
private fun channelColorFilter(
    red: Float = 1f,
    green: Float = 1f,
    blue: Float = 1f,
    offset: Float = 0f,
) = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            red, 0f, 0f, 0f, offset,
            0f, green, 0f, 0f, offset,
            0f, 0f, blue, 0f, offset,
            0f, 0f, 0f, 1f, 0f
        )
    )
)

fun ImageBitmap.adjustBrightness(value: Float) =
    if (value == 0f) this else applyColorFilter(brightnessColorFilter(value))

fun ImageBitmap.adjustContrast(value: Float) =
    if (value == 1f) this else applyColorFilter(contrastColorFilter(value))

fun ImageBitmap.applySepia(amount: Float) =
    if (amount == 0f) this else applyColorFilter(sepiaColorFilter(amount))

fun ImageBitmap.applyGrey(saturation: Float) =
    if (saturation == 1f) this else applyColorFilter(greyColorFilter(saturation))

fun ImageBitmap.adjustSaturation(value: Float) =
    if (value == 1f) this else applyColorFilter(saturationColorFilter(value))

fun ImageBitmap.adjustWarmth(value: Float) =
    if (value == 0f) this else applyColorFilter(warmthColorFilter(value))

fun ImageBitmap.adjustTint(value: Float) =
    if (value == 0f) this else applyColorFilter(tintColorFilter(value))

fun ImageBitmap.applyPreset(preset: FilterPreset, amount: Float) =
    if (amount == 0f) this else applyColorFilter(presetColorFilter(preset, amount))

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
