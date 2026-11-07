/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.lineageos.canvas.R
import org.lineageos.canvas.models.Action
import org.lineageos.canvas.ui.composables.ActionButton
import org.lineageos.canvas.ui.composables.CanvasBottomBar
import org.lineageos.canvas.ui.composables.CanvasImage
import org.lineageos.canvas.ui.composables.ImageInformation
import org.lineageos.canvas.ui.composables.StrokeColorPalette
import org.lineageos.canvas.ui.composables.StrokeWidthControls

private val MARKER_STROKE_WIDTH = 4.dp
private val HIGHLIGHTER_STROKE_WIDTH = 16.dp

private val STROKE_WIDTH_RANGE = 1f..48f

/**
 * Add marker strokes to the current image.
 */
@Composable
fun MarkerScreen(
    imageBitmap: ImageBitmap,
    onAddAction: (Action) -> Unit,
    onCancel: () -> Unit,
) = StrokeScreen(
    imageBitmap = imageBitmap,
    initialColor = Color.Red,
    defaultStrokeWidth = MARKER_STROKE_WIDTH,
    createAction = { points, color, strokeWidth ->
        Action.Drawing.Marker(
            points = points,
            color = color,
            strokeWidth = strokeWidth,
        )
    },
    onAddAction = onAddAction,
    onCancel = onCancel,
)

/**
 * Add highlighter strokes to the current image.
 */
@Composable
fun HighlighterScreen(
    imageBitmap: ImageBitmap,
    onAddAction: (Action) -> Unit,
    onCancel: () -> Unit,
) = StrokeScreen(
    imageBitmap = imageBitmap,
    initialColor = Color.Yellow,
    colorTransform = { it.copy(alpha = 0.4f) },
    defaultStrokeWidth = HIGHLIGHTER_STROKE_WIDTH,
    createAction = { points, color, strokeWidth ->
        Action.Drawing.Highlighter(
            points = points,
            color = color,
            strokeWidth = strokeWidth,
        )
    },
    onAddAction = onAddAction,
    onCancel = onCancel,
)

