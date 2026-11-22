/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.composables

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color.colorToHSV
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ChecksSdkIntAtLeast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
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

// The eye dropper constants are inlined and only used once it's known to be available
@SuppressLint("InlinedApi")
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

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val isEyeDropperAvailable = remember(context) { context.isEyeDropperAvailable() }
    val eyeDropperLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data
            ?.takeIf { result.resultCode == Activity.RESULT_OK }
            ?.takeIf { it.hasExtra(Intent.EXTRA_COLOR) }
            ?.getIntExtra(Intent.EXTRA_COLOR, 0)
            ?.let {
                // Keep the opacity, the picked color is always opaque
                val hsv = FloatArray(3).also { hsv -> colorToHSV(it, hsv) }
                hue = hsv[0]
                saturation = hsv[1]
                brightness = hsv[2]
            }
        scope.launch { sheetState.show() }
    }

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
        sheetState = sheetState,
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .checkerboard()
                        .background(color),
                )

                if (isEyeDropperAvailable) {
                    val description = stringResource(R.string.color_pick_from_screen)

                    ToolbarTooltip(description) {
                        FilledTonalIconButton(
                            onClick = {
                                // Hide the sheet so that the whole image can be picked from
                                scope.launch {
                                    sheetState.hide()
                                    eyeDropperLauncher.launch(
                                        Intent(Intent.ACTION_OPEN_EYE_DROPPER)
                                    )
                                }
                            },
                            modifier = Modifier.size(56.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Colorize,
                                contentDescription = description,
                            )
                        }
                    }
                }
            }

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

/**
 * Whether the system eye dropper (Android 17+) can be launched. It's behind a feature flag, so
 * also check that something actually handles the intent.
 */
@ChecksSdkIntAtLeast(api = 37)
private fun Context.isEyeDropperAvailable() = Build.VERSION.SDK_INT >= 37 &&
        packageManager.resolveActivity(Intent(Intent.ACTION_OPEN_EYE_DROPPER), 0) != null
