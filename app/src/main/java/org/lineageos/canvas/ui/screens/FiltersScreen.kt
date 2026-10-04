/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.screens

import androidx.annotation.StringRes
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.FilterBAndW
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import org.lineageos.canvas.R
import org.lineageos.canvas.ext.greyColorFilter
import org.lineageos.canvas.ext.sepiaColorFilter
import org.lineageos.canvas.models.Action
import org.lineageos.canvas.ui.composables.CanvasBottomBar
import org.lineageos.canvas.ui.composables.CanvasImage
import org.lineageos.canvas.ui.composables.ToolbarTooltip

private enum class FilterChoice(
    @param:StringRes val label: Int,
    val icon: ImageVector,
    val defaultAmount: Float? = null,
) {
    ORIGINAL(R.string.filter_none, Icons.Filled.Image) {
        override fun colorFilter(amount: Float): ColorFilter? = null
        override fun action(amount: Float): Action.Adjustment? = null
    },
    GREY(R.string.filter_grey, Icons.Filled.FilterBAndW, 1f) {
        override fun colorFilter(amount: Float) = greyColorFilter(1f - amount)
        override fun action(amount: Float) =
            amount.takeIf { it != 0f }?.let { Action.Adjustment.Grey(1f - it) }
    },
    SEPIA(R.string.filter_sepia, Icons.Filled.FilterVintage, 1f) {
        override fun colorFilter(amount: Float) = sepiaColorFilter(amount)
        override fun action(amount: Float) =
            amount.takeIf { it != 0f }?.let(Action.Adjustment::Sepia)
    };

    val adjustable get() = defaultAmount != null

    abstract fun colorFilter(amount: Float): ColorFilter?
    abstract fun action(amount: Float): Action.Adjustment?
}

private data class FilterSelection(
    val filter: FilterChoice,
    val amount: Float = filter.defaultAmount ?: 0f,
) {
    val colorFilter get() = filter.colorFilter(amount)
    val action get() = filter.action(amount)

    fun withAmount(value: Float) = copy(amount = value.coerceIn(0f, 1f))
}

@Composable
fun FiltersScreen(
    imageBitmap: ImageBitmap,
    cropRect: IntRect?,
    onConfirm: (Action.Adjustment?) -> Unit,
    onCancel: () -> Unit,
) {
    var selection by remember { mutableStateOf(FilterSelection(FilterChoice.ORIGINAL)) }
    var showSlider by remember { mutableStateOf(false) }
    val colorFilter = remember(selection) { selection.colorFilter }

    BackHandler(enabled = showSlider) { showSlider = false }

    Column(modifier = Modifier.fillMaxSize()) {
        CanvasImage(
            imageBitmap = imageBitmap,
            cropRect = cropRect,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            colorFilter = colorFilter,
        )

        if (showSlider) {
            FilterSlider(
                selection = selection,
                onValueChange = { selection = selection.withAmount(it) },
                onBack = { showSlider = false },
            )
        } else {
            FilterRail(
                selection = selection,
                onFilterClick = { filter ->
                    if (filter == selection.filter && filter.adjustable) {
                        showSlider = true
                    } else {
                        selection = FilterSelection(filter)
                    }
                },
            )
        }

        CanvasBottomBar(
            onCancel = onCancel,
            onConfirm = { onConfirm(selection.action) },
        ) {
            if (!showSlider) {
                Text(
                    text = stringResource(
                        if (selection.filter == FilterChoice.ORIGINAL) {
                            R.string.filter_select_hint
                        } else {
                            R.string.filter_tap_again_to_adjust
                        }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun FilterRail(
    selection: FilterSelection,
    onFilterClick: (FilterChoice) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        items(
            FilterChoice.entries
        ) { filter ->
            val description = stringResource(filter.label)
            val amount = filter.defaultAmount?.let { default ->
                if (filter == selection.filter) selection.amount else default
            }
            val isSelected = selection.filter == filter

            ToolbarTooltip(description) {
                Surface(
                    onClick = { onFilterClick(filter) },
                    modifier = Modifier
                        .size(width = 88.dp, height = 88.dp)
                        .semantics { selected = isSelected },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    contentColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = filter.icon,
                            contentDescription = null,
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (amount != null) {
                            Text(
                                text = stringResource(R.string.adjustment_percent, amount * 100),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSlider(
    selection: FilterSelection,
    onValueChange: (Float) -> Unit,
    onBack: () -> Unit,
) {
    val label = stringResource(selection.filter.label)
    val backDescription = stringResource(R.string.back)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolbarTooltip(backDescription) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = backDescription,
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        Slider(
            value = selection.amount,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            modifier = Modifier
                .weight(1f)
                .height(72.dp)
                .semantics { contentDescription = label },
        )

        Spacer(modifier = Modifier.width(4.dp))

        // We must take into account for 100% otherwise the slider will jump.
        Box(modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(
                text = stringResource(R.string.adjustment_percent, 100f),
                modifier = Modifier
                    .alpha(0f)
                    .clearAndSetSemantics {},
            )
            Text(
                text = stringResource(R.string.adjustment_percent, selection.amount * 100),
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}
