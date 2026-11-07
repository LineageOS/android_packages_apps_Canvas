/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.ChangeHistory
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.Rectangle
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ToggleButton
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.lineageos.canvas.R
import org.lineageos.canvas.ext.drawHandles
import org.lineageos.canvas.ext.drawSelectionOverlay
import org.lineageos.canvas.ext.drawShape
import org.lineageos.canvas.ext.getHandleForOffset
import org.lineageos.canvas.ext.updateRectWithDrag
import org.lineageos.canvas.ext.updateRectWithDragKeepingRatio
import org.lineageos.canvas.models.Action
import org.lineageos.canvas.models.Handle
import org.lineageos.canvas.models.ShapeType
import org.lineageos.canvas.ui.composables.ActionButton
import org.lineageos.canvas.ui.composables.CanvasBottomBar
import org.lineageos.canvas.ui.composables.CanvasImage
import org.lineageos.canvas.ui.composables.ImageInformation
import org.lineageos.canvas.ui.composables.StrokeColorPalette
import org.lineageos.canvas.ui.composables.StrokeWidthControls
import org.lineageos.canvas.ui.composables.ToolbarTooltip
import org.lineageos.canvas.ui.theme.defaultCropOverlayStyle
import kotlin.math.abs

private val DEFAULT_STROKE_WIDTH = 4.dp
private val STROKE_WIDTH_RANGE = 1f..48f
private val MINIMUM_SHAPE_SIZE = 16.dp

