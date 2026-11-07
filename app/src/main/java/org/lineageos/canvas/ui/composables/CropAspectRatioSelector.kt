/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import org.lineageos.canvas.R
import org.lineageos.canvas.models.CropAspectRatio

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CropAspectRatioSelector(
    selectedAspectRatio: CropAspectRatio,
    isLandscape: Boolean,
    onAspectRatioSelected: (CropAspectRatio) -> Unit,
    modifier: Modifier = Modifier,
) {
    val aspectRatios = CropAspectRatio.entries

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        aspectRatios.forEachIndexed { index, aspectRatio ->
            val label = when (aspectRatio) {
                CropAspectRatio.FREE -> stringResource(R.string.crop_aspect_ratio_free)
                else -> when (isLandscape) {
                    true -> stringResource(
                        R.string.crop_aspect_ratio, aspectRatio.width, aspectRatio.height
                    )

                    false -> stringResource(
                        R.string.crop_aspect_ratio, aspectRatio.height, aspectRatio.width
                    )
                }
            }

            ToggleButton(
                checked = aspectRatio == selectedAspectRatio,
                onCheckedChange = { onAspectRatioSelected(aspectRatio) },
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    aspectRatios.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
            ) {
                Text(text = label, maxLines = 1)
            }
        }
    }
}
