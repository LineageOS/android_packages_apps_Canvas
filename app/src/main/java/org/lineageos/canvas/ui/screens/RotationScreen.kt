/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import org.lineageos.canvas.R
import org.lineageos.canvas.models.Action
import org.lineageos.canvas.models.RotationStep
import org.lineageos.canvas.ui.composables.CanvasBottomBar
import org.lineageos.canvas.ui.composables.CanvasImage
import org.lineageos.canvas.ui.composables.ToolbarTooltip
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun RotationScreen(
    imageBitmap: ImageBitmap,
    cropRect: IntRect?,
    onConfirm: (List<Action>) -> Unit,
    onCancel: () -> Unit,
) {
    var quarterTurns by remember { mutableIntStateOf(0) }
    val degrees by animateFloatAsState(quarterTurns * 90f)
    var mirrorHorizontally by remember { mutableStateOf(false) }
    var mirrorVertically by remember { mutableStateOf(false) }
    val mirrorScaleX by animateFloatAsState(if (mirrorHorizontally) -1f else 1f)
    val mirrorScaleY by animateFloatAsState(if (mirrorVertically) -1f else 1f)

    Column(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (imageBitmap.width > 0 && imageBitmap.height > 0 &&
                constraints.maxWidth > 0 && constraints.maxHeight > 0
            ) {
                val fitScale = min(
                    maxWidth.value / imageBitmap.width,
                    maxHeight.value / imageBitmap.height,
                )
                val fittedWidth = (imageBitmap.width * fitScale).dp
                val fittedHeight = (imageBitmap.height * fitScale).dp

                CanvasImage(
                    imageBitmap = imageBitmap,
                    cropRect = cropRect,
                    modifier = Modifier
                        .size(fittedWidth, fittedHeight)
                        .graphicsLayer {
                            scaleX = mirrorScaleX
                            scaleY = mirrorScaleY
                        }
                        .graphicsLayer {
                            val radians = degrees * PI.toFloat() / 180f
                            val cosine = abs(cos(radians))
                            val sine = abs(sin(radians))
                            val boundsWidth = size.width * cosine + size.height * sine
                            val boundsHeight = size.width * sine + size.height * cosine
                            val scale = min(
                                constraints.maxWidth / boundsWidth,
                                constraints.maxHeight / boundsHeight,
                            ).coerceAtMost(1f)

                            rotationZ = degrees
                            scaleX = scale
                            scaleY = scale
                        },
                )
            }
        }

        CanvasBottomBar(
            onCancel = onCancel,
            onConfirm = {
                val rotation = RotationStep.entries[
                    quarterTurns.mod(RotationStep.entries.size)
                ]
                onConfirm(
                    listOfNotNull(
                        rotation.takeIf { it != RotationStep.ROT_0 }?.let {
                            Action.Transformation.Rotation(it)
                        },
                        Action.Transformation.Mirror(
                            horizontal = mirrorHorizontally,
                            vertical = mirrorVertically,
                        ).takeIf { mirrorHorizontally || mirrorVertically },
                    )
                )
            },
        ) {
            RotationBottomBarContent(
                onRotateCounterclockwise = { quarterTurns-- },
                onRotateClockwise = { quarterTurns++ },
                mirrorHorizontally = mirrorHorizontally,
                onMirrorHorizontallyChange = { mirrorHorizontally = it },
                mirrorVertically = mirrorVertically,
                onMirrorVerticallyChange = { mirrorVertically = it },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RotationBottomBarContent(
    onRotateCounterclockwise: () -> Unit,
    onRotateClockwise: () -> Unit,
    mirrorHorizontally: Boolean,
    onMirrorHorizontallyChange: (Boolean) -> Unit,
    mirrorVertically: Boolean,
    onMirrorVerticallyChange: (Boolean) -> Unit,
) {
    HorizontalFloatingToolbar(expanded = true) {
        val counterclockwiseDescription = stringResource(R.string.rotate_counterclockwise)
        ToolbarTooltip(counterclockwiseDescription) {
            IconButton(onClick = onRotateCounterclockwise) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.RotateLeft,
                    contentDescription = counterclockwiseDescription,
                )
            }
        }

        val clockwiseDescription = stringResource(R.string.rotate_clockwise)
        ToolbarTooltip(clockwiseDescription) {
            IconButton(onClick = onRotateClockwise) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.RotateRight,
                    contentDescription = clockwiseDescription,
                )
            }
        }

        val mirrorHorizontallyDescription = stringResource(R.string.mirror_horizontally)
        ToolbarTooltip(mirrorHorizontallyDescription) {
            IconToggleButton(
                checked = mirrorHorizontally,
                onCheckedChange = onMirrorHorizontallyChange,
            ) {
                Icon(
                    imageVector = Icons.Filled.Flip,
                    contentDescription = mirrorHorizontallyDescription,
                )
            }
        }

        val mirrorVerticallyDescription = stringResource(R.string.mirror_vertically)
        ToolbarTooltip(mirrorVerticallyDescription) {
            IconToggleButton(
                checked = mirrorVertically,
                onCheckedChange = onMirrorVerticallyChange,
            ) {
                Icon(
                    imageVector = Icons.Filled.Flip,
                    contentDescription = mirrorVerticallyDescription,
                    modifier = Modifier.rotate(90f),
                )
            }
        }
    }
}
