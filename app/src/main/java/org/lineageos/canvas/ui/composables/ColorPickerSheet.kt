/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.composables

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.lineageos.canvas.R

val hueSpectrum = List(7) { Color.hsv(it * 60f % 360f, 1f, 1f) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerSheet(
    initialColor: Color,
    onConfirm: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialHsv = remember(initialColor) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initialColor.toArgb(), it) }
    }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var brightness by remember { mutableFloatStateOf(initialHsv[2]) }
    var opacity by remember { mutableFloatStateOf(initialColor.alpha) }

    val color = Color.hsv(hue, saturation, brightness, opacity)
    val opaqueColor = color.copy(alpha = 1f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.color_custom),
                style = MaterialTheme.typography.titleLarge,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .checkerboard()
                    .background(color)
            )

            ColorSlider(
                label = R.string.color_hue,
                value = hue,
                onValueChange = { hue = it },
                valueRange = 0f..360f,
                brush = Brush.horizontalGradient(hueSpectrum),
            )
            ColorSlider(
                label = R.string.color_saturation,
                value = saturation,
                onValueChange = { saturation = it },
                brush = Brush.horizontalGradient(
                    listOf(Color.hsv(hue, 0f, brightness), Color.hsv(hue, 1f, brightness))
                ),
            )
            ColorSlider(
                label = R.string.color_brightness,
                value = brightness,
                onValueChange = { brightness = it },
                brush = Brush.horizontalGradient(
                    listOf(Color.Black, Color.hsv(hue, saturation, 1f))
                ),
            )
            ColorSlider(
                label = R.string.color_opacity,
                value = opacity,
                onValueChange = { opacity = it },
                brush = Brush.horizontalGradient(
                    listOf(opaqueColor.copy(alpha = 0f), opaqueColor)
                ),
                checkered = true,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(onClick = { onConfirm(color) }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        }
    }
}

@Composable
private fun ColorSlider(
    @StringRes label: Int,
    value: Float,
    onValueChange: (Float) -> Unit,
    brush: Brush,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    checkered: Boolean = false,
) {
    val description = stringResource(label)

    Column {
        Text(
            text = description,
            style = MaterialTheme.typography.labelLarge,
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.semantics { contentDescription = description },
            track = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(CircleShape)
                        .then(if (checkered) Modifier.checkerboard() else Modifier)
                        .background(brush)
                )
            },
        )
    }
}

private fun Modifier.checkerboard() = drawBehind {
    val cell = 8.dp.toPx()
    drawRect(Color.White)
    var y = 0
    while (y * cell < size.height) {
        var x = y % 2
        while (x * cell < size.width) {
            drawRect(
                color = Color.LightGray,
                topLeft = Offset(x * cell, y * cell),
                size = Size(cell, cell),
            )
            x += 2
        }
        y++
    }
}
