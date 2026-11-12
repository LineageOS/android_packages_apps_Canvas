/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import org.lineageos.canvas.models.Action
import org.lineageos.canvas.ui.composables.AdjustmentControls
import org.lineageos.canvas.ui.composables.CanvasBottomBar
import org.lineageos.canvas.ui.composables.CanvasImage

@Composable
fun ColorAdjustmentScreen(
    imageBitmap: ImageBitmap,
    cropRect: IntRect?,
    @StringRes label: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    neutralValue: Float,
    colorFilter: (Float) -> ColorFilter,
    toAction: (Float) -> Action.Adjustment,
    onConfirm: (Action.Adjustment?) -> Unit,
    onCancel: () -> Unit,
) {
    var value by remember { mutableFloatStateOf(neutralValue) }

    Column(modifier = Modifier.fillMaxSize()) {
        CanvasImage(
            imageBitmap = imageBitmap,
            cropRect = cropRect,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            colorFilter = remember(value) { colorFilter(value) },
        )

        AdjustmentControls(
            label = label,
            value = value,
            onValueChange = { value = it },
            valueRange = valueRange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )

        CanvasBottomBar(
            onConfirm = {
                onConfirm(value.takeIf { it != neutralValue }?.let(toAction))
            },
            onCancel = onCancel,
        )
    }
}
