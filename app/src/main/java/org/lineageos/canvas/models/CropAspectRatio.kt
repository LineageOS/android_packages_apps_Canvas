/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.models

enum class CropAspectRatio(val width: Int, val height: Int) {
    FREE(0, 0),
    SQUARE(1, 1),
    RATIO_3_2(3, 2),
    RATIO_4_3(4, 3),
    RATIO_16_9(16, 9);

    val canSwapOrientation: Boolean
        get() = width != height

    fun ratio(isLandscape: Boolean): Float? = when (this) {
        FREE -> null
        else -> when (isLandscape) {
            true -> width.toFloat() / height
            false -> height.toFloat() / width
        }
    }
}
