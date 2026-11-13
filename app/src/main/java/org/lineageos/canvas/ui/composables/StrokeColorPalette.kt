/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.composables

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.lineageos.canvas.R

private data class StrokeColorOption(
    val color: Color,
    val description: Int,
)

private val strokeColors = listOf(
    StrokeColorOption(Color.Red, R.string.color_red),
    StrokeColorOption(Color.Magenta, R.string.color_magenta),
    StrokeColorOption(Color.Blue, R.string.color_blue),
    StrokeColorOption(Color.Cyan, R.string.color_cyan),
    StrokeColorOption(Color.Green, R.string.color_green),
    StrokeColorOption(Color.Yellow, R.string.color_yellow),
    StrokeColorOption(Color.White, R.string.color_white),
    StrokeColorOption(Color.Black, R.string.color_black),
)

@Composable
fun StrokeColorPalette(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            strokeColors.forEach { option ->
                val isSelected = option.color == selectedColor
                val description = stringResource(option.description)
                val size = animateDpAsState(
                    targetValue = if (isSelected) 36.dp else 24.dp,
                    label = "stroke color size",
                )

                ToolbarTooltip(description) {
                    IconButton(
                        onClick = { onColorSelected(option.color) },
                        modifier = Modifier.semantics {
                            contentDescription = description
                            selected = isSelected
                            role = Role.RadioButton
                        },
                    ) {
                        Spacer(
                            modifier = Modifier
                                .size(size.value)
                                .background(option.color, CircleShape)
                        )
                    }
                }
            }

            CustomColorButton(
                selectedColor = selectedColor.takeIf { color ->
                    strokeColors.none { it.color == color }
                },
                onClick = { showPicker = true },
            )
        }
    }

    if (showPicker) {
        ColorPickerSheet(
            initialColor = selectedColor,
            onConfirm = {
                onColorSelected(it)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun CustomColorButton(
    selectedColor: Color?,
    onClick: () -> Unit,
) {
    val description = stringResource(R.string.color_custom)
    val isSelected = selectedColor != null
    val size = animateDpAsState(
        targetValue = if (isSelected) 36.dp else 24.dp,
        label = "custom color size",
    )
    val spectrum = Brush.sweepGradient(hueSpectrum)

    ToolbarTooltip(description) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.semantics {
                contentDescription = description
                selected = isSelected
                role = Role.RadioButton
            },
        ) {
            Spacer(
                modifier = Modifier
                    .size(size.value)
                    .then(
                        if (selectedColor != null) {
                            Modifier
                                .border(4.dp, spectrum, CircleShape)
                                .padding(6.dp)
                                .background(selectedColor, CircleShape)
                        } else {
                            Modifier.background(spectrum, CircleShape)
                        }
                    )
            )
        }
    }
}
