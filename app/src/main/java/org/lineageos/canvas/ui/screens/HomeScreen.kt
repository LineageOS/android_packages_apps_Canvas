/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.unit.dp
import org.lineageos.canvas.R
import org.lineageos.canvas.models.EditMode
import org.lineageos.canvas.ui.composables.CanvasImage
import org.lineageos.canvas.ui.composables.CanvasBottomBar
import org.lineageos.canvas.ui.composables.HomeBottomBarContent

/**
 * Start screen. Here you can select which edit mode you wanna go to, revert or redo changes and
 * save the result.
 */
@Composable
fun HomeScreen(
    imageBitmap: ImageBitmap,
    originalBitmap: ImageBitmap,
    cropRect: IntRect?,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    currentCategory: EditMode.Category?,
    onCategorySelected: (EditMode.Category?) -> Unit,
    onEditModeSelected: (EditMode) -> Unit,
) {
    BackHandler(enabled = currentCategory != null) {
        onCategorySelected(null)
    }

    val canCompare = canUndo
    var showOriginal by remember { mutableStateOf(false) }
    val isShowingOriginal = canCompare && showOriginal

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            CompareImage(
                imageBitmap = imageBitmap,
                originalBitmap = originalBitmap,
                cropRect = cropRect,
                canCompare = canCompare,
                isShowingOriginal = isShowingOriginal,
                onShowOriginalChange = { showOriginal = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            )

            OriginalLabel(
                visible = isShowingOriginal,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp),
            )
        }

        CanvasBottomBar {
            HomeBottomBarContent(
                canUndo = canUndo,
                canRedo = canRedo,
                onUndo = onUndo,
                onRedo = onRedo,
                currentCategory = currentCategory,
                onCategorySelected = onCategorySelected,
                onEditModeSelected = onEditModeSelected,
            )
        }
    }
}

@Composable
private fun CompareImage(
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

@Composable
private fun OriginalLabel(
    visible: Boolean,
    modifier: Modifier = Modifier,
) = AnimatedVisibility(
    visible = visible,
    enter = fadeIn(),
    exit = fadeOut(),
    modifier = modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
    ) {
        Text(
            text = stringResource(R.string.compare_original),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
