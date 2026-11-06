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
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import org.lineageos.canvas.R
import org.lineageos.canvas.models.EditMode

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
        EditMode.FILTERS -> Icons.Filled.Filter
    }
