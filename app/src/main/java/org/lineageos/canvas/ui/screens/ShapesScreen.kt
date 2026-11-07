/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.screens

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.lineageos.canvas.R
import org.lineageos.canvas.ext.drawShape
import org.lineageos.canvas.models.Action
import org.lineageos.canvas.models.ShapeType
import org.lineageos.canvas.ui.composables.ActionButton
import org.lineageos.canvas.ui.composables.CanvasBottomBar
import org.lineageos.canvas.ui.composables.EditableShape
import org.lineageos.canvas.ui.composables.ImageInformation
import org.lineageos.canvas.ui.composables.ShapeEditor
import org.lineageos.canvas.ui.composables.StrokeColorPalette
import org.lineageos.canvas.ui.composables.StrokeWidthControls
import org.lineageos.canvas.ui.composables.ToolbarTooltip
import org.lineageos.canvas.ui.composables.rememberShapeEditorState
import org.lineageos.canvas.ui.composables.squared
import org.lineageos.canvas.ui.composables.viewToImageScale

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
    val state = rememberShapeEditorState<PendingShape>()
    val strokeWidthInteractionSource = remember { MutableInteractionSource() }

    val density = LocalDensity.current

    fun imageStrokeWidth(info: ImageInformation?) = with(density) {
        strokeWidth.dp.toPx() * (info?.viewToImageScale ?: 1f)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ShapeEditor(
            imageBitmap = imageBitmap,
            state = state,
            createShape = { point, info ->
                PendingShape(
                    type = type,
                    start = point,
                    end = point,
                    color = color,
                    filled = filled,
                    strokeWidth = imageStrokeWidth(info),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            keepSquare = keepSquare,
            onImageInformation = { imageInformation = it },
        ) { info ->
            state.shapes.forEach { shape ->
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

        ShapeTypeSelector(
            selectedType = type,
            onTypeSelected = {
                type = it
                state.selectedIndex = null
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
                state.updateSelected { shape -> shape.copy(strokeWidth = width) }
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
                state.updateSelected { shape -> shape.copy(color = it) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )

        CanvasBottomBar(
            onConfirm = {
                state.shapes.forEach { onAddAction(it.toAction()) }
                onCancel()
            },
            onCancel = onCancel,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ActionButton(
                    canDo = state.canUndo,
                    onDo = state::undo,
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = R.string.undo,
                )
                ActionButton(
                    canDo = state.canRedo,
                    onDo = state::redo,
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = R.string.redo,
                )
                ShapeOptionToggle(
                    checked = filled,
                    onCheckedChange = {
                        filled = it
                        state.updateSelected { shape -> shape.copy(filled = it) }
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
                            state.updateSelected { shape -> shape.squared(info) }
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
    override val start: IntOffset,
    override val end: IntOffset,
    val color: Color,
    val filled: Boolean,
    override val strokeWidth: Float,
) : EditableShape<PendingShape> {
    override val isLinear get() = type.isLinear

    override fun withPoints(start: IntOffset, end: IntOffset) = copy(start = start, end = end)

    fun toAction() = Action.Drawing.Shape(
        type = type,
        start = start,
        end = end,
        color = color,
        filled = filled,
        strokeWidth = strokeWidth,
    )
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
