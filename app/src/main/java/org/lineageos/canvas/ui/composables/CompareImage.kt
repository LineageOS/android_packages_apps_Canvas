/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.composables

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntRect
import org.lineageos.canvas.R

@Composable
fun CompareImage(
    imageBitmap: ImageBitmap,
    originalBitmap: ImageBitmap,
    cropRect: IntRect?,
    canCompare: Boolean,
    isShowingOriginal: Boolean,
    onShowOriginalChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val accessibilityActionLabel = stringResource(
        when (isShowingOriginal) {
            true -> R.string.compare_show_edited
            false -> R.string.compare_show_original
        }
    )

    CanvasImage(
        imageBitmap = if (isShowingOriginal) originalBitmap else imageBitmap,
        cropRect = if (isShowingOriginal) null else cropRect,
        modifier = modifier
            .pointerInput(canCompare) {
                if (!canCompare) return@pointerInput

                detectTapGestures(
                    onLongPress = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onShowOriginalChange(true)
                    },
                    onPress = {
                        tryAwaitRelease()
                        onShowOriginalChange(false)
                    },
                )
            }
            .semantics {
                if (canCompare) {
                    customActions = listOf(
                        CustomAccessibilityAction(accessibilityActionLabel) {
                            onShowOriginalChange(!isShowingOriginal)
                            true
                        }
                    )
                }
            },
    )
}
