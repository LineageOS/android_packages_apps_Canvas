/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Square
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.lineageos.canvas.R
import org.lineageos.canvas.ext.blurredPreview
import org.lineageos.canvas.models.Action
import org.lineageos.canvas.models.RedactMode
import org.lineageos.canvas.ui.composables.ActionButton
import org.lineageos.canvas.ui.composables.CanvasBottomBar
import org.lineageos.canvas.ui.composables.EditableShape
import org.lineageos.canvas.ui.composables.ShapeEditor
import org.lineageos.canvas.ui.composables.rememberShapeEditorState
import org.lineageos.canvas.ui.composables.viewRect
import kotlin.math.roundToInt

@Composable
fun RedactScreen(
    imageBitmap: ImageBitmap,
    onAddAction: (Action) -> Unit,
    onCancel: () -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf(RedactMode.BLUR) }
    val state = rememberShapeEditorState<RedactArea>()
    val blurredBitmap by produceState<ImageBitmap?>(null, imageBitmap) {
        value = withContext(Dispatchers.Default) { imageBitmap.blurredPreview() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ShapeEditor(
            imageBitmap = imageBitmap,
            state = state,
            createShape = { point, _ -> RedactArea(point, point, mode) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { info ->
            val scale = blurredBitmap?.let { it.width.toFloat() / imageBitmap.width } ?: 1f

            state.shapes.forEach { area ->
                val viewRect = area.viewRect(info)
                val dstOffset = viewRect.topLeft.round()
                val dstSize = IntSize(
                    viewRect.width.roundToInt().coerceAtLeast(1),
                    viewRect.height.roundToInt().coerceAtLeast(1),
                )

                when (val blurred = blurredBitmap?.takeIf { area.mode == RedactMode.BLUR }) {
                    null -> drawRect(
                        color = Color.Black,
                        topLeft = viewRect.topLeft,
                        size = viewRect.size,
                    )

                    else -> {
                        val rect = area.rect
                        val srcOffset = IntOffset(
                            (rect.left * scale).roundToInt().coerceIn(0, blurred.width - 1),
                            (rect.top * scale).roundToInt().coerceIn(0, blurred.height - 1),
                        )
                        val srcSize = IntSize(
                            (rect.width * scale).roundToInt()
                                .coerceIn(1, blurred.width - srcOffset.x),
                            (rect.height * scale).roundToInt()
                                .coerceIn(1, blurred.height - srcOffset.y),
                        )

                        drawImage(
                            image = blurred,
                            srcOffset = srcOffset,
                            srcSize = srcSize,
                            dstOffset = dstOffset,
                            dstSize = dstSize,
                        )
                    }
                }
            }
        }

        RedactModeSelector(
            selectedMode = mode,
            onModeSelected = {
                mode = it
                state.updateSelected { area -> area.copy(mode = it) }
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
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RedactModeSelector(
    selectedMode: RedactMode,
    onModeSelected: (RedactMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modes = RedactMode.entries

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        modes.forEachIndexed { index, mode ->
            ToggleButton(
                checked = mode == selectedMode,
                onCheckedChange = { onModeSelected(mode) },
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
            ) {
                Icon(
                    imageVector = when (mode) {
                        RedactMode.BLUR -> Icons.Filled.BlurOn
                        RedactMode.MASK -> Icons.Filled.Square
                    },
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(
                    text = stringResource(
                        when (mode) {
                            RedactMode.BLUR -> R.string.redact_blur
                            RedactMode.MASK -> R.string.redact_mask
                        }
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

private data class RedactArea(
    override val start: IntOffset,
    override val end: IntOffset,
    val mode: RedactMode,
) : EditableShape<RedactArea> {
    override val isLinear = false
    override val strokeWidth = 0f

    val rect
        get() = IntRect(
            left = minOf(start.x, end.x),
            top = minOf(start.y, end.y),
            right = maxOf(start.x, end.x),
            bottom = maxOf(start.y, end.y),
        )

    override fun withPoints(start: IntOffset, end: IntOffset) = copy(start = start, end = end)

    fun toAction() = Action.Drawing.Redact(rect = rect, mode = mode)
}
