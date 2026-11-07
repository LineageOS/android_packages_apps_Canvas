/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.lineageos.canvas.ext.drawHandles
import org.lineageos.canvas.ext.drawSelectionOverlay
import org.lineageos.canvas.ext.getHandleForOffset
import org.lineageos.canvas.ext.updateRectWithDrag
import org.lineageos.canvas.ext.updateRectWithDragKeepingRatio
import org.lineageos.canvas.models.Handle
import org.lineageos.canvas.ui.theme.defaultCropOverlayStyle
import kotlin.math.abs

private val MINIMUM_SHAPE_SIZE = 16.dp

interface EditableShape<T : EditableShape<T>> {
    val start: IntOffset
    val end: IntOffset
    val isLinear: Boolean
    val strokeWidth: Float

    fun withPoints(start: IntOffset, end: IntOffset): T
}

class ShapeEditorState<T : EditableShape<T>> {
    var shapes by mutableStateOf(emptyList<T>())
        private set
    var selectedIndex by mutableStateOf<Int?>(null)
    private var undoneShapes by mutableStateOf(emptyList<T>())

    val selectedShape get() = selectedIndex?.let(shapes::getOrNull)
    val canUndo get() = shapes.isNotEmpty()
    val canRedo get() = undoneShapes.isNotEmpty()

    fun add(shape: T) {
        shapes += shape
        undoneShapes = emptyList()
        selectedIndex = shapes.lastIndex
    }

    fun update(index: Int, shape: T) {
        shapes = shapes.toMutableList().also { it[index] = shape }
    }

    fun updateSelected(transform: (T) -> T) {
        selectedIndex?.let { update(it, transform(shapes[it])) }
    }

    fun removeLast() {
        shapes = shapes.dropLast(1)
        selectedIndex = null
    }

    fun undo() {
        undoneShapes += shapes.last()
        removeLast()
    }

    fun redo() {
        shapes += undoneShapes.last()
        undoneShapes = undoneShapes.dropLast(1)
        selectedIndex = null
    }
}

@Composable
fun <T : EditableShape<T>> rememberShapeEditorState() = remember { ShapeEditorState<T>() }

