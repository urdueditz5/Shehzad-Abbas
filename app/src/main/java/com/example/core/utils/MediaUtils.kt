package com.example.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

object MediaUtils {

    /**
     * Compresses a user-selected Photo Picker image URI into a lightweight Base64 JPEG Data URI
     * (< 180 KB) so it fits safely inside Firestore rules (<= 250,000 chars) without requiring
     * broad storage permissions or Firebase Storage Blaze configuration.
     */
    suspend fun compressUriToDataUri(context: Context, uri: Uri, maxDim: Int = 720): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val input = context.contentResolver.openInputStream(uri)
                    ?: error("Unable to open selected image.")
                val original = input.use { BitmapFactory.decodeStream(it) }
                    ?: error("Selected file is not a valid image.")

                val ratio = max(original.width, original.height).toFloat() / maxDim.toFloat()
                val scaled = if (ratio > 1f) {
                    Bitmap.createScaledBitmap(
                        original,
                        (original.width / ratio).toInt().coerceAtLeast(1),
                        (original.height / ratio).toInt().coerceAtLeast(1),
                        true
                    )
                } else {
                    original
                }

                var quality = 78
                var bytes: ByteArray
                do {
                    val out = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                    bytes = out.toByteArray()
                    quality -= 12
                } while (bytes.size > 150_000 && quality >= 30)

                if (scaled !== original) scaled.recycle()
                original.recycle()

                val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                val dataUri = "data:image/jpeg;base64,$b64"
                require(dataUri.length <= 240_000) {
                    "Image is still too large after compression. Please pick a smaller image."
                }
                dataUri
            }
        }

    fun getUriFileSizeBytes(context: Context, uri: Uri): Long? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (idx >= 0 && !cursor.isNull(idx)) cursor.getLong(idx) else null
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun isValidHttpsUrl(url: String): Boolean {
        val trimmed = url.trim()
        return trimmed.startsWith("https://") && trimmed.length in 10..1000
    }
}
