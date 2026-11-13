/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.composables

import android.graphics.Color.colorToHSV
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
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

private data class SliderConfig(
    @get:StringRes val label: Int,
    val value: Float,
    val onValueChange: (Float) -> Unit,
    val colors: List<Color>,
    val range: ClosedFloatingPointRange<Float> = 0f..1f,
    val checkered: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerSheet(
    initialColor: Color,
    onConfirm: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialHsv = remember(initialColor) {
        FloatArray(3).also { colorToHSV(initialColor.toArgb(), it) }
    }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var brightness by remember { mutableFloatStateOf(initialHsv[2]) }
    var opacity by remember { mutableFloatStateOf(initialColor.alpha) }

    val opaqueColor = Color.hsv(hue, saturation, brightness)
    val color = opaqueColor.copy(alpha = opacity)

    val sliders = listOf(
        SliderConfig(R.string.color_hue, hue, { hue = it }, hueSpectrum, range = 0f..360f),
        SliderConfig(
            R.string.color_saturation,
            saturation,
            { saturation = it },
            listOf(Color.hsv(hue, 0f, brightness), Color.hsv(hue, 1f, brightness)),
        ),
        SliderConfig(
            R.string.color_brightness,
            brightness,
            { brightness = it },
            listOf(Color.Black, Color.hsv(hue, saturation, 1f)),
        ),
        SliderConfig(
            R.string.color_opacity,
            opacity,
            { opacity = it },
            listOf(Color.Transparent, opaqueColor),
            checkered = true,
        ),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
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
                    .background(color),
            )

            sliders.forEach { config ->
                ColorSlider(config)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
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
private fun ColorSlider(config: SliderConfig) {
    val description = stringResource(config.label)

    Column {
        Text(
            text = description,
            style = MaterialTheme.typography.labelLarge,
        )
        Slider(
            value = config.value,
            onValueChange = config.onValueChange,
            valueRange = config.range,
            modifier = Modifier.semantics { contentDescription = description },
            track = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(CircleShape)
                        .checkerboard(config.checkered)
                        .background(Brush.horizontalGradient(config.colors)),
                )
            },
        )
    }
}

private fun Modifier.checkerboard(enabled: Boolean = true) =
    if (!enabled) this else drawWithCache {
        val cell = 8.dp.toPx()
        val cols = (size.width / cell).toInt() + 1
        val rows = (size.height / cell).toInt() + 1

        onDrawBehind {
            drawRect(Color.White)
            for (y in 0 until rows) {
                for (x in (y % 2) until cols step 2) {
                    drawRect(
                        color = Color.LightGray,
                        topLeft = Offset(x * cell, y * cell),
                        size = Size(cell, cell),
                    )
                }
            }
        }
    }
