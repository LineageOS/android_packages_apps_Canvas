/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.KeyboardShortcutGroup
import android.view.KeyboardShortcutInfo
import android.view.Menu
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.util.Consumer
import org.lineageos.canvas.models.EditStatus
import org.lineageos.canvas.models.ImageFormat
import org.lineageos.canvas.ui.CanvasApp
import org.lineageos.canvas.ui.navigation.Screen
import org.lineageos.canvas.ui.theme.CanvasTheme
import org.lineageos.canvas.viewmodels.EditViewModel
import org.lineageos.canvas.viewmodels.ResultOpsViewModel

class MainActivity : ComponentActivity() {
    // View models
    private val editViewModel: EditViewModel by viewModels()
    private val resultOpsViewModel: ResultOpsViewModel by viewModels()

    /**
     * Whether the home screen is shown. Keyboard shortcuts only apply there, so they never act on
     * the history while an edit is in progress.
     */
    private var isHomeScreen = true

    /**
     * The action of the save keyboard shortcut.
     */
    private var onSaveShortcut: () -> Unit = {}

    private val onNewIntentListener = Consumer<Intent> { intent ->
        val uri = intent.takeIf { it.action == Intent.ACTION_EDIT }?.data ?: run {
            finish()
            return@Consumer
        }

        if (ImageFormat.fromMimeTypeOrNull(contentResolver.getType(uri)) == null) {
            finish()
            return@Consumer
        }

        val isWritable = (intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0

        editViewModel.setUri(uri, isWritable)
        resultOpsViewModel.setUri(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onNewIntentListener.accept(intent)
        addOnNewIntentListener(onNewIntentListener)

        enableEdgeToEdge()
        setContent {
            val saveStatus by resultOpsViewModel.saveStatus.collectAsState()
            val image by resultOpsViewModel.image.collectAsState()
            val finalBitmap by editViewModel.finalResultBitmap.collectAsState()
            val isWritable by editViewModel.isWritable.collectAsState()

            LaunchedEffect(saveStatus) {
                when (val status = saveStatus) {
                    is EditStatus.Saved -> {
                        setResult(RESULT_OK)
                        finish()
                    }

                    is EditStatus.Shared -> {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = status.mimeType
                            putExtra(Intent.EXTRA_STREAM, status.uri)
                            clipData = ClipData.newUri(
                                contentResolver,
                                getString(R.string.app_name),
                                status.uri,
                            )
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(shareIntent, getString(R.string.share)))
                    }

                    else -> {}
                }
            }

            val onDocumentCreated: (Uri?, ImageFormat) -> Unit = { uri, format ->
                uri?.let { uri ->
                    editViewModel.finalResultBitmap.value?.let {
                        resultOpsViewModel.saveImageToUri(it, uri, format)
                    }
                }
            }
            val createJpegDocumentLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument(ImageFormat.JPEG.mimeType),
            ) { uri -> onDocumentCreated(uri, ImageFormat.JPEG) }
            val createPngDocumentLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument(ImageFormat.PNG.mimeType),
            ) { uri -> onDocumentCreated(uri, ImageFormat.PNG) }
            val createWebpDocumentLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument(ImageFormat.WEBP.mimeType),
            ) { uri -> onDocumentCreated(uri, ImageFormat.WEBP) }

            val onSave: () -> Unit = {
                finalBitmap?.let {
                    resultOpsViewModel.saveImage(it)
                }
            }
            val onSaveAs: () -> Unit = {
                image?.format?.let {
                    val fileName = resultOpsViewModel.suggestedFilename(it)
                    when (it) {
                        ImageFormat.JPEG -> createJpegDocumentLauncher.launch(fileName)
                        ImageFormat.PNG -> createPngDocumentLauncher.launch(fileName)
                        ImageFormat.WEBP -> createWebpDocumentLauncher.launch(fileName)
                    }
                }
            }

            SideEffect {
                onSaveShortcut = if (isWritable) onSave else onSaveAs
            }

            CanvasTheme {
                CanvasApp(
                    editViewModel = editViewModel,
                    onClose = {
                        setResult(RESULT_CANCELED, null)
                        finish()
                    },
                    onSave = onSave,
                    onSaveAs = onSaveAs,
                    onShare = {
                        finalBitmap?.let {
                            resultOpsViewModel.shareImage(it)
                        }
                    },
                    onCurrentScreenChange = { isHomeScreen = it == Screen.Home },
                )
            }
        }
    }

    override fun onKeyShortcut(keyCode: Int, event: KeyEvent): Boolean {
        if (!isHomeScreen) {
            return super.onKeyShortcut(keyCode, event)
        }

        when {
            keyCode == KeyEvent.KEYCODE_Z && event.hasModifiers(KeyEvent.META_CTRL_ON) ->
                editViewModel.undo()

            keyCode == KeyEvent.KEYCODE_Z &&
                    event.hasModifiers(KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_ON) ->
                editViewModel.redo()

            keyCode == KeyEvent.KEYCODE_S && event.hasModifiers(KeyEvent.META_CTRL_ON) ->
                onSaveShortcut()

            else -> return super.onKeyShortcut(keyCode, event)
        }

        return true
    }

    override fun onProvideKeyboardShortcuts(
        data: MutableList<KeyboardShortcutGroup>?,
        menu: Menu?,
        deviceId: Int,
    ) {
        super.onProvideKeyboardShortcuts(data, menu, deviceId)

        if (!isHomeScreen) {
            return
        }

        data?.add(
            KeyboardShortcutGroup(
                getString(R.string.app_name),
                listOf(
                    KeyboardShortcutInfo(
                        getString(R.string.undo),
                        KeyEvent.KEYCODE_Z,
                        KeyEvent.META_CTRL_ON,
                    ),
                    KeyboardShortcutInfo(
                        getString(R.string.redo),
                        KeyEvent.KEYCODE_Z,
                        KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_ON,
                    ),
                    KeyboardShortcutInfo(
                        getString(
                            when (editViewModel.isWritable.value) {
                                true -> R.string.save
                                false -> R.string.save_as
                            }
                        ),
                        KeyEvent.KEYCODE_S,
                        KeyEvent.META_CTRL_ON,
                    ),
                ),
            )
        )
    }

    override fun onDestroy() {
        removeOnNewIntentListener(onNewIntentListener)

        super.onDestroy()
    }
}
