/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ext

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import org.lineageos.canvas.models.ShapeType
import org.lineageos.canvas.ui.theme.CropOverlayStyle
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

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

fun DrawScope.drawSelectionOverlay(rect: Rect, style: CropOverlayStyle) {
    drawRect(
        color = style.borderColor,
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.height),
        style = Stroke(width = 1.dp.toPx()),
    )

    drawHandles(handleOffsets(rect), style)
}

fun DrawScope.drawHandles(offsets: List<Offset>, style: CropOverlayStyle) {
    offsets.forEach { offset ->
        drawCircle(style.handleColor, style.handleRadius.toPx(), offset)
    }
}

fun DrawScope.drawShape(
    type: ShapeType,
    start: Offset,
    end: Offset,
    color: Color,
    filled: Boolean,
    strokeWidth: Float,
) {
    val stroke = Stroke(
        width = strokeWidth,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round,
    )
    val style = when (filled && !type.isLinear) {
        true -> Fill
        false -> stroke
    }
    val rect = Rect(
        left = minOf(start.x, end.x),
        top = minOf(start.y, end.y),
        right = maxOf(start.x, end.x),
        bottom = maxOf(start.y, end.y),
    )

    when (type) {
        ShapeType.RECTANGLE -> drawRect(
            color = color,
            topLeft = rect.topLeft,
            size = rect.size,
            style = style,
        )

        ShapeType.OVAL -> drawOval(
            color = color,
            topLeft = rect.topLeft,
            size = rect.size,
            style = style,
        )

        ShapeType.TRIANGLE -> drawPath(
            path = polygonPath(
                listOf(
                    Offset(rect.center.x, rect.top),
                    Offset(rect.right, rect.bottom),
                    Offset(rect.left, rect.bottom),
                )
            ),
            color = color,
            style = style,
        )

        ShapeType.STAR -> drawPath(
            path = polygonPath(
                List(STAR_POINTS * 2) { index ->
                    val angle = -PI / 2 + index * PI / STAR_POINTS
                    val radius = when (index % 2) {
                        0 -> 1f
                        else -> STAR_INNER_RADIUS
                    }
                    Offset(
                        rect.center.x + rect.width / 2 * radius * cos(angle).toFloat(),
                        rect.center.y + rect.height / 2 * radius * sin(angle).toFloat(),
                    )
                }
            ),
            color = color,
            style = style,
        )

        ShapeType.LINE -> drawLine(
            color = color,
            start = start,
            end = end,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )

        ShapeType.ARROW -> {
            val angle = atan2(end.y - start.y, end.x - start.x)
            val headLength = strokeWidth * ARROW_HEAD_SCALE
            val head = listOf(-1, 1).map { side ->
                val headAngle = angle + PI.toFloat() + side * ARROW_HEAD_ANGLE
                Offset(
                    end.x + headLength * cos(headAngle),
                    end.y + headLength * sin(headAngle),
                )
            }

            drawLine(
                color = color,
                start = start,
                end = end,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawPath(
                path = Path().apply {
                    moveTo(head[0].x, head[0].y)
                    lineTo(end.x, end.y)
                    lineTo(head[1].x, head[1].y)
                },
                color = color,
                style = stroke,
            )
        }
    }
}

private fun polygonPath(points: List<Offset>) = Path().apply {
    moveTo(points.first().x, points.first().y)
    points.drop(1).forEach { lineTo(it.x, it.y) }
    close()
}

private const val STAR_POINTS = 5
private const val STAR_INNER_RADIUS = 0.4f
private const val ARROW_HEAD_SCALE = 4f
private const val ARROW_HEAD_ANGLE = (PI / 6).toFloat()
