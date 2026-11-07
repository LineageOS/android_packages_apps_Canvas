/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.screens

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import org.lineageos.canvas.R
import org.lineageos.canvas.ext.drawCropOverlay
import org.lineageos.canvas.models.Action
import org.lineageos.canvas.models.CropAspectRatio
import org.lineageos.canvas.models.Handle
import org.lineageos.canvas.ui.LocalSharedTransitionScope
import org.lineageos.canvas.ui.composables.CanvasBottomBar
import org.lineageos.canvas.ui.composables.CanvasImage
import org.lineageos.canvas.ui.composables.CropAspectRatioSelector
import org.lineageos.canvas.ui.composables.ImageInformation
import org.lineageos.canvas.ui.composables.ToolbarTooltip
import org.lineageos.canvas.ui.theme.CropOverlayStyle
import org.lineageos.canvas.ui.theme.defaultCropOverlayStyle
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun CropScreen(
    imageBitmap: ImageBitmap,
    initialCropRect: IntRect?,
    onConfirm: (Action?) -> Unit,
    onCancel: () -> Unit,
) {
    var imageInformation by remember { mutableStateOf<ImageInformation?>(null) }
    val imageBounds = IntRect(0, 0, imageBitmap.width, imageBitmap.height)
    var cropRect by remember(imageBitmap) {
        mutableStateOf(initialCropRect ?: imageBounds)
    }
    var aspectRatio by remember { mutableStateOf(CropAspectRatio.FREE) }
    var isLandscape by remember(imageBitmap) {
        mutableStateOf(imageBitmap.width >= imageBitmap.height)
    }
    val ratio = aspectRatio.ratio(isLandscape)

    fun selectAspectRatio(newAspectRatio: CropAspectRatio, newIsLandscape: Boolean) {
        aspectRatio = newAspectRatio
        isLandscape = newIsLandscape
        newAspectRatio.ratio(newIsLandscape)?.let {
            cropRect = cropRect.fitAspectRatio(it, imageBounds)
        }
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

            imageInformation?.let { imageInformation ->
                CropOverlay(
                    imageBounds = imageInformation.imageViewRect,
                    cropRect = imageInformation.originalBitmapRectToViewRect(cropRect),
                    aspectRatio = ratio,
                    onCropRectChange = { rect ->
                        cropRect = imageInformation.viewRectToOriginalBitmap(rect)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        CropAspectRatioSelector(
            selectedAspectRatio = aspectRatio,
            isLandscape = isLandscape,
            onAspectRatioSelected = { selectAspectRatio(it, isLandscape) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )

        CanvasBottomBar(
            onConfirm = {
                if (imageInformation != null) {
                    onConfirm(cropRect.takeIf { it != imageBounds }?.let {
                        Action.Transformation.Crop(it)
                    })
                }
            },
            onCancel = onCancel,
        ) {
            val description = stringResource(R.string.crop_swap_orientation)
            ToolbarTooltip(description) {
                IconButton(
                    onClick = { selectAspectRatio(aspectRatio, !isLandscape) },
                    enabled = aspectRatio.canSwapOrientation,
                ) {
                    Icon(
                        imageVector = when (isLandscape) {
                            true -> Icons.Filled.CropLandscape
                            false -> Icons.Filled.CropPortrait
                        },
                        contentDescription = description,
                    )
                }
            }
        }
    }
}

private fun IntRect.fitAspectRatio(ratio: Float, bounds: IntRect): IntRect {
    val width = minOf(bounds.width.toFloat(), bounds.height * ratio).roundToInt()
    val height = (width / ratio).roundToInt().coerceAtMost(bounds.height)
    val left = (center.x - width / 2).coerceIn(bounds.left, bounds.right - width)
    val top = (center.y - height / 2).coerceIn(bounds.top, bounds.bottom - height)

    return IntRect(left, top, left + width, top + height)
}

@Composable
private fun CropOverlay(
    imageBounds: Rect,
    cropRect: Rect,
    aspectRatio: Float?,
    onCropRectChange: (Rect) -> Unit,
    modifier: Modifier = Modifier,
    style: CropOverlayStyle = defaultCropOverlayStyle(),
) {
    var activeHandle by remember { mutableStateOf<Handle?>(null) }
    val currentCropRect by rememberUpdatedState(cropRect)
    val currentAspectRatio by rememberUpdatedState(aspectRatio)

    val handleHitRadiusPx = with(LocalDensity.current) {
        maxOf(style.handleRadius + 8.dp, 40.dp).toPx()
    }
    val minimumCropSizePx = with(LocalDensity.current) { 100.dp.toPx() }

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
            .pointerInput(imageBounds) {
                detectDragGestures(
                    onDragStart = { offset ->
                        activeHandle = getHandleForOffset(
                            offset = offset,
                            rect = currentCropRect,
                            hitRadius = handleHitRadiusPx,
                        )
                    },
                    onDragEnd = {
                        activeHandle = null
                    },
                    onDragCancel = {
                        activeHandle = null
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()

                        val activeHandle = activeHandle ?: return@detectDragGestures

                        if ((change.position.y > imageBounds.bottom && dragAmount.y < 0f)
                            || (change.position.y < imageBounds.top && dragAmount.y > 0f)
                            || (change.position.x > imageBounds.right && dragAmount.x < 0f)
                            || (change.position.x < imageBounds.left && dragAmount.x > 0f)
                        ) {
                            return@detectDragGestures
                        }

                        val newRect = currentAspectRatio?.takeIf {
                            activeHandle != Handle.CENTER
                        }?.let {
                            updateRectWithDragKeepingRatio(
                                currentCropRect,
                                activeHandle,
                                dragAmount,
                                imageBounds,
                                minimumCropSizePx,
                                it,
                            )
                        } ?: updateRectWithDrag(
                            currentCropRect,
                            activeHandle,
                            dragAmount,
                            imageBounds,
                            minimumCropSizePx,
                        )
                        onCropRectChange(newRect)
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

private fun updateRectWithDrag(
    rect: Rect,
    handle: Handle,
    drag: Offset,
    bounds: Rect,
    requestedMinimumSize: Float,
): Rect {
    val minWidth = minOf(requestedMinimumSize, bounds.width)
    val minHeight = minOf(requestedMinimumSize, bounds.height)

    if (handle == Handle.CENTER) {
        val width = rect.width.coerceIn(0f, bounds.width)
        val height = rect.height.coerceIn(0f, bounds.height)
        val maxLeft = maxOf(bounds.left, minOf(bounds.right, bounds.right - width))
        val maxTop = maxOf(bounds.top, minOf(bounds.bottom, bounds.bottom - height))
        val left = (rect.left + drag.x).coerceIn(bounds.left, maxLeft)
        val top = (rect.top + drag.y).coerceIn(bounds.top, maxTop)

        return Rect(
            left = left,
            top = top,
            right = minOf(bounds.right, left + width),
            bottom = minOf(bounds.bottom, top + height),
        )
    }

    val left = if (handle.movesLeft) {
        (rect.left + drag.x).coerceIn(
            bounds.left,
            maxOf(bounds.left, minOf(bounds.right, rect.right - minWidth)),
        )
    } else {
        rect.left
    }
    val right = if (handle.movesRight) {
        (rect.right + drag.x).coerceIn(
            minOf(bounds.right, maxOf(bounds.left, rect.left + minWidth)),
            bounds.right,
        )
    } else {
        rect.right
    }
    val top = if (handle.movesTop) {
        (rect.top + drag.y).coerceIn(
            bounds.top,
            maxOf(bounds.top, minOf(bounds.bottom, rect.bottom - minHeight)),
        )
    } else {
        rect.top
    }
    val bottom = if (handle.movesBottom) {
        (rect.bottom + drag.y).coerceIn(
            minOf(bounds.bottom, maxOf(bounds.top, rect.top + minHeight)),
            bounds.bottom,
        )
    } else {
        rect.bottom
    }

    return Rect(left, top, right, bottom)
}

private fun updateRectWithDragKeepingRatio(
    rect: Rect,
    handle: Handle,
    drag: Offset,
    bounds: Rect,
    requestedMinimumSize: Float,
    ratio: Float,
): Rect {
    val movesHorizontally = handle.movesLeft || handle.movesRight
    val movesVertically = handle.movesTop || handle.movesBottom

    val draggedWidth = when {
        handle.movesRight -> rect.width + drag.x
        handle.movesLeft -> rect.width - drag.x
        else -> rect.width
    }
    val draggedHeight = when {
        handle.movesBottom -> rect.height + drag.y
        handle.movesTop -> rect.height - drag.y
        else -> rect.height
    }
    val width = when {
        movesHorizontally && movesVertically -> when (abs(drag.x) >= abs(drag.y) * ratio) {
            true -> draggedWidth
            false -> draggedHeight * ratio
        }

        movesHorizontally -> draggedWidth
        else -> draggedHeight * ratio
    }

    val availableWidth = when {
        handle.movesRight -> bounds.right - rect.left
        handle.movesLeft -> rect.right - bounds.left
        else -> bounds.width
    }
    val availableHeight = when {
        handle.movesBottom -> bounds.bottom - rect.top
        handle.movesTop -> rect.bottom - bounds.top
        else -> bounds.height
    }
    val maxWidth = minOf(availableWidth, availableHeight * ratio)
    val minWidth = maxOf(requestedMinimumSize, requestedMinimumSize * ratio).coerceAtMost(maxWidth)

    val newWidth = width.coerceIn(minWidth, maxWidth)
    val newHeight = newWidth / ratio

    val left = when {
        handle.movesRight -> rect.left
        handle.movesLeft -> rect.right - newWidth
        else -> (rect.center.x - newWidth / 2).coerceIn(
            bounds.left,
            maxOf(bounds.left, bounds.right - newWidth),
        )
    }
    val top = when {
        handle.movesBottom -> rect.top
        handle.movesTop -> rect.bottom - newHeight
        else -> (rect.center.y - newHeight / 2).coerceIn(
            bounds.top,
            maxOf(bounds.top, bounds.bottom - newHeight),
        )
    }

    return Rect(left, top, left + newWidth, top + newHeight)
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
