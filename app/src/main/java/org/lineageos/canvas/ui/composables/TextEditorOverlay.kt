/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.composables

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.lineageos.canvas.R
import org.lineageos.canvas.models.TextBackground

/**
 * Full screen editor to write a text label and pick its style.
 *
 * @param onDismiss Called when the editor is closed without confirming
 * @param onConfirm Called with the text, its style and its background
 * @param modifier The [Modifier] to apply
 * @param initialText The text to edit, if any
 * @param initialStyle The style of the text to edit, if any
 * @param initialBackground The background of the text to edit
 */
@Composable
fun TextEditorOverlay(
    onDismiss: () -> Unit,
    onConfirm: (String, TextStyle, TextBackground) -> Unit,
    modifier: Modifier = Modifier,
    initialText: String = "",
    initialStyle: TextStyle? = null,
    initialBackground: TextBackground = TextBackground.NONE,
) {
    val textFieldState = rememberTextFieldState(initialText)

    var fontFamily by remember {
        mutableStateOf(initialStyle?.fontFamily ?: FontFamily.Default)
    }
    var textColor by remember { mutableStateOf(initialStyle?.color ?: Color.White) }
    var bold by remember { mutableStateOf(initialStyle?.fontWeight == FontWeight.Bold) }
    var italic by remember { mutableStateOf(initialStyle?.fontStyle == FontStyle.Italic) }
    var underlined by remember {
        mutableStateOf(
            initialStyle?.textDecoration?.contains(TextDecoration.Underline) == true
        )
    }
    var strikethrough by remember {
        mutableStateOf(
            initialStyle?.textDecoration?.contains(TextDecoration.LineThrough) == true
        )
    }
    var textAlign by remember { mutableStateOf(initialStyle?.textAlign ?: TextAlign.Center) }
    var background by remember { mutableStateOf(initialBackground) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val textStyle = TextStyle(
        color = textColor,
        fontSize = 32.sp,
        fontWeight = when (bold) {
            true -> FontWeight.Bold
            false -> FontWeight.Normal
        },
        fontStyle = when (italic) {
            true -> FontStyle.Italic
            false -> FontStyle.Normal
        },
        fontFamily = fontFamily,
        textDecoration = when {
            underlined && strikethrough -> TextDecoration.Underline + TextDecoration.LineThrough
            underlined -> TextDecoration.Underline
            strikethrough -> TextDecoration.LineThrough
            else -> TextDecoration.None
        },
        textAlign = textAlign,
    )

    Column(
        modifier = modifier.background(Color.Black.copy(alpha = 0.8f)),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top),
    ) {
        Row(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val cancelDescription = stringResource(R.string.cancel_action)
            ToolbarTooltip(cancelDescription) {
                IconButton(
                    onClick = onDismiss,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = cancelDescription,
                    )
                }
            }

            val confirmDescription = stringResource(R.string.confirm_action)
            ToolbarTooltip(confirmDescription) {
                IconButton(
                    onClick = {
                        val text = textFieldState.text.trim()
                        when (text.isNotBlank()) {
                            true -> onConfirm(
                                text.toString(),
                                textStyle,
                                background,
                            )

                            false -> {}
                        }
                    },
                    enabled = textFieldState.text.isNotBlank(),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = confirmDescription,
                    )
                }
            }
        }

        TextPropertiesRow {
            TextPropertyButton(
                imageVector = Icons.Default.FormatBold,
                checked = bold,
                contentDescription = R.string.text_decoration_bold,
            ) { bold = it }

            TextPropertyButton(
                imageVector = Icons.Default.FormatItalic,
                checked = italic,
                contentDescription = R.string.text_decoration_italic,
            ) { italic = it }

            TextPropertyButton(
                imageVector = Icons.Default.FormatUnderlined,
                checked = underlined,
                contentDescription = R.string.text_decoration_underlined,
            ) { underlined = it }

            TextPropertyButton(
                imageVector = Icons.Default.FormatStrikethrough,
                checked = strikethrough,
                contentDescription = R.string.text_decoration_strikethrough,
            ) { strikethrough = it }

            TextPropertyButton(
                imageVector = when (textAlign) {
                    TextAlign.Start -> Icons.AutoMirrored.Filled.FormatAlignLeft
                    TextAlign.End -> Icons.AutoMirrored.Filled.FormatAlignRight
                    else -> Icons.Default.FormatAlignCenter
                },
                checked = false,
                contentDescription = when (textAlign) {
                    TextAlign.Start -> R.string.text_alignment_left
                    TextAlign.End -> R.string.text_alignment_right
                    else -> R.string.text_alignment_center
                },
            ) {
                textAlign = when (textAlign) {
                    TextAlign.Start -> TextAlign.Center
                    TextAlign.Center -> TextAlign.End
                    else -> TextAlign.Start
                }
            }

            TextPropertyButton(
                imageVector = when (background) {
                    TextBackground.NONE -> Icons.Default.FormatColorReset
                    TextBackground.TRANSLUCENT -> Icons.Default.Opacity
                    TextBackground.SOLID -> Icons.Default.FormatColorFill
                },
                checked = background != TextBackground.NONE,
                contentDescription = when (background) {
                    TextBackground.NONE -> R.string.text_background_none
                    TextBackground.TRANSLUCENT -> R.string.text_background_translucent
                    TextBackground.SOLID -> R.string.text_background_solid
                },
            ) { background = background.next }
        }

        TextPropertiesRow {
            mapOf(
                Color.White to R.string.text_color_white,
                Color.Black to R.string.text_color_black,
                Color.Red to R.string.text_color_red,
                Color.Green to R.string.text_color_green,
                Color.Blue to R.string.text_color_blue,
                Color.Yellow to R.string.text_color_yellow,
                Color.Magenta to R.string.text_color_magenta,
                Color.Cyan to R.string.text_color_cyan,
            ).forEach {
                TextColorButton(
                    color = it.key,
                    currentColor = textColor,
                    contentDescription = it.value,
                ) { color -> textColor = color }
            }
        }

        TextField(
            state = textFieldState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .focusRequester(focusRequester),
            textStyle = textStyle.copy(
                color = background.textColor(textColor),
                background = background.backgroundColor(textColor) ?: Color.Unspecified,
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            placeholder = {
                Text(
                    text = stringResource(R.string.enter_text),
                    modifier = Modifier.fillMaxWidth(),
                    style = textStyle.copy(
                        color = textStyle.color.copy(alpha = 0.5f),
                    ),
                )
            },
        )
    }
}

@Composable
private fun TextPropertiesRow(
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(16.dp))

        content()

        Spacer(modifier = Modifier.width(16.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TextPropertyButton(
    imageVector: ImageVector,
    checked: Boolean,
    @StringRes contentDescription: Int,
    onCheckedChange: (Boolean) -> Unit,
) {
    val description = stringResource(contentDescription)
    ToolbarTooltip(description) {
        FilledIconToggleButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = description,
            )
        }
    }
}

@Composable
private fun TextColorButton(
    color: Color,
    currentColor: Color,
    @StringRes contentDescription: Int,
    onClick: (color: Color) -> Unit,
) {
    val checked = color == currentColor
    val description = stringResource(contentDescription)

    ToolbarTooltip(description) {
        OutlinedIconButton(
            onClick = { onClick(color) },
            modifier = Modifier.semantics { this.contentDescription = description },
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = color.copy(
                    alpha = when (checked) {
                        true -> 0.5f
                        false -> 1f
                    }
                ),
            )
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                )
            }
        }
    }
}
