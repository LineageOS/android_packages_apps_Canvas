/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import org.lineageos.canvas.R

private const val SNAP_DEGREES = 0.5f

/**
 * Ruler style dial used to pick a straighten angle.
 *
 * @param value The angle in degrees
 * @param onValueChange Called with the new angle
 * @param valueRange The allowed angles
 */
@Composable
fun StraightenDial(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = -45f..45f,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    val tickSpacingPx = with(LocalDensity.current) { 8.dp.toPx() }
    val tickWidthPx = with(LocalDensity.current) { 2.dp.toPx() }

    var rawValue by remember { mutableFloatStateOf(value) }

    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant
    val activeTickColor = MaterialTheme.colorScheme.primary

    val description = stringResource(R.string.straighten)
    val valueText = stringResource(R.string.straighten_degrees, value.roundToInt())

    Row(
        modifier =
            modifier.semantics(mergeDescendants = true) {
                contentDescription = description
                stateDescription = valueText
                progressBarRangeInfo = ProgressBarRangeInfo(value, valueRange)
                setProgress { target ->
                    onValueChange(target.coerceIn(valueRange))
                    true
                }
            },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = valueText,
            modifier = Modifier.width(48.dp),
            color = if (value == 0f) tickColor else activeTickColor,
            style = MaterialTheme.typography.titleMedium,
        )

        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .padding(end = 48.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            rawValue = 0f
                            currentOnValueChange(0f)
                        }
                    )
                }
                .pointerInput(valueRange) {
                    detectHorizontalDragGestures(onDragStart = { rawValue = currentValue }) {
                        change, dragAmount ->
                        change.consume()

                        rawValue = (rawValue - dragAmount / tickSpacingPx).coerceIn(valueRange)
                        val newValue = rawValue.takeIf { abs(it) >= SNAP_DEGREES } ?: 0f

                        if (newValue != currentValue) {
                            if (newValue == 0f) {
                                hapticFeedback.performHapticFeedback(
                                    HapticFeedbackType.TextHandleMove
                                )
                            }
                            currentOnValueChange(newValue)
                        }
                    }
                },
        ) {
            val centerX = size.width / 2f
            val firstTick = ceil(maxOf(valueRange.start, value - centerX / tickSpacingPx))
            val lastTick = floor(minOf(valueRange.endInclusive, value + centerX / tickSpacingPx))

            var tick = firstTick
            while (tick <= lastTick) {
                val x = centerX + (tick - value) * tickSpacingPx
                val isMajor = tick.roundToInt() % 5 == 0
                val tickHeight = size.height * if (isMajor) 0.6f else 0.35f
                val isBetweenZeroAndValue =
                    tick != 0f && tick * value > 0f && abs(tick) <= abs(value)

                drawLine(
                    color = if (isBetweenZeroAndValue) activeTickColor else tickColor,
                    start = Offset(x, (size.height - tickHeight) / 2f),
                    end = Offset(x, (size.height + tickHeight) / 2f),
                    strokeWidth = tickWidthPx,
                )
                tick++
            }

            drawLine(
                color = activeTickColor,
                start = Offset(centerX, 0f),
                end = Offset(centerX, size.height),
                strokeWidth = tickWidthPx,
            )
        }
    }
}
