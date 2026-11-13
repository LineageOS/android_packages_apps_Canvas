/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.composables

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import org.lineageos.canvas.R
import org.lineageos.canvas.models.EditMode
import org.lineageos.canvas.models.FilterPreset

@get:StringRes
val EditMode.titleResId: Int
    get() = when (this) {
        EditMode.CROP -> R.string.edit_mode_crop
        EditMode.ROTATION -> R.string.edit_mode_rotation
        EditMode.MARKER -> R.string.edit_mode_marker
        EditMode.HIGHLIGHTER -> R.string.edit_mode_highlighter
        EditMode.TEXT -> R.string.edit_mode_text
        EditMode.BRIGHTNESS -> R.string.edit_mode_brightness
        EditMode.CONTRAST -> R.string.edit_mode_contrast
        EditMode.SATURATION -> R.string.edit_mode_saturation
        EditMode.WARMTH -> R.string.edit_mode_warmth
        EditMode.TINT -> R.string.edit_mode_tint
        EditMode.FILTERS -> R.string.edit_mode_filters
    }

val EditMode.icon: ImageVector
    @Composable
    get() = when (this) {
        EditMode.CROP -> Icons.Filled.Crop
        EditMode.ROTATION -> Icons.Filled.CropRotate
        EditMode.MARKER -> ImageVector.vectorResource(R.drawable.ic_ink_pen)
        EditMode.HIGHLIGHTER -> ImageVector.vectorResource(R.drawable.ic_ink_highlighter)
        EditMode.TEXT -> Icons.Filled.TextFields
        EditMode.BRIGHTNESS -> Icons.Filled.BrightnessMedium
        EditMode.CONTRAST -> Icons.Filled.Contrast
        EditMode.SATURATION -> Icons.Filled.InvertColors
        EditMode.WARMTH -> Icons.Filled.Thermostat
        EditMode.TINT -> Icons.Filled.Palette
        EditMode.FILTERS -> Icons.Filled.Filter
    }

@get:StringRes
val FilterPreset.titleResId: Int
    get() = when (this) {
        FilterPreset.PUNCH -> R.string.filter_punch
        FilterPreset.VINTAGE -> R.string.filter_vintage
        FilterPreset.COOL -> R.string.filter_cool
        FilterPreset.NOIR -> R.string.filter_noir
        FilterPreset.FADE -> R.string.filter_fade
        FilterPreset.NEGATIVE -> R.string.filter_negative
    }
