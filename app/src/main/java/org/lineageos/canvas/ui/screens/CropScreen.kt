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
import org.lineageos.canvas.ext.getHandleForOffset
import org.lineageos.canvas.ext.updateRectWithDrag
import org.lineageos.canvas.ext.updateRectWithDragKeepingRatio
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
