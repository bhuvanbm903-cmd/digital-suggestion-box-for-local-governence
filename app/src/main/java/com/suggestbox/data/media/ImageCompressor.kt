package com.suggestbox.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Client-side media compression utility downscaling citizen photos before storage/upload,
 * preserving bandwidth on 2G/3G networks and stripping extraneous EXIF metadata.
 */
@Singleton
class ImageCompressor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Compresses an image [Uri] into the app cache directory (max 1280px edge, ~80% JPEG quality).
     * Non-image URIs (such as videos) are returned unchanged.
     */
    suspend fun compressIfImage(uri: Uri): String = withContext(Dispatchers.IO) {
        val mime = context.contentResolver.getType(uri).orEmpty()
        if (mime.startsWith("video/")) {
            return@withContext uri.toString()
        }

        runCatching {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }

            val sampleSize = calculateInSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, 1280, 1280)
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return@withContext uri.toString()

            val mediaDir = File(context.cacheDir, "media").apply { mkdirs() }
            val outFile = File(mediaDir, "sbx_${UUID.randomUUID().toString().take(8)}.jpg")
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 82, fos)
            }
            bitmap.recycle()
            Uri.fromFile(outFile).toString()
        }.getOrElse {
            uri.toString()
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
