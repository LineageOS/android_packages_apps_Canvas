/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.models

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Background drawn behind a text label.
 */
enum class TextBackground {
    /**
     * No background.
     */
    NONE,

    /**
     * A translucent background contrasting with the text color.
     */
    TRANSLUCENT,

    /**
     * A background of the picked color, with the text in a contrasting color.
     */
    SOLID;

    /**
     * The next background, to cycle through them.
     */
    val next: TextBackground
        get() = entries[(ordinal + 1) % entries.size]

    /**
     * The color of the text drawn over this background.
     *
     * @param color The picked color
     */
    fun textColor(color: Color) = when (this) {
        NONE, TRANSLUCENT -> color
        SOLID -> color.contrastingColor()
    }

    /**
     * The color of this background, or null if there is none.
     *
     * @param color The picked color
     */
    fun backgroundColor(color: Color) = when (this) {
        NONE -> null
        TRANSLUCENT -> color.contrastingColor().copy(alpha = 0.5f)
        SOLID -> color
    }

    private fun Color.contrastingColor() = when (luminance() > 0.5f) {
        true -> Color.Black
        false -> Color.White
    }
}