@Composable
fun <T : EditableShape<T>> ShapeEditor(
    imageBitmap: ImageBitmap,
    state: ShapeEditorState<T>,
    createShape: (point: IntOffset, info: ImageInformation) -> T,
    modifier: Modifier = Modifier,
    keepSquare: Boolean = false,
    onImageInformation: (ImageInformation) -> Unit = {},
    drawShapes: DrawScope.(ImageInformation) -> Unit,
) {
    var imageInformation by remember { mutableStateOf<ImageInformation?>(null) }
    var gesture by remember { mutableStateOf<ShapeGesture?>(null) }
    var createOrigin by remember { mutableStateOf(Offset.Zero) }

    val currentKeepSquare by rememberUpdatedState(keepSquare)
    val currentCreateShape by rememberUpdatedState(createShape)

    val style = defaultCropOverlayStyle()
    val density = LocalDensity.current
    val hitRadiusPx = with(density) { maxOf(style.handleRadius + 8.dp, 32.dp).toPx() }
    val minimumShapeSizePx = with(density) { MINIMUM_SHAPE_SIZE.toPx() }
    val handleRadiusPx = with(density) { style.handleRadius.toPx() }

    fun shapeIndexAt(info: ImageInformation, offset: Offset) =
        state.shapes.indices.lastOrNull {
            state.shapes[it].contains(info, offset, hitRadiusPx / 2)
        }

    Box(modifier = modifier) {
        CanvasImage(
            imageBitmap = imageBitmap,
            cropRect = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            imageInformation = it
            onImageInformation(it)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(imageInformation) {
                    detectTapGestures { offset ->
                        val info = imageInformation ?: return@detectTapGestures
                        state.selectedIndex = shapeIndexAt(info, offset)
                    }
                }
                .pointerInput(imageInformation) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val info = imageInformation ?: return@detectDragGestures

                            gesture = state.selectedShape?.hitTest(
                                info,
                                offset,
                                hitRadiusPx,
                                handleRadiusPx,
                            )

                            if (gesture == null) {
                                shapeIndexAt(info, offset)?.let {
                                    state.selectedIndex = it
                                    gesture = state.shapes[it].moveGesture
                                }
                            }

                            if (gesture == null && info.imageViewRect.contains(offset)) {
                                state.add(
                                    currentCreateShape(
                                        info.viewOffsetToOriginalBitmap(offset),
                                        info,
                                    )
                                )
                                createOrigin = offset
                                gesture = ShapeGesture.Create
                            }
                        },
                        onDragEnd = {
                            val info = imageInformation
                            val shape = state.selectedShape
                            if (gesture == ShapeGesture.Create && info != null && shape != null) {
                                val rect = shape.viewRect(info)
                                if (rect.width < minimumShapeSizePx &&
                                    rect.height < minimumShapeSizePx
                                ) {
                                    state.removeLast()
                                }
                            }
                            gesture = null
                        },
                        onDragCancel = {
                            gesture = null
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()

                            val info = imageInformation ?: return@detectDragGestures
                            val index = state.selectedIndex ?: return@detectDragGestures
                            val shape = state.shapes.getOrNull(index)
                                ?: return@detectDragGestures
                            val bounds = info.imageViewRect

                            val newShape = when (val currentGesture = gesture) {
                                null -> return@detectDragGestures

                                ShapeGesture.Create -> {
                                    val end = when (currentKeepSquare && !shape.isLinear) {
                                        true -> squareEnd(createOrigin, change.position, bounds)
                                        false -> change.position.coerceIn(bounds)
                                    }
                                    shape.withPoints(
                                        shape.start,
                                        info.viewOffsetToOriginalBitmap(end),
                                    )
                                }

                                is ShapeGesture.Resize -> {
                                    val rect = shape.viewRect(info)
                                    val newRect = when (
                                        currentKeepSquare && currentGesture.handle != Handle.CENTER
                                    ) {
                                        true -> updateRectWithDragKeepingRatio(
                                            rect,
                                            currentGesture.handle,
                                            dragAmount,
                                            bounds,
                                            minimumShapeSizePx,
                                            1f,
                                        )

                                        false -> updateRectWithDrag(
                                            rect,
                                            currentGesture.handle,
                                            dragAmount,
                                            bounds,
                                            minimumShapeSizePx,
                                        )
                                    }
                                    shape.withViewRect(info, newRect)
                                }

                                ShapeGesture.MoveStart -> shape.withPoints(
                                    info.viewOffsetToOriginalBitmap(
                                        change.position.coerceIn(bounds)
                                    ),
                                    shape.end,
                                )

                                ShapeGesture.MoveEnd -> shape.withPoints(
                                    shape.start,
                                    info.viewOffsetToOriginalBitmap(
                                        change.position.coerceIn(bounds)
                                    ),
                                )

                                ShapeGesture.MoveLine -> {
                                    val start = info.bitmapOffsetToView(shape.start)
                                    val end = info.bitmapOffsetToView(shape.end)
                                    val delta = Offset(
                                        dragAmount.x.coerceIn(
                                            bounds.left - minOf(start.x, end.x),
                                            bounds.right - maxOf(start.x, end.x),
                                        ),
                                        dragAmount.y.coerceIn(
                                            bounds.top - minOf(start.y, end.y),
                                            bounds.bottom - maxOf(start.y, end.y),
                                        ),
                                    )
                                    shape.withPoints(
                                        info.viewOffsetToOriginalBitmap(start + delta),
                                        info.viewOffsetToOriginalBitmap(end + delta),
                                    )
                                }
                            }

                            state.update(index, newShape)
                        },
                    )
                },
        ) {
            val info = imageInformation ?: return@Canvas
            val bounds = info.imageViewRect

            clipRect(bounds.left, bounds.top, bounds.right, bounds.bottom) {
                drawShapes(info)
            }

            state.selectedShape
                ?.takeIf { gesture != ShapeGesture.Create }
                ?.let { shape ->
                    when (shape.isLinear) {
                        true -> drawHandles(
                            listOf(
                                info.bitmapOffsetToView(shape.start),
                                info.bitmapOffsetToView(shape.end),
                            ),
                            style,
                        )

                        false -> drawSelectionOverlay(shape.viewRect(info), style)
                    }
                }
        }
    }
}

