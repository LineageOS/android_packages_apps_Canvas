/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.models

enum class ShapeType(val isLinear: Boolean = false) {
    RECTANGLE,
    OVAL,
    TRIANGLE,
    STAR,
    LINE(isLinear = true),
    ARROW(isLinear = true),
}
