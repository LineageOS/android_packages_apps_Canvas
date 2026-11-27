/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ext

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.geometry.center
import androidx.compose.ui.unit.toSize
import org.lineageos.canvas.models.TextBackground
import org.lineageos.canvas.ui.theme.CropOverlayStyle

private fun handleOffsets(rect: Rect) = listOf(
    Offset(rect.left, rect.top),
    Offset(rect.right, rect.top),
    Offset(rect.left, rect.bottom),
    Offset(rect.right, rect.bottom),
    Offset(rect.center.x, rect.top),
    Offset(rect.center.x, rect.bottom),
    Offset(rect.left, rect.center.y),
    Offset(rect.right, rect.center.y),
)

fun DrawScope.drawCropOverlay(rect: Rect, showHandles: Boolean, style: CropOverlayStyle) {
    drawRect(color = style.dimColor)

    drawRect(
        color = Color.Transparent,
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.height),
        blendMode = BlendMode.Clear,
    )

    drawRect(
        color = if (showHandles) style.handleColor else style.borderColor,
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.height),
        style = Stroke(width = style.strokeWidth.toPx()),
    )

    if (showHandles) {
        handleOffsets(rect).forEach { offset ->
            drawCircle(style.handleColor, style.handleRadius.toPx(), offset)
        }
    }
}

/**
 * Padding around a text label background, relative to its font size.
 */
private const val TEXT_LABEL_PADDING = 0.25f

/**
 * The padding around the background of this text label, in the units it was measured in.
 */
val TextLayoutResult.labelPadding: Float
    get() = with(layoutInput.density) { layoutInput.style.fontSize.toPx() } * TEXT_LABEL_PADDING

/**
 * Measure a text label in image pixels, with its text color resolved against its background.
 */
fun TextMeasurer.measureLabel(
    text: String,
    style: TextStyle,
    background: TextBackground,
) = measure(
    text = text,
    style = style.copy(color = background.textColor(style.color)),
    density = Density(1f),
)

/**
 * Draw a text label measured with [measureLabel], rotated around its center. Used by both the
 * preview and the final rendering so that they always match.
 *
 * @param textLayoutResult The measured text
 * @param topLeft The top left corner of the unrotated text
 * @param rotation The clockwise rotation in degrees
 * @param backgroundColor The color of the background, or null if there is none
 */
fun DrawScope.drawTextLabel(
    textLayoutResult: TextLayoutResult,
    topLeft: Offset,
    rotation: Float,
    backgroundColor: Color?,
) {
    val size = textLayoutResult.size.toSize()

    rotate(degrees = rotation, pivot = topLeft + size.center) {
        backgroundColor?.let {
            val padding = textLayoutResult.labelPadding

            drawRoundRect(
                color = it,
                topLeft = topLeft - Offset(padding, padding),
                size = Size(size.width + padding * 2, size.height + padding * 2),
                cornerRadius = CornerRadius(padding),
            )
        }

        drawText(textLayoutResult = textLayoutResult, topLeft = topLeft)
    }
}
