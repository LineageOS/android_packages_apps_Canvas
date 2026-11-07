/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntRect
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import org.lineageos.canvas.R
import org.lineageos.canvas.ext.brightnessColorFilter
import org.lineageos.canvas.ext.contrastColorFilter
import org.lineageos.canvas.ext.saturationColorFilter
import org.lineageos.canvas.ext.tintColorFilter
import org.lineageos.canvas.ext.warmthColorFilter
import org.lineageos.canvas.models.Action
import org.lineageos.canvas.models.CropRequest
import org.lineageos.canvas.models.EditMode
import org.lineageos.canvas.ui.LocalSharedTransitionScope
import org.lineageos.canvas.ui.screens.HomeScreen
import org.lineageos.canvas.ui.screens.CropScreen
import org.lineageos.canvas.ui.screens.ImageAdjustmentScreen
import org.lineageos.canvas.ui.screens.FiltersScreen
import org.lineageos.canvas.ui.screens.HighlighterScreen
import org.lineageos.canvas.ui.screens.MarkerScreen
import org.lineageos.canvas.ui.screens.RotationScreen
import org.lineageos.canvas.ui.screens.TextScreen

@Composable
fun CanvasNavDisplay(
    navigationBackStack: SnapshotStateList<Screen>,
    bitmapWithOverlayActions: ImageBitmap,
    finalResultBitmap: ImageBitmap,
    cropRect: IntRect,
    onAddAction: (Action) -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    currentCategory: EditMode.Category?,
    onCategorySelected: (EditMode.Category?) -> Unit,
    cropRequest: CropRequest?,
    onCropConfirmed: (IntRect) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val entryProvider = entryProvider {
        entry<Screen.Home> {
            HomeScreen(
                imageBitmap = finalResultBitmap,
                cropRect = cropRect,
                canUndo = canUndo,
                canRedo = canRedo,
                onUndo = onUndo,
                onRedo = onRedo,
                currentCategory = currentCategory,
                onCategorySelected = onCategorySelected,
                onEditModeSelected = { editMode ->
                    navigationBackStack.add(Screen.Edit(editMode))
                },
            )
        }

        entry<Screen.Edit> {
            when (it.editMode) {
                EditMode.CROP -> CropScreen(
                    imageBitmap = bitmapWithOverlayActions,
                    initialCropRect = cropRect,
                    onConfirm = { action ->
                        when (cropRequest) {
                            null -> {
                                action?.let(onAddAction)
                                navigationBackStack.removeLastOrNull()
                            }

                            else -> onCropConfirmed(action?.rect ?: cropRect)
                        }
                    },
                    onCancel = {
                        when (cropRequest) {
                            null -> navigationBackStack.removeLastOrNull()
                            else -> onClose()
                        }
                    },
                    lockedAspectRatio = cropRequest?.aspectRatio,
                )

                EditMode.ROTATION -> RotationScreen(
                    imageBitmap = bitmapWithOverlayActions,
                    cropRect = cropRect,
                    onConfirm = { actions ->
                        actions.forEach(onAddAction)
                        navigationBackStack.removeLastOrNull()
                    },
                    onCancel = navigationBackStack::removeLastOrNull,
                )

                EditMode.MARKER -> MarkerScreen(
                    imageBitmap = finalResultBitmap,
                    onAddAction = onAddAction,
                    onCancel = navigationBackStack::removeLastOrNull,
                )

                EditMode.HIGHLIGHTER -> HighlighterScreen(
                    imageBitmap = finalResultBitmap,
                    onAddAction = onAddAction,
                    onCancel = navigationBackStack::removeLastOrNull,
                )

                EditMode.TEXT -> TextScreen(
                    imageBitmap = finalResultBitmap,
                    cropRect = cropRect,
                    onAddAction = onAddAction,
                    onCancel = navigationBackStack::removeLastOrNull,
                )

                EditMode.BRIGHTNESS -> ImageAdjustmentScreen(
                    imageBitmap = bitmapWithOverlayActions,
                    cropRect = cropRect,
                    label = R.string.edit_mode_brightness,
                    valueRange = -1f..1f,
                    neutralValue = 0f,
                    colorFilter = ::brightnessColorFilter,
                    toAction = Action.Adjustment::Brightness,
                    onConfirm = { action ->
                        action?.let(onAddAction)
                        navigationBackStack.removeLastOrNull()
                    },
                    onCancel = navigationBackStack::removeLastOrNull,
                )

                EditMode.CONTRAST -> ImageAdjustmentScreen(
                    imageBitmap = bitmapWithOverlayActions,
                    cropRect = cropRect,
                    label = R.string.edit_mode_contrast,
                    valueRange = 0f..2f,
                    neutralValue = 1f,
                    colorFilter = ::contrastColorFilter,
                    toAction = Action.Adjustment::Contrast,
                    onConfirm = { action ->
                        action?.let(onAddAction)
                        navigationBackStack.removeLastOrNull()
                    },
                    onCancel = navigationBackStack::removeLastOrNull,
                )

                EditMode.SATURATION -> ImageAdjustmentScreen(
                    imageBitmap = bitmapWithOverlayActions,
                    cropRect = cropRect,
                    label = R.string.edit_mode_saturation,
                    valueRange = 0f..2f,
                    neutralValue = 1f,
                    colorFilter = ::saturationColorFilter,
                    toAction = Action.Adjustment::Saturation,
                    onConfirm = { action ->
                        action?.let(onAddAction)
                        navigationBackStack.removeLastOrNull()
                    },
                    onCancel = navigationBackStack::removeLastOrNull,
                )

                EditMode.WARMTH -> ImageAdjustmentScreen(
                    imageBitmap = bitmapWithOverlayActions,
                    cropRect = cropRect,
                    label = R.string.edit_mode_warmth,
                    valueRange = -1f..1f,
                    neutralValue = 0f,
                    colorFilter = ::warmthColorFilter,
                    toAction = Action.Adjustment::Warmth,
                    onConfirm = { action ->
                        action?.let(onAddAction)
                        navigationBackStack.removeLastOrNull()
                    },
                    onCancel = navigationBackStack::removeLastOrNull,
                )

                EditMode.TINT -> ImageAdjustmentScreen(
                    imageBitmap = bitmapWithOverlayActions,
                    cropRect = cropRect,
                    label = R.string.edit_mode_tint,
                    valueRange = -1f..1f,
                    neutralValue = 0f,
                    colorFilter = ::tintColorFilter,
                    toAction = Action.Adjustment::Tint,
                    onConfirm = { action ->
                        action?.let(onAddAction)
                        navigationBackStack.removeLastOrNull()
                    },
                    onCancel = navigationBackStack::removeLastOrNull,
                )

                EditMode.FILTERS -> FiltersScreen(
                    imageBitmap = bitmapWithOverlayActions,
                    cropRect = cropRect,
                    onConfirm = { action ->
                        action?.let(onAddAction)
                        navigationBackStack.removeLastOrNull()
                    },
                    onCancel = navigationBackStack::removeLastOrNull,
                )
            }
        }
    }

    NavDisplay(
        backStack = navigationBackStack,
        modifier = modifier,
        sharedTransitionScope = LocalSharedTransitionScope.current,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        popTransitionSpec = { fadeIn() togetherWith fadeOut() },
        predictivePopTransitionSpec = { fadeIn() togetherWith fadeOut() },
        entryProvider = entryProvider,
    )
}