@Composable
private fun StrokeScreen(
    imageBitmap: ImageBitmap,
    initialColor: Color,
    colorTransform: (Color) -> Color = { it },
    defaultStrokeWidth: Dp,
    createAction: (points: List<IntOffset>, color: Color, strokeWidth: Float) -> Action,
    onAddAction: (Action) -> Unit,
    onCancel: () -> Unit,
) {
    var imageInformation by remember { mutableStateOf<ImageInformation?>(null) }
    var isDrawing by remember { mutableStateOf(false) }
    var previewPoints by remember { mutableStateOf(emptyList<Offset>()) }
    var imagePoints by remember { mutableStateOf(emptyList<IntOffset>()) }
    var color by remember { mutableStateOf(initialColor) }
    var activeStrokeColor by remember { mutableStateOf(initialColor) }
    var activeStrokeWidth by remember { mutableFloatStateOf(0f) }
    var strokeWidth by rememberSaveable { mutableFloatStateOf(defaultStrokeWidth.value) }
    val strokeWidthInteractionSource = remember { MutableInteractionSource() }
    val isStrokeWidthPressed by strokeWidthInteractionSource.collectIsPressedAsState()
    val isStrokeWidthDragged by strokeWidthInteractionSource.collectIsDraggedAsState()
    val isAdjustingStrokeWidth = isStrokeWidthPressed || isStrokeWidthDragged
    var strokes by remember { mutableStateOf(emptyList<PendingStroke>()) }
    var undoneStrokes by remember { mutableStateOf(emptyList<PendingStroke>()) }

    val previewOutlineColor = MaterialTheme.colorScheme.outline

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            CanvasImage(
                imageBitmap = imageBitmap,
                cropRect = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) { imageInformation = it }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(imageInformation) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val info = imageInformation
                                isDrawing = info != null && info.imageViewRect.contains(offset)
                                if (isDrawing) {
                                    previewPoints = listOf(offset)
                                    imagePoints = listOf(info!!.viewOffsetToOriginalBitmap(offset))
                                    activeStrokeColor = colorTransform(color)
                                    activeStrokeWidth = strokeWidth.dp.toPx()
                                }
                            },
                            onDragEnd = {
                                if (isDrawing && imagePoints.isNotEmpty()) {
                                    val info = imageInformation
                                    val viewToImageScale = info?.let {
                                        it.bitmapSize.width / it.imageViewRect.width
                                    } ?: 1f
                                    strokes += PendingStroke(
                                        action = createAction(
                                            imagePoints.toList(),
                                            activeStrokeColor,
                                            activeStrokeWidth * viewToImageScale,
                                        ),
                                        previewPoints = previewPoints,
                                        color = activeStrokeColor,
                                        previewStrokeWidth = activeStrokeWidth,
                                    )
                                    undoneStrokes = emptyList()
                                }
                                isDrawing = false
                                previewPoints = emptyList()
                                imagePoints = emptyList()
                            },
                            onDragCancel = {
                                isDrawing = false
                                previewPoints = emptyList()
                                imagePoints = emptyList()
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                if (!isDrawing) return@detectDragGestures

                                val info = imageInformation ?: return@detectDragGestures
                                previewPoints = previewPoints + change.position
                                imagePoints = imagePoints + info.viewOffsetToOriginalBitmap(
                                    change.position,
                                )
                            },
                        )
                    },
            ) {
                fun drawStroke(
                    points: List<Offset>,
                    strokeColor: Color,
                    previewStrokeWidth: Float,
                ) {
                    if (points.size == 1) {
                        drawCircle(
                            color = strokeColor,
                            radius = previewStrokeWidth / 2f,
                            center = points.first(),
                        )
                    } else if (points.size > 1) {
                        val path = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            points.drop(1).forEach { point ->
                                lineTo(point.x, point.y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = strokeColor,
                            style = Stroke(
                                width = previewStrokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                        )
                    }
                }

                clipRect(
                    imageInformation?.imageViewRect?.left ?: 0f,
                    imageInformation?.imageViewRect?.top ?: 0f,
                    imageInformation?.imageViewRect?.right ?: size.width,
                    imageInformation?.imageViewRect?.bottom ?: size.height,
                ) {
                    strokes.forEach {
                        drawStroke(it.previewPoints, it.color, it.previewStrokeWidth)
                    }
                    drawStroke(previewPoints, activeStrokeColor, activeStrokeWidth)
                }

                if (isAdjustingStrokeWidth) {
                    val center = imageInformation?.imageViewRect?.center ?: this.center
                    val radius = strokeWidth.dp.toPx() / 2f
                    drawCircle(
                        color = colorTransform(color),
                        radius = radius,
                        center = center,
                    )
                    drawCircle(
                        color = previewOutlineColor,
                        radius = radius,
                        center = center,
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }
            }
        }

        StrokeWidthControls(
            value = strokeWidth,
            onValueChange = { strokeWidth = it },
            valueRange = STROKE_WIDTH_RANGE,
            interactionSource = strokeWidthInteractionSource,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )

        StrokeColorPalette(
            selectedColor = color,
            onColorSelected = { color = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )

        CanvasBottomBar(
            onConfirm = {
                strokes.forEach { onAddAction(it.action) }
                onCancel()
            },
            onCancel = onCancel,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ActionButton(
                    canDo = strokes.isNotEmpty(),
                    onDo = {
                        undoneStrokes += strokes.last()
                        strokes = strokes.dropLast(1)
                    },
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = R.string.undo,
                )
                ActionButton(
                    canDo = undoneStrokes.isNotEmpty(),
                    onDo = {
                        strokes += undoneStrokes.last()
                        undoneStrokes = undoneStrokes.dropLast(1)
                    },
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = R.string.redo,
                )
            }
        }
    }
}

/**
 * A stroke drawn in this screen that hasn't been committed yet.
 */
private data class PendingStroke(
    val action: Action,
    val previewPoints: List<Offset>,
    val color: Color,
    val previewStrokeWidth: Float,
)