fun EditableShape<*>.viewRect(info: ImageInformation): Rect {
    val start = info.bitmapOffsetToView(start)
    val end = info.bitmapOffsetToView(end)

    return Rect(
        left = minOf(start.x, end.x),
        top = minOf(start.y, end.y),
        right = maxOf(start.x, end.x),
        bottom = maxOf(start.y, end.y),
    )
}

fun <T : EditableShape<T>> T.withViewRect(info: ImageInformation, rect: Rect) = withPoints(
    info.viewOffsetToOriginalBitmap(rect.topLeft),
    info.viewOffsetToOriginalBitmap(rect.bottomRight),
)

fun <T : EditableShape<T>> T.squared(info: ImageInformation) = when (isLinear) {
    true -> this
    false -> withViewRect(info, viewRect(info).square())
}

val ImageInformation.viewToImageScale
    get() = bitmapSize.width / imageViewRect.width

private sealed interface ShapeGesture {
    data object Create : ShapeGesture
    data class Resize(val handle: Handle) : ShapeGesture
    data object MoveStart : ShapeGesture
    data object MoveEnd : ShapeGesture
    data object MoveLine : ShapeGesture
}

private val EditableShape<*>.moveGesture
    get() = when (isLinear) {
        true -> ShapeGesture.MoveLine
        false -> ShapeGesture.Resize(Handle.CENTER)
    }

private fun EditableShape<*>.contains(
    info: ImageInformation,
    offset: Offset,
    tolerance: Float,
) = when (isLinear) {
    true -> offset.distanceToSegment(
        info.bitmapOffsetToView(start),
        info.bitmapOffsetToView(end),
    ) <= tolerance + strokeWidth / info.viewToImageScale / 2

    false -> viewRect(info).inflate(tolerance).contains(offset)
}

private fun EditableShape<*>.hitTest(
    info: ImageInformation,
    offset: Offset,
    hitRadius: Float,
    minHitRadius: Float,
): ShapeGesture? {
    if (!isLinear) {
        val rect = viewRect(info)
        val handleHitRadius = minOf(
            hitRadius,
            maxOf(minHitRadius, minOf(rect.width, rect.height) / 4),
        )
        return getHandleForOffset(offset, rect, handleHitRadius)
            ?.let(ShapeGesture::Resize)
    }

    val start = info.bitmapOffsetToView(start)
    val end = info.bitmapOffsetToView(end)

    return when {
        (offset - end).getDistance() <= hitRadius -> ShapeGesture.MoveEnd
        (offset - start).getDistance() <= hitRadius -> ShapeGesture.MoveStart
        offset.distanceToSegment(start, end) <= hitRadius -> ShapeGesture.MoveLine
        else -> null
    }
}

private fun Offset.coerceIn(bounds: Rect) = Offset(
    x.coerceIn(bounds.left, bounds.right),
    y.coerceIn(bounds.top, bounds.bottom),
)

private fun Offset.distanceToSegment(start: Offset, end: Offset): Float {
    val segment = end - start
    val lengthSquared = segment.x * segment.x + segment.y * segment.y
    if (lengthSquared == 0f) return (this - start).getDistance()

    val t = (((x - start.x) * segment.x + (y - start.y) * segment.y) / lengthSquared)
        .coerceIn(0f, 1f)

    return (this - (start + segment * t)).getDistance()
}

private fun squareEnd(origin: Offset, position: Offset, bounds: Rect): Offset {
    val dx = position.x - origin.x
    val dy = position.y - origin.y
    val signX = if (dx < 0f) -1f else 1f
    val signY = if (dy < 0f) -1f else 1f
    val maxX = if (signX < 0f) origin.x - bounds.left else bounds.right - origin.x
    val maxY = if (signY < 0f) origin.y - bounds.top else bounds.bottom - origin.y
    val side = minOf(maxOf(abs(dx), abs(dy)), maxX, maxY)

    return Offset(origin.x + signX * side, origin.y + signY * side)
}

private fun Rect.square(): Rect {
    val side = minOf(width, height)

    return Rect(
        left = center.x - side / 2,
        top = center.y - side / 2,
        right = center.x + side / 2,
        bottom = center.y + side / 2,
    )
}
