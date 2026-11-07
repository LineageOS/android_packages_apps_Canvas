/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.models

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.unit.IntSize

data class CropRequest(
    val aspectRatio: Float?,
    val outputSize: IntSize?,
    val outputUri: Uri?,
    val outputFormat: ImageFormat,
    val returnData: Boolean,
) {
    companion object {
        const val ACTION_CROP = "com.android.camera.action.CROP"

        const val EXTRA_DATA = "data"

        private const val EXTRA_ASPECT_X = "aspectX"
        private const val EXTRA_ASPECT_Y = "aspectY"
        private const val EXTRA_OUTPUT_X = "outputX"
        private const val EXTRA_OUTPUT_Y = "outputY"
        private const val EXTRA_OUTPUT_FORMAT = "outputFormat"
        private const val EXTRA_RETURN_DATA = "return-data"

        fun fromIntent(intent: Intent): CropRequest {
            val aspectX = intent.getIntExtra(EXTRA_ASPECT_X, 0)
            val aspectY = intent.getIntExtra(EXTRA_ASPECT_Y, 0)
            val outputSize = IntSize(
                intent.getIntExtra(EXTRA_OUTPUT_X, 0),
                intent.getIntExtra(EXTRA_OUTPUT_Y, 0),
            ).takeIf { it.width > 0 && it.height > 0 }

            return CropRequest(
                aspectRatio = when {
                    aspectX > 0 && aspectY > 0 -> aspectX.toFloat() / aspectY
                    else -> outputSize?.let { it.width.toFloat() / it.height }
                },
                outputSize = outputSize,
                outputUri = intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT, Uri::class.java),
                outputFormat = when (intent.getStringExtra(EXTRA_OUTPUT_FORMAT)?.lowercase()) {
                    "png", ImageFormat.PNG.mimeType -> ImageFormat.PNG
                    "webp", ImageFormat.WEBP.mimeType -> ImageFormat.WEBP
                    else -> ImageFormat.JPEG
                },
                returnData = intent.getBooleanExtra(EXTRA_RETURN_DATA, false),
            )
        }
    }
}
