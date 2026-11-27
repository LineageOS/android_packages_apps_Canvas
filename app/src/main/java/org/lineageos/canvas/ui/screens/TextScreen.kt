/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import org.lineageos.canvas.R
import org.lineageos.canvas.ext.drawTextLabel
import org.lineageos.canvas.ext.labelPadding
import org.lineageos.canvas.ext.measureLabel
import org.lineageos.canvas.models.Action
import org.lineageos.canvas.models.TextBackground
import org.lineageos.canvas.ui.composables.ActionButton
import org.lineageos.canvas.ui.composables.CanvasBottomBar
import org.lineageos.canvas.ui.composables.CanvasImage
import org.lineageos.canvas.ui.composables.ImageInformation
import org.lineageos.canvas.ui.composables.TextEditorOverlay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Font size of new labels, as shown on screen.
 */
private const val DEFAULT_FONT_SIZE_SP = 32f

/**
 * Smallest font size a label can be pinched to, as shown on screen.
 */
private const val MIN_FONT_SIZE_SP = 8f

/**
 * Extra hit area around labels, so that small ones can still be grabbed.
 */
private val TOUCH_TOLERANCE = 16.dp

/**
 * Add labels on top of the image. Labels stay editable until confirmed: tapping one selects it,
 * tapping it again edits it, dragging moves it and pinching resizes and rotates it.
 */