@Composable
fun ShapesScreen(
    imageBitmap: ImageBitmap,
    onAddAction: (Action) -> Unit,
    onCancel: () -> Unit,
) {
    var imageInformation by remember { mutableStateOf<ImageInformation?>(null) }
    var type by rememberSaveable { mutableStateOf(ShapeType.RECTANGLE) }
    var color by remember { mutableStateOf(Color.Red) }
    var filled by rememberSaveable { mutableStateOf(false) }
    var keepSquare by rememberSaveable { mutableStateOf(false) }
    var strokeWidth by rememberSaveable { mutableFloatStateOf(DEFAULT_STROKE_WIDTH.value) }
    var shapes by remember { mutableStateOf(emptyList<PendingShape>()) }
    var undoneShapes by remember { mutableStateOf(emptyList<PendingShape>()) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var gesture by remember { mutableStateOf<ShapeGesture?>(null) }
    var createOrigin by remember { mutableStateOf(Offset.Zero) }
    val strokeWidthInteractionSource = remember { MutableInteractionSource() }

    val style = defaultCropOverlayStyle()
    val density = LocalDensity.current
    val hitRadiusPx = with(density) { maxOf(style.handleRadius + 8.dp, 32.dp).toPx() }
    val minimumShapeSizePx = with(density) { MINIMUM_SHAPE_SIZE.toPx() }
    val handleRadiusPx = with(density) { style.handleRadius.toPx() }

    fun imageStrokeWidth(info: ImageInformation?) = with(density) {
        strokeWidth.dp.toPx() * (info?.viewToImageScale ?: 1f)
    }

    fun updateSelected(transform: (PendingShape) -> PendingShape) {
        selectedIndex?.let { index ->
            shapes = shapes.toMutableList().also { it[index] = transform(it[index]) }
        }
    }

    fun shapeIndexAt(info: ImageInformation, offset: Offset) = shapes.indices.lastOrNull {
        shapes[it].contains(info, offset, hitRadiusPx / 2)
    }

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
                        detectTapGestures { offset ->
                            val info = imageInformation ?: return@detectTapGestures
                            selectedIndex = shapeIndexAt(info, offset)
                        }
                    }
                    .pointerInput(imageInformation) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val info = imageInformation ?: return@detectDragGestures

                                gesture = selectedIndex?.let {
                                    shapes[it].hitTest(info, offset, hitRadiusPx, handleRadiusPx)
                                }

                                if (gesture == null) {
                                    shapeIndexAt(info, offset)?.let {
                                        selectedIndex = it
                                        gesture = shapes[it].moveGesture
                                    }
                                }

                                if (gesture == null && info.imageViewRect.contains(offset)) {
                                    val point = info.viewOffsetToOriginalBitmap(offset)
                                    shapes += PendingShape(
                                        type = type,
                                        start = point,
                                        end = point,
                                        color = color,
                                        filled = filled,
                                        strokeWidth = imageStrokeWidth(info),
                                    )
                                    undoneShapes = emptyList()
                                    selectedIndex = shapes.lastIndex
                                    createOrigin = offset
                                    gesture = ShapeGesture.Create
                                }
                            },
                            onDragEnd = {
                                val info = imageInformation
                                val shape = selectedIndex?.let(shapes::getOrNull)
                                if (gesture == ShapeGesture.Create && info != null && shape != null) {
                                    val rect = shape.viewRect(info)
                                    if (rect.width < minimumShapeSizePx &&
                                        rect.height < minimumShapeSizePx
                                    ) {
                                        shapes = shapes.dropLast(1)
                                        selectedIndex = null
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
                                val index = selectedIndex ?: return@detectDragGestures
                                val shape = shapes.getOrNull(index) ?: return@detectDragGestures
                                val bounds = info.imageViewRect

                                val newShape = when (val currentGesture = gesture) {
                                    null -> return@detectDragGestures

                                    ShapeGesture.Create -> {
                                        val end = when (keepSquare && !shape.type.isLinear) {
                                            true -> squareEnd(createOrigin, change.position, bounds)
                                            false -> change.position.coerceIn(bounds)
                                        }
                                        shape.copy(end = info.viewOffsetToOriginalBitmap(end))
                                    }

                                    is ShapeGesture.Resize -> {
                                        val rect = shape.viewRect(info)
                                        val newRect = when (
                                            keepSquare && currentGesture.handle != Handle.CENTER
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

                                    ShapeGesture.MoveStart -> shape.copy(
                                        start = info.viewOffsetToOriginalBitmap(
                                            change.position.coerceIn(bounds)
                                        ),
                                    )

                                    ShapeGesture.MoveEnd -> shape.copy(
                                        end = info.viewOffsetToOriginalBitmap(
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
                                        shape.copy(
                                            start = info.viewOffsetToOriginalBitmap(start + delta),
                                            end = info.viewOffsetToOriginalBitmap(end + delta),
                                        )
                                    }
                                }

                                shapes = shapes.toMutableList().also { it[index] = newShape }
                            },
                        )
                    },
            ) {
                val info = imageInformation ?: return@Canvas
                val bounds = info.imageViewRect

                clipRect(bounds.left, bounds.top, bounds.right, bounds.bottom) {
                    shapes.forEach { shape ->
                        drawShape(
                            type = shape.type,
                            start = info.bitmapOffsetToView(shape.start),
                            end = info.bitmapOffsetToView(shape.end),
                            color = shape.color,
                            filled = shape.filled,
                            strokeWidth = shape.strokeWidth / info.viewToImageScale,
                        )
                    }
                }

                selectedIndex?.let(shapes::getOrNull)
                    ?.takeIf { gesture != ShapeGesture.Create }
                    ?.let { shape ->
                        when (shape.type.isLinear) {
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

        ShapeTypeSelector(
            selectedType = type,
            onTypeSelected = {
                type = it
                selectedIndex = null
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )

        StrokeWidthControls(
            value = strokeWidth,
            onValueChange = {
                strokeWidth = it
                val width = imageStrokeWidth(imageInformation)
                updateSelected { shape -> shape.copy(strokeWidth = width) }
            },
            valueRange = STROKE_WIDTH_RANGE,
            interactionSource = strokeWidthInteractionSource,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )

        StrokeColorPalette(
            selectedColor = color,
            onColorSelected = {
                color = it
                updateSelected { shape -> shape.copy(color = it) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )

        CanvasBottomBar(
            onConfirm = {
                shapes.forEach { onAddAction(it.toAction()) }
                onCancel()
            },
            onCancel = onCancel,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ActionButton(
                    canDo = shapes.isNotEmpty(),
                    onDo = {
                        undoneShapes += shapes.last()
                        shapes = shapes.dropLast(1)
                        selectedIndex = null
                    },
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = R.string.undo,
                )
                ActionButton(
                    canDo = undoneShapes.isNotEmpty(),
                    onDo = {
                        shapes += undoneShapes.last()
                        undoneShapes = undoneShapes.dropLast(1)
                        selectedIndex = null
                    },
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = R.string.redo,
                )
                ShapeOptionToggle(
                    checked = filled,
                    onCheckedChange = {
                        filled = it
                        updateSelected { shape -> shape.copy(filled = it) }
                    },
                    enabled = !type.isLinear,
                    imageVector = Icons.Filled.FormatColorFill,
                    description = stringResource(R.string.shape_filled),
                )
                ShapeOptionToggle(
                    checked = keepSquare,
                    onCheckedChange = {
                        keepSquare = it
                        val info = imageInformation
                        if (it && info != null) {
                            updateSelected { shape ->
                                when (shape.type.isLinear) {
                                    true -> shape
                                    false -> shape.withViewRect(info, shape.viewRect(info).square())
                                }
                            }
                        }
                    },
                    enabled = !type.isLinear,
                    imageVector = Icons.Outlined.CropSquare,
                    description = stringResource(R.string.shape_keep_square),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ShapeTypeSelector(
    selectedType: ShapeType,
    onTypeSelected: (ShapeType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val types = ShapeType.entries

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        types.forEachIndexed { index, type ->
            val description = stringResource(type.label)

            ToggleButton(
                checked = type == selectedType,
                onCheckedChange = { onTypeSelected(type) },
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        role = Role.RadioButton
                        contentDescription = description
                    },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    types.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
            ) {
                Icon(
                    imageVector = type.icon,
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun ShapeOptionToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
    imageVector: ImageVector,
    description: String,
) {
    ToolbarTooltip(description) {
        FilledTonalIconToggleButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = description,
            )
        }
    }
}

private data class PendingShape(
    val type: ShapeType,
    val start: IntOffset,
    val end: IntOffset,
    val color: Color,
    val filled: Boolean,
    val strokeWidth: Float,
) {
    fun toAction() = Action.Drawing.Shape(
        type = type,
        start = start,
        end = end,
        color = color,
        filled = filled,
        strokeWidth = strokeWidth,
    )

    fun viewRect(info: ImageInformation): Rect {
        val start = info.bitmapOffsetToView(start)
        val end = info.bitmapOffsetToView(end)

        return Rect(
            left = minOf(start.x, end.x),
            top = minOf(start.y, end.y),
            right = maxOf(start.x, end.x),
            bottom = maxOf(start.y, end.y),
        )
    }

    fun withViewRect(info: ImageInformation, rect: Rect) = copy(
        start = info.viewOffsetToOriginalBitmap(rect.topLeft),
        end = info.viewOffsetToOriginalBitmap(rect.bottomRight),
    )

    val moveGesture
        get() = when (type.isLinear) {
            true -> ShapeGesture.MoveLine
            false -> ShapeGesture.Resize(Handle.CENTER)
        }

    fun contains(info: ImageInformation, offset: Offset, tolerance: Float) = when (type.isLinear) {
        true -> offset.distanceToSegment(
            info.bitmapOffsetToView(start),
            info.bitmapOffsetToView(end),
        ) <= tolerance + strokeWidth / info.viewToImageScale / 2

        false -> viewRect(info).inflate(tolerance).contains(offset)
    }

    fun hitTest(
        info: ImageInformation,
        offset: Offset,
        hitRadius: Float,
        minHitRadius: Float,
    ): ShapeGesture? {
        if (!type.isLinear) {
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
}

private sealed interface ShapeGesture {
    data object Create : ShapeGesture
    data class Resize(val handle: Handle) : ShapeGesture
    data object MoveStart : ShapeGesture
    data object MoveEnd : ShapeGesture
    data object MoveLine : ShapeGesture
}

private val ShapeType.label
    get() = when (this) {
        ShapeType.RECTANGLE -> R.string.shape_rectangle
        ShapeType.OVAL -> R.string.shape_oval
        ShapeType.TRIANGLE -> R.string.shape_triangle
        ShapeType.STAR -> R.string.shape_star
        ShapeType.LINE -> R.string.shape_line
        ShapeType.ARROW -> R.string.shape_arrow
    }

private val ShapeType.icon
    get() = when (this) {
        ShapeType.RECTANGLE -> Icons.Outlined.Rectangle
        ShapeType.OVAL -> Icons.Outlined.Circle
        ShapeType.TRIANGLE -> Icons.Outlined.ChangeHistory
        ShapeType.STAR -> Icons.Filled.StarBorder
        ShapeType.LINE -> Icons.Filled.HorizontalRule
        ShapeType.ARROW -> Icons.Filled.ArrowOutward
    }

private val ImageInformation.viewToImageScale
    get() = bitmapSize.width / imageViewRect.width

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
