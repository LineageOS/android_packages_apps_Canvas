/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.lineageos.canvas.R
import org.lineageos.canvas.models.Action
import org.lineageos.canvas.models.EditMode
import org.lineageos.canvas.models.HistoryList

/**
 * Bottom sheet listing every action of the history, oldest first. Selecting an entry restores
 * the image as it was right after that action, without discarding the following ones.
 *
 * @param history The history snapshot to show
 * @param onGoTo Called with the index of the selected action, -1 for the original image
 * @param onDismissRequest Called when the sheet should be closed
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistorySheet(
    history: HistoryList.Snapshot<Action>,
    onGoTo: (Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    // The original image is the first row, so the row of action i is i + 1
    val count = history.allElements.size + 1
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = history.currentIndex + 1,
    )

    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Text(
            text = stringResource(R.string.history),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            item {
                HistoryItem(
                    index = 0,
                    count = count,
                    icon = Icons.Filled.Image,
                    title = stringResource(R.string.history_original),
                    detail = null,
                    isCurrent = history.currentIndex == -1,
                    isReverted = false,
                    onClick = { onGoTo(-1) },
                )
            }

            itemsIndexed(history.allElements) { index, action ->
                HistoryItem(
                    index = index + 1,
                    count = count,
                    icon = action.icon,
                    title = stringResource(action.titleResId),
                    detail = action.detail(),
                    isCurrent = index == history.currentIndex,
                    isReverted = index > history.currentIndex,
                    onClick = { onGoTo(index) },
                )
            }
        }
    }
}

/**
 * A single-selection segmented list item. Reverted actions keep their container but use the
 * de-emphasized on-surface-variant content color, since they can still be selected to redo them.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HistoryItem(
    index: Int,
    count: Int,
    icon: ImageVector,
    title: String,
    detail: String?,
    isCurrent: Boolean,
    isReverted: Boolean,
    onClick: () -> Unit,
) {
    SegmentedListItem(
        selected = isCurrent,
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        leadingContent = { Icon(imageVector = icon, contentDescription = null) },
        supportingContent = detail?.let { { Text(it) } },
        colors = if (isReverted) {
            val variant = MaterialTheme.colorScheme.onSurfaceVariant
            ListItemDefaults.segmentedColors(
                contentColor = variant,
                leadingContentColor = variant,
                supportingContentColor = variant,
            )
        } else {
            ListItemDefaults.segmentedColors()
        },
    ) {
        Text(title)
    }
}

/**
 * The [EditMode] that produces this [Action], if any.
 */
private val Action.editMode: EditMode?
    get() = when (this) {
        is Action.Adjustment.Brightness -> EditMode.BRIGHTNESS
        is Action.Adjustment.Contrast -> EditMode.CONTRAST
        is Action.Adjustment.Grey -> EditMode.FILTERS
        is Action.Adjustment.Sepia -> EditMode.FILTERS
        is Action.Transformation.Crop -> EditMode.CROP
        is Action.Transformation.Rotation -> EditMode.ROTATION
        is Action.Drawing.Marker -> EditMode.MARKER
        is Action.Drawing.Highlighter -> EditMode.HIGHLIGHTER
        is Action.Drawing.Text -> EditMode.TEXT
        is Action.Drawing.Eraser -> null
    }

private val Action.icon: ImageVector
    @Composable
    get() = editMode?.icon ?: Icons.Filled.AutoFixNormal

private val Action.titleResId: Int
    get() = when (this) {
        is Action.Adjustment.Grey -> R.string.filter_grey
        is Action.Adjustment.Sepia -> R.string.filter_sepia
        is Action.Drawing.Eraser -> R.string.history_eraser
        else -> editMode?.titleResId ?: error("No title for $this")
    }

/**
 * A short description of the action parameters, matching how they were shown when editing.
 */
@Composable
private fun Action.detail(): String? = when (this) {
    is Action.Adjustment.Brightness -> stringResource(R.string.adjustment_percent, value * 100)
    is Action.Adjustment.Contrast -> stringResource(R.string.adjustment_percent, value * 100)
    is Action.Adjustment.Grey -> stringResource(
        R.string.adjustment_percent, (1f - saturation) * 100
    )

    is Action.Adjustment.Sepia -> stringResource(R.string.adjustment_percent, amount * 100)
    is Action.Transformation.Crop -> stringResource(
        R.string.history_crop_size, rect.width, rect.height
    )

    is Action.Transformation.Rotation -> stringResource(
        R.string.history_rotation_degrees, rotation.degrees.toInt()
    )

    is Action.Drawing.Text -> text
    is Action.Drawing.Marker,
    is Action.Drawing.Highlighter,
    is Action.Drawing.Eraser -> null
}
