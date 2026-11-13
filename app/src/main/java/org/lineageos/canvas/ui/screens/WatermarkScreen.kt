/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import org.lineageos.canvas.R
import org.lineageos.canvas.ext.drawWatermark
import org.lineageos.canvas.models.Action
import org.lineageos.canvas.ui.composables.AdjustmentControls
import org.lineageos.canvas.ui.composables.CanvasBottomBar
import org.lineageos.canvas.ui.composables.CanvasImage
import org.lineageos.canvas.ui.composables.ImageInformation
import org.lineageos.canvas.ui.composables.StrokeColorPalette

private const val DEFAULT_TEXT_SIZE = 0.05f
private const val DEFAULT_OPACITY = 0.35f

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WatermarkScreen(
    imageBitmap: ImageBitmap,
    cropRect: IntRect?,
    onConfirm: (Action.Drawing.Watermark?) -> Unit,
    onCancel: () -> Unit,
) {
    var imageInformation by remember { mutableStateOf<ImageInformation?>(null) }
    var text by rememberSaveable { mutableStateOf("") }
    var relativeSize by rememberSaveable { mutableFloatStateOf(DEFAULT_TEXT_SIZE) }
    var opacity by rememberSaveable { mutableFloatStateOf(DEFAULT_OPACITY) }
    var color by remember { mutableStateOf(Color.White) }
    val textMeasurer = rememberTextMeasurer()

    val textSize = relativeSize * minOf(imageBitmap.width, imageBitmap.height)
    val watermarkColor = color.copy(alpha = color.alpha * opacity)
    val isImeVisible = WindowInsets.isImeVisible

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
        ) {
            CanvasImage(
                imageBitmap = imageBitmap,
                cropRect = cropRect,
                modifier = Modifier.fillMaxSize(),
            ) { imageInformation = it }

            imageInformation?.let { info ->
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val rect = info.imageViewRect
                    val factor = rect.width / info.bitmapSize.width
                    clipRect(rect.left, rect.top, rect.right, rect.bottom) {
                        translate(rect.left, rect.top) {
                            scale(factor, pivot = Offset.Zero) {
                                drawWatermark(
                                    text = text,
                                    color = watermarkColor,
                                    textSize = textSize,
                                    textMeasurer = textMeasurer,
                                    area = info.bitmapSize.toSize(),
                                )
                            }
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            label = { Text(stringResource(R.string.watermark_text)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )

        if (!isImeVisible) {
            AdjustmentControls(
                label = R.string.watermark_size,
                value = relativeSize,
                onValueChange = { relativeSize = it },
                valueRange = 0.02f..0.15f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )

            AdjustmentControls(
                label = R.string.color_opacity,
                value = opacity,
                onValueChange = { opacity = it },
                valueRange = 0.05f..1f,
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
                    onConfirm(
                        text.takeIf { it.isNotBlank() }?.let {
                            Action.Drawing.Watermark(
                                text = it,
                                color = watermarkColor,
                                textSize = textSize,
                            )
                        }
                    )
                },
                onCancel = onCancel,
            )
        }
    }
}
