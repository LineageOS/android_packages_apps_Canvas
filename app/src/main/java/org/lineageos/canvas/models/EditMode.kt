/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.models

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

/**
 * UI edit mode.
 */
enum class EditMode(val category: Category) {
    /**
     * Crop the image.
     */
    CROP(Category.TRANSFORMATION),

    /**
     * Image rotation.
     */
    ROTATION(Category.TRANSFORMATION),

    /**
     * Free drawing on the image.
     */
    MARKER(Category.DRAWING),

    /**
     * Highlight drawing on the image.
     */
    HIGHLIGHTER(Category.DRAWING),

    /**
     * Add text overlay.
     */
    TEXT(Category.DRAWING),

    /**
     * Brightness adjustment.
     */
    BRIGHTNESS(Category.ADJUSTMENTS),

    /**
     * Contrast adjustment.
     */
    CONTRAST(Category.ADJUSTMENTS),

    /**
     * Saturation adjustment.
     */
    SATURATION(Category.ADJUSTMENTS),

    /**
     * Warmth adjustment.
     */
    WARMTH(Category.ADJUSTMENTS),

    /**
     * Tint adjustment.
     */
    TINT(Category.ADJUSTMENTS),

    /**
     * Image filters.
     */
    FILTERS(Category.ADJUSTMENTS);

    val titleResId: Int
        get() = when (this) {
            CROP -> R.string.edit_mode_crop
            ROTATION -> R.string.edit_mode_rotation
            MARKER -> R.string.edit_mode_marker
            HIGHLIGHTER -> R.string.edit_mode_highlighter
            TEXT -> R.string.edit_mode_text
            BRIGHTNESS -> R.string.edit_mode_brightness
            CONTRAST -> R.string.edit_mode_contrast
            SATURATION -> R.string.edit_mode_saturation
            WARMTH -> R.string.edit_mode_warmth
            TINT -> R.string.edit_mode_tint
            FILTERS -> R.string.edit_mode_filters
        }

    val icon: ImageVector
        @Composable
        get() = when (this) {
            CROP -> Icons.Filled.Crop
            ROTATION -> Icons.Filled.CropRotate
            MARKER -> ImageVector.vectorResource(R.drawable.ic_ink_pen)
            HIGHLIGHTER -> ImageVector.vectorResource(R.drawable.ic_ink_highlighter)
            TEXT -> Icons.Filled.TextFields
            BRIGHTNESS -> Icons.Filled.BrightnessMedium
            CONTRAST -> Icons.Filled.Contrast
            SATURATION -> Icons.Filled.InvertColors
            WARMTH -> Icons.Filled.Thermostat
            TINT -> Icons.Filled.Palette
            FILTERS -> Icons.Filled.Filter
        }

    companion object {
        private val groupedByCategory = entries.groupBy(EditMode::category)

        fun ofCategory(category: Category) = groupedByCategory[category].orEmpty()
    }

    /**
     * Edit mode categories.
     */
    enum class Category {
        /**
         * Transformation of the image.
         */
        TRANSFORMATION,

        /**
         * Adjustments applied to the image.
         */
        ADJUSTMENTS,

        /**
         * Drawing on the image.
         */
        DRAWING,
    }
}
