/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ext

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.hypot

private const val WATERMARK_ANGLE = -30f

fun DrawScope.drawWatermark(
    text: String,
    color: Color,
    textSize: Float,
    textMeasurer: TextMeasurer,
    area: Size = size,
) {
    if (text.isBlank() || textSize <= 0f) return

    val layout = textMeasurer.measure(
        text = text,
        style = TextStyle(
            color = color,
            fontSize = textSize.toSp(),
            fontWeight = FontWeight.Bold,
        ),
        softWrap = false,
    )
    val columnStep = layout.size.width + textSize * 2f
    val rowStep = layout.size.height + textSize * 2f
    val extent = hypot(area.width, area.height) / 2f + columnStep
    val origin = Offset(area.width / 2f, area.height / 2f)

    clipRect(right = area.width, bottom = area.height) {
        rotate(WATERMARK_ANGLE, pivot = origin) {
            var row = 0
            var y = -extent
            while (y <= extent) {
                var x = -extent + if (row % 2 == 0) 0f else columnStep / 2f
                while (x <= extent) {
                    drawText(
                        textLayoutResult = layout,
                        topLeft = origin + Offset(
                            x - layout.size.width / 2f,
                            y - layout.size.height / 2f,
                        ),
                    )
                    x += columnStep
                }
                y += rowStep
                row++
            }
        }
    }
}
