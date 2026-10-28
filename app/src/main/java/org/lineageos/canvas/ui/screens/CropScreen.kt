/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.screens

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toRect
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import kotlin.math.min
import org.lineageos.canvas.ext.constrainCrop
import org.lineageos.canvas.ext.drawCropOverlay
import org.lineageos.canvas.ext.moveCrop
import org.lineageos.canvas.ext.resizeCrop
import org.lineageos.canvas.ext.roundInwards
import org.lineageos.canvas.ext.straightenCropBounds
import org.lineageos.canvas.models.Action
import org.lineageos.canvas.models.Handle
import org.lineageos.canvas.ui.LocalSharedTransitionScope
import org.lineageos.canvas.ui.composables.CanvasBottomBar
import org.lineageos.canvas.ui.composables.CanvasImage
import org.lineageos.canvas.ui.composables.StraightenDial
import org.lineageos.canvas.ui.theme.CropOverlayStyle
import org.lineageos.canvas.ui.theme.defaultCropOverlayStyle

@Composable
fun CropScreen(
    imageBitmap: ImageBitmap,
    initialCropRect: IntRect?,
    onConfirm: (Action?) -> Unit,
    onCancel: () -> Unit,
) {
    val imageBounds = IntRect(0, 0, imageBitmap.width, imageBitmap.height)
    val imageSize = Size(imageBitmap.width.toFloat(), imageBitmap.height.toFloat())
    var degrees by remember(imageBitmap) { mutableFloatStateOf(0f) }
    var cropRect by remember(imageBitmap) {
        mutableStateOf((initialCropRect ?: imageBounds).toRect())
    }
    var requestedCropRect by remember(imageBitmap) { mutableStateOf(cropRect) }
    var isAutoCrop by remember(imageBitmap) {
        mutableStateOf(initialCropRect == null || initialCropRect == imageBounds)
    }
    var dragStartCropRect by remember { mutableStateOf<Rect?>(null) }
    var imageViewRect by remember { mutableStateOf<Rect?>(null) }
    val minimumCropSizePx = with(LocalDensity.current) { 100.dp.toPx() }
    val paddingPx = with(LocalDensity.current) { 16.dp.toPx() }

    Column(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clipToBounds(),
            contentAlignment = Alignment.Center,
        ) {
            val availableWidth = constraints.maxWidth - 2 * paddingPx
            val availableHeight = constraints.maxHeight - 2 * paddingPx

            if (imageBitmap.width > 0 && imageBitmap.height > 0 &&
                availableWidth > 0 && availableHeight > 0
            ) {
                val scale = min(
                    availableWidth / imageSize.width,
                    availableHeight / imageSize.height,
                )

                CanvasImage(
                    imageBitmap = imageBitmap,
                    cropRect = null,
                    modifier = Modifier.size(
                        width = with(LocalDensity.current) { (imageSize.width * scale).toDp() },
                        height = with(LocalDensity.current) { (imageSize.height * scale).toDp() },
                    ),
                    transform = { rotationZ = degrees },
                ) { imageViewRect = it.imageViewRect }

                imageViewRect?.let { imageViewRect ->
                    val viewScale = imageViewRect.width / imageSize.width

                    fun imageToView(rect: Rect) = Rect(
                        topLeft = imageViewRect.topLeft + rect.topLeft * viewScale,
                        bottomRight = imageViewRect.topLeft + rect.bottomRight * viewScale,
                    )

                    CropOverlay(
                        cropRect = imageToView(cropRect),
                        onDragStart = { dragStartCropRect = cropRect },
                        onDrag = { handle, totalDrag ->
                            val startCropRect = dragStartCropRect ?: return@CropOverlay
                            val imageDrag = totalDrag / viewScale

                            isAutoCrop = false
                            cropRect = if (handle == Handle.CENTER) {
                                moveCrop(
                                    crop = cropRect,
                                    offset = startCropRect.topLeft + imageDrag - cropRect.topLeft,
                                    imageSize = imageSize,
                                    degrees = degrees,
                                )
                            } else {
                                resizeCrop(
                                    old = cropRect,
                                    new = startCropRect.resizedBy(
                                        handle,
                                        imageDrag,
                                        minimumCropSizePx / viewScale,
                                    ),
                                    imageSize = imageSize,
                                    degrees = degrees,
                                )
                            }
                            requestedCropRect = cropRect
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        StraightenDial(
            value = degrees,
            onValueChange = { newDegrees ->
                cropRect = if (isAutoCrop) {
                    straightenCropBounds(imageSize, newDegrees)
                } else {
                    constrainCrop(requestedCropRect, imageSize, newDegrees)
                }
                degrees = newDegrees
            },
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        CanvasBottomBar(
            onConfirm = {
                val rect = cropRect.roundInwards()
                onConfirm(Action.Transformation.Crop(rect, degrees).takeIf {
                    degrees != 0f || rect != imageBounds
                })
            },
            onCancel = onCancel,
        )
    }
}

@Composable
private fun CropOverlay(
    cropRect: Rect,
    onDragStart: () -> Unit,
    onDrag: (handle: Handle, totalDrag: Offset) -> Unit,
    modifier: Modifier = Modifier,
    style: CropOverlayStyle = defaultCropOverlayStyle(),
) {
    var activeHandle by remember { mutableStateOf<Handle?>(null) }
    var totalDrag by remember { mutableStateOf(Offset.Zero) }
    val currentCropRect by rememberUpdatedState(cropRect)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)

    val handleHitRadiusPx = with(LocalDensity.current) {
        maxOf(style.handleRadius + 8.dp, 40.dp).toPx()
    }

    val transitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedContentScope.current
    val overlayModifier = with(transitionScope) {
        modifier.renderInSharedTransitionScopeOverlay(zIndexInOverlay = 1f)
    }
    val animatedOverlayModifier = with(animatedVisibilityScope) {
        overlayModifier.animateEnterExit(enter = fadeIn(), exit = fadeOut())
    }

    Canvas(
        modifier = animatedOverlayModifier
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        activeHandle = getHandleForOffset(
                            offset = offset,
                            rect = currentCropRect,
                            hitRadius = handleHitRadiusPx,
                        )
                        totalDrag = Offset.Zero
                        currentOnDragStart()
                    },
                    onDragEnd = {
                        activeHandle = null
                    },
                    onDragCancel = {
                        activeHandle = null
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()

                        totalDrag += dragAmount
                        activeHandle?.let { currentOnDrag(it, totalDrag) }
                    },
                )
            },
    ) {
        drawCropOverlay(
            rect = currentCropRect,
            showHandles = true,
            style = style,
        )
    }
}

/**
 * Move the edges of this rectangle selected by [handle] by [drag], without making it smaller than
 * [requestedMinimumSize] (or its current size, if already smaller).
 */
private fun Rect.resizedBy(handle: Handle, drag: Offset, requestedMinimumSize: Float): Rect {
    val minWidth = minOf(requestedMinimumSize, width)
    val minHeight = minOf(requestedMinimumSize, height)

    return Rect(
        left = if (handle.movesLeft) minOf(left + drag.x, right - minWidth) else left,
        top = if (handle.movesTop) minOf(top + drag.y, bottom - minHeight) else top,
        right = if (handle.movesRight) maxOf(right + drag.x, left + minWidth) else right,
        bottom = if (handle.movesBottom) maxOf(bottom + drag.y, top + minHeight) else bottom,
    )
}

private fun getHandleForOffset(offset: Offset, rect: Rect, hitRadius: Float): Handle? {
    val handles = listOf(
        Handle.TOP_LEFT to Offset(rect.left, rect.top),
        Handle.TOP to Offset(rect.center.x, rect.top),
        Handle.TOP_RIGHT to Offset(rect.right, rect.top),
        Handle.LEFT to Offset(rect.left, rect.center.y),
        Handle.RIGHT to Offset(rect.right, rect.center.y),
        Handle.BOTTOM_LEFT to Offset(rect.left, rect.bottom),
        Handle.BOTTOM to Offset(rect.center.x, rect.bottom),
        Handle.BOTTOM_RIGHT to Offset(rect.right, rect.bottom),
    )
    val hitRadiusSquared = hitRadius * hitRadius
    val closestHandle = handles.minByOrNull { (_, position) ->
        val dx = offset.x - position.x
        val dy = offset.y - position.y
        dx * dx + dy * dy
    }

    if (closestHandle != null) {
        val (handle, position) = closestHandle
        val dx = offset.x - position.x
        val dy = offset.y - position.y
        if (dx * dx + dy * dy <= hitRadiusSquared) return handle
    }

    return Handle.CENTER.takeIf { rect.contains(offset) }
}