@Composable
fun TextScreen(
    imageBitmap: ImageBitmap,
    cropRect: IntRect?,
    onAddAction: (Action) -> Unit,
    onCancel: () -> Unit,
) {
    var imageInformation by remember { mutableStateOf<ImageInformation?>(null) }
    var labels by remember { mutableStateOf(emptyList<PendingLabel>()) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var editor by remember { mutableStateOf<LabelEditor?>(null) }

    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val selectionColor = MaterialTheme.colorScheme.primary

    val imageInfo = imageInformation
    val imagePixelsPerSp = imageInfo?.let {
        density.density * density.fontScale / it.bitmapToViewScale
    } ?: 1f

    BackHandler(enabled = editor != null || selectedIndex != null) {
        when (editor) {
            null -> selectedIndex = null
            else -> editor = null
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            CanvasImage(
                imageBitmap = imageBitmap,
                cropRect = cropRect,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) { imageInformation = it }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(imageInformation, editor) {
                        // The editor overlay doesn't consume all touches
                        if (editor != null) return@pointerInput

                        val info = imageInformation ?: return@pointerInput
                        val scale = info.bitmapToViewScale
                        val tolerance = TOUCH_TOLERANCE.toPx() / scale

                        fun labelAt(point: Offset) = labels.indexOfLast {
                            it.contains(point, it.measure(textMeasurer), tolerance)
                        }.takeIf { it >= 0 }

                        awaitEachGesture {
                            val down = awaitFirstDown()
                            val downPoint = info.viewToBitmap(down.position)
                            val previouslySelectedIndex = selectedIndex
                            var targetIndex = labelAt(downPoint)
                            var isTransforming = false
                            var panSinceDown = Offset.Zero

                            do {
                                val event = awaitPointerEvent()
                                val pointerCount = event.changes.count { it.pressed }

                                // A second finger can pinch the selected label from anywhere
                                if (targetIndex == null && pointerCount > 1) {
                                    targetIndex = selectedIndex
                                }

                                val pan = event.calculatePan()
                                val zoom = event.calculateZoom()
                                val rotation = event.calculateRotation()

                                if (!isTransforming) {
                                    panSinceDown += pan
                                    isTransforming = pointerCount > 1 ||
                                            panSinceDown.getDistance() > viewConfiguration.touchSlop
                                    if (isTransforming) {
                                        targetIndex?.let { selectedIndex = it }
                                    }
                                }

                                if (isTransforming) {
                                    targetIndex?.let { index ->
                                        labels = labels.toMutableList().apply {
                                            this[index] = this[index].transform(
                                                pan = pan / scale,
                                                zoom = zoom,
                                                rotation = rotation,
                                                minFontSize = MIN_FONT_SIZE_SP * imagePixelsPerSp,
                                                bounds = info.bitmapSize.toSize(),
                                            )
                                        }
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            } while (event.changes.any { it.pressed })

                            if (!isTransforming) {
                                when (val index = targetIndex) {
                                    null -> when {
                                        selectedIndex != null -> selectedIndex = null
                                        info.imageViewRect.contains(down.position) -> {
                                            editor = LabelEditor.New(downPoint)
                                        }
                                    }

                                    previouslySelectedIndex -> editor = LabelEditor.Existing(index)
                                    else -> selectedIndex = index
                                }
                            }
                        }
                    },
            ) {
                val info = imageInformation ?: return@Canvas
                val scale = info.bitmapToViewScale
                val origin = info.bitmapOffsetToView(IntOffset.Zero)

                clipRect(
                    info.imageViewRect.left,
                    info.imageViewRect.top,
                    info.imageViewRect.right,
                    info.imageViewRect.bottom,
                ) {
                    // Draw in image pixels, exactly like the final rendering
                    translate(origin.x, origin.y) {
                        scale(scale, pivot = Offset.Zero) {
                            labels.forEachIndexed { index, label ->
                                val layout = label.measure(textMeasurer)

                                drawTextLabel(
                                    textLayoutResult = layout,
                                    topLeft = label.topLeft(layout),
                                    rotation = label.rotation,
                                    backgroundColor = label.background.backgroundColor(
                                        label.style.color
                                    ),
                                )

                                if (index == selectedIndex) {
                                    val padding = layout.labelPadding
                                    val size = layout.size.toSize()

                                    rotate(label.rotation, pivot = label.center) {
                                        drawRect(
                                            color = selectionColor,
                                            topLeft = label.topLeft(layout) -
                                                    Offset(padding, padding),
                                            size = Size(
                                                size.width + padding * 2,
                                                size.height + padding * 2,
                                            ),
                                            style = Stroke(width = 2.dp.toPx() / scale),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            when (val currentEditor = editor) {
                null -> {}

                is LabelEditor.New -> TextEditorOverlay(
                    onDismiss = { editor = null },
                    onConfirm = { text, textStyle, background ->
                        labels = labels + PendingLabel(
                            text = text,
                            style = textStyle.copy(
                                fontSize = (DEFAULT_FONT_SIZE_SP * imagePixelsPerSp).sp,
                            ),
                            background = background,
                            center = currentEditor.center,
                        )
                        selectedIndex = labels.lastIndex
                        editor = null
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                is LabelEditor.Existing -> {
                    val label = labels[currentEditor.index]

                    TextEditorOverlay(
                        onDismiss = { editor = null },
                        onConfirm = { text, textStyle, background ->
                            labels = labels.toMutableList().apply {
                                this[currentEditor.index] = label.copy(
                                    text = text,
                                    // Keep the size the label was pinched to
                                    style = textStyle.copy(fontSize = label.style.fontSize),
                                    background = background,
                                )
                            }
                            editor = null
                        },
                        modifier = Modifier.fillMaxSize(),
                        initialText = label.text,
                        initialStyle = label.style,
                        initialBackground = label.background,
                    )
                }
            }
        }

        CanvasBottomBar(
            onConfirm = {
                labels.forEach { onAddAction(it.toAction(it.measure(textMeasurer))) }
                onCancel()
            },
            onCancel = onCancel,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ActionButton(
                    canDo = selectedIndex != null,
                    onDo = { editor = selectedIndex?.let(LabelEditor::Existing) },
                    imageVector = Icons.Filled.Edit,
                    contentDescription = R.string.text_edit,
                )
                ActionButton(
                    canDo = selectedIndex != null,
                    onDo = {
                        selectedIndex?.let { index ->
                            labels = labels.filterIndexed { i, _ -> i != index }
                        }
                        selectedIndex = null
                    },
                    imageVector = Icons.Filled.Delete,
                    contentDescription = R.string.text_delete,
                )
            }
        }
    }
}

/**
 * What the text editor is open for.
 */
private sealed interface LabelEditor {
    /**
     * Write a new label.
     *
     * @param center The center of the label in image pixels
     */
    data class New(val center: Offset) : LabelEditor

    /**
     * Edit a pending label.
     *
     * @param index The index of the label
     */
    data class Existing(val index: Int) : LabelEditor
}

/**
 * A label added in this screen that hasn't been committed yet. Its geometry is kept as floats in
 * image pixels so that gestures don't accumulate rounding errors.
 *
 * @param text The text of the label
 * @param style The text style, with font size stored in image pixels and the picked color
 * @param background The background drawn behind the text
 * @param center The center of the label in image pixels
 * @param rotation The clockwise rotation in degrees around the center
 */
private data class PendingLabel(
    val text: String,
    val style: TextStyle,
    val background: TextBackground,
    val center: Offset,
    val rotation: Float = 0f,
) {
    fun measure(textMeasurer: TextMeasurer) = textMeasurer.measureLabel(text, style, background)

    fun topLeft(layout: TextLayoutResult) = center - layout.size.center.toOffset()

    /**
     * Whether [point] lies within this label, once rotated back to the label's axes.
     */
    fun contains(point: Offset, layout: TextLayoutResult, tolerance: Float): Boolean {
        val radians = Math.toRadians(rotation.toDouble())
        val delta = point - center
        val x = delta.x * cos(radians) + delta.y * sin(radians)
        val y = -delta.x * sin(radians) + delta.y * cos(radians)
        val margin = layout.labelPadding + tolerance

        return x in -(layout.size.width / 2f + margin)..(layout.size.width / 2f + margin) &&
                y in -(layout.size.height / 2f + margin)..(layout.size.height / 2f + margin)
    }

    /**
     * Apply a gesture, keeping the center within the image.
     */
    fun transform(
        pan: Offset,
        zoom: Float,
        rotation: Float,
        minFontSize: Float,
        bounds: Size,
    ) = copy(
        style = style.copy(
            fontSize = (style.fontSize.value * zoom).coerceIn(
                minFontSize,
                maxOf(minFontSize, bounds.maxDimension),
            ).sp,
        ),
        center = (center + pan).let {
            Offset(it.x.coerceIn(0f, bounds.width), it.y.coerceIn(0f, bounds.height))
        },
        rotation = (this.rotation + rotation) % 360f,
    )

    fun toAction(layout: TextLayoutResult) = Action.Drawing.Text(
        text = text,
        position = topLeft(layout).round(),
        style = style,
        rotation = rotation,
        background = background,
    )
}

/**
 * Scale from image pixels to view pixels.
 */
private val ImageInformation.bitmapToViewScale: Float
    get() = imageViewRect.width / bitmapSize.width

/**
 * Convert a point in the view to a point in image pixels, without rounding.
 */
private fun ImageInformation.viewToBitmap(offset: Offset) =
    (offset - bitmapOffsetToView(IntOffset.Zero)) / bitmapToViewScale
