/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.models

import androidx.annotation.StringRes
import org.lineageos.canvas.R

enum class FilterPreset {
    PUNCH,
    VINTAGE,
    COOL,
    NOIR,
    FADE,
    NEGATIVE;

    @get:StringRes
    val titleResId: Int
        get() = when (this) {
            PUNCH -> R.string.filter_punch
            VINTAGE -> R.string.filter_vintage
            COOL -> R.string.filter_cool
            NOIR -> R.string.filter_noir
            FADE -> R.string.filter_fade
            NEGATIVE -> R.string.filter_negative
        }
}
