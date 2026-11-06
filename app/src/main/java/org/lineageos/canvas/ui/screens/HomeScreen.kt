/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import org.lineageos.canvas.models.EditMode
import org.lineageos.canvas.ui.composables.CanvasBottomBar
import org.lineageos.canvas.ui.composables.CompareImage
import org.lineageos.canvas.ui.composables.HomeBottomBarContent
import org.lineageos.canvas.ui.composables.OriginalLabel

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
